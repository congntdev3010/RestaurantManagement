package controller.customer;

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

    /** Số điện thoại hợp lệ phải có 10 chữ số và bắt đầu bằng số 0. */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0\\d{9}$");

    /**
     * Yêu cầu đăng nhập, chuyển flash message sang request và hiển thị trang hồ sơ.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (requireUser(request, response) == null) {
            return;
        }

        // Chuyển thông báo khỏi session để sau lần hiển thị này nó không lặp lại.
        HttpSession session = request.getSession();
        request.setAttribute("profileMessage", session.getAttribute("profileMessage"));
        request.setAttribute("profileMessageType", session.getAttribute("profileMessageType"));
        request.setAttribute("profileOpenSection", session.getAttribute("profileOpenSection"));
        session.removeAttribute("profileMessage");
        session.removeAttribute("profileMessageType");
        session.removeAttribute("profileOpenSection");
        // Điểm tích lũy hiện có của khách (cùng nguồn với trang Điểm tích lũy).
        try (dao.LoyaltyDAO loyalty = new dao.LoyaltyDAO()) {
            request.setAttribute("points", loyalty.getBalance(((User) session.getAttribute("user")).getUserId()));
        }
        request.getRequestDispatcher("/WEB-INF/views/customer/profile.jsp").forward(request, response);
    }

    /**
     * Xử lý cập nhật hồ sơ hoặc đổi mật khẩu, sau đó redirect để tránh gửi lại form.
     */
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

        // Mỗi action có bộ kiểm tra riêng; chỉ DAO mới quyết định thao tác DB thành công.
        if ("updateProfile".equals(action)) {
            openSection = "profile";
            String fullName = trim(request.getParameter("fullName"));
            String phone = trim(request.getParameter("phone"));
            if (fullName == null || fullName.isEmpty() || fullName.length() > 30) {
                message = "Họ và tên là bắt buộc và tối đa 30 ký tự.";
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
            openSection = "password";
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");
            if (currentPassword == null || currentPassword.isEmpty()) {
                message = "Vui lòng nhập mật khẩu hiện tại.";
            } else if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 30) {
                message = "Mật khẩu mới phải có từ 8 đến 30 ký tự.";
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

        // Lưu thông báo một lần và mở lại đúng phần form sau redirect.
        HttpSession session = request.getSession();
        session.setAttribute("profileMessage", message);
        session.setAttribute("profileMessageType", messageType);
        session.setAttribute("profileOpenSection", openSection);
        response.sendRedirect(request.getContextPath() + "/profile");
    }

    /**
     * Lấy người dùng từ session; nếu chưa đăng nhập thì chuyển tới trang login.
     */
    private User requireUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        return (User) sessionUser;
    }

    /** Xóa khoảng trắng đầu/cuối nhưng giữ nguyên null để validation phát hiện dữ liệu thiếu. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
