package feature.user.dao;

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

    /**
     * Đăng nhập Google: tìm tài khoản chưa bị xóa theo email đã được Google xác
     * minh. Trả về cả tài khoản bị khóa (isActive = false) để servlet báo đúng
     * lý do.
     */
    public User findByEmail(String email) {
        String sql = "SELECT u.user_id, u.username, u.email, u.full_name, u.phone, u.role_id, r.role_name, u.is_active "
                + "FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.email = ? AND u.is_deleted = 0";
        if (connection == null) {
            return null;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Tạo tài khoản Customer cho người đăng nhập Google lần đầu. Mật khẩu là
     * chuỗi ngẫu nhiên (không ai biết) nên tài khoản chỉ vào được bằng Google,
     * hoặc qua "Quên mật khẩu" để tự đặt.
     */
    public boolean createGoogleCustomer(String email, String fullName) {
        String sql = "INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) "
                + "VALUES (NULL, ?, ?, ?, NULL, 1, 1)";
        if (connection == null) {
            return false;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, java.util.UUID.randomUUID().toString());
            ps.setNString(3, fullName == null || fullName.isBlank() ? email : fullName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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

    /**
     * Cập nhật tên và số điện thoại của tài khoản đang hoạt động, chưa bị xóa.
     */
    public boolean updateProfile(int userId, String fullName, String phone) {
        String sql = "UPDATE Users SET full_name = ?, phone = ? "
                + "WHERE user_id = ? AND is_active = 1 AND is_deleted = 0";
        if (connection == null) {
            return false;
        }

        // Điều kiện trạng thái bảo vệ dữ liệu kể cả khi DAO được gọi ngoài profile flow.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, fullName);
            statement.setString(2, phone);
            statement.setInt(3, userId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đổi mật khẩu khi mật khẩu hiện tại khớp và tài khoản còn hoạt động, chưa
     * bị xóa.
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        String sql = "UPDATE Users SET password = ? "
                + "WHERE user_id = ? AND password = ? AND is_active = 1 AND is_deleted = 0";
        if (connection == null) {
            return false;
        }

        // So sánh mật khẩu trong chính câu UPDATE để tránh cửa sổ race giữa kiểm tra và ghi.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newPassword);
            statement.setInt(2, userId);
            statement.setString(3, currentPassword);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy tài khoản chưa xóa theo phạm vi của Admin/Manager và các bộ lọc được
     * chọn.
     */
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
        if (connection == null) {
            return users;
        }

        // Manager chỉ nhận kết quả thuộc nhóm nhân viên; giá trị bộ lọc luôn bind an toàn.
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindManageableUserFilters(statement, roleId, nameQuery);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    users.add(readUser(results));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Lấy tài khoản đã xóa mềm, giữ nguyên giới hạn vai trò của Admin/Manager.
     */
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
        if (connection == null) {
            return users;
        }

        // Dữ liệu đã xóa được lọc riêng, không trộn với danh sách tài khoản đang hoạt động.
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindManageableUserFilters(statement, roleId, nameQuery);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    users.add(readUser(results));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Lấy các vai trò mà người thao tác được phép gán cho tài khoản.
     */
    public Map<Integer, String> getManageableRoles(boolean isAdmin) {
        Map<Integer, String> roles = new LinkedHashMap<>();
        String sql = "SELECT role_id, role_name FROM Roles "
                + (isAdmin ? "" : "WHERE role_name IN ('Server', 'Chef', 'Receptionist') ")
                + "ORDER BY role_id";
        try {
            if (connection == null) {
                return roles;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    roles.put(results.getInt("role_id"), results.getString("role_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return roles;
    }

    /**
     * Tìm tài khoản chưa xóa trong phạm vi vai trò mà Admin/Manager được quản
     * lý.
     */
    public User getManageableUserById(int userId, boolean isAdmin) {
        String sql = "SELECT u.user_id, u.username, u.email, u.full_name, u.phone, u.role_id, r.role_name, u.is_active "
                + "FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.user_id = ? AND u.is_deleted = 0 "
                + (isAdmin ? "" : "AND " + STAFF_ROLE_FILTER);
        try {
            if (connection == null) {
                return null;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet results = statement.executeQuery()) {
                    return results.next() ? readUser(results) : null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Tạo tài khoản mới sau khi xác nhận vai trò nằm trong phạm vi được phép.
     */
    public boolean createManagedUser(User user, boolean isAdmin) {
        if (connection == null || !isAllowedRole(user.getRoleId(), isAdmin)) {
            return false;
        }
        String sql = "INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) "
                + "SELECT ?, ?, ?, ?, ?, role_id, ? FROM Roles WHERE role_id = ?"
                + (isAdmin ? "" : " AND role_name IN ('Server', 'Chef', 'Receptionist')");
        // Điều kiện role được lặp lại trong INSERT để không phụ thuộc riêng vào kiểm tra trước đó.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setNString(4, user.getFullName());
            statement.setString(5, user.getPhone());
            statement.setBoolean(6, user.isIsActive());
            statement.setInt(7, user.getRoleId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cập nhật tài khoản trong phạm vi quyền; mật khẩu chỉ đổi nếu được nhập.
     */
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
        // Không cho Manager đổi Admin hoặc gán role ngoài phạm vi, kể cả khi request bị giả mạo.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getEmail());
            statement.setNString(3, user.getFullName());
            statement.setString(4, user.getPhone());
            statement.setInt(5, user.getRoleId());
            int userIdParameter = 6;
            if (updatePassword) {
                statement.setString(6, user.getPassword());
                userIdParameter++;
            }
            statement.setInt(userIdParameter, user.getUserId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xóa mềm để giữ lịch sử và không cho thao tác lên chính tài khoản đang
     * đăng nhập.
     */
    public boolean deleteManagedUser(int userId, int actorId, boolean isAdmin) {
        if (connection == null || userId == actorId || !isManageableUser(userId, isAdmin)) {
            return false;
        }
        String sql = "UPDATE Users SET is_active = 0, is_deleted = 1 WHERE user_id = ? AND user_id <> ? AND is_deleted = 0"
                + (isAdmin ? "" : " AND role_id IN (SELECT role_id FROM Roles WHERE role_name IN ('Server', 'Chef', 'Receptionist'))");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, actorId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Khôi phục tài khoản dưới trạng thái khóa; tài khoản phải thuộc phạm vi
     * được quản lý.
     */
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

    /**
     * Khóa hoặc mở tài khoản, không cho người thao tác tự đổi trạng thái của
     * mình.
     */
    public boolean setManagedUserActive(int userId, int actorId, boolean isActive, boolean isAdmin) {
        if (connection == null || userId == actorId || !isManageableUser(userId, isAdmin)) {
            return false;
        }
        String sql = "UPDATE Users SET is_active = ? WHERE user_id = ? AND user_id <> ? AND is_deleted = 0"
                + (isAdmin ? "" : " AND role_id IN (SELECT role_id FROM Roles WHERE role_name IN ('Server', 'Chef', 'Receptionist'))");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, isActive);
            statement.setInt(2, userId);
            statement.setInt(3, actorId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xác nhận role tồn tại và được phép gán bởi Admin hoặc Manager.
     */
    private boolean isAllowedRole(int roleId, boolean isAdmin) {
        String sql = "SELECT role_name FROM Roles WHERE role_id = ?";
        try {
            if (connection == null) {
                return false;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, roleId);
                try (ResultSet results = statement.executeQuery()) {
                    return results.next() && (isAdmin || isStaffRole(results.getString("role_name")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Trả về true nếu role ID tương ứng với vai trò Admin.
     */
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

    /**
     * Kiểm tra tài khoản còn hoạt động và nằm trong phạm vi vai trò được quản
     * lý.
     */
    private boolean isManageableUser(int userId, boolean isAdmin) {
        String sql = "SELECT 1 FROM Users u JOIN Roles r ON u.role_id = r.role_id WHERE u.user_id = ? AND u.is_deleted = 0 "
                + (isAdmin ? "" : "AND " + STAFF_ROLE_FILTER);
        try {
            if (connection == null) {
                return false;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet results = statement.executeQuery()) {
                    return results.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xác định ba vai trò nhân viên Manager được phép quản lý.
     */
    private boolean isStaffRole(String roleName) {
        return "Server".equalsIgnoreCase(roleName)
                || "Chef".equalsIgnoreCase(roleName)
                || "Receptionist".equalsIgnoreCase(roleName);
    }

    /**
     * Ánh xạ dòng kết quả truy vấn sang đối tượng User.
     */
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

    /**
     * Bind role và tên tìm kiếm theo đúng thứ tự các điều kiện tùy chọn của
     * danh sách.
     */
    private void bindManageableUserFilters(PreparedStatement statement, int roleId, String nameQuery)
            throws SQLException {
        int parameterIndex = 1;
        if (roleId > 0) {
            statement.setInt(parameterIndex++, roleId);
        }
        if (nameQuery != null && !nameQuery.isEmpty()) {
            statement.setNString(parameterIndex, toNameSearchPattern(nameQuery));
        }
    }

    /**
     * Bỏ dấu tiếng Việt và đổi khoảng trắng thành wildcard để tìm tên theo thứ
     * tự từ.
     */
    private String toNameSearchPattern(String nameQuery) {
        String normalizedNameQuery = Normalizer.normalize(nameQuery, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return "%" + normalizedNameQuery.replaceAll("\\s+", "%") + "%";
    }
}
