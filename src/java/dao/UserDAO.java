package dao;

import dal.DBContext;
import model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
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
            + "WHERE (u.username = ? OR u.email = ?) AND u.password = ? AND u.is_active = 1 AND u.is_deleted = 0";
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

    public boolean isUserActive(int userId) {
        String sql = "SELECT 1 FROM Users WHERE user_id = ? AND is_active = 1 AND is_deleted = 0";
        if (connection == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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

    // Lưu họ tên và số điện thoại mới cho tài khoản.
    public boolean updateProfile(int userId, String fullName, String phone) {
        String sql = "UPDATE Users SET full_name = ?, phone = ? WHERE user_id = ?";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setNString(1, fullName);
            ps.setString(2, phone);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Chỉ cập nhật mật khẩu nếu mật khẩu hiện tại trong form khớp dữ liệu trong DB.
    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        String sql = "UPDATE Users SET password = ? WHERE user_id = ? AND password = ?";
        try {
            if (connection == null) {
                return false;
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.setString(3, currentPassword);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Lấy danh sách tài khoản: Admin xem tất cả, Manager chỉ xem nhân viên.
    public List<User> getManageableUsers(boolean isAdmin, int roleId, String nameQuery) {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT u.user_id, u.username, u.email, u.full_name, u.phone, ")
                .append("u.role_id, r.role_name, u.is_active FROM Users u ")
            .append("JOIN Roles r ON u.role_id = r.role_id WHERE u.is_deleted = 0 ");
        if (!isAdmin) {
            sql.append("AND ").append(STAFF_ROLE_FILTER).append(' ');
        }
        if (roleId > 0) {
            sql.append("AND u.role_id = ? ");
        }
        if (nameQuery != null && !nameQuery.isEmpty()) {
            sql.append("AND REPLACE(REPLACE(u.full_name, N'Đ', N'D'), N'đ', N'd') ")
                    .append("COLLATE Latin1_General_CI_AI LIKE ? ");
        }
        sql.append("ORDER BY u.user_id DESC");
        try {
            if (connection == null) {
                return users;
            }
            PreparedStatement ps = connection.prepareStatement(sql.toString());
            int parameterIndex = 1;
            if (roleId > 0) {
                ps.setInt(parameterIndex++, roleId);
            }
            if (nameQuery != null && !nameQuery.isEmpty()) {
                String normalizedNameQuery = Normalizer.normalize(nameQuery, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .replace('đ', 'd')
                        .replace('Đ', 'D');
                    // Cho phép các từ nhập thiếu ký tự khớp theo đúng thứ tự trong tên.
                String namePattern = normalizedNameQuery.replaceAll("\\s+", "%");
                ps.setNString(parameterIndex, "%" + namePattern + "%");
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

    public List<User> getArchivedUsers(boolean isAdmin, int roleId, String nameQuery) {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT u.user_id, u.username, u.email, u.full_name, u.phone, ")
                .append("u.role_id, r.role_name, u.is_active FROM Users u ")
                .append("JOIN Roles r ON u.role_id = r.role_id WHERE u.is_deleted = 1 ");
        if (!isAdmin) {
            sql.append("AND ").append(STAFF_ROLE_FILTER).append(' ');
        }
        if (roleId > 0) {
            sql.append("AND u.role_id = ? ");
        }
        if (nameQuery != null && !nameQuery.isEmpty()) {
            sql.append("AND REPLACE(REPLACE(u.full_name, N'Đ', N'D'), N'đ', N'd') ")
                    .append("COLLATE Latin1_General_CI_AI LIKE ? ");
        }
        sql.append("ORDER BY u.user_id DESC");
        try {
            if (connection == null) {
                return users;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int parameterIndex = 1;
                if (roleId > 0) {
                    statement.setInt(parameterIndex++, roleId);
                }
                if (nameQuery != null && !nameQuery.isEmpty()) {
                    String normalizedNameQuery = Normalizer.normalize(nameQuery, Normalizer.Form.NFD)
                            .replaceAll("\\p{M}+", "")
                            .replace('đ', 'd')
                            .replace('Đ', 'D');
                    String namePattern = normalizedNameQuery.replaceAll("\\s+", "%");
                    statement.setNString(parameterIndex, "%" + namePattern + "%");
                }
                try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    users.add(readUser(results));
                }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Manager chỉ được chọn vai trò nhân viên; Admin được chọn mọi vai trò.
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

    // Tìm tài khoản cần sửa hoặc đổi trạng thái trong phạm vi được phép quản lý.
    public User getManageableUserById(int userId, boolean isAdmin) {
        String sql = "SELECT u.user_id, u.username, u.email, u.full_name, u.phone, u.role_id, r.role_name, u.is_active "
            + "FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.user_id = ? AND u.is_deleted = 0 "
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

    // Thêm tài khoản mới nếu vai trò được phép cấp cho người đang thao tác.
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

    // Cập nhật thông tin; chỉ đổi mật khẩu khi form có nhập mật khẩu mới.
    public boolean updateManagedUser(User user, boolean isAdmin) {
        if (connection == null || !isManageableUser(user.getUserId(), isAdmin)
                || !isAllowedRole(user.getRoleId(), isAdmin)) {
            return false;
        }
        User currentUser = getManageableUserById(user.getUserId(), true);
        if (currentUser == null) {
            return false;
        }
        if ("Admin".equalsIgnoreCase(currentUser.getRoleName()) && !isAdminRole(user.getRoleId())) {
            return false;
        }
        boolean updatePassword = user.getPassword() != null && !user.getPassword().isEmpty();
        String sql = "UPDATE Users SET username = ?, email = ?, full_name = ?, phone = ?, role_id = ?"
            + (updatePassword ? ", password = ?" : "") + " WHERE user_id = ? AND is_deleted = 0"
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

    // Xóa mềm thay vì xóa cứng để giữ lịch sử; không cho tự xóa hoặc vượt phạm vi quản lý.
    public boolean deleteManagedUser(int userId, int actorId, boolean isAdmin) {
        if (connection == null || userId == actorId || !isManageableUser(userId, isAdmin)) {
            return false;
        }
        String sql = "UPDATE Users SET is_active = 0, is_deleted = 1 WHERE user_id = ? AND user_id <> ? AND is_deleted = 0"
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

    public boolean restoreManagedUser(int userId, int actorId, boolean isAdmin) {
        if (connection == null || userId == actorId) {
            return false;
        }
        String sql = "UPDATE Users SET is_deleted = 0, is_active = 0 WHERE user_id = ? AND is_deleted = 1"
                + (isAdmin ? "" : " AND role_id IN (SELECT role_id FROM Roles WHERE role_name IN ('Server', 'Chef', 'Receptionist'))");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Khóa/mở tài khoản, đồng thời không cho người dùng tự đổi trạng thái của mình.
    public boolean setManagedUserActive(int userId, int actorId, boolean isActive, boolean isAdmin) {
        if (connection == null || userId == actorId || !isManageableUser(userId, isAdmin)) {
            return false;
        }
        String sql = "UPDATE Users SET is_active = ? WHERE user_id = ? AND user_id <> ? AND is_deleted = 0"
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

    // Kiểm tra vai trò được gán có nằm trong phạm vi quyền của người thao tác không.
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

    private boolean isAdminRole(int roleId) {
        String sql = "SELECT 1 FROM Roles WHERE role_id = ? AND role_name = 'Admin'";
        try {
            if (connection == null) {
                return false;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, roleId);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kiểm tra tài khoản mục tiêu có thuộc phạm vi quản lý không.
    private boolean isManageableUser(int userId, boolean isAdmin) {
        String sql = "SELECT 1 FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.user_id = ? AND u.is_deleted = 0 "
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

    // Xác định các vai trò nhân viên mà Manager được phép quản lý.
    private boolean isStaffRole(String roleName) {
        return "Server".equalsIgnoreCase(roleName)
                || "Chef".equalsIgnoreCase(roleName)
                || "Receptionist".equalsIgnoreCase(roleName);
    }

    // Đọc một dòng kết quả SQL và chuyển thành đối tượng User.
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
