package controller.admin;

import controller.customer.CartServlet;
import dao.ReviewDAO;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import utils.ReviewAccess;

@WebServlet(name = "ManageReviewsServlet", urlPatterns = {"/manage-reviews"})
public class ManageReviewsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User manager = requireManager(request, response);
        if (manager == null) return;
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        request.setAttribute("manageReviewMessage", session.getAttribute("manageReviewMessage"));
        request.setAttribute("manageReviewMessageType", session.getAttribute("manageReviewMessageType"));
        session.removeAttribute("manageReviewMessage");
        session.removeAttribute("manageReviewMessageType");
        try (ReviewDAO dao = new ReviewDAO()) {
            request.setAttribute("pendingReviews", dao.getPendingReviews());
        } catch (SQLException ex) {
            getServletContext().log("Could not load pending reviews", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải đánh giá chờ duyệt.");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/manage-reviews.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User manager = requireManager(request, response);
        if (manager == null) return;
        HttpSession session = request.getSession(false);
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int reviewId = positiveInt(request.getParameter("reviewId"));
        String action = request.getParameter("action");
        boolean success = false;
        String message;
        try (ReviewDAO dao = new ReviewDAO()) {
            if (reviewId > 0 && "approve".equals(action)) {
                success = dao.approve(reviewId, manager.getUserId());
                message = success ? "Đã duyệt đánh giá." : "Đánh giá không còn chờ duyệt.";
            } else if (reviewId > 0 && "reject".equals(action)) {
                success = dao.reject(reviewId);
                message = success ? "Đã từ chối và xóa đánh giá." : "Đánh giá không còn chờ duyệt.";
            } else {
                message = "Yêu cầu không hợp lệ.";
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not moderate review " + reviewId, ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể cập nhật đánh giá.");
            return;
        }
        session.setAttribute("manageReviewMessage", message);
        session.setAttribute("manageReviewMessageType", success ? "success" : "error");
        response.sendRedirect(request.getContextPath() + "/manage-reviews");
    }

    private User requireManager(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) value;
        if (!ReviewAccess.isManager(user)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    private int positiveInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return -1; }
    }
}
