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
import model.TableBill;
import model.TableBillItem;

/**
 * Luồng đơn: PENDING (chờ server duyệt) -> OPEN (đang phục vụ) -> PAID /
 * CANCELLED. Luồng món: PENDING -> COOKING (đã duyệt, hiện trên màn hình bếp)
 * -> SERVED; hoặc REJECTED (hết món).
 */
public class OrderDAO extends DBContext implements AutoCloseable {

    private String lastPaymentFailureReason;

    public static final int APPROVE_FAILED = -1;
    public static final int APPROVE_ORDER_CANCELLED = 0;
    public static final int APPROVE_ORDER_OPENED = 1;

    public int createOrder(Integer customerId, Integer createdBy, Integer tableId, Integer reservationId,
            String status, String customerName, String customerPhone, String customerEmail,
            String note, Map<Integer, Integer> quantities) {
        return createOrder(customerId, createdBy, tableId, reservationId, status, customerName,
                customerPhone, customerEmail, note, quantities, null);
    }

    /**
     * Tạo đơn mới. Mọi món vào ở trạng thái PENDING; quantities là số lượng
     * khách/server yêu cầu.
     */
    public int createOrder(Integer customerId, Integer createdBy, Integer tableId, Integer reservationId,
            String status, String customerName, String customerPhone, String customerEmail,
            String note, Map<Integer, Integer> quantities, Map<Integer, String> itemNotes) {
        if (connection == null || quantities == null || quantities.isEmpty()) {
            return -1;
        }
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
                    if (!keys.next()) {
                        throw new SQLException("Could not create order key.");
                    }
                    orderId = keys.getInt(1);
                }
            }
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
                int quantity = entry.getValue() == null ? 0 : entry.getValue();
                if (entry.getKey() == null || entry.getKey() < 1 || quantity < 1 || quantity > 99) {
                    throw new SQLException("Invalid order item.");
                }
                try (PreparedStatement dish = connection.prepareStatement(
                        "SELECT dish_name, unit, price FROM Dish WITH (UPDLOCK, HOLDLOCK) WHERE dish_id = ? AND status = 1")) {
                    dish.setInt(1, entry.getKey());
                    try (ResultSet rs = dish.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("A dish is unavailable.");
                        }
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
            if (total.precision() > 12) {
                throw new SQLException("Order total exceeds supported amount.");
            }
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
                    if (!rs.next()) {
                        throw new SQLException("Table does not exist.");
                    }
                    String tableStatus = rs.getString("table_status");
                    if (!"AVAILABLE".equals(tableStatus) && !"RESERVED".equals(tableStatus) && !"OCCUPIED".equals(tableStatus)) {
                        throw new SQLException("Table is not available for service.");
                    }
                }
            }
        }
        if (reservationId != null) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT table_id FROM Reservations WITH (UPDLOCK, HOLDLOCK) WHERE reservation_id = ? AND reservation_status = 'APPROVED'")) {
                ps.setInt(1, reservationId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Reservation is not approved.");
                    }
                    int reservedTable = rs.getInt("table_id");
                    boolean hasTable = !rs.wasNull();
                    if (hasTable && (tableId == null || reservedTable != tableId)) {
                        throw new SQLException("Order table must match its reservation.");
                    }
                }
            }
        }
    }

    public List<Order> getOrders(Integer ownerId, String statusFilter) {
        List<Order> orders = new ArrayList<>();
        if (connection == null) {
            return orders;
        }
        StringBuilder sql = new StringBuilder("SELECT o.*, t.table_name, (SELECT COUNT(*) FROM OrderItems x "
                + "WHERE x.order_id = o.order_id AND x.item_status = 'COOKING') AS cooking_count FROM Orders o "
                + "LEFT JOIN RestaurantTables t ON t.table_id=o.table_id WHERE 1=1 ");
        if (ownerId != null) {
            sql.append("AND o.user_id = ? ");
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND o.order_status = ? ");
        }
        // Order hủy đã được chốt khi bàn thanh toán (settled_at): giữ trong DB nhưng
        // loại khỏi danh sách vận hành "Đã hủy".
        if (ownerId == null && (statusFilter == null || "CANCELLED".equals(statusFilter))) {
            sql.append("AND NOT (o.order_status = 'CANCELLED' AND o.settled_at IS NOT NULL) ");
        }
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
            if (ownerId != null) {
                ps.setInt(i++, ownerId);
            }
            if (statusFilter != null && !statusFilter.isEmpty()) {
                ps.setString(i, statusFilter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = readOrder(rs);
                    o.setCookingCount(rs.getInt("cooking_count"));
                    orders.add(o);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Đọc đơn kèm món. ownerId = null nghĩa là không ràng buộc chủ đơn (nhân
     * viên, hoặc khách đã được servlet kiểm bằng session).
     */
    public Order getOrder(int orderId, Integer ownerId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT o.*, t.table_name FROM Orders o LEFT JOIN RestaurantTables t ON t.table_id=o.table_id "
                + "WHERE o.order_id = ?" + (ownerId == null ? "" : " AND o.user_id = ?");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            if (ownerId != null) {
                ps.setInt(2, ownerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Order order = readOrder(rs);
                try (PreparedStatement lines = connection.prepareStatement(
                        "SELECT * FROM OrderItems WHERE order_id = ? ORDER BY order_item_id")) {
                    lines.setInt(1, orderId);
                    try (ResultSet itemRs = lines.executeQuery()) {
                        while (itemRs.next()) {
                            order.getItems().add(readItem(itemRs));
                        }
                    }
                }
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Server duyệt từng món của đơn PENDING. approvedQuantities: orderItemId ->
     * số lượng phục vụ được. Món không có trong map (bỏ tick) hoặc số lượng < 1
     * bị REJECTED; số lượng lớn hơn khách gọi bị chặn xuống bằng số gọi. Còn ít
     * nhất một món COOKING thì đơn thành OPEN; nếu không còn món nào thì đơn tự
     * hủy.
     */
    public int approveItems(int orderId, int actorId, Map<Integer, Integer> approvedQuantities) {
        if (connection == null) {
            return APPROVE_FAILED;
        }
        try {
            connection.setAutoCommit(false);
            Integer tableId;
            try (PreparedStatement guard = connection.prepareStatement(
                    "SELECT table_id FROM Orders WITH (UPDLOCK, HOLDLOCK) WHERE order_id = ? AND order_status = 'PENDING'")) {
                guard.setInt(1, orderId);
                try (ResultSet rs = guard.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Order is not awaiting approval.");
                    }
                    tableId = nullableInt(rs, "table_id");
                }
            }
            List<int[]> pending = new ArrayList<>();
            try (PreparedStatement items = connection.prepareStatement(
                    "SELECT order_item_id, requested_quantity FROM OrderItems WHERE order_id = ? AND item_status = 'PENDING'")) {
                items.setInt(1, orderId);
                try (ResultSet rs = items.executeQuery()) {
                    while (rs.next()) {
                        pending.add(new int[]{rs.getInt(1), rs.getInt(2)});
                    }
                }
            }
            if (pending.isEmpty()) {
                throw new SQLException("Order has no pending items.");
            }
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

    /**
     * Server đánh dấu một món đang nấu là đã phục vụ.
     */
    public boolean markItemServed(int orderId, int orderItemId) {
        return execute("UPDATE oi SET item_status = 'SERVED' FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                + "WHERE oi.order_item_id = ? AND oi.order_id = ? AND oi.item_status = 'COOKING' AND o.order_status = 'OPEN'",
                orderItemId, orderId);
    }

    /**
     * Lễ tân chốt thanh toán khi mọi món đã phục vụ (hoặc bị từ chối), rồi giải
     * phóng bàn.
     */
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

    public TableBill getTableBill(int tableId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT COUNT(*) AS order_count, COALESCE(SUM(o.total_amount), 0) AS total_amount, "
                + "COALESCE((SELECT COUNT(*) FROM OrderItems oi JOIN Orders ox ON ox.order_id = oi.order_id "
                + "WHERE ox.table_id = ? AND ox.order_status IN ('PENDING', 'OPEN') "
                + "AND oi.item_status IN ('PENDING', 'COOKING')), 0) "
                + "+ CASE WHEN EXISTS (SELECT 1 FROM Orders op WHERE op.table_id = ? AND op.order_status = 'PENDING' "
                + "AND EXISTS (SELECT 1 FROM OrderItems opi WHERE opi.order_id = op.order_id "
                + "AND opi.item_status <> 'REJECTED')) "
                + "THEN 1 ELSE 0 END AS unfinished_count "
                + "FROM Orders o WHERE o.table_id = ? AND o.order_status = 'OPEN'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setInt(2, tableId);
            ps.setInt(3, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("order_count") == 0) {
                    clearStaleLoyaltyLink(tableId);
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
                applyLoyalty(bill);
                return bill;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Closes legacy OPEN orders whose every item has been rejected and total is
     * zero.
     */
    public void cancelZeroValueOpenOrders() {
        if (connection == null) {
            return;
        }
        String sql = "UPDATE Orders SET order_status = 'CANCELLED', closed_at = SYSDATETIME(), "
                + "cancelled_at = SYSDATETIME(), cancellation_reason = ? "
                + "WHERE order_status IN ('PENDING', 'OPEN') AND total_amount <= 0 "
                + "AND NOT EXISTS (SELECT 1 FROM OrderItems "
                + "WHERE order_id = Orders.order_id AND item_status IN ('PENDING', 'COOKING'))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setNString(1, "Tất cả món đã bị bếp hủy; order không phát sinh tiền.");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        if ("CASH".equals(paymentMethod) && (cashReceived == null || cashChange == null || cashReceived.compareTo(cashChange) < 0)) {
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
            int historyId;
            try (PreparedStatement history = connection.prepareStatement(
                    "INSERT INTO ServiceHistory (table_id, customer_name, customer_phone, customer_email, reservation_time, "
                    + "checked_in_at, paid_at, payment_method, total_amount, cash_received, cash_change, paid_by, "
                    + "loyalty_customer_id, loyalty_points_used, loyalty_discount, loyalty_points_earned) "
                    + "SELECT ?, MAX(o.customer_name), MAX(o.customer_phone), MAX(o.customer_email), "
                    + "MIN(CASE WHEN r.reservation_date IS NOT NULL AND r.reservation_time IS NOT NULL "
                    + "THEN DATEADD(SECOND, DATEDIFF(SECOND, '00:00:00', r.reservation_time), "
                    + "CAST(r.reservation_date AS DATETIME2)) END), MAX(r.checked_in_at), SYSDATETIME(), ?, ?, ?, ?, ?, ?, ?, ?, ? "
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
                history.setInt(11, tableId);
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

    private void clearStaleLoyaltyLink(int tableId) {
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
        bill.setPayableAmount(bill.getTotalAmount());
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
                int maxByTotal = bill.getTotalAmount().divideToIntegralValue(redeem).intValue();
                int use = Math.max(0, Math.min(rs.getInt("points_to_use"), Math.min(balance, maxByTotal)));
                BigDecimal discount = redeem.multiply(BigDecimal.valueOf(use));
                BigDecimal payable = bill.getTotalAmount().subtract(discount);
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
                bill.setPointsToEarn(payable.signum() > 0
                        ? payable.divideToIntegralValue(spend).intValue() * earnPoints : 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public boolean markQrTablePaid(int tableId, int actorId, String reference, BigDecimal transferAmount) {
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

    public TableBill getQrReceipt(int tableId, String reference) throws SQLException {
        String sql = "SELECT TOP 1 h.history_id, h.table_id, h.customer_name, h.customer_phone, h.customer_email, "
                + "h.reservation_time, h.checked_in_at, h.payment_method, h.total_amount, "
                + "h.cash_received, h.cash_change, h.loyalty_points_used, h.loyalty_discount, h.loyalty_points_earned "
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
                bill.setPayableAmount(bill.getTotalAmount().subtract(bill.getDiscountAmount()));
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

    public String getLastPaymentFailureReason() {
        return lastPaymentFailureReason;
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

    /**
     * Bàn này còn đơn đang chờ duyệt không (còn thì chưa được order thêm).
     */
    public boolean hasPendingOrder(int tableId) {
        if (connection == null) {
            return false;
        }
        String sql = "SELECT 1 FROM Orders WHERE table_id = ? AND order_status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy order đang chờ duyệt của một bàn để Server xem trực tiếp từ sơ đồ
     * bàn.
     */
    public Order getPendingOrderForTable(int tableId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT TOP 1 o.*, t.table_name FROM Orders o "
                + "LEFT JOIN RestaurantTables t ON t.table_id = o.table_id "
                + "WHERE o.table_id = ? AND o.order_status = 'PENDING' "
                + "ORDER BY o.created_at, o.order_id";
        int pendingOrderId = -1;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    pendingOrderId = rs.getInt("order_id");
                }
            }
            return pendingOrderId > 0 ? getOrder(pendingOrderId, null) : null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Các món đã được duyệt (đang nấu / đã phục vụ) trong những đơn đang phục
     * vụ của một bàn, theo thứ tự gọi.
     */
    public List<OrderItem> getApprovedItemsForTable(int tableId) {
        List<OrderItem> items = new ArrayList<>();
        if (connection == null) {
            return items;
        }
        String sql = "SELECT oi.* FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                + "WHERE o.table_id = ? AND o.order_status = 'OPEN' AND oi.item_status IN ('COOKING', 'SERVED') "
                + "ORDER BY o.created_at, oi.order_item_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(readItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Màn hình bếp: các món đã duyệt, chưa phục vụ, theo thứ tự duyệt.
     */
    public List<OrderItem> getKitchenItems() {
        List<OrderItem> items = new ArrayList<>();
        if (connection == null) {
            return items;
        }
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Server/manager điều chỉnh món đang nấu khi bếp phát hiện không thể phục
     * vụ đủ. quantity = 0 nghĩa là hủy món; quantity > 0 là số lượng thực tế
     * còn phục vụ được.
     */
    public boolean updateKitchenItem(int orderItemId, int quantity) {
        if (connection == null || quantity < 0) {
            return false;
        }
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
            try (PreparedStatement cancelEmpty = connection.prepareStatement(
                    "UPDATE Orders SET order_status = 'CANCELLED', closed_at = SYSDATETIME(), "
                    + "cancelled_at = SYSDATETIME(), cancellation_reason = ? "
                    + "WHERE order_id = ? AND order_status = 'OPEN' "
                    + "AND total_amount <= 0 "
                    + "AND NOT EXISTS (SELECT 1 FROM OrderItems "
                    + "WHERE order_id = Orders.order_id AND item_status IN ('PENDING', 'COOKING'))")) {
                cancelEmpty.setNString(1, "Tất cả món đã bị bếp hủy; order không phát sinh tiền.");
                cancelEmpty.setInt(2, orderId);
                cancelEmpty.executeUpdate();
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
     * Khách tự hủy đơn của mình khi còn chờ duyệt.
     */
    public boolean cancelCustomerOrder(int orderId, int ownerId, String reason, int actorId) {
        if (reason == null || reason.trim().isEmpty()) {
            return false;
        }
        return cancelPendingOrder(" AND user_id = ?",
                new int[]{orderId, ownerId}, actorId, reason);
    }

    public boolean cancelStaffOrder(int orderId, String reason, int actorId) {
        if (reason == null || reason.trim().isEmpty()) {
            return false;
        }
        return cancelPendingOrder("", new int[]{orderId}, actorId, reason);
    }

    /**
     * Hủy đơn và loại các món còn chờ duyệt trong cùng một transaction. Nếu
     * không cập nhật được đơn thì không thay đổi trạng thái món.
     */
    private boolean cancelPendingOrder(String ownerClause, int[] orderIds, int actorId, String reason) {
        if (connection == null) {
            return false;
        }
        String updateOrder = "UPDATE Orders SET order_status='CANCELLED', cancelled_by=?, cancelled_at=SYSDATETIME(), "
                + "cancellation_reason=?, closed_at=SYSDATETIME() WHERE order_id=? AND order_status='PENDING'" + ownerClause;
        try {
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement order = connection.prepareStatement(updateOrder)) {
                order.setInt(1, actorId);
                order.setNString(2, reason);
                for (int i = 0; i < orderIds.length; i++) {
                    order.setInt(i + 3, orderIds[i]);
                }
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
        if (connection == null) {
            return false;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setInt(i + 1, values[i]);
            }
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Order readOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setReservationId(nullableInt(rs, "reservation_id"));
        o.setTableId(nullableInt(rs, "table_id"));
        o.setTableName(rs.getNString("table_name"));
        o.setUserId(nullableInt(rs, "user_id"));
        o.setCreatedBy(nullableInt(rs, "created_by"));
        o.setCustomerName(rs.getNString("customer_name"));
        o.setCustomerPhone(rs.getString("customer_phone"));
        o.setCustomerEmail(rs.getString("customer_email"));
        o.setStatus(rs.getString("order_status"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setNote(rs.getNString("note"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        o.setClosedAt(rs.getTimestamp("closed_at"));
        o.setApprovedBy(nullableInt(rs, "approved_by"));
        o.setApprovedAt(rs.getTimestamp("approved_at"));
        o.setCancelledBy(nullableInt(rs, "cancelled_by"));
        o.setCancelledAt(rs.getTimestamp("cancelled_at"));
        o.setCancellationReason(rs.getNString("cancellation_reason"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaidBy(nullableInt(rs, "paid_by"));
        o.setPaidAt(rs.getTimestamp("paid_at"));
        o.setPaymentReference(rs.getNString("payment_reference"));
        return o;
    }

    private OrderItem readItem(ResultSet rs) throws SQLException {
        OrderItem i = new OrderItem();
        i.setOrderItemId(rs.getInt("order_item_id"));
        i.setOrderId(rs.getInt("order_id"));
        i.setDishId(nullableInt(rs, "dish_id"));
        i.setDishName(rs.getNString("dish_name"));
        i.setUnit(rs.getNString("unit"));
        i.setUnitPrice(rs.getBigDecimal("unit_price"));
        i.setQuantity(rs.getInt("quantity"));
        i.setLineTotal(rs.getBigDecimal("line_total"));
        i.setItemStatus(rs.getString("item_status"));
        i.setItemNote(rs.getNString("item_note"));
        i.setRequestedQuantity(rs.getInt("requested_quantity"));
        return i;
    }

    private Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
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
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
