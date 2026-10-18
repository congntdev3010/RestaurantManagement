package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderItem;

public class OrderDAO extends DBContext implements AutoCloseable {

    public int createOrder(Integer customerId, Integer createdBy, Integer tableId, Integer reservationId,
            String status, String customerName, String customerPhone, String customerEmail,
            String note, Map<Integer, Integer> quantities) {
        if (connection == null || quantities == null || quantities.isEmpty()) return -1;
        try {
            connection.setAutoCommit(false);
            validateTableAndReservation(tableId, reservationId);
            String insert = "INSERT INTO Orders (reservation_id, table_id, user_id, created_by, customer_name, "
                    + "customer_phone, customer_email, order_status, total_amount, note) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";
            int orderId;
            try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                setNullableInt(ps, 1, reservationId);
                setNullableInt(ps, 2, tableId);
                setNullableInt(ps, 3, customerId);
                setNullableInt(ps, 4, createdBy);
                ps.setString(5, customerName);
                ps.setString(6, customerPhone);
                ps.setString(7, customerEmail);
                ps.setString(8, status);
                ps.setString(9, note);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Could not create order key.");
                    orderId = keys.getInt(1);
                }
            }
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                int quantity = entry.getValue() == null ? 0 : entry.getValue();
                if (entry.getKey() == null || entry.getKey() < 1 || quantity < 1 || quantity > 99)
                    throw new SQLException("Invalid order item.");
                try (PreparedStatement dish = connection.prepareStatement(
                        "SELECT dish_name, unit, price FROM Dish WITH (UPDLOCK, HOLDLOCK) WHERE dish_id = ? AND status = 1")) {
                    dish.setInt(1, entry.getKey());
                    try (ResultSet rs = dish.executeQuery()) {
                        if (!rs.next()) throw new SQLException("A dish is unavailable.");
                        BigDecimal price = rs.getBigDecimal("price");
                        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                        try (PreparedStatement line = connection.prepareStatement(
                                "INSERT INTO OrderItems (order_id, dish_id, dish_name, unit, unit_price, quantity, line_total) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                            line.setInt(1, orderId);
                            line.setInt(2, entry.getKey());
                            line.setNString(3, rs.getNString("dish_name"));
                            line.setNString(4, rs.getNString("unit") == null ? "Phần" : rs.getNString("unit"));
                            line.setBigDecimal(5, price);
                            line.setInt(6, quantity);
                            line.setBigDecimal(7, lineTotal);
                            line.executeUpdate();
                        }
                        total = total.add(lineTotal);
                    }
                }
            }
            if (total.precision() > 12) throw new SQLException("Order total exceeds supported amount.");
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE Orders SET total_amount = ? WHERE order_id = ?")) {
                update.setBigDecimal(1, total);
                update.setInt(2, orderId);
                update.executeUpdate();
            }
            connection.commit();
            return orderId;
        } catch (SQLException e) {
            rollback();
            e.printStackTrace();
            return -1;
        } finally {
            resetAutoCommit();
        }
    }

    private void validateTableAndReservation(Integer tableId, Integer reservationId) throws SQLException {
        if (tableId != null) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT table_status FROM RestaurantTables WITH (UPDLOCK, HOLDLOCK) WHERE table_id = ?")) {
                ps.setInt(1, tableId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Table does not exist.");
                    String tableStatus = rs.getString("table_status");
                    if (!"AVAILABLE".equals(tableStatus) && !"RESERVED".equals(tableStatus) && !"OCCUPIED".equals(tableStatus))
                        throw new SQLException("Table is not available for service.");
                }
            }
        }
        if (reservationId != null) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT table_id FROM Reservations WITH (UPDLOCK, HOLDLOCK) WHERE reservation_id = ? AND reservation_status = 'APPROVED'")) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Reservation is not approved.");
                    int reservedTable = rs.getInt("table_id");
                    boolean hasTable = !rs.wasNull();
                    if (hasTable && (tableId == null || reservedTable != tableId))
                        throw new SQLException("Order table must match its reservation.");
                }
            }
        }
    }

    public List<Order> getOrders(Integer ownerId, String statusFilter) {
        List<Order> orders = new ArrayList<>();
        if (connection == null) return orders;
        StringBuilder sql = new StringBuilder("SELECT o.*, t.table_name FROM Orders o "
                + "LEFT JOIN RestaurantTables t ON t.table_id=o.table_id WHERE 1=1 ");
        if (ownerId != null) sql.append("AND o.user_id = ? ");
        if (statusFilter != null && !statusFilter.isEmpty()) sql.append("AND o.order_status = ? ");
        sql.append("ORDER BY o.created_at DESC, o.order_id DESC");
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int i = 1;
            if (ownerId != null) ps.setInt(i++, ownerId);
            if (statusFilter != null && !statusFilter.isEmpty()) ps.setString(i, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) orders.add(readOrder(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return orders;
    }

    public Order getOrder(int orderId, Integer ownerId) {
        if (connection == null) return null;
        String sql = "SELECT o.*, t.table_name FROM Orders o LEFT JOIN RestaurantTables t ON t.table_id=o.table_id "
                + "WHERE o.order_id = ?" + (ownerId == null ? "" : " AND o.user_id = ?");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            if (ownerId != null) ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Order order = readOrder(rs);
                try (PreparedStatement lines = connection.prepareStatement(
                        "SELECT * FROM OrderItems WHERE order_id = ? ORDER BY order_item_id")) {
                    lines.setInt(1, orderId);
                    try (ResultSet itemRs = lines.executeQuery()) {
                        while (itemRs.next()) order.getItems().add(readItem(itemRs));
                    }
                }
                return order;
            }
        } catch (SQLException e) { e.printStackTrace(); return null; }
    }

    public boolean updateCustomerPendingOrder(int orderId, int ownerId, Map<Integer, Integer> quantities) {
        return replaceItems(orderId, ownerId, quantities, "PENDING");
    }

    public boolean updateStaffOpenOrder(int orderId, Map<Integer, Integer> quantities) {
        return replaceItems(orderId, null, quantities, "OPEN");
    }

    private boolean replaceItems(int orderId, Integer ownerId, Map<Integer, Integer> quantities, String requiredStatus) {
        if (connection == null || quantities == null || quantities.isEmpty()) return false;
        try {
            connection.setAutoCommit(false);
            String guard = "SELECT order_id FROM Orders WITH (UPDLOCK, HOLDLOCK) WHERE order_id=? AND order_status=?"
                    + (ownerId == null ? "" : " AND user_id=?");
            try (PreparedStatement check = connection.prepareStatement(guard)) {
                check.setInt(1, orderId); check.setString(2, requiredStatus);
                if (ownerId != null) check.setInt(3, ownerId);
                try (ResultSet rs = check.executeQuery()) { if (!rs.next()) throw new SQLException("Order cannot be edited."); }
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM OrderItems WHERE order_id=?")) {
                delete.setInt(1, orderId); delete.executeUpdate();
            }
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                int quantity = entry.getValue() == null ? 0 : entry.getValue();
                if (quantity < 1 || quantity > 99) throw new SQLException("Invalid quantity.");
                try (PreparedStatement dish = connection.prepareStatement(
                        "SELECT dish_name, unit, price FROM Dish WITH (UPDLOCK, HOLDLOCK) WHERE dish_id=? AND status=1")) {
                    dish.setInt(1, entry.getKey());
                    try (ResultSet rs = dish.executeQuery()) {
                        if (!rs.next()) throw new SQLException("A dish is unavailable.");
                        BigDecimal price = rs.getBigDecimal("price"), lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                        try (PreparedStatement line = connection.prepareStatement(
                                "INSERT INTO OrderItems (order_id,dish_id,dish_name,unit,unit_price,quantity,line_total) VALUES (?,?,?,?,?,?,?)")) {
                            line.setInt(1, orderId); line.setInt(2, entry.getKey()); line.setNString(3, rs.getNString("dish_name"));
                            line.setNString(4, rs.getNString("unit") == null ? "Phần" : rs.getNString("unit"));
                            line.setBigDecimal(5, price); line.setInt(6, quantity); line.setBigDecimal(7, lineTotal); line.executeUpdate();
                        }
                        total = total.add(lineTotal);
                    }
                }
            }
            if (total.precision() > 12) throw new SQLException("Order total exceeds supported amount.");
            try (PreparedStatement update = connection.prepareStatement("UPDATE Orders SET total_amount=? WHERE order_id=?")) {
                update.setBigDecimal(1, total); update.setInt(2, orderId); update.executeUpdate();
            }
            connection.commit(); return true;
        } catch (SQLException e) { rollback(); e.printStackTrace(); return false; }
        finally { resetAutoCommit(); }
    }

    public boolean approve(int orderId, int actorId) {
        return execute("UPDATE Orders SET order_status='APPROVED', approved_by=?, approved_at=SYSDATETIME() "
                + "WHERE order_id=? AND order_status='PENDING'", actorId, orderId);
    }

    public boolean startServing(int orderId) {
        return execute("UPDATE Orders SET order_status='OPEN' WHERE order_id=? AND order_status='APPROVED'", orderId);
    }

    public boolean markPaid(int orderId) {
        return execute("UPDATE Orders SET order_status='PAID', closed_at=SYSDATETIME() WHERE order_id=? AND order_status='OPEN'", orderId);
    }

    public boolean cancelCustomerOrder(int orderId, int ownerId, String reason, int actorId) {
        if (reason == null || reason.trim().isEmpty()) return false;
        return cancel("UPDATE Orders SET order_status='CANCELLED', cancelled_by=?, cancelled_at=SYSDATETIME(), "
                + "cancellation_reason=?, closed_at=SYSDATETIME() WHERE order_id=? AND user_id=? AND order_status='PENDING'",
                actorId, reason, orderId, ownerId);
    }

    public boolean cancelStaffOrder(int orderId, String reason, int actorId) {
        if (reason == null || reason.trim().isEmpty()) return false;
        return cancel("UPDATE Orders SET order_status='CANCELLED', cancelled_by=?, cancelled_at=SYSDATETIME(), "
                + "cancellation_reason=?, closed_at=SYSDATETIME() WHERE order_id=? AND order_status IN ('PENDING','APPROVED','OPEN')",
                actorId, reason, orderId);
    }

    private boolean execute(String sql, int... values) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i=0;i<values.length;i++) ps.setInt(i+1, values[i]);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private boolean cancel(String sql, int actorId, String reason, int... ids) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, actorId); ps.setString(2, reason);
            for (int i=0;i<ids.length;i++) ps.setInt(i+3, ids[i]);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private Order readOrder(ResultSet rs) throws SQLException {
        Order o = new Order(); o.setOrderId(rs.getInt("order_id"));
        o.setReservationId(nullableInt(rs,"reservation_id")); o.setTableId(nullableInt(rs,"table_id"));
        o.setTableName(rs.getNString("table_name")); o.setUserId(nullableInt(rs,"user_id"));
        o.setCreatedBy(nullableInt(rs,"created_by")); o.setCustomerName(rs.getNString("customer_name"));
        o.setCustomerPhone(rs.getString("customer_phone")); o.setCustomerEmail(rs.getString("customer_email"));
        o.setStatus(rs.getString("order_status")); o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setNote(rs.getNString("note")); o.setCreatedAt(rs.getTimestamp("created_at")); o.setClosedAt(rs.getTimestamp("closed_at"));
        o.setApprovedBy(nullableInt(rs,"approved_by")); o.setApprovedAt(rs.getTimestamp("approved_at"));
        o.setCancelledBy(nullableInt(rs,"cancelled_by")); o.setCancelledAt(rs.getTimestamp("cancelled_at"));
        o.setCancellationReason(rs.getNString("cancellation_reason")); return o;
    }
    private OrderItem readItem(ResultSet rs) throws SQLException {
        OrderItem i = new OrderItem(); i.setOrderItemId(rs.getInt("order_item_id")); i.setOrderId(rs.getInt("order_id"));
        i.setDishId(nullableInt(rs,"dish_id")); i.setDishName(rs.getNString("dish_name")); i.setUnit(rs.getNString("unit"));
        i.setUnitPrice(rs.getBigDecimal("unit_price")); i.setQuantity(rs.getInt("quantity")); i.setLineTotal(rs.getBigDecimal("line_total")); return i;
    }
    private Integer nullableInt(ResultSet rs, String column) throws SQLException { int v=rs.getInt(column); return rs.wasNull()?null:v; }
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER); else ps.setInt(index,value);
    }
    private void rollback() { try { if (connection != null) connection.rollback(); } catch (SQLException ignored) { } }
    private void resetAutoCommit() { try { if (connection != null) connection.setAutoCommit(true); } catch (SQLException ignored) { } }
    @Override public void close() { try { if (connection != null) connection.close(); } catch (SQLException ignored) { } }
}
