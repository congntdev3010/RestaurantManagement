package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.RestaurantTable;

public class RestaurantTableDAO extends DBContext {

    private static final String STATUS_TRANSITION_CONDITION = "(table_status = ? "
            + "OR (? = 'AVAILABLE' AND table_status IN ('RESERVED', 'OCCUPIED', 'UNAVAILABLE')) "
            + "OR (? = 'RESERVED' AND table_status IN ('AVAILABLE', 'UNAVAILABLE')) "
            + "OR (? = 'OCCUPIED' AND table_status IN ('AVAILABLE', 'RESERVED', 'UNAVAILABLE')))";

    // Trả về toàn bộ bàn, sắp xếp theo khu vực rồi theo mã bàn.
    public List<RestaurantTable> getAllTables() throws SQLException {
        List<RestaurantTable> tables = new ArrayList<>();
        String sql = "SELECT table_id, table_name, table_type, capacity, "
            + "CASE WHEN table_status = 'UNAVAILABLE' THEN 'AVAILABLE' ELSE table_status END AS table_status, description "
                + "FROM RestaurantTables ORDER BY table_type, table_name";
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                tables.add(readTable(results));
            }
        }
        return tables;
    }

    // Tìm một bàn theo ID, hoặc trả về null nếu không tồn tại.
    public RestaurantTable getTableById(int tableId) {
        String sql = "SELECT table_id, table_name, table_type, capacity, "
            + "CASE WHEN table_status = 'UNAVAILABLE' THEN 'AVAILABLE' ELSE table_status END AS table_status, description "
                + "FROM RestaurantTables WHERE table_id = ?";
        try {
            if (connection == null) {
                return null;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, tableId);
                try (ResultSet results = statement.executeQuery()) {
                    return results.next() ? readTable(results) : null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Thêm bàn mới bằng các giá trị đã được servlet kiểm tra.
    public boolean createTable(RestaurantTable table) {
        String sql = "INSERT INTO RestaurantTables (table_name, table_type, capacity, table_status, description) "
                + "VALUES (?, ?, ?, ?, ?)";
        return executeUpdate(sql, statement -> bindTable(statement, table));
    }

    // Cập nhật thông tin bàn; ID được bind riêng sau các trường còn lại.
    public boolean updateTable(RestaurantTable table) {
        String sql = "UPDATE RestaurantTables SET table_name = ?, table_type = ?, capacity = ?, "
                + "table_status = ?, description = ? WHERE table_id = ? AND " + STATUS_TRANSITION_CONDITION;
        return executeUpdate(sql, statement -> {
            bindTable(statement, table);
            statement.setInt(6, table.getTableId());
            bindTransitionStatus(statement, table.getStatus(), 7);
        });
    }

    // Chỉ cập nhật trạng thái mà không thay đổi thông tin khác của bàn.
    public boolean updateTableStatus(int tableId, String status) {
        String sql = "UPDATE RestaurantTables SET table_status = ? WHERE table_id = ? AND "
                + STATUS_TRANSITION_CONDITION;
        return executeUpdate(sql, statement -> {
            statement.setString(1, status);
            statement.setInt(2, tableId);
            bindTransitionStatus(statement, status, 3);
        });
    }

    // Chỉ xóa bàn đang trống.
    public boolean deleteTable(int tableId) {
        String sql = "DELETE FROM RestaurantTables WHERE table_id = ? AND table_status IN ('AVAILABLE', 'UNAVAILABLE')";
        return executeUpdate(sql, statement -> statement.setInt(1, tableId));
    }

    private void bindTransitionStatus(PreparedStatement statement, String status, int firstIndex) throws SQLException {
        for (int index = firstIndex; index < firstIndex + 4; index++) {
            statement.setString(index, status);
        }
    }

    // Dùng chung phần mở/đóng statement và xử lý lỗi cho các lệnh ghi.
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

    // Gắn tham số của từng lệnh ghi vào PreparedStatement.
    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    // Gắn các trường dùng chung của bàn theo đúng thứ tự placeholder SQL.
    private void bindTable(PreparedStatement statement, RestaurantTable table) throws SQLException {
        statement.setNString(1, table.getTableName());
        statement.setNString(2, table.getTableType());
        statement.setInt(3, table.getCapacity());
        statement.setString(4, table.getStatus());
        statement.setNString(5, table.getDescription());
    }

    // Chuyển một dòng kết quả SQL thành đối tượng bàn.
    private RestaurantTable readTable(ResultSet results) throws SQLException {
        RestaurantTable table = new RestaurantTable();
        table.setTableId(results.getInt("table_id"));
        table.setTableName(results.getNString("table_name"));
        table.setTableType(results.getNString("table_type"));
        table.setCapacity(results.getInt("capacity"));
        table.setStatus(results.getString("table_status"));
        table.setDescription(results.getNString("description"));
        return table;
    }
}
