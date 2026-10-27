package utils;

import model.User;

public final class ReviewAccess {
    private ReviewAccess() {
    }

    public static boolean isCustomer(User user) {
        return hasRole(user, "Customer");
    }

    public static boolean isManager(User user) {
        return hasRole(user, "Manager");
    }

    private static boolean hasRole(User user, String role) {
        return user != null && user.getRoleName() != null && role.equalsIgnoreCase(user.getRoleName());
    }
}
