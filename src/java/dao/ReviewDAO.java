package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Review;

public class ReviewDAO extends DBContext implements AutoCloseable {
    public void submit(int customerId, int rating, String reviewText) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO Reviews (user_id, rating, review_text, status) VALUES (?, ?, ?, 'PENDING')")) {
            ps.setInt(1, customerId);
            ps.setInt(2, rating);
            ps.setNString(3, reviewText);
            ps.executeUpdate();
        }
    }

    public List<Review> getApprovedReviews() throws SQLException {
        requireConnection();
        String sql = "SELECT r.review_id, r.rating, r.review_text, r.status, r.created_at, "
                + "COALESCE(NULLIF(u.full_name, ''), u.username, 'Khách hàng') customer_name "
                + "FROM Reviews r JOIN Users u ON u.user_id=r.user_id "
                + "WHERE r.status='APPROVED' AND u.role_id=(SELECT role_id FROM Roles WHERE role_name='Customer') "
                + "ORDER BY r.created_at DESC, r.review_id DESC";
        return getReviews(sql);
    }

    public List<Review> getPendingReviews() throws SQLException {
        requireConnection();
        String sql = "SELECT r.review_id, r.rating, r.review_text, r.status, r.created_at, "
                + "COALESCE(NULLIF(u.full_name, ''), u.username, 'Khách hàng') customer_name "
                + "FROM Reviews r JOIN Users u ON u.user_id=r.user_id "
                + "WHERE r.status='PENDING' AND u.role_id=(SELECT role_id FROM Roles WHERE role_name='Customer') "
                + "ORDER BY r.created_at, r.review_id";
        return getReviews(sql);
    }

    public int[] getApprovedRatingSummary() throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) review_count, COALESCE(AVG(CAST(rating AS DECIMAL(4,2))), 0) average_rating "
                + "FROM Reviews r JOIN Users u ON u.user_id=r.user_id "
                + "WHERE r.status='APPROVED' AND u.role_id=(SELECT role_id FROM Roles WHERE role_name='Customer')");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new int[]{rs.getInt("review_count"), (int) Math.round(rs.getDouble("average_rating") * 100)};
        }
    }

    /** Returns customer review counts (total, pending, approved) and average rating x100. */
    public int[] getManagerReviewSummary() throws SQLException {
        requireConnection();
        String sql = "SELECT COUNT(*) total_count, "
                + "SUM(CASE WHEN r.status='PENDING' THEN 1 ELSE 0 END) pending_count, "
                + "SUM(CASE WHEN r.status='APPROVED' THEN 1 ELSE 0 END) approved_count, "
                + "COALESCE(AVG(CAST(r.rating AS DECIMAL(4,2))), 0) average_rating "
                + "FROM Reviews r JOIN Users u ON u.user_id=r.user_id "
                + "WHERE u.role_id=(SELECT role_id FROM Roles WHERE role_name='Customer')";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new int[]{rs.getInt("total_count"), rs.getInt("pending_count"),
                rs.getInt("approved_count"), (int) Math.round(rs.getDouble("average_rating") * 100)};
        }
    }

    public boolean approve(int reviewId, int managerId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE Reviews SET status='APPROVED', approved_by=?, approved_at=SYSDATETIME() "
                + "WHERE review_id=? AND status='PENDING'")) {
            ps.setInt(1, managerId);
            ps.setInt(2, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean reject(int reviewId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM Reviews WHERE review_id=? AND status='PENDING'")) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    private List<Review> getReviews(String sql) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Review review = new Review();
                review.setReviewId(rs.getInt("review_id"));
                review.setRating(rs.getInt("rating"));
                review.setReviewText(rs.getNString("review_text"));
                review.setStatus(rs.getString("status"));
                review.setCreatedAt(rs.getTimestamp("created_at"));
                review.setCustomerName(rs.getNString("customer_name"));
                reviews.add(review);
            }
        }
        return reviews;
    }

    private void requireConnection() throws SQLException {
        if (connection == null) throw new SQLException("Database connection is unavailable.");
    }
}
