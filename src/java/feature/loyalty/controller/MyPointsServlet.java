package feature.loyalty.controller;

import feature.loyalty.dao.LoyaltyDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;

/** Màn hình điểm tích lũy của khách: số dư, quy tắc quy đổi hiện hành và lịch sử cộng/trừ điểm. */
@WebServlet(name = "MyPointsServlet", urlPatterns = {"/my-points"})
public class MyPointsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if (!(u instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User user = (User) u;
        if (!"Customer".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try (LoyaltyDAO dao = new LoyaltyDAO()) {
            request.setAttribute("points", dao.getBalance(user.getUserId()));
            request.setAttribute("history", dao.getHistory(user.getUserId()));
            request.setAttribute("settings", dao.getSettings());
        }
        request.getRequestDispatcher("/WEB-INF/views/loyalty/my-points.jsp").forward(request, response);
    }
}
