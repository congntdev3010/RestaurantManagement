package dao;

import dal.DBContext;
import model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO extends DBContext {

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

    // 3. Tạo tài khoản Customer mới (mặc định is_active = 0)
    public boolean registerCustomer(User user) {
        String sql = "INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) VALUES (?, ?, ?, ?, ?, 1, 0)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getEmail());
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
}
