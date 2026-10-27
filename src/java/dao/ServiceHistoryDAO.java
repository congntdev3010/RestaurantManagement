package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.PaidBill;
import model.TableBillItem;

/** Đọc lịch sử các đơn đã thanh toán (ServiceHistory) cho Lễ tân và Manager. */
public class ServiceHistoryDAO extends DBContext {

    private static final String SELECT_BILL
            = "SELECT h.history_id, t.table_name, h.customer_name, h.customer_phone, h.paid_at, "
            + "h.payment_method, h.total_amount, COALESCE(h.loyalty_discount, 0) AS loyalty_discount, "
            + "COALESCE(h.loyalty_points_used, 0) AS points_used, COALESCE(h.loyalty_points_earned, 0) AS points_earned, "
            + "h.cash_received, h.cash_change, u.full_name AS paid_by_name "
            + "FROM ServiceHistory h "
            + "LEFT JOIN RestaurantTables t ON t.table_id = h.table_id "
            + "LEFT JOIN Users u ON u.user_id = h.paid_by ";

    /**
     * Danh sách đơn đã thanh toán, mới nhất trước, lọc theo khoảng ngày (null = không giới hạn).
     * Giới hạn 500 dòng để trang không quá nặng.
     */
    public List<PaidBill> search(LocalDate from, LocalDate to) throws SQLException {
        List<PaidBill> list = new ArrayList<>();
        requireConnection();
        String sql = "SELECT TOP 500 * FROM (" + SELECT_BILL
                + ") AS x WHERE (? IS NULL OR x.paid_at >= ?) AND (? IS NULL OR x.paid_at < ?) "
                + "ORDER BY x.paid_at DESC, x.history_id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp start = from == null ? null : Timestamp.valueOf(from.atStartOfDay());
            Timestamp end = to == null ? null : Timestamp.valueOf(to.plusDays(1).atStartOfDay());
            if (start == null) { ps.setNull(1, Types.TIMESTAMP); ps.setNull(2, Types.TIMESTAMP); }
            else { ps.setTimestamp(1, start); ps.setTimestamp(2, start); }
            if (end == null) { ps.setNull(3, Types.TIMESTAMP); ps.setNull(4, Types.TIMESTAMP); }
            else { ps.setTimestamp(3, end); ps.setTimestamp(4, end); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readBill(rs));
                }
            }
        }
        return list;
    }

    /** Một đơn kèm danh sách món; null nếu không tồn tại. */
    public PaidBill getById(int historyId) throws SQLException {
        requireConnection();
        PaidBill bill = null;
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BILL + "WHERE h.history_id = ?")) {
            ps.setInt(1, historyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bill = readBill(rs);
                }
            }
        }
        if (bill == null) {
            return null;
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

    /** Id đơn thanh toán mới nhất của một bàn (dùng ngay sau khi thanh toán); null nếu chưa có. */
    public Integer findLatestHistoryId(int tableId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TOP 1 history_id FROM ServiceHistory WHERE table_id = ? ORDER BY history_id DESC")) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private void requireConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }
    }

    private PaidBill readBill(ResultSet rs) throws SQLException {
        PaidBill bill = new PaidBill();
        bill.setHistoryId(rs.getInt("history_id"));
        bill.setTableName(rs.getNString("table_name"));
        bill.setCustomerName(rs.getNString("customer_name"));
        bill.setCustomerPhone(rs.getNString("customer_phone"));
        Timestamp paid = rs.getTimestamp("paid_at");
        if (paid != null) {
            bill.setPaidAt(paid.toLocalDateTime());
        }
        bill.setPaymentMethod(rs.getString("payment_method"));
        BigDecimal total = rs.getBigDecimal("total_amount");
        bill.setTotalAmount(total == null ? BigDecimal.ZERO : total);
        bill.setDiscountAmount(rs.getBigDecimal("loyalty_discount"));
        bill.setPointsUsed(rs.getInt("points_used"));
        bill.setPointsEarned(rs.getInt("points_earned"));
        bill.setCashReceived(rs.getBigDecimal("cash_received"));
        bill.setCashChange(rs.getBigDecimal("cash_change"));
        bill.setPaidByName(rs.getNString("paid_by_name"));
        return bill;
    }
}
