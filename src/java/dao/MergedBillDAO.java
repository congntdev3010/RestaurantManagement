package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import model.TableBill;

/** Bill gộp theo các bàn đang phục vụ, order mới tự được tính khi đã duyệt. */
public class MergedBillDAO extends DBContext {
    private String error;
    public String getError() { return error; }
    public static String reference(int id, int version) { return "RMG" + id + "V" + version; }

    public Integer activeBillForTable(int tableId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT mbt.merged_bill_id FROM MergedBillTables mbt JOIN MergedBills b ON b.merged_bill_id = mbt.merged_bill_id "
                + "WHERE mbt.table_id = ? AND mbt.active = 1 AND b.bill_status = 'OPEN'")) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }

    }

    public List<TableBill> activeBills() throws SQLException {
        List<TableBill> bills = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT merged_bill_id FROM MergedBills WHERE bill_status = 'OPEN' ORDER BY merged_bill_id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    TableBill bill = bill(id);
                    if (bill != null) {
                        bill.setMergedBillId(id);
                        bills.add(bill);
                    }
                }
            }
        }
        return bills;
    }

    public int create(List<Integer> selected, int actorId) throws SQLException {
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(selected));
        if (ids.size() < 2 || ids.size() > 10) throw new SQLException("Chọn từ 2 đến 10 bàn có bill.");
        connection.setAutoCommit(false);
        try {
            // Khóa bàn theo thứ tự cố định để tránh deadlock với yêu cầu gộp đồng thời.
            List<Integer> sorted = new ArrayList<>(ids);
            java.util.Collections.sort(sorted);
            for (int tid : sorted) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT table_status FROM RestaurantTables WITH (UPDLOCK, HOLDLOCK) WHERE table_id = ?")) {
                    ps.setInt(1, tid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"OCCUPIED".equals(rs.getString(1))) throw new SQLException("Bàn không đang phục vụ: " + tid);
                    }
                }
                if (activeBillForTable(tid) != null) throw new SQLException("Bàn đã nằm trong bill gộp: " + tid);
                try (PreparedStatement ps = connection.prepareStatement(
                       "SELECT COALESCE(SUM(total_amount), 0) FROM Orders WHERE table_id = ? AND order_status = 'OPEN'")) {
                    ps.setInt(1, tid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getBigDecimal(1).signum() <= 0) {
                            throw new SQLException("Bàn chưa có bill có thể thanh toán: " + tid);
                        }
                    }
                }
                // Không bỏ sót voucher/điểm đã gắn vào bill đơn; yêu cầu tháo trước khi gộp.
                if (hasLink("TableBillVoucher", tid) || hasLink("TableBillLoyalty", tid))
                    throw new SQLException("Bỏ voucher/điểm đã gắn ở bàn " + tid + " trước khi gộp.");
            }
            int id;
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO MergedBills(primary_table_id, created_by) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ids.get(0)); ps.setInt(2, actorId); ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { if (!rs.next()) throw new SQLException("Không tạo được bill."); id = rs.getInt(1); }
            }
            for (int tid : sorted) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO MergedBillTables(merged_bill_id, table_id) VALUES (?, ?)")) {
                    ps.setInt(1, id); ps.setInt(2, tid); ps.executeUpdate();
                }
            }
            connection.commit(); return id;
        } catch (SQLException e) { connection.rollback(); throw e; }
        finally { connection.setAutoCommit(true); }
    }

    private boolean hasLink(String table, int tid) throws SQLException {
        // Tên bảng nội bộ cố định, không lấy từ request.
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM " + table + " WHERE table_id = ?")) {
            ps.setInt(1, tid);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public List<Integer> tables(int id) throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT t.table_id FROM MergedBillTables t JOIN MergedBills b ON b.merged_bill_id = t.merged_bill_id "
                + "WHERE t.merged_bill_id = ? AND t.active = 1 AND b.bill_status = 'OPEN' ORDER BY t.table_id")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(rs.getInt(1)); }
        }
        return list;
    }

    public TableBill bill(int id) throws SQLException {
        List<Integer> ids = tables(id);
        if (ids.size() < 2) return null;
        TableBill combined = new TableBill();
        combined.setTableId(ids.get(0));
        StringBuilder names = new StringBuilder();
        try (BillDAO bills = new BillDAO()) {
            for (int tid : ids) {
                TableBill part = bills.getTableBill(tid);
                if (part == null) return null;
                if (names.length() > 0) names.append(", ");
                names.append(part.getTableName());
                combined.setOrderCount(combined.getOrderCount() + part.getOrderCount());
                combined.setUnfinishedItemCount(combined.getUnfinishedItemCount() + part.getUnfinishedItemCount());
                combined.setPendingOrder(combined.isPendingOrder() || part.isPendingOrder());
                combined.setTotalAmount(combined.getTotalAmount().add(part.getTotalAmount()));
                combined.getItems().addAll(part.getItems());
                if (combined.getCustomerName() == null) {
                    combined.setCustomerName(part.getCustomerName());
                    combined.setCustomerPhone(part.getCustomerPhone());
                    combined.setCustomerEmail(part.getCustomerEmail());
                    combined.setReservationTime(part.getReservationTime());
                    combined.setCheckedInAt(part.getCheckedInAt());
                }
            }
        }
        applyMergedLinks(combined, ids.get(0));
        combined.setTableName(names.toString());
        return combined;
    }

    private void applyMergedLinks(TableBill bill, int primaryTableId) throws SQLException {
        bill.setVoucherId(0);
        bill.setVoucherCode(null);
        bill.setVoucherPercent(BigDecimal.ZERO);
        bill.setVoucherDiscount(BigDecimal.ZERO);
        bill.setPayableAmount(bill.getTotalAmount());
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT v.voucher_id, v.code, v.discount_percent FROM TableBillVoucher b "
                + "JOIN Vouchers v ON v.voucher_id = b.voucher_id "
                + "WHERE b.table_id = ? AND v.status = 'ACTIVE'")) {
            ps.setInt(1, primaryTableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal percent = rs.getBigDecimal("discount_percent");
                    bill.setVoucherId(rs.getInt("voucher_id"));
                    bill.setVoucherCode(rs.getString("code").trim());
                    bill.setVoucherPercent(percent);
                    bill.setVoucherDiscount(bill.getTotalAmount().multiply(percent)
                            .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP));
                }
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT l.customer_id, u.full_name, u.phone, u.email, u.loyalty_points, l.points_to_use, "
                + "s.earn_spend_amount, s.earn_points, s.redeem_value_per_point "
                + "FROM TableBillLoyalty l JOIN Users u ON u.user_id = l.customer_id "
                + "CROSS JOIN LoyaltySettings s WHERE l.table_id = ? AND s.settings_id = 1")) {
            ps.setInt(1, primaryTableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal redeem = rs.getBigDecimal("redeem_value_per_point");
                    int max = redeem.signum() > 0
                            ? bill.getTotalAmount().subtract(bill.getVoucherDiscount()).divideToIntegralValue(redeem).intValue() : 0;
                    int use = Math.max(0, Math.min(rs.getInt("points_to_use"),
                            Math.min(rs.getInt("loyalty_points"), max)));
                    BigDecimal discount = redeem.multiply(BigDecimal.valueOf(use));
                    bill.setLoyaltyCustomerId(rs.getInt("customer_id"));
                    bill.setLoyaltyCustomerName(rs.getNString("full_name"));
                    bill.setLoyaltyCustomerPhone(rs.getNString("phone"));
                    bill.setLoyaltyCustomerEmail(rs.getNString("email"));
                    bill.setCustomerName(bill.getLoyaltyCustomerName());
                    bill.setCustomerPhone(bill.getLoyaltyCustomerPhone());
                    bill.setCustomerEmail(bill.getLoyaltyCustomerEmail());
                    bill.setLoyaltyBalance(rs.getInt("loyalty_points"));
                    bill.setPointsToUse(use);
                    bill.setDiscountAmount(discount);
                    bill.setPayableAmount(bill.getTotalAmount().subtract(bill.getVoucherDiscount()).subtract(discount));
                    BigDecimal spend = rs.getBigDecimal("earn_spend_amount");
                    bill.setPointsToEarn(spend.signum() > 0
                            ? bill.getPayableAmount().divideToIntegralValue(spend).intValue() * rs.getInt("earn_points") : 0);
                }
            }
        }
    }

    public String applyVoucher(int id, String rawCode, int actorId) throws SQLException {
        String code = VoucherDAO.normalizeCode(rawCode);
        if (code == null) return "Mã voucher phải gồm đúng 8 ký tự chữ hoặc số.";
        int primary = tables(id).get(0);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT voucher_id, status FROM Vouchers WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "Mã voucher không tồn tại.";
                if (!"ACTIVE".equals(rs.getString("status"))) return "Mã voucher đã được sử dụng.";
                int voucherId = rs.getInt("voucher_id");
                try (PreparedStatement merge = connection.prepareStatement(
                        "MERGE TableBillVoucher AS t USING (SELECT ? AS table_id) AS s ON t.table_id=s.table_id "
                        + "WHEN MATCHED THEN UPDATE SET voucher_id=?, set_by=?, updated_at=SYSDATETIME() "
                        + "WHEN NOT MATCHED THEN INSERT(table_id,voucher_id,set_by) VALUES(?,?,?);")) {
                    merge.setInt(1, primary); merge.setInt(2, voucherId); merge.setInt(3, actorId);
                    merge.setInt(4, primary); merge.setInt(5, voucherId); merge.setInt(6, actorId);
                    merge.executeUpdate();
                }
            }
        }
        return null;
    }

    public boolean setLoyalty(int id, int customerId, int points, int actorId) throws SQLException {
        int primary = tables(id).get(0);
        try (PreparedStatement ps = connection.prepareStatement(
                "MERGE TableBillLoyalty AS t USING (SELECT ? AS table_id) AS s ON t.table_id=s.table_id "
                + "WHEN MATCHED THEN UPDATE SET customer_id=?, points_to_use=?, set_by=?, updated_at=SYSDATETIME() "
                + "WHEN NOT MATCHED THEN INSERT(table_id,customer_id,points_to_use,set_by) VALUES(?,?,?,?);")) {
            ps.setInt(1, primary); ps.setInt(2, customerId); ps.setInt(3, points); ps.setInt(4, actorId);
            ps.setInt(5, primary); ps.setInt(6, customerId); ps.setInt(7, points); ps.setInt(8, actorId);
            return ps.executeUpdate() > 0;
        }
    }

    public void clearVoucher(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM TableBillVoucher WHERE table_id = (SELECT TOP 1 table_id FROM MergedBillTables WHERE merged_bill_id=? AND active=1)")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public void clearLoyalty(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM TableBillLoyalty WHERE table_id = (SELECT TOP 1 table_id FROM MergedBillTables WHERE merged_bill_id=? AND active=1)")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public void discardPaymentLinks(int id) throws SQLException {
        clearVoucher(id);
        clearLoyalty(id);
    }

    /** Mã QR phát hành chỉ khi tất cả món đã hoàn tất. Mỗi lần thay đổi tiền phải dùng version mới. */
    public String issueQr(int id) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT qr_version, qr_amount FROM MergedBills WITH (UPDLOCK, HOLDLOCK) WHERE merged_bill_id = ? AND bill_status = 'OPEN'")) {
            lock.setInt(1, id);
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next()) { connection.rollback(); return null; }
                TableBill current = bill(id);
                if (current == null || !current.isReadyForPayment()) { connection.rollback(); return null; }
                int v = rs.getInt(1);
                BigDecimal amount = rs.getBigDecimal(2);
                if (amount == null || amount.compareTo(current.getPayableAmount()) != 0) {
                    if (amount != null) v++;
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE MergedBills SET qr_version = ?, qr_amount = ? WHERE merged_bill_id = ?")) {
                        ps.setInt(1, v); ps.setBigDecimal(2, current.getPayableAmount()); ps.setInt(3, id); ps.executeUpdate();
                    }
                }
                connection.commit(); return reference(id, v);
            }
        } catch (SQLException e) { connection.rollback(); throw e; }
        finally { connection.setAutoCommit(true); }
    }

    public boolean needsReview(int id) throws SQLException { return hasPendingQrTransfer(id); }

    public Integer paidHistory(int id, String ref) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT h.history_id FROM ServiceHistory h JOIN MergedBills b ON b.merged_bill_id = h.merged_bill_id "
                + "WHERE b.merged_bill_id = ? AND b.bill_status = 'PAID' AND h.payment_method = 'QR' "
                + "AND ? = CONCAT('RMG', b.merged_bill_id, 'V', b.qr_version)")) {
            ps.setInt(1, id); ps.setString(2, ref);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    /** Lưu giao dịch webhook trước khi chốt: mã cũ/sai tiền vẫn giữ để đối soát. */
    public boolean receiveQr(int id, int version, String providerId, BigDecimal amount, int actorId) throws SQLException {
        if (providerId == null || providerId.trim().isEmpty() || providerId.length() > 100
                || amount == null || amount.signum() <= 0) return false;
        String ref = reference(id, version);
        try (PreparedStatement duplicate = connection.prepareStatement(
                "SELECT transfer_status FROM MergedBillTransfers WHERE provider_transaction_id=?")) {
            duplicate.setString(1, providerId);
            try (ResultSet rs = duplicate.executeQuery()) {
                if (rs.next()) return "APPLIED".equals(rs.getString(1));
            }
        }
        BigDecimal current = null;
        int currentVersion = -1;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT qr_version, qr_amount FROM MergedBills WHERE merged_bill_id=? AND bill_status='OPEN'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { currentVersion = rs.getInt(1); current = rs.getBigDecimal(2); }
            }
        }
        boolean valid = currentVersion == version && current != null && current.compareTo(amount) == 0;
        TableBill live = valid ? bill(id) : null;
        valid = valid && live != null && live.isReadyForPayment() && live.getPayableAmount().compareTo(amount) == 0;
        // REVIEW trước, để khoản tiền đã tới không bị bỏ quên khi dữ liệu thay đổi.
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO MergedBillTransfers(merged_bill_id,provider_transaction_id,payment_reference,amount,transfer_status) "
                + "VALUES(?,?,?,?,'REVIEW')")) {
            ps.setInt(1,id); ps.setString(2,providerId); ps.setString(3,ref); ps.setBigDecimal(4,amount); ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) return false;
            throw e;
        }
        if (!valid || !pay(id,actorId,"QR",ref,null,amount)) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE MergedBillTransfers SET transfer_status='APPLIED' WHERE provider_transaction_id=? AND transfer_status='REVIEW'")) {
            ps.setString(1,providerId); ps.executeUpdate();
        }
        return true;
    }

    public boolean pay(int id, int actorId, String method, String ref, BigDecimal received, BigDecimal expected) throws SQLException {
        error = null;
        if (!"CASH".equals(method) && !"QR".equals(method)) return false;
        connection.setAutoCommit(false);
        try {
            int primary, version;
            BigDecimal qrAmount;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT primary_table_id, qr_version, qr_amount FROM MergedBills WITH (UPDLOCK, HOLDLOCK) "
                    + "WHERE merged_bill_id = ? AND bill_status = 'OPEN'")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Bill đã thanh toán hoặc đã hủy.");
                    primary = rs.getInt(1); version = rs.getInt(2); qrAmount = rs.getBigDecimal(3);
                }
            }
            List<Integer> ids = tables(id);
            if (ids.size() < 2) throw new SQLException("Bill không còn đủ bàn.");
            for (int tid : ids) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT table_status FROM RestaurantTables WITH (UPDLOCK, HOLDLOCK) WHERE table_id = ?")) {
                    ps.setInt(1, tid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || !"OCCUPIED".equals(rs.getString(1))) throw new SQLException("Trạng thái bàn đã thay đổi.");
                    }
                }
            }
            // Khóa tập order mở sau khi khóa bàn. createOrder cũng khóa bàn trước khi chèn.
            int count = 0;
            BigDecimal total = BigDecimal.ZERO;
            for (int tid : ids) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT order_id, total_amount FROM Orders WITH (UPDLOCK, HOLDLOCK) WHERE table_id = ? AND order_status = 'OPEN'")) {
                    ps.setInt(1, tid);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) { count++; total = total.add(rs.getBigDecimal(2)); }
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT 1 FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                        + "WHERE o.table_id = ? AND o.order_status = 'OPEN' AND oi.item_status IN ('PENDING','COOKING')")) {
                    ps.setInt(1, tid);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) throw new SQLException("Còn món chưa phục vụ ở bàn " + tid); }
                }
            }
            if (count == 0 || total.signum() <= 0) throw new SQLException("Không có bill cần thanh toán.");
            TableBill liveBill = bill(id);
            if (liveBill == null) throw new SQLException("Bill gộp không còn hợp lệ.");
            BigDecimal payable = liveBill.getPayableAmount();
            if (hasPendingQrTransfer(id) && !"QR".equals(method)) throw new SQLException("Bill có khoản chuyển cần đối soát, không thể thanh toán lần nữa.");
            if ("QR".equals(method)) {
                if (ref == null || !ref.equals(reference(id, version)) || qrAmount == null
                        || qrAmount.compareTo(payable) != 0 || expected == null || expected.compareTo(payable) != 0)
                    throw new SQLException("QR hoặc số tiền đã thay đổi; cần đối soát khoản chuyển trước khi thanh toán.");
            } else if (received == null || received.compareTo(payable) < 0 || expected == null || expected.compareTo(payable) != 0) {
                throw new SQLException("Tổng bill vừa đổi hoặc tiền khách đưa chưa đủ; tải lại hóa đơn.");
            }
            int historyId;
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ServiceHistory(table_id, merged_bill_id, customer_name, customer_phone, customer_email, "
                    + "reservation_time, checked_in_at, paid_at, payment_method, total_amount, cash_received, cash_change, paid_by, "
                    + "loyalty_customer_id, loyalty_points_used, loyalty_discount, loyalty_points_earned, voucher_discount_percent, voucher_discount) "
                    + "SELECT ?, ?, MAX(o.customer_name), MAX(o.customer_phone), MAX(o.customer_email), "
                    + "MIN(CASE WHEN r.reservation_date IS NOT NULL THEN DATEADD(SECOND, DATEDIFF(SECOND,'00:00:00',r.reservation_time),CAST(r.reservation_date AS DATETIME2)) END), "
                    + "MAX(r.checked_in_at), SYSDATETIME(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? "
                    + "FROM Orders o LEFT JOIN Reservations r ON r.reservation_id = o.reservation_id "
                    + "JOIN MergedBillTables mt ON mt.table_id = o.table_id AND mt.active = 1 "
                    + "WHERE mt.merged_bill_id = ? AND o.order_status = 'OPEN'", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, primary); ps.setInt(2, id); ps.setString(3, method); ps.setBigDecimal(4, total);
                if (received == null) { ps.setNull(5, java.sql.Types.DECIMAL); ps.setNull(6, java.sql.Types.DECIMAL); }
                else { ps.setBigDecimal(5, received); ps.setBigDecimal(6, received.subtract(payable)); }
                ps.setInt(7, actorId);
                if (liveBill.isHasLoyaltyCustomer()) ps.setInt(8, liveBill.getLoyaltyCustomerId());
                else ps.setNull(8, java.sql.Types.INTEGER);
                ps.setInt(9, liveBill.getPointsToUse());
                ps.setBigDecimal(10, liveBill.getDiscountAmount());
                ps.setInt(11, liveBill.getPointsToEarn());
                ps.setBigDecimal(12, liveBill.getVoucherPercent());
                ps.setBigDecimal(13, liveBill.getVoucherDiscount());
                ps.setInt(14, id);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { if (!rs.next()) throw new SQLException("Không lưu được hóa đơn."); historyId = rs.getInt(1); }
            }
            for (int tid : ids) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ServiceHistoryTables(history_id, table_id) VALUES (?, ?)")) {
                    ps.setInt(1, historyId); ps.setInt(2, tid); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ServiceHistoryItems(history_id,dish_name,unit,unit_price,quantity,line_total) "
                        + "SELECT ?, oi.dish_name,oi.unit,oi.unit_price,oi.quantity,oi.line_total FROM OrderItems oi "
                        + "JOIN Orders o ON o.order_id = oi.order_id WHERE o.table_id = ? AND o.order_status = 'OPEN' AND oi.item_status <> 'REJECTED'")) {
                    ps.setInt(1, historyId); ps.setInt(2, tid); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Orders SET order_status='PAID', closed_at=SYSDATETIME(), paid_at=SYSDATETIME(), "
                        + "paid_by=?, payment_method=?, payment_reference=?, cash_received=?, cash_change=? "
                        + "WHERE table_id=? AND order_status='OPEN'")) {
                    ps.setInt(1, actorId); ps.setString(2, method); ps.setString(3, ref);
                    if (received == null) { ps.setNull(4, java.sql.Types.DECIMAL); ps.setNull(5, java.sql.Types.DECIMAL); }
                    else { ps.setBigDecimal(4, received); ps.setBigDecimal(5, received.subtract(payable)); }
                    ps.setInt(6, tid); ps.executeUpdate();
                }
                // Giữ đúng hành vi bill đơn: đơn chờ duyệt bị hủy khi bàn đã thanh toán.
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE oi SET item_status='REJECTED',line_total=0 FROM OrderItems oi JOIN Orders o ON o.order_id=oi.order_id "
                        + "WHERE o.table_id=? AND o.order_status='PENDING' AND oi.item_status='PENDING'")) {
                    ps.setInt(1, tid); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Orders SET order_status='CANCELLED', cancelled_at=SYSDATETIME(), closed_at=SYSDATETIME(), "
                        + "settled_at=SYSDATETIME(), cancellation_reason=N'Bàn đã thanh toán khi đơn chưa được duyệt.' "
                        + "WHERE table_id=? AND order_status='PENDING'")) { ps.setInt(1, tid); ps.executeUpdate(); }
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Orders SET settled_at=SYSDATETIME() WHERE table_id=? AND order_status='CANCELLED' AND settled_at IS NULL")) {
                    ps.setInt(1, tid); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE RestaurantTables SET table_status='AVAILABLE' WHERE table_id=? AND table_status='OCCUPIED' "
                        + "AND NOT EXISTS(SELECT 1 FROM Orders o WHERE o.table_id=? AND o.order_status IN ('OPEN','PENDING'))")) {
                    ps.setInt(1, tid); ps.setInt(2, tid);
                    if (ps.executeUpdate() != 1) throw new SQLException("Không thể giải phóng bàn " + tid);
                }
            }
            if (liveBill.isHasLoyaltyCustomer()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE ServiceHistory SET customer_name=?, customer_phone=?, customer_email=? WHERE history_id=?")) {
                    ps.setNString(1, liveBill.getLoyaltyCustomerName());
                    ps.setNString(2, liveBill.getLoyaltyCustomerPhone());
                    ps.setNString(3, liveBill.getLoyaltyCustomerEmail());
                    ps.setInt(4, historyId);
                    ps.executeUpdate();
                }
            }
            if (liveBill.isHasVoucher()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Vouchers SET status='USED', used_at=SYSDATETIME() WHERE voucher_id=? AND status='ACTIVE'")) {
                    ps.setInt(1, liveBill.getVoucherId());
                    if (ps.executeUpdate() != 1) throw new SQLException("Voucher đã được sử dụng hoặc không còn hiệu lực.");
                }
            }
            applyMergedLoyalty(liveBill, historyId, actorId);
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM TableBillVoucher WHERE table_id=(SELECT TOP 1 table_id FROM MergedBillTables WHERE merged_bill_id=? AND active=1)")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM TableBillLoyalty WHERE table_id=(SELECT TOP 1 table_id FROM MergedBillTables WHERE merged_bill_id=? AND active=1)")) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE MergedBills SET bill_status='PAID',closed_at=SYSDATETIME() WHERE merged_bill_id=? AND bill_status='OPEN'")) {
                ps.setInt(1, id); if (ps.executeUpdate() != 1) throw new SQLException("Bill đã thay đổi.");
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE MergedBillTables SET active=0 WHERE merged_bill_id=? AND active=1")) {
                ps.setInt(1, id); if (ps.executeUpdate() != ids.size()) throw new SQLException("Nhóm bàn đã thay đổi.");
            }
            connection.commit(); return true;
        } catch (SQLException e) { error = e.getMessage(); connection.rollback(); return false; }
        finally { connection.setAutoCommit(true); }
    }

    private void applyMergedLoyalty(TableBill bill, int historyId, int actorId) throws SQLException {
        if (!bill.isHasLoyaltyCustomer()) return;
        BigDecimal spend = null;
        BigDecimal redeem = null;
        int earnPoints = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT earn_spend_amount, earn_points, redeem_value_per_point FROM LoyaltySettings WHERE settings_id=1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                spend = rs.getBigDecimal(1);
                earnPoints = rs.getInt(2);
                redeem = rs.getBigDecimal(3);
            }
        }
        if (bill.getPointsToUse() > 0) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Users SET loyalty_points=loyalty_points-? OUTPUT inserted.loyalty_points WHERE user_id=? AND loyalty_points>=?")) {
                ps.setInt(1, bill.getPointsToUse());
                ps.setInt(2, bill.getLoyaltyCustomerId());
                ps.setInt(3, bill.getPointsToUse());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Khách không đủ điểm tích lũy để sử dụng.");
                    insertLoyaltyTxn(bill, historyId, "REDEEM", -bill.getPointsToUse(), rs.getInt(1),
                            bill.getDiscountAmount(), spend, earnPoints, redeem, actorId);
                }
            }
        }
        if (bill.getPointsToEarn() > 0) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE Users SET loyalty_points=loyalty_points+? OUTPUT inserted.loyalty_points WHERE user_id=?")) {
                ps.setInt(1, bill.getPointsToEarn());
                ps.setInt(2, bill.getLoyaltyCustomerId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Không cộng được điểm tích lũy.");
                    insertLoyaltyTxn(bill, historyId, "EARN", bill.getPointsToEarn(), rs.getInt(1),
                            bill.getPayableAmount(), spend, earnPoints, redeem, actorId);
                }
            }
        }
    }

    private void insertLoyaltyTxn(TableBill bill, int historyId, String type, int points, int balance,
            BigDecimal moneyValue, BigDecimal spend, int earnPoints, BigDecimal redeem, int actorId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO LoyaltyTransactions(user_id,history_id,txn_type,points,balance_after,bill_amount,money_value,"
                + "earn_spend_amount,earn_points,redeem_value_per_point,note,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, bill.getLoyaltyCustomerId());
            ps.setInt(2, historyId);
            ps.setString(3, type);
            ps.setInt(4, points);
            ps.setInt(5, balance);
            ps.setBigDecimal(6, bill.getTotalAmount());
            ps.setBigDecimal(7, moneyValue);
            ps.setBigDecimal(8, spend);
            ps.setInt(9, earnPoints);
            ps.setBigDecimal(10, redeem);
            ps.setString(11, "Bill gộp");
            ps.setInt(12, actorId);
            ps.executeUpdate();
        }
    }

    private boolean hasPendingQrTransfer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM MergedBillTransfers WHERE merged_bill_id=? AND transfer_status='REVIEW'")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean cancel(int id) throws SQLException {
        if (hasPendingQrTransfer(id)) throw new SQLException("Bill có chuyển khoản cần đối soát; không thể hủy gộp.");
        connection.setAutoCommit(false);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE MergedBills SET bill_status='CANCELLED',closed_at=SYSDATETIME() WHERE merged_bill_id=? AND bill_status='OPEN'")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() != 1) { connection.rollback(); return false; }
            discardPaymentLinks(id);
            try (PreparedStatement member = connection.prepareStatement(
                    "UPDATE MergedBillTables SET active=0 WHERE merged_bill_id=? AND active=1")) {
                member.setInt(1, id); member.executeUpdate();
            }
            connection.commit(); return true;
        } catch (SQLException e) { connection.rollback(); throw e; }
        finally { connection.setAutoCommit(true); }
    }
}
