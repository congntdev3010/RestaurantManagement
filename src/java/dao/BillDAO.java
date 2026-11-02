package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.TableBill;
import model.TableBillItem;

/**
 * Tính bill đang mở của bàn: món, tổng tiền, voucher, điểm thành viên, số tiền
 * phải trả. Chỉ đọc (và gỡ liên kết mồ côi); việc chốt thanh toán nằm ở
 * PaymentDAO. Tách từ OrderDAO/PaymentDAO.
 */
public class BillDAO extends DBContext implements AutoCloseable {

    public TableBill getTableBill(int tableId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT COUNT(*) AS order_count, COALESCE(SUM(o.total_amount), 0) AS total_amount, "
                + "COALESCE((SELECT COUNT(*) FROM OrderItems oi JOIN Orders ox ON ox.order_id = oi.order_id "
                + "WHERE ox.table_id = ? AND ox.order_status = 'OPEN' "
                + "AND oi.item_status IN ('PENDING', 'COOKING')), 0) AS unfinished_count, "
                + "CASE WHEN EXISTS (SELECT 1 FROM Orders op WHERE op.table_id = ? AND op.order_status = 'PENDING') THEN 1 ELSE 0 END AS has_pending "
                
                
                
                + "FROM Orders o WHERE o.table_id = ? AND o.order_status = 'OPEN'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setInt(2, tableId);
            ps.setInt(3, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("order_count") == 0) {
                    clearStaleLoyaltyLink(tableId);
                    clearStaleVoucherLink(tableId);
                    return null;
                }
                TableBill bill = new TableBill();
                bill.setTableId(tableId);
                try (PreparedStatement tn = connection.prepareStatement(
                        "SELECT table_name FROM RestaurantTables WHERE table_id = ?")) {
                    tn.setInt(1, tableId);
                    try (ResultSet tnRs = tn.executeQuery()) {
                        if (tnRs.next()) {
                            bill.setTableName(tnRs.getNString("table_name"));
                        }
                    }
                }
                bill.setOrderCount(rs.getInt("order_count"));
                bill.setUnfinishedItemCount(rs.getInt("unfinished_count"));
                bill.setPendingOrder(rs.getInt("has_pending") == 1);
                bill.setTotalAmount(rs.getBigDecimal("total_amount"));
                try (PreparedStatement items = connection.prepareStatement(
                        "SELECT oi.dish_name, oi.unit, oi.unit_price, oi.quantity, oi.line_total "
                        + "FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                        + "WHERE o.table_id = ? AND o.order_status = 'OPEN' AND oi.item_status <> 'REJECTED' "
                        + "ORDER BY oi.order_item_id")) {
                    items.setInt(1, tableId);
                    try (ResultSet itemRs = items.executeQuery()) {
                        while (itemRs.next()) {
                            TableBillItem item = new TableBillItem();
                            item.setDishName(itemRs.getNString("dish_name"));
                            item.setUnit(itemRs.getNString("unit"));
                            item.setUnitPrice(itemRs.getBigDecimal("unit_price"));
                            item.setQuantity(itemRs.getInt("quantity"));
                            item.setLineTotal(itemRs.getBigDecimal("line_total"));
                            bill.getItems().add(item);
                        }
                    }
                }
                try (PreparedStatement customer = connection.prepareStatement(
                        "SELECT TOP 1 o.customer_name, o.customer_phone, o.customer_email, r.reservation_date, "
                        + "r.reservation_time, r.checked_in_at FROM Orders o "
                        + "LEFT JOIN Reservations r ON r.reservation_id = o.reservation_id "
                        + "WHERE o.table_id = ? AND o.order_status = 'OPEN' "
                        + "ORDER BY o.created_at")) {
                    customer.setInt(1, tableId);
                    try (ResultSet customerRs = customer.executeQuery()) {
                        if (customerRs.next()) {
                            bill.setCustomerName(customerRs.getNString("customer_name"));
                            bill.setCustomerPhone(customerRs.getString("customer_phone"));
                            bill.setCustomerEmail(customerRs.getString("customer_email"));
                            java.sql.Date date = customerRs.getDate("reservation_date");
                            java.sql.Time time = customerRs.getTime("reservation_time");
                            if (date != null && time != null) {
                                bill.setReservationTime(java.time.LocalDateTime.of(date.toLocalDate(), time.toLocalTime()));
                            }
                            java.sql.Timestamp checkedIn = customerRs.getTimestamp("checked_in_at");
                            if (checkedIn != null) {
                                bill.setCheckedInAt(checkedIn.toLocalDateTime());
                            }
                        }
                    }
                }
                applyVoucher(bill);
                applyLoyalty(bill);
                return bill;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Đọc mã voucher đang áp cho bàn (chỉ khi mã còn ACTIVE) và tính tiền giảm theo %, làm tròn đồng (HALF_UP,
     * khớp với CK_ServiceHistory_Voucher). Phải chạy TRƯỚC applyLoyalty để điểm không vượt phần còn lại.
     */
    private void applyVoucher(TableBill bill) {
        bill.setVoucherId(0);
        bill.setVoucherCode(null);
        bill.setVoucherPercent(BigDecimal.ZERO);
        bill.setVoucherDiscount(BigDecimal.ZERO);
        String sql = "SELECT v.voucher_id, v.code, v.discount_percent FROM TableBillVoucher b "
                + "JOIN Vouchers v ON v.voucher_id = b.voucher_id "
                + "WHERE b.table_id = ? AND v.status = 'ACTIVE'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bill.getTableId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                // Đọc hết vào biến tạm trước: cột code là CHAR(8) nên dùng getString (getNString sẽ lỗi).
                BigDecimal percent = rs.getBigDecimal("discount_percent");
                int voucherId = rs.getInt("voucher_id");
                String code = rs.getString("code");
                bill.setVoucherId(voucherId);
                bill.setVoucherCode(code == null ? null : code.trim());
                bill.setVoucherPercent(percent);
                bill.setVoucherDiscount(bill.getTotalAmount().multiply(percent)
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearStaleVoucherLink(int tableId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM TableBillVoucher WHERE table_id = ?")) {
            ps.setInt(1, tableId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearStaleLoyaltyLink(int tableId) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM TableBillLoyalty WHERE table_id = ?")) {
            ps.setInt(1, tableId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Đọc khách được gắn vào bill + quy tắc quy đổi, rồi tính điểm dùng, tiền
     * giảm, số phải trả, điểm được cộng.
     */
    private void applyLoyalty(TableBill bill) {
        bill.setPayableAmount(bill.getTotalAmount().subtract(bill.getVoucherDiscount()));
        String sql = "SELECT l.customer_id, u.full_name, u.phone, u.email, u.loyalty_points, l.points_to_use, "
                + "s.earn_spend_amount, s.earn_points, s.redeem_value_per_point "
                + "FROM TableBillLoyalty l JOIN Users u ON u.user_id = l.customer_id "
                + "CROSS JOIN LoyaltySettings s WHERE l.table_id = ? AND s.settings_id = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bill.getTableId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                int balance = rs.getInt("loyalty_points");
                BigDecimal redeem = rs.getBigDecimal("redeem_value_per_point");
                BigDecimal spend = rs.getBigDecimal("earn_spend_amount");
                int earnPoints = rs.getInt("earn_points");
                int maxByTotal = (redeem != null && redeem.signum() > 0)
                        ? bill.getTotalAmount().subtract(bill.getVoucherDiscount()).divideToIntegralValue(redeem).intValue()
                        : 0;
                int use = Math.max(0, Math.min(rs.getInt("points_to_use"), Math.min(balance, maxByTotal)));
                BigDecimal discount = (redeem != null && redeem.signum() > 0)
                        ? redeem.multiply(BigDecimal.valueOf(use))
                        : BigDecimal.ZERO;
                BigDecimal payable = bill.getTotalAmount().subtract(discount).subtract(bill.getVoucherDiscount());
                bill.setLoyaltyCustomerId(rs.getInt("customer_id"));
                bill.setLoyaltyCustomerName(rs.getNString("full_name"));
                bill.setLoyaltyCustomerPhone(rs.getNString("phone"));
                bill.setLoyaltyCustomerEmail(rs.getNString("email"));
                // Khách thành viên: họ tên + SĐT lấy từ tài khoản được gắn, ghi thẳng vào bill thay cho thông tin của đơn gốc.
                bill.setCustomerName(bill.getLoyaltyCustomerName());
                bill.setCustomerPhone(bill.getLoyaltyCustomerPhone());
                bill.setCustomerEmail(bill.getLoyaltyCustomerEmail());
                bill.setLoyaltyBalance(balance);
                bill.setPointsToUse(use);
                bill.setDiscountAmount(discount);
                bill.setPayableAmount(payable);
                bill.setPointsToEarn((payable.signum() > 0 && spend != null && spend.signum() > 0)
                        ? payable.divideToIntegralValue(spend).intValue() * earnPoints : 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
