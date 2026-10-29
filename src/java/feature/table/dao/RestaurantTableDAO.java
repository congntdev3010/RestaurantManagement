package feature.table.dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.RestaurantTable;

public class RestaurantTableDAO extends DBContext {

    // Dùng chung danh sách cột cho các truy vấn đọc bàn.
    private static final String TABLE_COLUMNS
            = "table_id, table_name, table_type, capacity, table_status, description, qr_token";

    // Chỉ cho phép giữ nguyên trạng thái hoặc chuyển giữa các trạng thái hợp lệ.
    private static final String STATUS_TRANSITION_CONDITION = "(table_status = ? "
            + "OR (? = 'AVAILABLE' AND table_status IN ('RESERVED', 'OCCUPIED')) "
            + "OR (? = 'RESERVED' AND table_status = 'AVAILABLE') "
            + "OR (? = 'OCCUPIED' AND table_status IN ('AVAILABLE', 'RESERVED')))";

    /**
     * Lấy danh sách bàn và sắp xếp theo khu vực rồi theo tên bàn.
     *
     * @throws SQLException nếu không có kết nối hoặc truy vấn thất bại
     */
    public List<RestaurantTable> getAllTables() throws SQLException {
        List<RestaurantTable> tables = new ArrayList<>();
        String sql = "SELECT " + TABLE_COLUMNS
                + " FROM RestaurantTables ORDER BY table_type, table_name";
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }

