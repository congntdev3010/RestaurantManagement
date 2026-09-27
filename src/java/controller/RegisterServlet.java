package controller;

import dao.UserDAO;
import model.User;
import utils.EmailUtils;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String pass = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        
        UserDAO dao = new UserDAO();

        if (dao.checkEmailExists(email)) {
            request.setAttribute("error", "Email này đã được đăng ký!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        User newUser = new User(email, email, pass, fullName, phone, 1);
        if (dao.registerCustomer(newUser)) {
            String otp = EmailUtils.generateOTP();
            dao.saveOTP(email, otp);
            EmailUtils.sendOTPEmail(email, otp);

            HttpSession session = request.getSession();
            session.setAttribute("emailRegister", email);
            response.sendRedirect("verify-otp.jsp");
        } else {
            request.setAttribute("error", "Đăng ký thất bại, vui lòng thử lại!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }
}