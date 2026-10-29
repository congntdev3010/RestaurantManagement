package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import model.Voucher;

/**
 * Voucher giảm giá theo % tổng hóa đơn: tìm khách đủ điều kiện, phát mã (mỗi
 * khách một mã), áp mã cho bill của bàn. Việc đánh dấu USED khi thanh toán nằm
 * trong OrderDAO.markTablePaid để cùng một giao dịch với hóa đơn.
 */
public class VoucherDAO extends DBContext {

    public static final int CODE_LENGTH = 8;
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9]{8}");
    private static final int ERR_UNIQUE_INDEX = 2601;
    private static final int ERR_UNIQUE_KEY = 2627;
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Tổng khách đã chi = tiền khách thực trả trên mỗi hóa đơn. */
    private static final String PAID_EXPR = "h.total_amount - h.loyalty_discount - h.voucher_discount";

    /** Khách đủ điều kiện, kèm tổng đã chi. */
    public static class Recipient {

        private final int userId;
        private final String fullName;
        private final String email;
        private final BigDecimal totalSpent;

        Recipient(int userId, String fullName, String email, BigDecimal totalSpent) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.totalSpent = totalSpent;
        }

        public int getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public BigDecimal getTotalSpent() { return totalSpent; }
    }

    /**
     * Customer đang hoạt động có tổng đã chi LỚN HƠN requiredSpent. Hóa đơn tính
     * cho khách qua loyalty_customer_id, hoặc qua Gmail nếu bill không gắn tài
     * khoản.
     */
    public List<Recipient> findEligibleCustomers(BigDecimal requiredSpent) throws SQLException {
        requireConnection();
        String sql = "SELECT u.user_id, u.full_name, u.email, COALESCE(SUM(" + PAID_EXPR + "), 0) AS total_spent "
                + "FROM Users u JOIN Roles r ON r.role_id = u.role_id "
                + "LEFT JOIN ServiceHistory h ON h.loyalty_customer_id = u.user_id "
                + "OR (h.loyalty_customer_id IS NULL AND h.customer_email = u.email) "
                + "WHERE r.role_name = 'Customer' AND u.is_active = 1 AND u.is_deleted = 0 "
                + "GROUP BY u.user_id, u.full_name, u.email "
                + "HAVING COALESCE(SUM(" + PAID_EXPR + "), 0) > ? "
                + "ORDER BY total_spent DESC, u.full_name";
        List<Recipient> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, requiredSpent);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Recipient(rs.getInt(1), rs.getNString(2), rs.getNString(3), rs.getBigDecimal(4)));
                }
            }
        }
        return list;
    }

    /**
     * Tạo một chiến dịch rồi phát mỗi khách trong userIds một mã, trong MỘT giao
     * dịch. Điều kiện khách được kiểm lại ngay trong giao dịch; có khách không đủ
     * điều kiện thì không tạo gì cả (IllegalArgumentException).
     */
    public List<Voucher> issueVouchers(BigDecimal discountPercent, BigDecimal requiredSpent,
            List<Integer> userIds, int managerId) throws SQLException {
        requireConnection();
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn khách nhận voucher.");
        }
        try {
            connection.setAutoCommit(false);
            Map<Integer, Recipient> eligible = new HashMap<>();
            for (Recipient r : findEligibleCustomers(requiredSpent)) {
                eligible.put(r.getUserId(), r);
            }
            List<Recipient> recipients = new ArrayList<>();
            for (Integer id : userIds) {
                Recipient r = id == null ? null : eligible.get(id);
                if (r == null) {
                    throw new IllegalArgumentException("Có khách không còn đủ điều kiện (tổng đã chi phải lớn hơn "
                            + requiredSpent.toPlainString() + " đ). Vui lòng tìm lại danh sách.");
                }
                recipients.add(r);
            }
            int campaignId = insertCampaign(discountPercent, requiredSpent, managerId);
            List<Voucher> issued = new ArrayList<>();
            Set<String> batchCodes = new HashSet<>();
            for (Recipient r : recipients) {
                String code = newCode(batchCodes);
                int voucherId = insertVoucher(campaignId, code, r, discountPercent, requiredSpent);
                Voucher v = new Voucher();
                v.setVoucherId(voucherId);
                v.setCode(code);
                v.setDiscountPercent(discountPercent);
                v.setCustomerName(r.getFullName());
                v.setCustomerEmail(r.getEmail());
                issued.add(v);
            }
            connection.commit();
            return issued;
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly();
            throw e;
        } finally {
            resetAutoCommit();
        }
    }

    /**
     * Áp mã cho bàn đang thanh toán. Trả về null nếu thành công, ngược lại là
     * lý do để hiển thị cho lễ tân.
     */
    public String applyToTable(int tableId, String rawCode, int actorId) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            return "Mã voucher phải gồm đúng 8 ký tự chữ hoặc số.";
        }
        if (connection == null) {
            return "Không kết nối được CSDL.";
        }
        try {
            int voucherId = 0;
            String status = null;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT voucher_id, status FROM Vouchers WHERE code = ?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return "Mã voucher không tồn tại.";
                    }
                    voucherId = rs.getInt(1);
                    status = rs.getString(2);
                }
            }
            if (!"ACTIVE".equals(status)) {
                return "Mã voucher đã được sử dụng.";
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT table_id FROM TableBillVoucher WHERE voucher_id = ? AND table_id <> ?")) {
                ps.setInt(1, voucherId);
                ps.setInt(2, tableId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return "Mã voucher này đang được áp cho bàn khác.";
                    }
                }
            }
            String merge = "MERGE TableBillVoucher AS t USING (SELECT ? AS table_id) AS s ON t.table_id = s.table_id "
                    + "WHEN MATCHED THEN UPDATE SET voucher_id = ?, set_by = ?, updated_at = SYSDATETIME() "
                    + "WHEN NOT MATCHED THEN INSERT (table_id, voucher_id, set_by) VALUES (?, ?, ?);";
            try (PreparedStatement ps = connection.prepareStatement(merge)) {
                ps.setInt(1, tableId);
                ps.setInt(2, voucherId);
                ps.setInt(3, actorId);
                ps.setInt(4, tableId);
                ps.setInt(5, voucherId);
                ps.setInt(6, actorId);
                ps.executeUpdate();
            }
            return null;
        } catch (SQLException e) {
            if (e.getErrorCode() == ERR_UNIQUE_KEY || e.getErrorCode() == ERR_UNIQUE_INDEX) {
                return "Mã voucher này vừa được áp cho bàn khác.";
            }
            e.printStackTrace();
            return "Không thể áp mã voucher.";
        }
    }

    /** Bỏ mã khỏi bàn (lễ tân bấm bỏ mã, hoặc bàn đã thanh toán). */
    public void clearTableVoucher(int tableId) {
        if (connection == null) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM TableBillVoucher WHERE table_id = ?")) {
            ps.setInt(1, tableId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Chuẩn hóa mã nhập vào (bỏ khoảng trắng, viết hoa); null nếu sai định dạng. */
    public static String normalizeCode(String raw) {
        if (raw == null) {
            return null;
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        return CODE_FORMAT.matcher(code).matches() ? code : null;
    }

    private int insertCampaign(BigDecimal discountPercent, BigDecimal requiredSpent, int managerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO VoucherCampaigns (discount_percent, required_total_spent, created_by) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, discountPercent);
            ps.setBigDecimal(2, requiredSpent);
            ps.setInt(3, managerId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không tạo được chiến dịch voucher.");
                }
                return keys.getInt(1);
            }
        }
    }

    private int insertVoucher(int campaignId, String code, Recipient r, BigDecimal discountPercent,
            BigDecimal requiredSpent) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO Vouchers (campaign_id, code, customer_id, sent_to_email, discount_percent, "
                + "required_total_spent, customer_total_spent) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, campaignId);
            ps.setString(2, code);
            ps.setInt(3, r.getUserId());
            ps.setNString(4, r.getEmail());
            ps.setBigDecimal(5, discountPercent);
            ps.setBigDecimal(6, requiredSpent);
            ps.setBigDecimal(7, r.getTotalSpent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không tạo được voucher.");
                }
                return keys.getInt(1);
            }
        }
    }

    /** Mã ngẫu nhiên 8 ký tự, không trùng trong lô này và không trùng trong CSDL. */
    private String newCode(Set<String> batchCodes) throws SQLException {
        for (int attempt = 0; attempt < 50; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (batchCodes.contains(code) || codeExists(code)) {
                continue;
            }
            batchCodes.add(code);
            return code;
        }
        throw new SQLException("Không sinh được mã voucher duy nhất, vui lòng thử lại.");
    }

    private boolean codeExists(String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM Vouchers WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void requireConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Không kết nối được CSDL.");
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Giao dịch đã hỏng; lỗi gốc đã được ném ra ngoài.
        }
    }

    private void resetAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Không đặt lại được chế độ tự commit; kết nối sẽ được đóng ngay sau đó.
        }
    }
}
