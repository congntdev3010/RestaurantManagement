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

/**
 * Luồng đơn: PENDING (chờ server duyệt) -> OPEN (đang phục vụ) -> PAID / CANCELLED.
 * Luồng món: PENDING -> COOKING (đã duyệt, hiện trên màn hình bếp) -> SERVED; hoặc REJECTED (hết món).
 */
public class OrderDAO extends DBContext implements AutoCloseable {

    public static final int APPROVE_FAILED = -1;
    public static final int APPROVE_ORDER_CANCELLED = 0;
    public static final int APPROVE_ORDER_OPENED = 1;

    public int createOrder(Integer customerId, Integer createdBy, Integer tableId, Integer reservationId,
            String status, String customerName, String customerPhone, String customerEmail,
            String note, Map<Integer, Integer> quantities) {
        return createOrder(customerId, createdBy, tableId, reservationId, status, customerName,
                customerPhone, customerEmail, note, quantities, null);
    }

    /** Tạo đơn mới. Mọi món vào ở trạng thái PENDING; quantities là số lượng khách/server yêu cầu. */
    public int createOrder(Integer customerId, Integer createdBy, Integer tableId, Integer reservationId,
            String status, String customerName, String customerPhone, String customerEmail,
            String note, Map<Integer, Integer> quantities, Map<Integer, String> itemNotes) {
        if (connection == null || quantities == null || quantities.isEmpty()) return -1;
        try {
            connection.setAutoCommit(false);
            validateTableAndReservation(tableId, reservationId);
            // Server order hộ khách (createdBy != null) thì bàn tự chuyển sang đang phục vụ.
            if (tableId != null && createdBy != null) {
                try (PreparedStatement occupy = connection.prepareStatement(
                        "UPDATE RestaurantTables SET table_status = 'OCCUPIED' "
                        + "WHERE table_id = ? AND table_status IN ('AVAILABLE', 'RESERVED')")) {
                    occupy.setInt(1, tableId);
                    occupy.executeUpdate();
                }
            }
            String insert = "INSERT INTO Orders (reservation_id, table_id, user_id, created_by, customer_name, "
                    + "customer_phone, customer_email, order_status, total_amount, note) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";
            int orderId;
            try (PreparedStatement ps = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                setNullableInt(ps, 1, reservationId);
                setNullableInt(ps, 2, tableId);
                setNullableInt(ps, 3, customerId);
                setNullableInt(ps, 4, createdBy);
                ps.setNString(5, customerName);
                ps.setString(6, customerPhone);
                ps.setString(7, customerEmail);
                ps.setString(8, status);
                ps.setNString(9, note);
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
                                "INSERT INTO OrderItems (order_id, dish_id, dish_name, unit, unit_price, quantity, "
                                + "requested_quantity, line_total, item_note, item_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')")) {
                            line.setInt(1, orderId);
                            line.setInt(2, entry.getKey());
                            line.setNString(3, rs.getNString("dish_name"));
                            line.setNString(4, rs.getNString("unit") == null ? "Phần" : rs.getNString("unit"));
                            line.setBigDecimal(5, price);
                            line.setInt(6, quantity);
                            line.setInt(7, quantity);
                            line.setBigDecimal(8, lineTotal);
                            line.setNString(9, itemNotes == null ? null : itemNotes.get(entry.getKey()));
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
        StringBuilder sql = new StringBuilder("SELECT o.*, t.table_name, (SELECT COUNT(*) FROM OrderItems x "
                + "WHERE x.order_id = o.order_id AND x.item_status = 'COOKING') AS cooking_count FROM Orders o "
                + "LEFT JOIN RestaurantTables t ON t.table_id=o.table_id WHERE 1=1 ");
        if (ownerId != null) sql.append("AND o.user_id = ? ");
        if (statusFilter != null && !statusFilter.isEmpty()) sql.append("AND o.order_status = ? ");
        // Đơn chờ duyệt luôn lên đầu danh sách, sau đó đến đơn mới nhất.
        // Đơn đang phục vụ: gọi trước hiện trước; các danh sách khác: mới nhất lên đầu.
        if (ownerId == null) {
            // Nhân viên: chờ duyệt rồi đang phục vụ lên đầu, đơn CŨ NHẤT trước để ưu tiên phục vụ; đơn đã đóng thì mới nhất trước.
            sql.append("ORDER BY CASE o.order_status WHEN 'PENDING' THEN 0 WHEN 'OPEN' THEN 1 ELSE 2 END, "
                    + "CASE WHEN o.order_status IN ('PENDING', 'OPEN') THEN o.created_at END ASC, "
                    + "CASE WHEN o.order_status IN ('PENDING', 'OPEN') THEN o.order_id END ASC, "
                    + "o.created_at DESC, o.order_id DESC");
        } else {
            sql.append("ORDER BY o.created_at DESC, o.order_id DESC");
        }
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int i = 1;
            if (ownerId != null) ps.setInt(i++, ownerId);
            if (statusFilter != null && !statusFilter.isEmpty()) ps.setString(i, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { Order o = readOrder(rs); o.setCookingCount(rs.getInt("cooking_count")); orders.add(o); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return orders;
    }

    /** Đọc đơn kèm món. ownerId = null nghĩa là không ràng buộc chủ đơn (nhân viên, hoặc khách đã được servlet kiểm bằng session). */
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

    /**
     * Server duyệt từng món của đơn PENDING. approvedQuantities: orderItemId -> số lượng phục vụ được.
     * Món không có trong map (bỏ tick) hoặc số lượng < 1 bị REJECTED; số lượng lớn hơn khách gọi bị chặn xuống bằng số gọi.
     * Còn ít nhất một món COOKING thì đơn thành OPEN; nếu không còn món nào thì đơn tự hủy.
     */
    public int approveItems(int orderId, int actorId, Map<Integer, Integer> approvedQuantities) {
        if (connection == null) return APPROVE_FAILED;
        try {
            connection.setAutoCommit(false);
            Integer tableId;
            try (PreparedStatement guard = connection.prepareStatement(
                    "SELECT table_id FROM Orders WITH (UPDLOCK, HOLDLOCK) WHERE order_id = ? AND order_status = 'PENDING'")) {
                guard.setInt(1, orderId);
                try (ResultSet rs = guard.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Order is not awaiting approval.");
                    tableId = nullableInt(rs, "table_id");
                }
            }
            List<int[]> pending = new ArrayList<>();
            try (PreparedStatement items = connection.prepareStatement(
                    "SELECT order_item_id, requested_quantity FROM OrderItems WHERE order_id = ? AND item_status = 'PENDING'")) {
                items.setInt(1, orderId);
                try (ResultSet rs = items.executeQuery()) {
                    while (rs.next()) pending.add(new int[]{rs.getInt(1), rs.getInt(2)});
                }
            }
            if (pending.isEmpty()) throw new SQLException("Order has no pending items.");
            int cooking = 0;
            for (int[] item : pending) {
                Integer wanted = approvedQuantities == null ? null : approvedQuantities.get(item[0]);
                int quantity = wanted == null ? 0 : Math.min(wanted, item[1]);
                if (quantity < 1) {
                    try (PreparedStatement reject = connection.prepareStatement(
                            "UPDATE OrderItems SET item_status = 'REJECTED', line_total = 0 WHERE order_item_id = ?")) {
                        reject.setInt(1, item[0]);
                        reject.executeUpdate();
                    }
                } else {
                    try (PreparedStatement accept = connection.prepareStatement(
                            "UPDATE OrderItems SET item_status = 'COOKING', quantity = ?, line_total = unit_price * ? "
                            + "WHERE order_item_id = ?")) {
                        accept.setInt(1, quantity);
                        accept.setInt(2, quantity);
                        accept.setInt(3, item[0]);
                        accept.executeUpdate();
                    }
                    cooking++;
                }
            }
            try (PreparedStatement total = connection.prepareStatement(
                    "UPDATE Orders SET total_amount = (SELECT COALESCE(SUM(line_total), 0) FROM OrderItems WHERE order_id = ?) "
                    + "WHERE order_id = ?")) {
                total.setInt(1, orderId);
                total.setInt(2, orderId);
                total.executeUpdate();
            }
            int result;
            if (cooking == 0) {
                try (PreparedStatement cancel = connection.prepareStatement(
                        "UPDATE Orders SET order_status = 'CANCELLED', cancelled_by = ?, cancelled_at = SYSDATETIME(), "
                        + "cancellation_reason = ?, closed_at = SYSDATETIME() WHERE order_id = ?")) {
                    cancel.setInt(1, actorId);
                    cancel.setNString(2, "Tất cả món trong đơn đã hết");
                    cancel.setInt(3, orderId);
                    cancel.executeUpdate();
                }
                result = APPROVE_ORDER_CANCELLED;
            } else {
                try (PreparedStatement open = connection.prepareStatement(
                        "UPDATE Orders SET order_status = 'OPEN', approved_by = ?, approved_at = SYSDATETIME() WHERE order_id = ?")) {
                    open.setInt(1, actorId);
                    open.setInt(2, orderId);
                    open.executeUpdate();
                }
                if (tableId != null) {
                    try (PreparedStatement table = connection.prepareStatement(
                            "UPDATE RestaurantTables SET table_status = 'OCCUPIED' "
                            + "WHERE table_id = ? AND table_status IN ('AVAILABLE', 'RESERVED')")) {
                        table.setInt(1, tableId);
                        table.executeUpdate();
                    }
                }
                result = APPROVE_ORDER_OPENED;
            }
            connection.commit();
            return result;
        } catch (SQLException e) {
            rollback();
            e.printStackTrace();
            return APPROVE_FAILED;
        } finally {
            resetAutoCommit();
        }
    }

    /** Server đánh dấu một món đang nấu là đã phục vụ. */
    public boolean markItemServed(int orderId, int orderItemId) {
        return execute("UPDATE oi SET item_status = 'SERVED' FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                + "WHERE oi.order_item_id = ? AND oi.order_id = ? AND oi.item_status = 'COOKING' AND o.order_status = 'OPEN'",
                orderItemId, orderId);
    }

    /** Thanh toán chỉ khi mọi món đã được phục vụ (hoặc bị từ chối). */
    public boolean markPaid(int orderId) {
        return execute("UPDATE Orders SET order_status = 'PAID', closed_at = SYSDATETIME() "
                + "WHERE order_id = ? AND order_status = 'OPEN' "
                + "AND NOT EXISTS (SELECT 1 FROM OrderItems WHERE order_id = Orders.order_id AND item_status IN ('PENDING', 'COOKING'))",
                orderId);
    }

    /** Bàn này còn đơn đang chờ duyệt không (còn thì chưa được order thêm). */
    public boolean hasPendingOrder(int tableId) {
        if (connection == null) return false;
        String sql = "SELECT 1 FROM Orders WHERE table_id = ? AND order_status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /** Lấy order đang chờ duyệt của một bàn để Server xem trực tiếp từ sơ đồ bàn. */
    public Order getPendingOrderForTable(int tableId) {
        if (connection == null) return null;
        String sql = "SELECT TOP 1 o.*, t.table_name FROM Orders o "
                + "LEFT JOIN RestaurantTables t ON t.table_id = o.table_id "
                + "WHERE o.table_id = ? AND o.order_status = 'PENDING' "
                + "ORDER BY o.created_at, o.order_id";
        int pendingOrderId = -1;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) pendingOrderId = rs.getInt("order_id");
            }
            return pendingOrderId > 0 ? getOrder(pendingOrderId, null) : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Các món đã được duyệt (đang nấu / đã phục vụ) trong những đơn đang phục vụ của một bàn, theo thứ tự gọi. */
    public List<OrderItem> getApprovedItemsForTable(int tableId) {
        List<OrderItem> items = new ArrayList<>();
        if (connection == null) return items;
        String sql = "SELECT oi.* FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                + "WHERE o.table_id = ? AND o.order_status = 'OPEN' AND oi.item_status IN ('COOKING', 'SERVED') "
                + "ORDER BY o.created_at, oi.order_item_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(readItem(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    /** Màn hình bếp: các món đã duyệt, chưa phục vụ, theo thứ tự duyệt. */
    public List<OrderItem> getKitchenItems() {
        List<OrderItem> items = new ArrayList<>();
        if (connection == null) return items;
        String sql = "SELECT oi.*, t.table_name, o.approved_at FROM OrderItems oi "
                + "JOIN Orders o ON o.order_id = oi.order_id "
                + "LEFT JOIN RestaurantTables t ON t.table_id = o.table_id "
                + "WHERE oi.item_status = 'COOKING' AND o.order_status = 'OPEN' "
                + "ORDER BY o.approved_at, oi.order_item_id";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OrderItem item = readItem(rs);
                item.setTableName(rs.getNString("table_name"));
                item.setApprovedAt(rs.getTimestamp("approved_at"));
                items.add(item);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    /**
     * Server/manager điều chỉnh món đang nấu khi bếp phát hiện không thể phục vụ đủ.
     * quantity = 0 nghĩa là hủy món; quantity > 0 là số lượng thực tế còn phục vụ được.
     */
    public boolean updateKitchenItem(int orderItemId, int quantity) {
        if (connection == null || quantity < 0) return false;
        try {
            connection.setAutoCommit(false);
            int orderId;
            int currentQuantity;
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT oi.order_id, oi.quantity FROM OrderItems oi WITH (UPDLOCK, HOLDLOCK) "
                    + "JOIN Orders o WITH (UPDLOCK, HOLDLOCK) ON o.order_id = oi.order_id "
                    + "WHERE oi.order_item_id = ? AND oi.item_status = 'COOKING' AND o.order_status = 'OPEN'")) {
                lock.setInt(1, orderItemId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (!rs.next()) {
                        rollback();
                        return false;
                    }
                    orderId = rs.getInt("order_id");
                    currentQuantity = rs.getInt("quantity");
                }
            }
            if (quantity > currentQuantity) {
                rollback();
                return false;
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE OrderItems SET item_status = ?, quantity = ?, line_total = "
                    + "CASE WHEN ? = 0 THEN 0 ELSE unit_price * ? END WHERE order_item_id = ?")) {
                update.setString(1, quantity == 0 ? "REJECTED" : "COOKING");
                // Quantity có constraint > 0; món bị hủy vẫn giữ số lượng khách gọi,
                // chỉ đổi trạng thái và đưa thành tiền về 0.
                update.setInt(2, quantity == 0 ? currentQuantity : quantity);
                update.setInt(3, quantity);
                update.setInt(4, quantity);
                update.setInt(5, orderItemId);
                if (update.executeUpdate() != 1) {
                    rollback();
                    return false;
                }
            }
            try (PreparedStatement total = connection.prepareStatement(
                    "UPDATE Orders SET total_amount = (SELECT COALESCE(SUM(line_total), 0) "
                    + "FROM OrderItems WHERE order_id = ?) WHERE order_id = ?")) {
                total.setInt(1, orderId);
                total.setInt(2, orderId);
                total.executeUpdate();
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

    /** Khách tự hủy đơn của mình khi còn chờ duyệt. */
    public boolean cancelCustomerOrder(int orderId, int ownerId, String reason, int actorId) {
        if (reason == null || reason.trim().isEmpty()) return false;
        return cancelPendingOrder(" AND user_id = ?",
                new int[]{orderId, ownerId}, actorId, reason);
    }

    public boolean cancelStaffOrder(int orderId, String reason, int actorId) {
        if (reason == null || reason.trim().isEmpty()) return false;
        return cancelPendingOrder("", new int[]{orderId}, actorId, reason);
    }

    /**
     * Hủy đơn và loại các món còn chờ duyệt trong cùng một transaction.
     * Nếu không cập nhật được đơn thì không thay đổi trạng thái món.
     */
    private boolean cancelPendingOrder(String ownerClause, int[] orderIds, int actorId, String reason) {
        if (connection == null) return false;
        String updateOrder = "UPDATE Orders SET order_status='CANCELLED', cancelled_by=?, cancelled_at=SYSDATETIME(), "
                + "cancellation_reason=?, closed_at=SYSDATETIME() WHERE order_id=? AND order_status='PENDING'" + ownerClause;
        try {
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement order = connection.prepareStatement(updateOrder)) {
                order.setInt(1, actorId);
                order.setNString(2, reason);
                for (int i = 0; i < orderIds.length; i++) order.setInt(i + 3, orderIds[i]);
                updated = order.executeUpdate();
            }
            if (updated != 1) {
                rollback();
                return false;
            }
            try (PreparedStatement items = connection.prepareStatement(
                    "UPDATE OrderItems SET item_status='REJECTED', line_total=0 "
                    + "WHERE order_id=? AND item_status='PENDING'")) {
                items.setInt(1, orderIds[0]);
                items.executeUpdate();
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

    private boolean execute(String sql, int... values) {
        if (connection == null) return false;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i=0;i<values.length;i++) ps.setInt(i+1, values[i]);
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
        i.setUnitPrice(rs.getBigDecimal("unit_price")); i.setQuantity(rs.getInt("quantity")); i.setLineTotal(rs.getBigDecimal("line_total"));
        i.setItemStatus(rs.getString("item_status")); i.setItemNote(rs.getNString("item_note")); i.setRequestedQuantity(rs.getInt("requested_quantity")); return i;
    }
    private Integer nullableInt(ResultSet rs, String column) throws SQLException { int v=rs.getInt(column); return rs.wasNull()?null:v; }
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.INTEGER); else ps.setInt(index,value);
    }
    private void rollback() { try { if (connection != null) connection.rollback(); } catch (SQLException ignored) { } }
    private void resetAutoCommit() { try { if (connection != null) connection.setAutoCommit(true); } catch (SQLException ignored) { } }
    @Override public void close() { try { if (connection != null) connection.close(); } catch (SQLException ignored) { } }
}
