package controller;

import dao.UserDAO;
import model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "VerifyOTPServlet", urlPatterns = {"/verify-otp"})
public class VerifyOTPServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String inputOtp = request.getParameter("otp");
        HttpSession session = request.getSession();

        User tempUser = (User) session.getAttribute("tempUser");
        String sessionOtp = (String) session.getAttribute("registerOTP");

        // Nếu hết hạn session hoặc truy cập trái phép
        if (tempUser == null || sessionOtp == null) {
            request.setAttribute("error", "Phiên xác thực đã hết hạn. Vui lòng đăng ký lại!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // So sánh OTP người dùng nhập với OTP trong Session
        if (inputOtp != null && inputOtp.trim().equals(sessionOtp)) {
            UserDAO dao = new UserDAO();
            
            // Nhập đúng OTP -> Mới chính thức lưu tài khoản vào Database (is_active = 1)
            if (dao.registerCustomer(tempUser)) {
                // Xóa dữ liệu tạm khỏi Session
                session.removeAttribute("tempUser");
                session.removeAttribute("registerOTP");

                response.sendRedirect("login.jsp");
            } else {
                request.setAttribute("error", "Đã có lỗi xảy ra khi tạo tài khoản, vui lòng thử lại!");
                request.getRequestDispatcher("verify-otp.jsp").forward(request, response);
            }
        } else {
            request.setAttribute("error", "Mã OTP không chính xác!");
            request.getRequestDispatcher("verify-otp.jsp").forward(request, response);
        }
    }
}