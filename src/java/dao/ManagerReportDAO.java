package dao;

import dal.DBContext;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.CategorySalesReport;
import model.DishSales;
import model.RevenueSummary;

public class ManagerReportDAO extends DBContext {

    public RevenueSummary getRevenueSummary(LocalDate today) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN paid_at >= ? AND paid_at < ? "
                + "THEN total_amount - COALESCE(loyalty_discount, 0) - COALESCE(voucher_discount, 0) ELSE 0 END), 0) AS today_revenue, "
                + "COALESCE(SUM(CASE WHEN paid_at >= ? AND paid_at < ? "
                + "THEN total_amount - COALESCE(loyalty_discount, 0) - COALESCE(voucher_discount, 0) ELSE 0 END), 0) AS month_revenue, "
                + "COALESCE(SUM(CASE WHEN paid_at >= ? AND paid_at < ? "
                + "THEN total_amount - COALESCE(loyalty_discount, 0) - COALESCE(voucher_discount, 0) ELSE 0 END), 0) AS year_revenue "
                + "FROM ServiceHistory";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(monthStart.atStartOfDay()));
            ps.setTimestamp(4, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
            ps.setTimestamp(5, Timestamp.valueOf(yearStart.atStartOfDay()));
            ps.setTimestamp(6, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Could not calculate revenue summary.");
                }
                RevenueSummary summary = new RevenueSummary();
                summary.setToday(rs.getBigDecimal("today_revenue"));
                summary.setMonth(rs.getBigDecimal("month_revenue"));
                summary.setYear(rs.getBigDecimal("year_revenue"));
                return summary;
            }
        }
    }

    public List<CategorySalesReport> getSalesByCategory() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }
        String sql = "WITH DishSales AS ("
                + "SELECT c.category_id, c.category_name, d.dish_id, d.dish_name, "
                + "COALESCE(s.quantity_sold, 0) AS quantity_sold "
                + "FROM Category c "
                + "LEFT JOIN Dish d ON d.category_id = c.category_id "
                + "LEFT JOIN ("
                + "SELECT oi.dish_id, SUM(oi.quantity) AS quantity_sold "
                + "FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                + "WHERE o.order_status = 'PAID' AND (oi.item_status IS NULL OR oi.item_status <> 'REJECTED') "
                + "AND oi.dish_id IS NOT NULL GROUP BY oi.dish_id"
                + ") s ON s.dish_id = d.dish_id "
                + "WHERE c.status = 1"
                + "), RankedDishes AS ("
                + "SELECT category_id, category_name, dish_id, dish_name, quantity_sold, "
                + "ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY quantity_sold DESC, dish_name ASC) AS best_rank, "
                + "ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY quantity_sold ASC, dish_name ASC) AS least_rank "
                + "FROM DishSales"
                + ") "
                + "SELECT category_id, category_name, dish_id, dish_name, quantity_sold, best_rank, least_rank "
                + "FROM RankedDishes ORDER BY category_id, best_rank";
        Map<Integer, CategorySalesReport> reports = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int categoryId = rs.getInt("category_id");
                CategorySalesReport report = reports.get(categoryId);
                if (report == null) {
                    report = new CategorySalesReport(rs.getNString("category_name"));
                    reports.put(categoryId, report);
                }
                String dishName = rs.getNString("dish_name");
                if (dishName == null) {
                    continue;
                }
                DishSales dish = new DishSales(dishName, rs.getInt("quantity_sold"));
                if (rs.getInt("best_rank") <= 5) {
                    report.getTopDishes().add(dish);
                }
                if (rs.getInt("least_rank") == 1) {
                    report.setLeastChosenDish(dish);
                }
            }
        }
        return new ArrayList<>(reports.values());
    }
}
