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
    public ManagerDashboard getDashboard(String selectedPeriod) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }

        ManagerDashboard dashboard = new ManagerDashboard();
        setPeriod(dashboard, selectedPeriod);
        loadSummary(dashboard);
        loadCustomerCounts(dashboard);
        loadCancellationStats(dashboard);
        loadTablePerformance(dashboard);
        loadLoyaltyStats(dashboard);
        loadBestSellers(dashboard);
        loadLeastSold(dashboard);
        return dashboard;
    }

    private void setPeriod(ManagerDashboard dashboard, String period) {
        switch (period == null ? "" : period.toUpperCase(java.util.Locale.ROOT)) {
            case "TODAY":
                dashboard.setPeriod("TODAY");
                dashboard.setPeriodLabel("Hôm nay");
                dashboard.setPreviousPeriodLabel("hôm qua");
                break;
            case "MONTH":
                dashboard.setPeriod("MONTH");
                dashboard.setPeriodLabel("Tháng này");
                dashboard.setPreviousPeriodLabel("tháng trước");
                break;
            case "QUARTER":
                dashboard.setPeriod("QUARTER");
                dashboard.setPeriodLabel("Quý này");
                dashboard.setPreviousPeriodLabel("quý trước");
                break;
            case "YEAR":
                dashboard.setPeriod("YEAR");
                dashboard.setPeriodLabel("Năm nay");
                dashboard.setPreviousPeriodLabel("năm trước");
                break;
            default:
                dashboard.setPeriod("WEEK");
                dashboard.setPeriodLabel("Tuần này");
                dashboard.setPreviousPeriodLabel("tuần trước");
                break;
        }
    }

    private static String periodStartExpression() {
        return "CASE ? "
                + "WHEN 'TODAY' THEN CONVERT(date, GETDATE()) "
                + "WHEN 'WEEK' THEN DATEADD(day, "
                + "-(DATEDIFF(day, CONVERT(date, '19000101'), CONVERT(date, GETDATE())) % 7), "
                + "CONVERT(date, GETDATE())) "
                + "WHEN 'MONTH' THEN DATEFROMPARTS(YEAR(GETDATE()), MONTH(GETDATE()), 1) "
                + "WHEN 'QUARTER' THEN DATEADD(quarter, DATEDIFF(quarter, 0, GETDATE()), 0) "
                + "WHEN 'YEAR' THEN DATEFROMPARTS(YEAR(GETDATE()), 1, 1) END";
    }

    private static String previousPeriodStartExpression() {
        return "CASE ? "
                + "WHEN 'TODAY' THEN DATEADD(day, -1, period_start) "
                + "WHEN 'WEEK' THEN DATEADD(day, -7, period_start) "
                + "WHEN 'MONTH' THEN DATEADD(month, -1, period_start) "
                + "WHEN 'QUARTER' THEN DATEADD(quarter, -1, period_start) "
                + "WHEN 'YEAR' THEN DATEADD(year, -1, period_start) END";
    }

    private static String periodBoundsCte() {
        return "WITH period_base AS (SELECT " + periodStartExpression()
                + " AS period_start, DATEADD(day, 1, CONVERT(date, GETDATE())) AS period_end), "
                + "period_bounds AS (SELECT period_start, period_end, "
                + previousPeriodStartExpression() + " AS previous_start FROM period_base)";
    }

    private static void bindPeriod(PreparedStatement statement, String period, int firstIndex)
            throws SQLException {
        statement.setString(firstIndex, period);
        statement.setString(firstIndex + 1, period);
    }

    private void loadSummary(ManagerDashboard dashboard) throws SQLException {
        String sql = periodBoundsCte() + " SELECT "
                + "COALESCE(SUM(CASE WHEN o.order_status = 'PAID' "
                + "AND COALESCE(o.paid_at, o.closed_at) >= p.period_start "
                + "AND COALESCE(o.paid_at, o.closed_at) < p.period_end "
                + "THEN o.total_amount ELSE 0 END), 0) AS today_revenue, "
                + "COALESCE(SUM(CASE WHEN o.order_status = 'PAID' "
                + "AND COALESCE(o.paid_at, o.closed_at) >= p.previous_start "
                + "AND COALESCE(o.paid_at, o.closed_at) < CASE "
                + "WHEN DATEADD(day, DATEDIFF(day, p.period_start, p.period_end), p.previous_start) > p.period_start "
                + "THEN p.period_start "
                + "ELSE DATEADD(day, DATEDIFF(day, p.period_start, p.period_end), p.previous_start) END "
                + "THEN o.total_amount ELSE 0 END), 0) AS yesterday_revenue, "
                + "COALESCE(SUM(CASE WHEN o.order_status <> 'CANCELLED' "
                + "AND o.created_at >= p.period_start "
                + "AND o.created_at < p.period_end "
                + "THEN 1 ELSE 0 END), 0) AS today_order_count "
                + "FROM dbo.Orders o CROSS JOIN period_bounds p";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    dashboard.setTodayRevenue(results.getBigDecimal("today_revenue"));
                    dashboard.setYesterdayRevenue(results.getBigDecimal("yesterday_revenue"));
                    dashboard.setTodayOrderCount(results.getInt("today_order_count"));
                }
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
        String sql = periodBoundsCte() + ", customer_orders AS ("
                + "SELECT CASE "
                + "WHEN o.user_id IS NOT NULL THEN CONCAT('U:', o.user_id) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_email)), '') IS NOT NULL "
                + "THEN CONCAT('E:', LOWER(LTRIM(RTRIM(o.customer_email)))) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_phone)), '') IS NOT NULL "
                + "THEN CONCAT('P:', LTRIM(RTRIM(o.customer_phone))) "
                + "ELSE CONCAT('O:', o.order_id) END AS customer_key, "
                + "MIN(o.created_at) AS first_order_at, "
                + "MAX(CASE WHEN o.created_at >= p.period_start "
                + "AND o.created_at < p.period_end THEN 1 ELSE 0 END) AS ordered_today "
                + "FROM dbo.Orders o CROSS JOIN period_bounds p WHERE o.order_status <> 'CANCELLED' "
                + "GROUP BY CASE "
                + "WHEN o.user_id IS NOT NULL THEN CONCAT('U:', o.user_id) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_email)), '') IS NOT NULL "
                + "THEN CONCAT('E:', LOWER(LTRIM(RTRIM(o.customer_email)))) "
                + "WHEN NULLIF(LTRIM(RTRIM(o.customer_phone)), '') IS NOT NULL "
                + "THEN CONCAT('P:', LTRIM(RTRIM(o.customer_phone))) "
                + "ELSE CONCAT('O:', o.order_id) END, p.period_start, p.period_end) "
                + "SELECT COALESCE(SUM(CASE WHEN ordered_today = 1 "
                + "AND first_order_at >= p.period_start THEN 1 ELSE 0 END), 0) AS new_customers, "
                + "COALESCE(SUM(CASE WHEN ordered_today = 1 "
                + "AND first_order_at < p.period_start THEN 1 ELSE 0 END), 0) AS returning_customers "
                + "FROM customer_orders CROSS JOIN period_bounds p";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    dashboard.setNewCustomerCount(results.getInt("new_customers"));
                    dashboard.setReturningCustomerCount(results.getInt("returning_customers"));
                }
            }
        }
    }

    private void loadCancellationStats(ManagerDashboard dashboard) throws SQLException {
        String sql = periodBoundsCte() + " SELECT COUNT(*) AS all_orders, "
                + "COALESCE(SUM(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_orders "
                + "FROM dbo.Orders CROSS JOIN period_bounds p "
                + "WHERE created_at >= p.period_start AND created_at < p.period_end";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    dashboard.setAllTodayOrderCount(results.getInt("all_orders"));
                    dashboard.setCancelledOrderCount(results.getInt("cancelled_orders"));
                }
            }
        }
    }

    private void loadTablePerformance(ManagerDashboard dashboard) throws SQLException {
        String dailySql = periodBoundsCte() + " SELECT COUNT(*) AS service_count, "
                + "AVG(CASE WHEN checked_in_at IS NOT NULL AND paid_at >= checked_in_at "
                + "THEN CAST(DATEDIFF(SECOND, checked_in_at, paid_at) AS DECIMAL(12,2)) / 60 END) "
                + "AS average_service_minutes "
                + "FROM dbo.ServiceHistory CROSS JOIN period_bounds p "
                + "WHERE paid_at >= p.period_start AND paid_at < p.period_end";
        try (PreparedStatement statement = connection.prepareStatement(dailySql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    dashboard.setTodayServiceCount(results.getInt("service_count"));
                    dashboard.setAverageServiceMinutes(results.getBigDecimal("average_service_minutes"));
                }
            }
        }

        String tableSql = periodBoundsCte() + " SELECT TOP (5) t.table_name, COUNT(*) AS service_count "
                + "FROM dbo.ServiceHistory h JOIN dbo.RestaurantTables t ON t.table_id = h.table_id "
                + "CROSS JOIN period_bounds p "
                + "WHERE h.paid_at >= p.period_start AND h.paid_at < p.period_end "
                + "GROUP BY t.table_id, t.table_name "
                + "ORDER BY COUNT(*) DESC, t.table_name";
        try (PreparedStatement statement = connection.prepareStatement(tableSql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    dashboard.getBusiestTables().add(new TableTurnover(
                            results.getNString("table_name"), results.getInt("service_count")));
                }
            }
        }
    }

    private void loadLoyaltyStats(ManagerDashboard dashboard) throws SQLException {
        String historySql = periodBoundsCte() + " SELECT COUNT(DISTINCT loyalty_customer_id) AS loyalty_customers, "
                + "COALESCE(SUM(loyalty_points_earned), 0) AS points_earned, "
                + "COALESCE(SUM(loyalty_points_used), 0) AS points_used "
                + "FROM dbo.ServiceHistory CROSS JOIN period_bounds p "
                + "WHERE paid_at >= p.period_start AND paid_at < p.period_end";
        try (PreparedStatement statement = connection.prepareStatement(historySql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    dashboard.setLoyaltyCustomerCount(results.getInt("loyalty_customers"));
                    dashboard.setLoyaltyPointsEarnedToday(results.getInt("points_earned"));
                    dashboard.setLoyaltyPointsUsedToday(results.getInt("points_used"));
                }
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

        String sql = periodBoundsCte() + ", daily_sales AS ("
                + "SELECT oi.dish_id, SUM(oi.quantity) AS quantity "
                + "FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.order_id = oi.order_id "
                + "CROSS JOIN period_bounds p "
                + "WHERE o.order_status <> 'CANCELLED' "
                + "AND o.created_at >= p.period_start AND o.created_at < p.period_end "
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
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
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
    }

    private void loadLeastSold(ManagerDashboard dashboard) throws SQLException {
        Map<Integer, CategorySales> categories = new LinkedHashMap<>();
        String categorySql = "SELECT category_id, category_name FROM dbo.Category "
                + "WHERE status = 1 ORDER BY category_id";
        try (PreparedStatement statement = connection.prepareStatement(categorySql);
                ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                CategorySales category = new CategorySales(results.getNString("category_name"));
                categories.put(results.getInt("category_id"), category);
                dashboard.getLeastSoldCategories().add(category);
            }
        }

        String sql = periodBoundsCte() + ", dish_sales AS ("
                + "SELECT oi.dish_id, SUM(oi.quantity) AS quantity "
                + "FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.order_id = oi.order_id "
                + "CROSS JOIN period_bounds p "
                + "WHERE o.order_status <> 'CANCELLED' "
                + "AND o.created_at >= p.period_start AND o.created_at < p.period_end "
                + "GROUP BY oi.dish_id), ranked_dishes AS ("
                + "SELECT c.category_id, c.category_name, d.dish_name, d.unit, "
                + "COALESCE(ds.quantity, 0) AS quantity, "
                + "ROW_NUMBER() OVER (PARTITION BY c.category_id "
                + "ORDER BY COALESCE(ds.quantity, 0), d.dish_name) AS rank_in_category "
                + "FROM dbo.Dish d JOIN dbo.Category c ON c.category_id = d.category_id "
                + "LEFT JOIN dish_sales ds ON ds.dish_id = d.dish_id "
                + "WHERE d.status = 1 AND c.status = 1) "
                + "SELECT category_id, category_name, dish_name, unit, quantity "
                + "FROM ranked_dishes WHERE rank_in_category <= 5 "
                + "ORDER BY category_id, rank_in_category";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPeriod(statement, dashboard.getPeriod(), 1);
            try (ResultSet results = statement.executeQuery()) {
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
    }
}
