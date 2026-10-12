package dao;

import dal.DBContext;
import model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserDAO extends DBContext {

    private static final String STAFF_ROLE_FILTER = "r.role_name IN ('Server', 'Chef', 'Receptionist')";

    // 1. Kiểm tra đăng nhập (Chỉ cho phép tài khoản đã kích hoạt isActive = 1)
    public User login(String userOrEmail, String password) {
        String sql = "SELECT u.user_id, u.username, u.email, u.full_name, u.phone, u.role_id, r.role_name, u.is_active "
                + "FROM Users u JOIN Roles r ON u.role_id = r.role_id "
                + "WHERE (u.username = ? OR u.email = ?) AND u.password = ? AND u.is_active = 1";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, userOrEmail);
            ps.setString(2, userOrEmail);
            ps.setString(3, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                u.setFullName(rs.getNString("full_name"));
                u.setPhone(rs.getString("phone"));
                u.setRoleId(rs.getInt("role_id"));
                u.setRoleName(rs.getString("role_name"));
                u.setIsActive(rs.getBoolean("is_active"));
                return u;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Kiểm tra Tên tài khoản đã tồn tại chưa
    public boolean checkUsernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 2. Kiểm tra Email đã tồn tại chưa
    public boolean checkEmailExists(String email) {
        String sql = "SELECT user_id FROM Users WHERE email = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Lưu tài khoản chính thức vào DB với is_active = 1
    public boolean registerCustomer(User user) {
        String sql = "INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) VALUES (?, ?, ?, ?, ?, 1, 1)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setNString(4, user.getFullName());
            ps.setString(5, user.getPhone());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4. Lưu OTP mới vào Database
    public boolean saveOTP(String email, String otpCode) {
        String sql = "INSERT INTO OTP_Verifications (email, otp_code, expired_at) VALUES (?, ?, DATEADD(MINUTE, 5, GETDATE()))";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, otpCode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 5. Xác thực mã OTP
    public boolean verifyOTP(String email, String otpCode) {
        String sql = "SELECT otp_id FROM OTP_Verifications WHERE email = ? AND otp_code = ? AND is_used = 0 AND expired_at > GETDATE()";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, otpCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Đánh dấu OTP đã sử dụng
                String updateOtp = "UPDATE OTP_Verifications SET is_used = 1 WHERE email = ? AND otp_code = ?";
                PreparedStatement ps2 = connection.prepareStatement(updateOtp);
                ps2.setString(1, email);
                ps2.setString(2, otpCode);
                ps2.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 6. Kích hoạt tài khoản User (is_active = 1)
    public boolean activateUser(String email) {
        String sql = "UPDATE Users SET is_active = 1 WHERE email = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Cập nhật mật khẩu mới theo Email
    public boolean updatePasswordByEmail(String email, String newPassword) {
        String sql = "UPDATE Users SET password = ? WHERE email = ?";
        try {
            if (connection == null) {
                return false;
            }

            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, email);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<User> getManageableUsers(boolean isAdmin, Integer roleId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.email, u.full_name, u.phone, u.role_id, r.role_name, u.is_active "
                + "FROM Users u JOIN Roles r ON u.role_id = r.role_id "
                + (isAdmin ? (roleId == null ? "" : "WHERE u.role_id = ? ") : "WHERE " + STAFF_ROLE_FILTER + " ")
                + "ORDER BY u.user_id DESC";
        try {
            if (connection == null) {
                return users;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            if (isAdmin && roleId != null) {
                ps.setInt(1, roleId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(readUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public Map<Integer, String> getManageableRoles(boolean isAdmin) {
        Map<Integer, String> roles = new LinkedHashMap<>();
        String sql = "SELECT role_id, role_name FROM Roles "
                + (isAdmin ? "" : "WHERE role_name IN ('Server', 'Chef', 'Receptionist') ")
                + "ORDER BY role_id";
        try {
            if (connection == null) {
                return roles;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roles.put(rs.getInt("role_id"), rs.getString("role_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    public User getManageableUserById(int userId, boolean isAdmin) {
        String sql = "SELECT u.user_id, u.username, u.email, u.full_name, u.phone, u.role_id, r.role_name, u.is_active "
                + "FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.user_id = ? "
                + (isAdmin ? "" : "AND " + STAFF_ROLE_FILTER);
        try {
            if (connection == null) {
                return null;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? readUser(rs) : null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createManagedUser(User user, boolean isAdmin) {
        if (connection == null || !isAllowedRole(user.getRoleId(), isAdmin)) {
            return false;
        }
        String sql = "INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) "
                + "SELECT ?, ?, ?, ?, ?, role_id, ? FROM Roles WHERE role_id = ?"
                + (isAdmin ? "" : " AND role_name IN ('Server', 'Chef', 'Receptionist')");
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setNString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            ps.setBoolean(6, user.isIsActive());
            ps.setInt(7, user.getRoleId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateManagedUser(User user, boolean isAdmin) {
        if (connection == null || !isManageableUser(user.getUserId(), isAdmin)
                || !isAllowedRole(user.getRoleId(), isAdmin)) {
            return false;
        }
        boolean updatePassword = user.getPassword() != null && !user.getPassword().isEmpty();
        String sql = "UPDATE Users SET username = ?, email = ?, full_name = ?, phone = ?, role_id = ?"
                + (updatePassword ? ", password = ?" : "") + " WHERE user_id = ?"
                + (isAdmin ? "" : " AND role_id IN (SELECT role_id FROM Roles WHERE role_name IN ('Server', 'Chef', 'Receptionist'))");
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setNString(3, user.getFullName());
            ps.setString(4, user.getPhone());
            ps.setInt(5, user.getRoleId());
            int userIdParameter = 6;
            if (updatePassword) {
                ps.setString(6, user.getPassword());
                userIdParameter++;
            }
            ps.setInt(userIdParameter, user.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteManagedUser(int userId, int actorId, boolean isAdmin) {
        if (connection == null || userId == actorId || !isManageableUser(userId, isAdmin)) {
            return false;
        }
        String sql = "DELETE FROM Users WHERE user_id = ? AND user_id <> ?"
                + (isAdmin ? "" : " AND role_id IN (SELECT role_id FROM Roles WHERE role_name IN ('Server', 'Chef', 'Receptionist'))");
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, actorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setManagedUserActive(int userId, int actorId, boolean isActive, boolean isAdmin) {
        if (connection == null || userId == actorId || !isManageableUser(userId, isAdmin)) {
            return false;
        }
        String sql = "UPDATE Users SET is_active = ? WHERE user_id = ? AND user_id <> ?"
                + (isAdmin ? "" : " AND role_id IN (SELECT role_id FROM Roles WHERE role_name IN ('Server', 'Chef', 'Receptionist'))");
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setBoolean(1, isActive);
            ps.setInt(2, userId);
            ps.setInt(3, actorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isAllowedRole(int roleId, boolean isAdmin) {
        String sql = "SELECT role_name FROM Roles WHERE role_id = ?";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return false;
            }
            return isAdmin || isStaffRole(rs.getString("role_name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isManageableUser(int userId, boolean isAdmin) {
        String sql = "SELECT 1 FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.user_id = ? "
                + (isAdmin ? "" : "AND " + STAFF_ROLE_FILTER);
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isStaffRole(String roleName) {
        return "Server".equalsIgnoreCase(roleName)
                || "Chef".equalsIgnoreCase(roleName)
                || "Receptionist".equalsIgnoreCase(roleName);
    }

    private User readUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getNString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setRoleId(rs.getInt("role_id"));
        user.setRoleName(rs.getString("role_name"));
        user.setIsActive(rs.getBoolean("is_active"));
        return user;
    }
}