        // Đóng statement và result set ngay sau khi đã ánh xạ xong kết quả.
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                tables.add(readTable(results));
            }
        }
        return tables;
    }

    /**
     * Tìm bàn theo ID; trả về null nếu không tồn tại và ném SQLException nếu truy vấn lỗi.
     */
    public RestaurantTable getTableById(int tableId) throws SQLException {
        String sql = "SELECT " + TABLE_COLUMNS + " FROM RestaurantTables WHERE table_id = ?";
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tableId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readTable(results) : null;
            }
        }
    }

    /** Tìm bàn theo token QR; null nếu token sai. Không ném lỗi để trang khách xử lý nhẹ nhàng. */
    public RestaurantTable getTableByQrToken(String token) {
        if (token == null || token.length() != 36 || connection == null) {
            return null;
        }
        String sql = "SELECT " + TABLE_COLUMNS + " FROM RestaurantTables WHERE qr_token = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? readTable(results) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Thêm bàn mới bằng dữ liệu đã được servlet kiểm tra. */
    public boolean createTable(RestaurantTable table) {
        String sql = "INSERT INTO RestaurantTables (table_name, table_type, capacity, table_status, description) "
                + "VALUES (?, ?, ?, ?, ?)";
        return executeUpdate(sql, statement -> bindTable(statement, table));
    }

    /**
     * Cập nhật thông tin bàn (tên, loại, sức chứa, ghi chú).
     * KHÔNG đổi trạng thái: trạng thái bàn do nghiệp vụ order/thanh toán quản lý.
     */
    public boolean updateTable(RestaurantTable table) {
        String sql = "UPDATE RestaurantTables SET table_name = ?, table_type = ?, capacity = ?, description = ? "
                + "WHERE table_id = ?";
        return executeUpdate(sql, statement -> {
            statement.setNString(1, table.getTableName());
            statement.setNString(2, table.getTableType());
            statement.setInt(3, table.getCapacity());
            statement.setNString(4, table.getDescription());
            statement.setInt(5, table.getTableId());
        });
    }

    /** Chỉ cập nhật trạng thái, không thay đổi thông tin còn lại của bàn. */
    public boolean updateTableStatus(int tableId, String status) {
        String sql = "UPDATE RestaurantTables SET table_status = ? WHERE table_id = ? AND "
                + STATUS_TRANSITION_CONDITION;
        return executeUpdate(sql, statement -> {
            statement.setString(1, status);
            statement.setInt(2, tableId);
            bindTransitionStatus(statement, status, 3);
        });
    }

    /** Điều kiện SQL: bàn không còn order PENDING/OPEN nào. */
    private static final String NO_ACTIVE_ORDER = "NOT EXISTS (SELECT 1 FROM Orders o "
            + "WHERE o.table_id = RestaurantTables.table_id AND o.order_status IN ('PENDING', 'OPEN'))";

    /** Nhả bàn đang phục vụ nhưng không có order (khách đến rồi về). Bàn còn order thì trả về false. */
    public boolean releaseIfNoOrder(int tableId) {
        String sql = "UPDATE RestaurantTables SET table_status = 'AVAILABLE' "
                + "WHERE table_id = ? AND table_status = 'OCCUPIED' AND " + NO_ACTIVE_ORDER;
        return executeUpdate(sql, statement -> statement.setInt(1, tableId));
    }

    /** Tập id các bàn OCCUPIED không có order — dùng để hiện nút "Bàn trống". */
    public java.util.Set<Integer> getReleasableTableIds() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        if (connection == null) {
            return ids;
        }
        String sql = "SELECT table_id FROM RestaurantTables WHERE table_status = 'OCCUPIED' AND " + NO_ACTIVE_ORDER;
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("table_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    /** Mở bàn cho khách vãng lai: chỉ chuyển được khi bàn đang AVAILABLE/RESERVED, bàn đang phục vụ trả về false. */
    public boolean occupyIfFree(int tableId) {
        String sql = "UPDATE RestaurantTables SET table_status = 'OCCUPIED' "
                + "WHERE table_id = ? AND table_status IN ('AVAILABLE', 'RESERVED')";
        return executeUpdate(sql, statement -> statement.setInt(1, tableId));
    }

    /**
     * Gộp bàn cho khách vãng lai: mở TẤT CẢ các bàn trong một transaction.
     * Chỉ thành công khi mọi bàn đều đang AVAILABLE/RESERVED; một bàn bận thì không bàn nào được mở.
     */
    public boolean occupyAllIfFree(java.util.Collection<Integer> tableIds) {
        if (connection == null || tableIds.isEmpty()) {
            return false;
        }
        String sql = "UPDATE RestaurantTables SET table_status = 'OCCUPIED' "
                + "WHERE table_id = ? AND table_status IN ('AVAILABLE', 'RESERVED')";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int id : new java.util.LinkedHashSet<>(tableIds)) {
                    statement.setInt(1, id);
                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return false;
                    }
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Gắn trạng thái vào bốn placeholder của điều kiện chuyển trạng thái SQL.
     */
    private void bindTransitionStatus(PreparedStatement statement, String status, int firstIndex) throws SQLException {
        for (int index = firstIndex; index < firstIndex + 4; index++) {
            statement.setString(index, status);
        }
    }

    /**
     * Thực thi lệnh ghi với quản lý vòng đời PreparedStatement dùng chung.
     */
    private boolean executeUpdate(String sql, StatementBinder binder) {
        if (connection == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Bộ gắn tham số để dùng chung hàm thực thi các câu lệnh ghi. */
    @FunctionalInterface
    private interface StatementBinder {
        /** Gắn các giá trị vào placeholder của PreparedStatement. */
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Gắn năm trường dùng chung theo thứ tự placeholder của lệnh INSERT. */
    private void bindTable(PreparedStatement statement, RestaurantTable table) throws SQLException {
        statement.setNString(1, table.getTableName());
        statement.setNString(2, table.getTableType());
        statement.setInt(3, table.getCapacity());
        statement.setString(4, table.getStatus());
        statement.setNString(5, table.getDescription());
    }

    /** Ánh xạ một dòng kết quả SQL sang model bàn. */
    private RestaurantTable readTable(ResultSet results) throws SQLException {
        RestaurantTable table = new RestaurantTable();
        table.setTableId(results.getInt("table_id"));
        table.setTableName(results.getNString("table_name"));
        table.setTableType(results.getNString("table_type"));
        table.setCapacity(results.getInt("capacity"));
        table.setStatus(results.getString("table_status"));
        table.setDescription(results.getNString("description"));
        table.setQrToken(results.getString("qr_token"));
        return table;
    }
}
