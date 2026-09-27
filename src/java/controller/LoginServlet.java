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

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String account = request.getParameter("account");
        String pass = request.getParameter("password");

        UserDAO dao = new UserDAO();
        User user = dao.login(account, pass);

        if (user != null) {
            HttpSession session = request.getSession();
            session.setAttribute("account", user);
            response.sendRedirect("home.jsp");
        } else {
            request.setAttribute("error", "Tài khoản/Mật khẩu không đúng hoặc chưa được kích hoạt!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
// 1. Đọc dữ liệu từ Form
//        String account = request.getParameter("account");
//        String pass = request.getParameter("password");
//
//        // In thẳng kết quả nhận được ra màn hình Web để kiểm tra
//        response.setContentType("text/html;charset=UTF-8");
//        java.io.PrintWriter out = response.getWriter();
//        out.println("<h3>Account nhận được: " + account + "</h3>");
//        out.println("<h3>Password nhận được: " + pass + "</h3>");
//
//        UserDAO dao = new UserDAO();
//        User user = dao.login(account, pass);
//
//        out.println("<h3>Kết quả Login DAO: " + (user != null ? "Thành công - " + user.getFullName() : "NULL (Thất bại)") + "</h3>");
//        return; // Tạm dừng redirect để xem kết quả in trên web
    }
}
