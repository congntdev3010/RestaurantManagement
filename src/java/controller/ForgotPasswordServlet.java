package controller;

import dao.UserDAO;
import utils.EmailUtils;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email");
        if (email != null) email = email.trim();

        UserDAO dao = new UserDAO();

        // 1. Kiểm tra Email có tồn tại trong hệ thống hay không
        if (!dao.checkEmailExists(email)) {
            request.setAttribute("forgotError", "Email này chưa được đăng ký trong hệ thống!");
            request.setAttribute("showForgotModal", true); // Giữ modal mở nếu có lỗi
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // 2. Tạo mật khẩu mới 8 ký tự
        String newPassword = EmailUtils.generateRandomPassword();

        // 3. Cập nhật vào DB & gửi Email
        if (dao.updatePasswordByEmail(email, newPassword)) {
            EmailUtils.sendNewPasswordEmail(email, newPassword);
            
            // Đặt thông báo thành công để hiển thị ở trang Đăng nhập
            request.setAttribute("success", "Hệ thống đã gửi mật khẩu mới qua email của bạn!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        } else {
            request.setAttribute("forgotError", "Đã có lỗi xảy ra, vui lòng thử lại sau!");
            request.setAttribute("showForgotModal", true);
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}