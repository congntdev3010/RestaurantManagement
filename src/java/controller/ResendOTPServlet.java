package controller;

import model.User;
import utils.EmailUtils;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ResendOTPServlet", urlPatterns = {"/resend-otp"})
public class ResendOTPServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        
        // 1. Lấy email từ Session (kiểm tra cả registerEmail và tempUser)
        String email = (String) session.getAttribute("registerEmail");
        if (email == null) {
            User tempUser = (User) session.getAttribute("tempUser");
            if (tempUser != null) {
                email = tempUser.getEmail();
            }
        }

        // Nếu hoàn toàn không có email trong Session -> mới chuyển hướng về register.jsp
        if (email == null || email.trim().isEmpty()) {
            response.sendRedirect("register.jsp");
            return;
        }

        // 2. Tạo mã OTP mới
        String newOTP = EmailUtils.generateOTP();

        // 3. Cập nhật lại OTP mới và reset mốc thời gian vào Session (dùng đồng bộ key "registerOTP")
        session.setAttribute("registerOTP", newOTP);
        session.setAttribute("otpTime", System.currentTimeMillis());
        session.setAttribute("registerEmail", email);

        // 4. Gửi Mail
        boolean sent = EmailUtils.sendOTPEmail(email, newOTP);

        if (sent) {
            request.setAttribute("success", "Mã OTP mới đã được gửi đến email của bạn!");
        } else {
            request.setAttribute("error", "Gửi email thất bại. Vui lòng thử lại sau!");
        }

        request.getRequestDispatcher("verify-otp.jsp").forward(request, response);
    }
}