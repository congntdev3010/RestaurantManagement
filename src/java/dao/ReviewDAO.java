package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.RestaurantReview;

public class ReviewDAO extends DBContext {
    public List<RestaurantReview> findApproved() throws SQLException {
        return findByStatus("APPROVED");
    }

    public List<RestaurantReview> findPending() throws SQLException {
        return findByStatus("PENDING");
    }

    private List<RestaurantReview> findByStatus(String status) throws SQLException {
        requireConnection();
        List<RestaurantReview> rows = new ArrayList<>();
        String sql = "SELECT r.review_id,r.history_id,r.customer_id,r.rating,r.review_text,r.status,r.created_at, "
                + "COALESCE(NULLIF(u.full_name,''),u.username) customer_name,t.table_name "
                + "FROM RestaurantReviews r JOIN Users u ON u.user_id=r.customer_id "
                + "JOIN ServiceHistory h ON h.history_id=r.history_id LEFT JOIN RestaurantTables t ON t.table_id=h.table_id "
                + "WHERE r.status=? ORDER BY r.created_at DESC,r.review_id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(read(rs)); }
        }
        return rows;
    }

    public double averageRating() throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement("SELECT AVG(CAST(rating AS DECIMAL(4,2))) FROM RestaurantReviews WHERE status='APPROVED'")) {
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getDouble(1); }
        }
    }

    public int approvedCount() throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM RestaurantReviews WHERE status='APPROVED'")) {
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public boolean hasCustomerReview(int historyId, int customerId) throws SQLException {
        return hasReview(historyId, customerId);
    }

    public boolean hasReview(int historyId, int customerId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM RestaurantReviews WHERE history_id=? AND customer_id=?")) {
            ps.setInt(1, historyId); ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean submit(int historyId, int customerId, int rating, String text) throws SQLException {
        requireConnection();
        String sql = "INSERT INTO RestaurantReviews(history_id,customer_id,rating,review_text) "
                + "SELECT history_id,loyalty_customer_id,?,? FROM ServiceHistory WHERE history_id=? AND loyalty_customer_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rating); ps.setString(2, text); ps.setInt(3, historyId); ps.setInt(4, customerId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setStatus(int reviewId, String status, int managerId) throws SQLException {
        requireConnection();
        String sql = "UPDATE RestaurantReviews SET status=?,reviewed_at=SYSDATETIME(),reviewed_by=? WHERE review_id=? AND status='PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, managerId); ps.setInt(3, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    private RestaurantReview read(ResultSet rs) throws SQLException {
        RestaurantReview r = new RestaurantReview();
        r.setReviewId(rs.getInt("review_id")); r.setHistoryId(rs.getInt("history_id"));
        r.setCustomerId(rs.getInt("customer_id")); r.setRating(rs.getInt("rating"));
        r.setReviewText(rs.getNString("review_text")); r.setStatus(rs.getString("status"));
        r.setCustomerName(rs.getNString("customer_name")); r.setTableName(rs.getNString("table_name"));
        Timestamp ts = rs.getTimestamp("created_at"); if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        return r;
    }

    private void requireConnection() throws SQLException {
        if (connection == null) throw new SQLException("Database connection is unavailable.");
    }
}
