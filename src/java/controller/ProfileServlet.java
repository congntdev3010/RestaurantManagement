package controller;

import dao.UserDAO;
import java.io.IOException;
import java.util.regex.Pattern;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    // Hiển thị hồ sơ và lấy thông báo sau khi người dùng gửi form.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User user = requireUser(request, response);
        if (user == null) {
            return;
        }

        HttpSession session = request.getSession();
        request.setAttribute("profileMessage", session.getAttribute("profileMessage"));
        request.setAttribute("profileMessageType", session.getAttribute("profileMessageType"));
        request.setAttribute("profileOpenSection", session.getAttribute("profileOpenSection"));
        session.removeAttribute("profileMessage");
        session.removeAttribute("profileMessageType");
        session.removeAttribute("profileOpenSection");
        request.getRequestDispatcher("/profile.jsp").forward(request, response);
    }

    // Xử lý cập nhật thông tin cá nhân hoặc đổi mật khẩu.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = requireUser(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        String message;
        String messageType = "error";
        String openSection;
        UserDAO dao = new UserDAO();

        if ("updateProfile".equals(action)) {
            // Kiểm tra dữ liệu trước khi lưu, rồi cập nhật thông tin trong session.
            openSection = "profile";
            String fullName = trim(request.getParameter("fullName"));
            String phone = trim(request.getParameter("phone"));
            if (fullName == null || fullName.isEmpty() || fullName.length() > 100) {
                message = "Họ và tên là bắt buộc và tối đa 100 ký tự.";
            } else if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
                message = "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số.";
            } else if (dao.updateProfile(user.getUserId(), fullName, phone)) {
                user.setFullName(fullName);
                user.setPhone(phone);
                message = "Đã cập nhật thông tin cá nhân.";
                messageType = "success";
            } else {
                message = "Không thể cập nhật thông tin. Vui lòng thử lại.";
            }
        } else if ("changePassword".equals(action)) {
            // Chỉ đổi mật khẩu khi mật khẩu hiện tại đúng và xác nhận khớp.
            openSection = "password";
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            if (currentPassword == null || currentPassword.isEmpty()) {
                message = "Vui lòng nhập mật khẩu hiện tại.";
            } else if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 255) {
                message = "Mật khẩu mới phải có từ 8 đến 255 ký tự.";
            } else if (!newPassword.equals(confirmPassword)) {
                message = "Mật khẩu xác nhận không khớp.";
            } else if (newPassword.equals(currentPassword)) {
                message = "Mật khẩu mới phải khác mật khẩu hiện tại.";
            } else if (dao.changePassword(user.getUserId(), currentPassword, newPassword)) {
                message = "Đã đổi mật khẩu thành công.";
                messageType = "success";
            } else {
                message = "Mật khẩu hiện tại không đúng hoặc không thể cập nhật.";
            }
        } else {
            openSection = "profile";
            message = "Thao tác không hợp lệ.";
        }

        HttpSession session = request.getSession();
        session.setAttribute("profileMessage", message);
        session.setAttribute("profileMessageType", messageType);
        session.setAttribute("profileOpenSection", openSection);
        response.sendRedirect(request.getContextPath() + "/profile");
    }

    private User requireUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Chưa đăng nhập thì quay về trang đăng nhập.
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        return (User) sessionUser;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}