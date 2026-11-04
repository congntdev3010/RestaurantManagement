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
import model.ManagerDashboard.TableTurnover;

public class ManagerDashboardDAO extends DBContext {
    public ManagerDashboard getDashboard() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }

        ManagerDashboard dashboard = new ManagerDashboard();
        loadSummary(dashboard);
        loadCustomerCounts(dashboard);
        loadCancellationStats(dashboard);
        loadTablePerformance(dashboard);
        loadLoyaltyStats(dashboard);
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

    private void loadCancellationStats(ManagerDashboard dashboard) throws SQLException {
        String sql = "SELECT COUNT(*) AS all_orders, "
                + "COALESCE(SUM(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_orders "
                + "FROM dbo.Orders "
                + "WHERE created_at >= CONVERT(date, GETDATE()) "
                + "AND created_at < DATEADD(day, 1, CONVERT(date, GETDATE()))";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setAllTodayOrderCount(results.getInt("all_orders"));
                dashboard.setCancelledOrderCount(results.getInt("cancelled_orders"));
            }
        }
    }

    private void loadTablePerformance(ManagerDashboard dashboard) throws SQLException {
        String dailySql = "SELECT COUNT(*) AS service_count, "
                + "AVG(CASE WHEN checked_in_at IS NOT NULL AND paid_at >= checked_in_at "
                + "THEN CAST(DATEDIFF(SECOND, checked_in_at, paid_at) AS DECIMAL(12,2)) / 60 END) "
                + "AS average_service_minutes "
                + "FROM dbo.ServiceHistory "
                + "WHERE paid_at >= CONVERT(date, GETDATE()) "
                + "AND paid_at < DATEADD(day, 1, CONVERT(date, GETDATE()))";
        try (PreparedStatement statement = connection.prepareStatement(dailySql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setTodayServiceCount(results.getInt("service_count"));
                dashboard.setAverageServiceMinutes(results.getBigDecimal("average_service_minutes"));
            }
        }

        String tableSql = "SELECT TOP (5) t.table_name, COUNT(*) AS service_count "
                + "FROM dbo.ServiceHistory h JOIN dbo.RestaurantTables t ON t.table_id = h.table_id "
                + "WHERE h.paid_at >= CONVERT(date, GETDATE()) "
                + "AND h.paid_at < DATEADD(day, 1, CONVERT(date, GETDATE())) "
                + "GROUP BY t.table_id, t.table_name "
                + "ORDER BY COUNT(*) DESC, t.table_name";
        try (PreparedStatement statement = connection.prepareStatement(tableSql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                dashboard.getBusiestTables().add(new TableTurnover(
                        results.getNString("table_name"), results.getInt("service_count")));
            }
        }
    }

    private void loadLoyaltyStats(ManagerDashboard dashboard) throws SQLException {
        String historySql = "SELECT COUNT(DISTINCT loyalty_customer_id) AS loyalty_customers, "
                + "COALESCE(SUM(loyalty_points_earned), 0) AS points_earned, "
                + "COALESCE(SUM(loyalty_points_used), 0) AS points_used "
                + "FROM dbo.ServiceHistory "
                + "WHERE paid_at >= CONVERT(date, GETDATE()) "
                + "AND paid_at < DATEADD(day, 1, CONVERT(date, GETDATE()))";
        try (PreparedStatement statement = connection.prepareStatement(historySql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setLoyaltyCustomerCount(results.getInt("loyalty_customers"));
                dashboard.setLoyaltyPointsEarnedToday(results.getInt("points_earned"));
                dashboard.setLoyaltyPointsUsedToday(results.getInt("points_used"));
            }
        }

        String balanceSql = "SELECT COALESCE(SUM(u.loyalty_points), 0) AS outstanding_points "
                + "FROM dbo.Users u JOIN dbo.Roles r ON r.role_id = u.role_id "
                + "WHERE r.role_name = 'Customer' AND u.is_deleted = 0";
        try (PreparedStatement statement = connection.prepareStatement(balanceSql);
                ResultSet results = statement.executeQuery()) {
            if (results.next()) {
                dashboard.setOutstandingLoyaltyPoints(results.getInt("outstanding_points"));
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
