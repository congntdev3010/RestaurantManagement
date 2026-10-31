package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.ManagerDashboard;
import model.ManagerDashboard.CategorySales;
import model.ManagerDashboard.DishSales;

public class ManagerDashboardDAO extends DBContext {
    public ManagerDashboard getDashboard() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }

        ManagerDashboard dashboard = new ManagerDashboard();
        loadSummary(dashboard);
        loadCustomerCounts(dashboard);
        loadBestSellers(dashboard);
        loadLeastSold(dashboard);
        return dashboard;
    }

    private void loadSummary(ManagerDashboard dashboard) throws SQLException {
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN o.order_status = 'PAID' "
                + "AND COALESCE(o.paid_at, o.closed_at) >= CONVERT(date, GETDATE()) "
                + "AND COALESCE(o.paid_at, o.closed_at) < DATEADD(day, 1, CONVERT(date, GETDATE())) "
                + "THEN o.total_amount ELSE 0 END), 0) AS today_revenue, "
                + "COALESCE(SUM(CASE WHEN o.order_status = 'PAID' "
                + "AND COALESCE(o.paid_at, o.closed_at) >= DATEADD(day, -1, CONVERT(date, GETDATE())) "
                + "AND COALESCE(o.paid_at, o.closed_at) < CONVERT(date, GETDATE()) "
                + "THEN o.total_amount ELSE 0 END), 0) AS yesterday_revenue, "
                + "COALESCE(SUM(CASE WHEN o.order_status <> 'CANCELLED' "
                + "AND o.created_at >= CONVERT(date, GETDATE()) "
                + "AND o.created_at < DATEADD(day, 1, CONVERT(date, GETDATE())) "
                + "THEN 1 ELSE 0 END), 0) AS today_order_count "
                + "FROM dbo.Orders o";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setTodayRevenue(results.getBigDecimal("today_revenue"));
                dashboard.setYesterdayRevenue(results.getBigDecimal("yesterday_revenue"));
                dashboard.setTodayOrderCount(results.getInt("today_order_count"));
            }
        }

        String tableSql = "SELECT COUNT(*) AS total_tables, "
                + "COALESCE(SUM(CASE WHEN table_status = 'OCCUPIED' THEN 1 ELSE 0 END), 0) AS occupied_tables "
                + "FROM dbo.RestaurantTables";
        try (PreparedStatement statement = connection.prepareStatement(tableSql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setTotalTableCount(results.getInt("total_tables"));
                dashboard.setOccupiedTableCount(results.getInt("occupied_tables"));
            }
        }
    }

    private void loadCustomerCounts(ManagerDashboard dashboard) throws SQLException {
        String sql = "WITH customer_orders AS ("
                + "SELECT CASE "
                + "WHEN o.user_id IS NOT NULL THEN CONCAT('U:', o.user_id) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_email)), '') IS NOT NULL "
                + "THEN CONCAT('E:', LOWER(LTRIM(RTRIM(o.customer_email)))) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_phone)), '') IS NOT NULL "
                + "THEN CONCAT('P:', LTRIM(RTRIM(o.customer_phone))) "
                + "ELSE CONCAT('O:', o.order_id) END AS customer_key, "
                + "MIN(o.created_at) AS first_order_at, "
                + "MAX(CASE WHEN o.created_at >= CONVERT(date, GETDATE()) "
                + "AND o.created_at < DATEADD(day, 1, CONVERT(date, GETDATE())) THEN 1 ELSE 0 END) AS ordered_today "
                + "FROM dbo.Orders o WHERE o.order_status <> 'CANCELLED' "
                + "GROUP BY CASE "
                + "WHEN o.user_id IS NOT NULL THEN CONCAT('U:', o.user_id) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_email)), '') IS NOT NULL "
                + "THEN CONCAT('E:', LOWER(LTRIM(RTRIM(o.customer_email)))) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_phone)), '') IS NOT NULL "
                + "THEN CONCAT('P:', LTRIM(RTRIM(o.customer_phone))) "
                + "ELSE CONCAT('O:', o.order_id) END) "
                + "SELECT COALESCE(SUM(CASE WHEN ordered_today = 1 "
                + "AND first_order_at >= CONVERT(date, GETDATE()) THEN 1 ELSE 0 END), 0) AS new_customers, "
                + "COALESCE(SUM(CASE WHEN ordered_today = 1 "
                + "AND first_order_at < CONVERT(date, GETDATE()) THEN 1 ELSE 0 END), 0) AS returning_customers "
                + "FROM customer_orders";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setNewCustomerCount(results.getInt("new_customers"));
                dashboard.setReturningCustomerCount(results.getInt("returning_customers"));
            }
        }
    }

    private void loadBestSellers(ManagerDashboard dashboard) throws SQLException {
        Map<Integer, CategorySales> categories = new LinkedHashMap<>();
        String categorySql = "SELECT category_id, category_name FROM dbo.Category "
                + "WHERE status = 1 ORDER BY category_id";
        try (PreparedStatement statement = connection.prepareStatement(categorySql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                CategorySales category = new CategorySales(results.getNString("category_name"));
                categories.put(results.getInt("category_id"), category);
                dashboard.getCategorySales().add(category);
            }
        }

        String sql = "WITH daily_sales AS ("
                + "SELECT oi.dish_id, SUM(oi.quantity) AS quantity "
                + "FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.order_id = oi.order_id "
                + "WHERE o.order_status <> 'CANCELLED' "
                + "AND o.created_at >= CONVERT(date, GETDATE()) "
                + "AND o.created_at < DATEADD(day, 1, CONVERT(date, GETDATE())) "
                + "GROUP BY oi.dish_id), dish_sales AS ("
                + "SELECT c.category_id, c.category_name, d.dish_name, d.unit, "
                + "COALESCE(ds.quantity, 0) AS quantity, "
                + "ROW_NUMBER() OVER (PARTITION BY c.category_id "
                + "ORDER BY COALESCE(ds.quantity, 0) DESC, d.dish_name) AS rank_in_category "
                + "FROM dbo.Category c JOIN dbo.Dish d ON d.category_id = c.category_id "
                + "LEFT JOIN daily_sales ds ON ds.dish_id = d.dish_id "
                + "WHERE c.status = 1 AND d.status = 1) "
                + "SELECT category_id, category_name, dish_name, unit, quantity "
                + "FROM dish_sales WHERE rank_in_category <= 5 AND quantity > 0 "
                + "ORDER BY category_id, rank_in_category";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                CategorySales category = categories.get(results.getInt("category_id"));
                if (category != null) {
                    category.getDishes().add(new DishSales(results.getNString("dish_name"),
                            results.getNString("category_name"), results.getNString("unit"),
                            results.getInt("quantity")));
                }
            }
        }
    }

    private void loadLeastSold(ManagerDashboard dashboard) throws SQLException {
        String sql = "WITH dish_sales AS ("
                + "SELECT oi.dish_id, SUM(oi.quantity) AS quantity "
                + "FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.order_id = oi.order_id "
                + "WHERE o.order_status <> 'CANCELLED' "
                + "AND o.created_at >= CONVERT(date, GETDATE()) "
                + "AND o.created_at < DATEADD(day, 1, CONVERT(date, GETDATE())) "
                + "GROUP BY oi.dish_id) "
                + "SELECT TOP (5) d.dish_name, c.category_name, d.unit, "
                + "COALESCE(ds.quantity, 0) AS quantity "
                + "FROM dbo.Dish d JOIN dbo.Category c ON c.category_id = d.category_id "
                + "LEFT JOIN dish_sales ds ON ds.dish_id = d.dish_id "
                + "WHERE d.status = 1 AND c.status = 1 "
                + "ORDER BY COALESCE(ds.quantity, 0), d.dish_name";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                dashboard.getLeastSoldDishes().add(new DishSales(results.getNString("dish_name"),
                        results.getNString("category_name"), results.getNString("unit"),
                        results.getInt("quantity")));
            }
        }
    }
}
