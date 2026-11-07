package dao;

import dal.DBContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.CustomerReview;
import model.CustomerReviewSummary;

public class CustomerReviewDAO extends DBContext {
    private static final String SELECT_REVIEW
            = "SELECT r.review_id, r.history_id, u.full_name AS customer_name, t.table_name, "
            + "r.rating, r.comment, r.created_at, r.status "
            + "FROM dbo.CustomerReviews r "
            + "JOIN dbo.Users u ON u.user_id = r.customer_id "
            + "JOIN dbo.ServiceHistory h ON h.history_id = r.history_id "
            + "LEFT JOIN dbo.RestaurantTables t ON t.table_id = h.table_id ";

    public List<CustomerReview> findApproved() throws SQLException {
        List<CustomerReview> reviews = new ArrayList<>();
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TOP 100 r.review_id, r.history_id, u.full_name AS customer_name, t.table_name, "
                + "r.rating, r.comment, r.created_at, r.status "
                + "FROM dbo.CustomerReviews r "
                + "JOIN dbo.Users u ON u.user_id = r.customer_id "
                + "JOIN dbo.ServiceHistory h ON h.history_id = r.history_id "
                + "LEFT JOIN dbo.RestaurantTables t ON t.table_id = h.table_id "
                + "WHERE r.status = 'APPROVED' ORDER BY r.moderated_at DESC, r.review_id DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reviews.add(readReview(rs));
            }
        }
        return reviews;
    }

    public List<CustomerReview> findPending() throws SQLException {
        List<CustomerReview> reviews = new ArrayList<>();
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                SELECT_REVIEW + "WHERE r.status = 'PENDING' ORDER BY r.created_at, r.review_id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reviews.add(readReview(rs));
            }
        }
        return reviews;
    }

    public List<CustomerReview> findAll() throws SQLException {
        List<CustomerReview> reviews = new ArrayList<>();
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TOP 300 * FROM (" + SELECT_REVIEW + ") AS all_reviews "
                + "ORDER BY all_reviews.created_at DESC, all_reviews.review_id DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reviews.add(readReview(rs));
            }
        }
        return reviews;
    }

    public CustomerReviewSummary getSummary() throws SQLException {
        requireConnection();
        CustomerReviewSummary summary = new CustomerReviewSummary();
        String sql = "SELECT COUNT(*) AS total_count, "
                + "COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_count, "
                + "COALESCE(SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END), 0) AS approved_count, "
                + "COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejected_count, "
                + "COALESCE(AVG(CASE WHEN status = 'APPROVED' THEN CAST(rating AS DECIMAL(4,2)) END), 0) AS average_rating, "
                + "COALESCE(SUM(CASE WHEN status = 'APPROVED' AND rating = 1 THEN 1 ELSE 0 END), 0) AS one_star_count, "
                + "COALESCE(SUM(CASE WHEN status = 'APPROVED' AND rating = 2 THEN 1 ELSE 0 END), 0) AS two_star_count, "
                + "COALESCE(SUM(CASE WHEN status = 'APPROVED' AND rating = 3 THEN 1 ELSE 0 END), 0) AS three_star_count, "
                + "COALESCE(SUM(CASE WHEN status = 'APPROVED' AND rating = 4 THEN 1 ELSE 0 END), 0) AS four_star_count, "
                + "COALESCE(SUM(CASE WHEN status = 'APPROVED' AND rating = 5 THEN 1 ELSE 0 END), 0) AS five_star_count "
                + "FROM dbo.CustomerReviews";
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                summary.setTotalCount(rs.getInt("total_count"));
                summary.setPendingCount(rs.getInt("pending_count"));
                summary.setApprovedCount(rs.getInt("approved_count"));
                summary.setRejectedCount(rs.getInt("rejected_count"));
                summary.setAverageRating(rs.getDouble("average_rating"));
                summary.setOneStarCount(rs.getInt("one_star_count"));
                summary.setTwoStarCount(rs.getInt("two_star_count"));
                summary.setThreeStarCount(rs.getInt("three_star_count"));
                summary.setFourStarCount(rs.getInt("four_star_count"));
                summary.setFiveStarCount(rs.getInt("five_star_count"));
            }
        }
        return summary;
    }

    public Set<Integer> findSubmittedHistoryIds(int customerId) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT history_id FROM dbo.CustomerReviews WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    public boolean hasSubmittedReview(int historyId, int customerId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM dbo.CustomerReviews WHERE history_id = ? AND customer_id = ?")) {
            ps.setInt(1, historyId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean submit(int historyId, int customerId, int rating, String comment) throws SQLException {
        requireConnection();
        String sql = "INSERT INTO dbo.CustomerReviews (history_id, customer_id, rating, comment) "
                + "SELECT ?, ?, ?, ? "
                + "WHERE EXISTS (SELECT 1 FROM dbo.ServiceHistory "
                + "WHERE history_id = ? AND loyalty_customer_id = ?) "
                + "AND NOT EXISTS (SELECT 1 FROM dbo.CustomerReviews WHERE history_id = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, historyId);
            ps.setInt(2, customerId);
            ps.setInt(3, rating);
            ps.setNString(4, comment);
            ps.setInt(5, historyId);
            ps.setInt(6, customerId);
            ps.setInt(7, historyId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean moderate(int reviewId, int managerId, String status) throws SQLException {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            return false;
        }
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE dbo.CustomerReviews SET status = ?, moderated_by = ?, moderated_at = SYSDATETIME() "
                + "WHERE review_id = ? AND status = 'PENDING'")) {
            ps.setString(1, status);
            ps.setInt(2, managerId);
            ps.setInt(3, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(int reviewId) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM dbo.CustomerReviews WHERE review_id = ?")) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    private CustomerReview readReview(ResultSet rs) throws SQLException {
        CustomerReview review = new CustomerReview();
        review.setReviewId(rs.getInt("review_id"));
        review.setHistoryId(rs.getInt("history_id"));
        review.setCustomerName(rs.getNString("customer_name"));
        review.setTableName(rs.getNString("table_name"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getNString("comment"));
        review.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            review.setCreatedAt(created.toLocalDateTime());
        }
        return review;
    }

    private void requireConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is unavailable.");
        }
    }
}
