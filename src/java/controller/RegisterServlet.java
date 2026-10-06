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

    // Regex chuẩn Email & Số điện thoại bắt đầu bằng 0 và đủ 10 chữ số
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final String PHONE_REGEX = "^0\\d{9}$";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String pass = request.getParameter("password");
        String confirmPass = request.getParameter("confirmPassword");

        if (fullName != null) fullName = fullName.trim();
        if (username != null) username = username.trim();
        if (email != null) email = email.trim();
        if (phone != null) phone = phone.trim();

        UserDAO dao = new UserDAO();

        // 1. Kiểm tra Username duy nhất
        if (dao.checkUsernameExists(username)) {
            request.setAttribute("error", "Tên tài khoản đã tồn tại, vui lòng chọn tên khác!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 2. Kiểm tra định dạng & trùng lặp Email
        if (email == null || !email.matches(EMAIL_REGEX)) {
            request.setAttribute("error", "Định dạng email không hợp lệ!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }
        if (dao.checkEmailExists(email)) {
            request.setAttribute("error", "Email này đã được đăng ký!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 3. Kiểm tra Số điện thoại (bắt đầu từ 0 và đủ 10 chữ số)
        if (phone == null || !phone.matches(PHONE_REGEX)) {
            request.setAttribute("error", "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 4. Kiểm tra độ dài Mật khẩu (tối thiểu 8 ký tự)
        if (pass == null || pass.length() < 8) {
            request.setAttribute("error", "Mật khẩu phải có tối thiểu 8 ký tự!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 5. Kiểm tra Mật khẩu nhập lại
        if (!pass.equals(confirmPass)) {
            request.setAttribute("error", "Mật khẩu nhập lại không khớp!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // Tất cả hợp lệ -> Tạo OTP và lưu dữ liệu TẠM vào Session (chưa chèn DB)
        String otp = EmailUtils.generateOTP();
        boolean sent = EmailUtils.sendOTPEmail(email, otp);

        if (sent) {
            User tempUser = new User(username, email, pass, fullName, phone, 1);
            
            HttpSession session = request.getSession();
            session.setAttribute("tempUser", tempUser);
            session.setAttribute("registerEmail", email);
            session.setAttribute("registerOTP", otp);
            session.setAttribute("otpTime", System.currentTimeMillis());

            response.sendRedirect("verify-otp.jsp");
        } else {
            request.setAttribute("error", "Gửi mã OTP thất bại, vui lòng kiểm tra lại Email!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }
}