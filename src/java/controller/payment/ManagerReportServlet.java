package controller.payment;

import dao.ManagerReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import model.User;

@WebServlet(name = "ManagerReportServlet", urlPatterns = {"/manager-report"})
public class ManagerReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!"Manager".equalsIgnoreCase(((User) sessionUser).getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LocalDate today = LocalDate.now();
        try (ManagerReportDAO dao = new ManagerReportDAO()) {
            request.setAttribute("revenueSummary", dao.getRevenueSummary(today));
            request.setAttribute("categoryReports", dao.getSalesByCategory());
            request.setAttribute("reportDate", today);
        } catch (java.sql.SQLException e) {
            throw new ServletException("Không thể tải báo cáo quản lý.", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/payment/manager-report.jsp").forward(request, response);
    }
}
