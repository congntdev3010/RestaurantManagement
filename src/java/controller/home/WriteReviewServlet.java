package controller.home;

import dao.ReviewDAO;
import dao.ServiceHistoryDAO;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.PaidBill;
import model.User;

@WebServlet(name = "WriteReviewServlet", urlPatterns = {"/write-review"})
public class WriteReviewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User customer = getCustomer(request, response);
        if (customer == null) return;
        int historyId = parseId(request.getParameter("historyId"));
        try (ServiceHistoryDAO bills = new ServiceHistoryDAO(); ReviewDAO reviews = new ReviewDAO()) {
            PaidBill bill = historyId > 0 ? bills.getForCustomer(historyId, customer.getUserId()) : null;
            if (bill == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            if (reviews.hasReview(historyId, customer.getUserId())) {
                request.setAttribute("alreadyReviewed", true);
            }
            request.setAttribute("bill", bill);
            request.getRequestDispatcher("/WEB-INF/views/review/write-review.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Could not load review form", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private User getCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        User user = (User) value;
        if (!"Customer".equalsIgnoreCase(user.getRoleName())) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return null; }
        return user;
    }

    private int parseId(String value) { try { return Integer.parseInt(value); } catch (Exception ex) { return -1; } }
}
