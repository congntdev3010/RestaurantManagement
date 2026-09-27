package controller;

import dao.UserDAO;
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
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("emailRegister");
        String otpInput = request.getParameter("otp");

        if (email == null) {
            response.sendRedirect("register.jsp");
            return;
        }

        UserDAO dao = new UserDAO();
        if (dao.verifyOTP(email, otpInput)) {
            dao.activateUser(email);
            session.removeAttribute("emailRegister");
            request.setAttribute("success", "Xác thực thành công! Vui lòng đăng nhập.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Mã OTP không đúng hoặc đã hết hạn!");
            request.getRequestDispatcher("verify-otp.jsp").forward(request, response);
        }
    }
}