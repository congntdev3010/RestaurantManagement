package controller.staff;

import dao.ManagerDashboardDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.User;

@WebServlet(name = "ManagerDashboardServlet", urlPatterns = {"/manager-dashboard"})
public class ManagerDashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User user = (User) value;
        if (user.getRoleName() == null || !"Manager".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (ManagerDashboardDAO dashboardDao = new ManagerDashboardDAO()) {
            request.setAttribute("dashboard", dashboardDao.getDashboard());
            request.getRequestDispatcher("/WEB-INF/views/staff/manager-dashboard.jsp")
                    .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Không thể tải dữ liệu dashboard quản lý.", e);
        }
    }
}
