import model.User;
import utils.ReviewAccess;

/** Database-independent checks for review submission and moderation role gates. */
public class ReviewPolicyCheck {
    public static void main(String[] args) {
        check("Customer may submit", ReviewAccess.isCustomer(user("Customer")));
        check("Every non-Customer role is denied submission",
                !ReviewAccess.isCustomer(user("Manager"))
                && !ReviewAccess.isCustomer(user("Server"))
                && !ReviewAccess.isCustomer(user("Chef"))
                && !ReviewAccess.isCustomer(user("Receptionist"))
                && !ReviewAccess.isCustomer(user("Admin"))
                && !ReviewAccess.isCustomer(null));
        check("Only Manager may moderate",
                ReviewAccess.isManager(user("Manager"))
                && !ReviewAccess.isManager(user("Customer"))
                && !ReviewAccess.isManager(user("Server"))
                && !ReviewAccess.isManager(user("Chef"))
                && !ReviewAccess.isManager(user("Receptionist"))
                && !ReviewAccess.isManager(user("Admin"))
                && !ReviewAccess.isManager(null));
        System.out.println("ALL REVIEW POLICY CHECKS COMPLETED");
    }

    private static User user(String role) {
        User user = new User();
        user.setRoleName(role);
        return user;
    }

    private static void check(String label, boolean passed) {
        if (!passed) throw new AssertionError(label);
        System.out.println("PASS " + label);
    }
}
