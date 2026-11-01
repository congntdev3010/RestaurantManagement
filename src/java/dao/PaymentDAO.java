package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import model.TableBill;
import model.TableBillItem;

/**
 * Thanh toán bàn: tiền mặt, QR (SePay webhook), tính bill (voucher, điểm
 * thành viên), chốt hóa đơn vào ServiceHistory. Tách từ OrderDAO.
 */
public class PaymentDAO extends DBContext implements AutoCloseable {

    private String lastPaymentFailureReason;
    private BigDecimal lastCashChange;
    /** Tính bill (món, voucher, điểm) nằm ở BillDAO; PaymentDAO chỉ chốt thanh toán. */
    private final BillDAO bills = new BillDAO();

    /** Mã tham chiếu chuyển khoản của một bill: gắn với số đơn OPEN nhỏ nhất của bàn. */
    public static String qrReference(int tableId, int billKey) {
        return "RM-TABLE-" + tableId + "-" + billKey;
    }

    public boolean markPaid(int orderId, int actorId, String paymentMethod, String paymentReference) {
        if (connection == null || (!"CASH".equals(paymentMethod) && !"QR".equals(paymentMethod))) {
            return false;
        }
        try {
            connection.setAutoCommit(false);
            Integer tableId = null;
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT table_id FROM Orders WITH (UPDLOCK, HOLDLOCK) WHERE order_id = ? AND order_status = 'OPEN' "
                    + "AND NOT EXISTS (SELECT 1 FROM OrderItems WHERE order_id = Orders.order_id "
                    + "AND item_status IN ('PENDING', 'COOKING'))")) {
                lock.setInt(1, orderId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) {
                        rollback();
                        return false;
                    }
                    tableId = nullableInt(rs, "table_id");
                }
            }
            try (PreparedStatement paid = connection.prepareStatement(
                    "UPDATE Orders SET order_status = 'PAID', closed_at = SYSDATETIME(), "
                    + "paid_at = SYSDATETIME(), paid_by = ?, payment_method = ?, payment_reference = ? "
                    + "WHERE order_id = ? AND order_status = 'OPEN'")) {
                paid.setInt(1, actorId);
                paid.setString(2, paymentMethod);
                paid.setNString(3, paymentReference);
                paid.setInt(4, orderId);
                if (paid.executeUpdate() != 1) {
                    rollback();
                    return false;
                }
            }
            if (tableId != null) {
                settleCancelledOrders(tableId);
                try (PreparedStatement table = connection.prepareStatement(
                        "UPDATE RestaurantTables SET table_status = 'AVAILABLE' "
                        + "WHERE table_id = ? AND table_status = 'OCCUPIED'")) {
                    table.setInt(1, tableId);
                    table.executeUpdate();
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            rollback();
            e.printStackTrace();
            return false;
        } finally {
            resetAutoCommit();
        }
    }

    /** Bill đang mở của bàn: uỷ quyền sang BillDAO (xem BillDAO.getTableBill). */
    public TableBill getTableBill(int tableId) {
        return bills.getTableBill(tableId);
    }

    /**
     * Thanh toán gộp toàn bộ order OPEN của một bàn trong một giao dịch.
     */
    public boolean markTablePaid(int tableId, int actorId, String paymentMethod, String paymentReference,
            BigDecimal cashReceived, BigDecimal cashChange) {
        lastPaymentFailureReason = null;
        if (connection == null || (!"CASH".equals(paymentMethod) && !"QR".equals(paymentMethod))) {
            return false;
        }
        if ("CASH".equals(paymentMethod) && cashReceived == null) {
            return false;
        }
        try {
            connection.setAutoCommit(false);
            int orderCount;
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT COUNT(*) AS order_count FROM Orders WITH (UPDLOCK, HOLDLOCK) "
                    + "WHERE table_id = ? AND order_status = 'OPEN'")) {
                lock.setInt(1, tableId);
                try (ResultSet rs = lock.executeQuery()) {
                    rs.next();
                    orderCount = rs.getInt("order_count");
                }

            }
            if (orderCount == 0 || hasUnfinishedTableItems(tableId)) {
                lastPaymentFailureReason = orderCount == 0 ? "Không có order OPEN." : "Bàn còn order hoặc món chưa hoàn tất.";
                rollback();
                return false;
            }
            TableBill bill = getTableBill(tableId);
            if (bill == null) {
                lastPaymentFailureReason = "Không đọc được bill OPEN của bàn.";
                rollback();
                return false;
            }
            if ("CASH".equals(paymentMethod) && cashReceived.compareTo(bill.getPayableAmount()) < 0) {
                lastPaymentFailureReason = "Tiền khách đưa chưa đủ.";
                rollback();
                return false;
            }
            if ("CASH".equals(paymentMethod)) {
                // Tiền thối luôn tính lại ở server, không tin giá trị gửi từ form.
                cashChange = cashReceived.subtract(bill.getPayableAmount());
                lastCashChange = cashChange;
            }
            // Voucher: khóa mã trong cùng giao dịch; nếu đã USED (bàn khác vừa dùng) thì hủy thanh toán.
            if (bill.isHasVoucher()) {
                try (PreparedStatement use = connection.prepareStatement(
                        "UPDATE Vouchers SET status = 'USED', used_at = SYSDATETIME() WHERE voucher_id = ? AND status = 'ACTIVE'")) {
                    use.setInt(1, bill.getVoucherId());
                    if (use.executeUpdate() != 1) {
                        lastPaymentFailureReason = "Mã voucher đã được dùng hoặc không còn hiệu lực. Vui lòng bỏ mã và áp lại.";
                        rollback();
                        return false;
                    }
                }
            }
            int historyId;
            try (PreparedStatement history = connection.prepareStatement(
                    "INSERT INTO ServiceHistory (table_id, customer_name, customer_phone, customer_email, reservation_time, "
                    + "checked_in_at, paid_at, payment_method, total_amount, cash_received, cash_change, paid_by, "
                    + "loyalty_customer_id, loyalty_points_used, loyalty_discount, loyalty_points_earned, voucher_id, voucher_discount_percent, voucher_discount) "
                    + "SELECT ?, MAX(o.customer_name), MAX(o.customer_phone), MAX(o.customer_email), "
                    + "MIN(CASE WHEN r.reservation_date IS NOT NULL AND r.reservation_time IS NOT NULL "
                    + "THEN DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', r.reservation_time), "
                    + "CAST(r.reservation_date AS DATETIME2)) END), MAX(r.checked_in_at), SYSDATETIME(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? "
                    + "FROM Orders o LEFT JOIN Reservations r ON r.reservation_id = o.reservation_id "
                    + "WHERE o.table_id = ? AND o.order_status = 'OPEN'",
                    Statement.RETURN_GENERATED_KEYS)) {
                history.setInt(1, tableId);
                history.setString(2, paymentMethod);
                history.setBigDecimal(3, bill.getTotalAmount());
                if (cashReceived == null) {
                    history.setNull(4, java.sql.Types.DECIMAL);
                } else {
                    history.setBigDecimal(4, cashReceived);
                }
                if (cashChange == null) {
                    history.setNull(5, java.sql.Types.DECIMAL);
                } else {
                    history.setBigDecimal(5, cashChange);
                }
                history.setInt(6, actorId);
                if (bill.getLoyaltyCustomerId() > 0) {
                    history.setInt(7, bill.getLoyaltyCustomerId());
                } else {
                    history.setNull(7, java.sql.Types.INTEGER);
                }
                history.setInt(8, loyaltyUsed(bill));
                history.setBigDecimal(9, bill.getDiscountAmount());
                history.setInt(10, bill.getPointsToEarn());
                if (bill.isHasVoucher()) {
                    history.setInt(11, bill.getVoucherId());
                } else {
                    history.setNull(11, java.sql.Types.INTEGER);
                }
                history.setBigDecimal(12, bill.getVoucherPercent());
                history.setBigDecimal(13, bill.getVoucherDiscount());
                history.setInt(14, tableId);
                history.executeUpdate();
                try (ResultSet keys = history.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Could not create service history.");
                    }
                    historyId = keys.getInt(1);
                }
            }
            try (PreparedStatement items = connection.prepareStatement(
                    "INSERT INTO ServiceHistoryItems (history_id, dish_name, unit, unit_price, quantity, line_total) "
                    + "SELECT ?, oi.dish_name, oi.unit, oi.unit_price, oi.quantity, oi.line_total "
                    + "FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                    + "WHERE o.table_id = ? AND o.order_status = 'OPEN' AND oi.item_status <> 'REJECTED'")) {
                items.setInt(1, historyId);
                items.setInt(2, tableId);
                items.executeUpdate();
            }
            try (PreparedStatement paid = connection.prepareStatement(
                    "UPDATE Orders SET order_status = 'PAID', closed_at = SYSDATETIME(), paid_at = SYSDATETIME(), "
                    + "paid_by = ?, payment_method = ?, payment_reference = ?, cash_received = ?, cash_change = ? "
                    + "WHERE table_id = ? AND order_status = 'OPEN'")) {
                paid.setInt(1, actorId);
                paid.setString(2, paymentMethod);
                paid.setNString(3, paymentReference);
                if (cashReceived == null) {
                    paid.setNull(4, java.sql.Types.DECIMAL);
                } else {
                    paid.setBigDecimal(4, cashReceived);
                }
                if (cashChange == null) {
                    paid.setNull(5, java.sql.Types.DECIMAL);
                } else {
                    paid.setBigDecimal(5, cashChange);
                }
                paid.setInt(6, tableId);
                if (paid.executeUpdate() != orderCount) {
                    lastPaymentFailureReason = "Order đã thay đổi trong lúc thanh toán.";
                    rollback();
                    return false;
                }
            }
            try (PreparedStatement table = connection.prepareStatement(
                    "UPDATE RestaurantTables SET table_status = 'AVAILABLE' "
                    + "WHERE table_id = ? AND table_status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED')")) {
                settleCancelledOrders(tableId);
                if (!applyLoyaltyOnPayment(bill, historyId, actorId)) {
                    rollback();
                    return false;
                }
                // Bàn đã thanh toán thì gỡ mã khỏi bàn (mã đã USED ở trên).
                try (PreparedStatement delVoucher = connection.prepareStatement("DELETE FROM TableBillVoucher WHERE table_id = ?")) {
                    delVoucher.setInt(1, tableId);
                    delVoucher.executeUpdate();
                }
                table.setInt(1, tableId);
                if (table.executeUpdate() != 1) {
                    lastPaymentFailureReason = "Không cập nhật được trạng thái bàn.";
                    rollback();
                    return false;
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            rollback();
            e.printStackTrace();
            return false;
        } finally {
            resetAutoCommit();
        }
    }

    /**
     * Chốt các order HỦY của bàn khi bàn đã thanh toán, để chúng không còn hiện
     * ở danh sách "Đã hủy" (chỉ ghi settled_at cho order chưa chốt).
     */
    private void settleCancelledOrders(int tableId) throws SQLException {
        try (PreparedStatement settle = connection.prepareStatement(
                "UPDATE Orders SET settled_at = SYSDATETIME() WHERE table_id = ? "
                + "AND order_status = 'CANCELLED' AND settled_at IS NULL")) {
            settle.setInt(1, tableId);
            settle.executeUpdate();
        }
    }

    private int loyaltyUsed(TableBill bill) {
        return bill.getPointsToUse();
    }

    /** Gỡ liên kết điểm còn sót của bàn (uỷ quyền sang BillDAO). */
    public void clearStaleLoyaltyLink(int tableId) {
        bills.clearStaleLoyaltyLink(tableId);
    }

    /**
     * Trong giao dịch thanh toán: trừ điểm đã dùng, cộng điểm mới, ghi lịch sử,
     * gỡ liên kết khách.
     */
    private boolean applyLoyaltyOnPayment(TableBill bill, int historyId, int actorId) throws SQLException {
        int customerId = bill.getLoyaltyCustomerId();
        if (customerId > 0) {
            // Ghi tên + SĐT của khách thành viên vào chính hóa đơn đã lưu (không phụ thuộc đơn gốc).
            try (PreparedStatement snap = connection.prepareStatement(
                    "UPDATE ServiceHistory SET customer_name = ?, customer_phone = ?, customer_email = ? WHERE history_id = ?")) {
                snap.setNString(1, bill.getLoyaltyCustomerName());
                snap.setNString(2, bill.getLoyaltyCustomerPhone());
                snap.setNString(3, bill.getLoyaltyCustomerEmail());
                snap.setInt(4, historyId);
                snap.executeUpdate();
            }
            int used = bill.getPointsToUse();
            int earned = bill.getPointsToEarn();
            BigDecimal spend = null;
            int earnPoints = 0;
            BigDecimal redeem = null;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT earn_spend_amount, earn_points, redeem_value_per_point FROM LoyaltySettings WHERE settings_id = 1"); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    spend = rs.getBigDecimal(1);
                    earnPoints = rs.getInt(2);
                    redeem = rs.getBigDecimal(3);
                }
            }
            if (used > 0) {
                Integer balance = changePoints(customerId, -used, used);
                if (balance == null) {
                    lastPaymentFailureReason = "Khách không đủ điểm tích lũy để sử dụng.";
                    return false;
                }
                insertLoyaltyTxn(customerId, historyId, "REDEEM", -used, balance, bill.getTotalAmount(),
                        bill.getDiscountAmount(), spend, earnPoints, redeem,
                        "Dùng điểm giảm giá hóa đơn bàn " + bill.getTableId(), actorId);
            }
            if (earned > 0) {
                Integer balance = changePoints(customerId, earned, 0);
                if (balance == null) {
                    lastPaymentFailureReason = "Không cộng được điểm tích lũy.";
                    return false;
                }
                insertLoyaltyTxn(customerId, historyId, "EARN", earned, balance, bill.getTotalAmount(),
                        bill.getPayableAmount(), spend, earnPoints, redeem,
                        "Tích điểm từ hóa đơn bàn " + bill.getTableId(), actorId);
            }
        }
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM TableBillLoyalty WHERE table_id = ?")) {
            del.setInt(1, bill.getTableId());
            del.executeUpdate();
        }
        return true;
    }

    /**
     * Cộng/trừ điểm, trả về số dư mới; null nếu không đủ điểm (minRequired >
     * 0).
     */
    private Integer changePoints(int userId, int delta, int minRequired) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE Users SET loyalty_points = loyalty_points + ? OUTPUT inserted.loyalty_points "
                + "WHERE user_id = ? AND loyalty_points >= ?")) {
            ps.setInt(1, delta);
            ps.setInt(2, userId);
            ps.setInt(3, minRequired);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private void insertLoyaltyTxn(int userId, int historyId, String type, int points, int balanceAfter,
            BigDecimal billAmount, BigDecimal moneyValue, BigDecimal spend, int earnPoints, BigDecimal redeem,
            String note, int actorId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO LoyaltyTransactions (user_id, history_id, txn_type, points, balance_after, bill_amount, "
                + "money_value, earn_spend_amount, earn_points, redeem_value_per_point, note, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, historyId);
            ps.setString(3, type);
            ps.setInt(4, points);
            ps.setInt(5, balanceAfter);
            ps.setBigDecimal(6, billAmount);
            ps.setBigDecimal(7, moneyValue);
            ps.setBigDecimal(8, spend);
            ps.setInt(9, earnPoints);
            ps.setBigDecimal(10, redeem);
            ps.setNString(11, note);
            ps.setInt(12, actorId);
            ps.executeUpdate();
        }
    }

    private boolean hasUnfinishedTableItems(int tableId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM Orders o LEFT JOIN OrderItems oi ON oi.order_id = o.order_id "
                + "WHERE o.table_id = ? AND ((o.order_status = 'PENDING' "
                + "AND oi.item_status IN ('PENDING', 'COOKING')) "
                + "OR (o.order_status = 'OPEN' AND oi.item_status IN ('PENDING', 'COOKING')))")) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Số đơn OPEN nhỏ nhất của bàn (định danh bill đang mở); null nếu không có bill. */
    public Integer getBillKey(int tableId) {
        if (connection == null) {
            return null;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT MIN(order_id) FROM Orders WHERE table_id = ? AND order_status = 'OPEN'")) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int value = rs.getInt(1);
                return rs.wasNull() ? null : value;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean markQrTablePaid(int tableId, int actorId, String reference, BigDecimal transferAmount) {
        // Webhook phải báo đúng mã của bill đang mở: chuyển khoản cũ không chốt được bill mới.
        Integer billKey = getBillKey(tableId);
        if (billKey == null || !qrReference(tableId, billKey).equals(reference)) {
            lastPaymentFailureReason = "Mã chuyển khoản không khớp bill đang mở của bàn.";
            return false;
        }
        TableBill bill = getTableBill(tableId);
        if (bill == null) {
            lastPaymentFailureReason = "Không có bill OPEN cho table_id=" + tableId + ".";
            return false;
        }
        if (transferAmount == null || bill.getPayableAmount().compareTo(transferAmount) != 0) {
            lastPaymentFailureReason = "Số tiền không khớp. Bill=" + bill.getPayableAmount() + ", nhận=" + transferAmount + ".";
            return false;
        }
        return markTablePaid(tableId, actorId, "QR", reference, null, null);
    }

    public TableBill getQrReceipt(int tableId) throws SQLException {
        String sql = "SELECT TOP 1 h.history_id, h.table_id, h.customer_name, h.customer_phone, h.customer_email, "
                + "h.reservation_time, h.checked_in_at, h.payment_method, h.total_amount, "
                + "h.cash_received, h.cash_change, h.loyalty_points_used, h.loyalty_discount, h.loyalty_points_earned, h.voucher_id, h.voucher_discount_percent, h.voucher_discount "
                + "FROM ServiceHistory h WHERE h.table_id = ? AND h.payment_method = 'QR' "
                + "ORDER BY h.history_id DESC";
        TableBill bill = new TableBill();
        int historyId;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                historyId = rs.getInt("history_id");
                bill.setTableId(rs.getInt("table_id"));
                try (PreparedStatement tn = connection.prepareStatement(
                        "SELECT table_name FROM RestaurantTables WHERE table_id = ?")) {
                    tn.setInt(1, rs.getInt("table_id"));
                    try (ResultSet tnRs = tn.executeQuery()) {
                        if (tnRs.next()) {
                            bill.setTableName(tnRs.getNString("table_name"));
                        }
                    }
                }
                bill.setCustomerName(rs.getNString("customer_name"));
                bill.setCustomerPhone(rs.getNString("customer_phone"));
                bill.setCustomerEmail(rs.getNString("customer_email"));
                java.sql.Timestamp reservation = rs.getTimestamp("reservation_time");
                java.sql.Timestamp checkedIn = rs.getTimestamp("checked_in_at");
                if (reservation != null) {
                    bill.setReservationTime(reservation.toLocalDateTime());
                }
                if (checkedIn != null) {
                    bill.setCheckedInAt(checkedIn.toLocalDateTime());
                }
                bill.setPaymentMethod(rs.getString("payment_method"));
                bill.setTotalAmount(rs.getBigDecimal("total_amount"));
                bill.setCashReceived(rs.getBigDecimal("cash_received"));
                bill.setPointsToUse(rs.getInt("loyalty_points_used"));
                bill.setDiscountAmount(rs.getBigDecimal("loyalty_discount"));
                bill.setPointsToEarn(rs.getInt("loyalty_points_earned"));
                bill.setVoucherId(rs.getInt("voucher_id"));
                bill.setVoucherPercent(rs.getBigDecimal("voucher_discount_percent"));
                bill.setVoucherDiscount(rs.getBigDecimal("voucher_discount"));
                bill.setPayableAmount(bill.getTotalAmount().subtract(bill.getDiscountAmount()).subtract(bill.getVoucherDiscount()));
                bill.setCashChange(rs.getBigDecimal("cash_change"));
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT dish_name, unit, unit_price, quantity, line_total FROM ServiceHistoryItems "
                + "WHERE history_id = ? ORDER BY history_item_id")) {
            ps.setInt(1, historyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableBillItem item = new TableBillItem();
                    item.setDishName(rs.getNString("dish_name"));
                    item.setUnit(rs.getNString("unit"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));
                    bill.getItems().add(item);
                }
            }
        }
        return bill;
    }

    public boolean isQrPaymentCompleted(int tableId, String reference) throws SQLException {
        String sql = "SELECT CASE WHEN EXISTS (SELECT 1 FROM Orders "
                + "WHERE table_id = ? AND order_status = 'PAID' AND payment_method = 'QR' "
                + "AND payment_reference = ?) THEN 1 ELSE 0 END";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setNString(2, reference);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    public TableBill getQrReceipt(int tableId, String reference) throws SQLException {
        return getQrReceipt(tableId);
    }

    public BigDecimal getLastCashChange() {
        return lastCashChange;
    }

    public String getLastPaymentFailureReason() {
        return lastPaymentFailureReason;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    private void rollback() {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    private void resetAutoCommit() {
        try {
            if (connection != null) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void close() {
        bills.close();
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
