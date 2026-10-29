package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.LoyaltySettings;
import model.LoyaltyTransaction;

/**
 * Điểm tích lũy: quy tắc quy đổi, tìm khách, gắn khách vào bill của bàn, số dư
 * và lịch sử.
 */
public class LoyaltyDAO extends DBContext {

    /**
     * Tài khoản customer tìm được.
     */
    public static class Customer {

        public final int userId;
        public final String fullName;
        public final String email;
        public final String phone;
        public final int points;

        Customer(int userId, String fullName, String email, String phone, int points) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.points = points;
        }

        public int getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public int getPoints() {
            return points;
        }
    }

    public LoyaltySettings getSettings() {
        LoyaltySettings s = new LoyaltySettings();
        if (connection == null) {
            return s;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT earn_spend_amount, earn_points, redeem_value_per_point FROM LoyaltySettings WHERE settings_id = 1"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                s.setEarnSpendAmount(rs.getBigDecimal(1));
                s.setEarnPoints(rs.getInt(2));
                s.setRedeemValuePerPoint(rs.getBigDecimal(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s;
    }

    public boolean updateSettings(BigDecimal earnSpendAmount, int earnPoints, BigDecimal redeemValuePerPoint, int managerId) {
        if (connection == null) {
            return false;
        }
        String sql = "UPDATE LoyaltySettings SET earn_spend_amount = ?, earn_points = ?, redeem_value_per_point = ?, "
                + "updated_by = ?, updated_at = SYSDATETIME() WHERE settings_id = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, earnSpendAmount);
            ps.setInt(2, earnPoints);
            ps.setBigDecimal(3, redeemValuePerPoint);
            ps.setInt(4, managerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ngày giờ + tên người cập nhật quy tắc lần cuối, dạng "dd/MM/yyyy HH:mm ·
     * tên"; rỗng nếu chưa có.
     */
    public String getLastUpdateText() {
        if (connection == null) {
            return "";
        }
        String sql = "SELECT s.updated_at, u.full_name FROM LoyaltySettings s "
                + "LEFT JOIN Users u ON u.user_id = s.updated_by WHERE s.settings_id = 1 AND s.updated_by IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + " · " + rs.getNString(2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Tìm tài khoản customer đang hoạt động theo tên đăng nhập, email hoặc số
     * điện thoại.
     */
    public Customer findCustomer(String keyword) {
        if (connection == null || keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT TOP 1 u.user_id, u.full_name, u.email, u.phone, u.loyalty_points "
                + "FROM Users u JOIN Roles r ON r.role_id = u.role_id "
                + "WHERE r.role_name = 'Customer' AND u.is_active = 1 AND u.is_deleted = 0 "
                + "AND (u.email = ? OR u.phone = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String k = keyword.trim();
            ps.setNString(1, k);
            ps.setNString(2, k);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(rs.getInt(1), rs.getNString(2), rs.getNString(3), rs.getNString(4), rs.getInt(5));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gắn (hoặc đổi) khách + số điểm muốn dùng cho bill của bàn.
     */
    public boolean setBillLink(int tableId, int customerId, int pointsToUse, int actorId) {
        if (connection == null) {
            return false;
        }
        String sql = "MERGE TableBillLoyalty AS t USING (SELECT ? AS table_id) AS s ON t.table_id = s.table_id "
                + "WHEN MATCHED THEN UPDATE SET customer_id = ?, points_to_use = ?, set_by = ?, updated_at = SYSDATETIME() "
                + "WHEN NOT MATCHED THEN INSERT (table_id, customer_id, points_to_use, set_by) VALUES (?, ?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setInt(2, customerId);
            ps.setInt(3, pointsToUse);
            ps.setInt(4, actorId);
            ps.setInt(5, tableId);
            ps.setInt(6, customerId);
            ps.setInt(7, pointsToUse);
            ps.setInt(8, actorId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearBillLink(int tableId) {
        if (connection == null) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM TableBillLoyalty WHERE table_id = ?")) {
            ps.setInt(1, tableId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getBalance(int userId) {
        if (connection == null) {
            return 0;
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT loyalty_points FROM Users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Lịch sử điểm của một khách, mới nhất trước.
     */
    public List<LoyaltyTransaction> getHistory(int userId) {
        List<LoyaltyTransaction> list = new ArrayList<>();
        if (connection == null) {
            return list;
        }
        String sql = "SELECT TOP 200 txn_id, txn_type, points, balance_after, bill_amount, money_value, note, created_at "
                + "FROM LoyaltyTransactions WHERE user_id = ? ORDER BY created_at DESC, txn_id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoyaltyTransaction t = new LoyaltyTransaction();
                    t.setTxnId(rs.getInt("txn_id"));
                    t.setTxnType(rs.getString("txn_type"));
                    t.setPoints(rs.getInt("points"));
                    t.setBalanceAfter(rs.getInt("balance_after"));
                    t.setBillAmount(rs.getBigDecimal("bill_amount"));
                    t.setMoneyValue(rs.getBigDecimal("money_value"));
                    t.setNote(rs.getNString("note"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        t.setCreatedAt(ts.toLocalDateTime());
                    }
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
