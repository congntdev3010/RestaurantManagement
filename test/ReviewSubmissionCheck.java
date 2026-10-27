import dal.DBContext;
import dao.ReviewDAO;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import model.Review;
import model.User;
import utils.ReviewAccess;

/** SQL Server integration check. Run after database/add_reviews(17).sql. */
public class ReviewSubmissionCheck {
    private static int failures;

    public static void main(String[] args) throws Exception {
        Fixture fixture = new Fixture();
        try {
            fixture.create();
            testRoleAccess();
            testSubmissionAndModeration(fixture);
        } finally {
            fixture.cleanup();
            fixture.close();
        }
        System.out.println(failures == 0 ? "ALL REVIEW CHECKS PASSED" : failures + " REVIEW CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void testRoleAccess() {
        check("Customer may submit", ReviewAccess.isCustomer(user("Customer")));
        check("Non-Customer roles may not submit", !ReviewAccess.isCustomer(user("Manager"))
                && !ReviewAccess.isCustomer(user("Server"))
                && !ReviewAccess.isCustomer(user("Chef"))
                && !ReviewAccess.isCustomer(user("Receptionist"))
                && !ReviewAccess.isCustomer(user("Admin"))
                && !ReviewAccess.isCustomer(null));
        check("Only Manager may moderate", ReviewAccess.isManager(user("Manager"))
                && !ReviewAccess.isManager(user("Customer"))
                && !ReviewAccess.isManager(user("Server"))
                && !ReviewAccess.isManager(user("Chef"))
                && !ReviewAccess.isManager(user("Receptionist"))
                && !ReviewAccess.isManager(user("Admin"))
                && !ReviewAccess.isManager(null));
    }

    private static void testSubmissionAndModeration(Fixture fixture) throws Exception {
        try (ReviewDAO dao = new ReviewDAO()) {
            int baselineCount = dao.getApprovedRatingSummary()[0];

            dao.submit(fixture.customerId, 5, "Tốt");
            int pendingId = fixture.latestReviewId(fixture.customerId);
            check("Logged-in Customer submits without an order", pendingId > 0);
            check("New review remains pending and hidden",
                    containsId(dao.getPendingReviews(), pendingId)
                    && !containsId(dao.getApprovedReviews(), pendingId));
            check("Pending review does not change public rating count",
                    dao.getApprovedRatingSummary()[0] == baselineCount);

            check("Manager approval succeeds", dao.approve(pendingId, fixture.managerId));
            check("Approved review becomes public", containsId(dao.getApprovedReviews(), pendingId));
            check("Approved review contributes to public rating count",
                    dao.getApprovedRatingSummary()[0] == baselineCount + 1);

            dao.submit(fixture.customerId, 2, "Chưa phù hợp");
            int rejectedId = fixture.latestReviewId(fixture.customerId);
            check("Second restaurant review can be submitted", rejectedId > pendingId);
            check("Reject deletes pending review", dao.reject(rejectedId));
            check("Rejected review is removed", !fixture.reviewExists(rejectedId));
            check("Rejected review never appears publicly",
                    !containsId(dao.getApprovedReviews(), rejectedId));
            check("Rejected review does not change public rating count",
                    dao.getApprovedRatingSummary()[0] == baselineCount + 1);

            dao.submit(fixture.secondCustomerId, 4, null);
            int nonApprovedCustomerReview = fixture.latestReviewId(fixture.secondCustomerId);
            check("Other Customer can submit independently", nonApprovedCustomerReview > 0);
            check("Only approved reviews count publicly",
                    dao.getApprovedRatingSummary()[0] == baselineCount + 1);
        }
    }

    private static boolean containsId(List<Review> reviews, int id) {
        for (Review review : reviews) if (review.getReviewId() == id) return true;
        return false;
    }

    private static User user(String role) {
        User user = new User();
        user.setRoleName(role);
        return user;
    }

    private static void check(String name, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + name);
        if (!passed) failures++;
    }

    private static final class Fixture extends DBContext {
        private final String suffix = UUID.randomUUID().toString().replace("-", "");
        private final List<Integer> userIds = new ArrayList<>();
        private int customerId;
        private int secondCustomerId;
        private int managerId;

        private void create() throws SQLException {
            if (connection == null) throw new SQLException("Database connection is unavailable.");
            customerId = createUser("Customer", "rv-c-" + suffix);
            secondCustomerId = createUser("Customer", "rv-o-" + suffix);
            managerId = createUser("Manager", "rv-m-" + suffix);
        }

        private int createUser(String role, String value) throws SQLException {
            int roleId;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT role_id FROM Roles WHERE role_name=?")) {
                ps.setString(1, role);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Required role not found: " + role);
                    roleId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO Users (username,email,password,full_name,role_id,is_active) "
                    + "VALUES (?,?,?,?,?,1)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, value);
                ps.setString(2, value + "@example.test");
                ps.setString(3, "test-only");
                ps.setString(4, value);
                ps.setInt(5, roleId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    int id = rs.getInt(1);
                    userIds.add(id);
                    return id;
                }
            }
        }

        private int latestReviewId(int userId) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT TOP 1 review_id FROM Reviews WHERE user_id=? ORDER BY review_id DESC")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Review not found for test user " + userId);
                    return rs.getInt(1);
                }
            }
        }

        private boolean reviewExists(int reviewId) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT 1 FROM Reviews WHERE review_id=?")) {
                ps.setInt(1, reviewId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }

        private void cleanup() {
            if (connection == null) return;
            try {
                for (int id : userIds) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM Reviews WHERE user_id=? OR approved_by=?")) {
                        ps.setInt(1, id);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = connection.prepareStatement("DELETE FROM Users WHERE user_id=?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Could not clean review test fixtures: " + ex.getMessage());
                failures++;
            }
        }
    }
}
