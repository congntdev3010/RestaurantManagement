package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.RestaurantTable;

public class RestaurantTableDAO extends DBContext {

    public List<RestaurantTable> getAllTables() {
        List<RestaurantTable> tables = new ArrayList<>();
        String sql = "SELECT table_id, table_name, table_type, capacity, table_status, description "
                + "FROM RestaurantTables ORDER BY table_type, table_name";
        try {
            if (connection == null) {
                return tables;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet results = statement.executeQuery();
            while (results.next()) {
                tables.add(readTable(results));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    public RestaurantTable getTableById(int tableId) {
        String sql = "SELECT table_id, table_name, table_type, capacity, table_status, description "
                + "FROM RestaurantTables WHERE table_id = ?";
        try {
            if (connection == null) {
                return null;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, tableId);
            ResultSet results = statement.executeQuery();
            return results.next() ? readTable(results) : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createTable(RestaurantTable table) {
        String sql = "INSERT INTO RestaurantTables (table_name, table_type, capacity, table_status, description) "
                + "VALUES (?, ?, ?, ?, ?)";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            bindTable(statement, table);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTable(RestaurantTable table) {
        String sql = "UPDATE RestaurantTables SET table_name = ?, table_type = ?, capacity = ?, "
                + "table_status = ?, description = ? WHERE table_id = ?";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            bindTable(statement, table);
            statement.setInt(6, table.getTableId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTableStatus(int tableId, String status) {
        String sql = "UPDATE RestaurantTables SET table_status = ? WHERE table_id = ?";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, status);
            statement.setInt(2, tableId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteTable(int tableId) {
        String sql = "DELETE FROM RestaurantTables WHERE table_id = ?";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, tableId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void bindTable(PreparedStatement statement, RestaurantTable table) throws SQLException {
        statement.setNString(1, table.getTableName());
        statement.setNString(2, table.getTableType());
        statement.setInt(3, table.getCapacity());
        statement.setString(4, table.getStatus());
        statement.setNString(5, table.getDescription());
    }

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
