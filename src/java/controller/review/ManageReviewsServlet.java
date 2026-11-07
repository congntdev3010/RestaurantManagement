package controller.review;

import dao.CustomerReviewDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

@WebServlet(name = "ManageReviewsServlet", urlPatterns = {"/manage-reviews"})
public class ManageReviewsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User manager = requireManager(request, response);
        if (manager == null) {
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession();
        ensureToken(session);
        request.setAttribute("reviewMessage", session.getAttribute("manageReviewMessage"));
        request.setAttribute("reviewMessageType", session.getAttribute("manageReviewMessageType"));
        session.removeAttribute("manageReviewMessage");
        session.removeAttribute("manageReviewMessageType");
        try (CustomerReviewDAO dao = new CustomerReviewDAO()) {
            request.setAttribute("reviews", dao.findAll());
            request.setAttribute("reviewSummary", dao.getSummary());
        } catch (SQLException ex) {
            getServletContext().log("Could not load customer review management data", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải dữ liệu quản lý đánh giá.");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/review/manage-reviews.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User manager = requireManager(request, response);
        if (manager == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (!validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int reviewId = positiveInt(request.getParameter("reviewId"));
        String action = request.getParameter("action");
        String targetStatus = request.getParameter("targetStatus");
        String message = "Không thể cập nhật đánh giá.";
        String type = "error";
        try (CustomerReviewDAO dao = new CustomerReviewDAO()) {
            if (reviewId > 0 && "delete".equals(action) && dao.delete(reviewId)) {
                message = "Đã xóa đánh giá.";
                type = "success";
            } else if (reviewId > 0 && "moderate".equals(action)
                    && dao.moderate(reviewId, manager.getUserId(), targetStatus)) {
                message = "Đã cập nhật trạng thái đánh giá.";
                type = "success";
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not update customer review", ex);
            message = "Có lỗi khi cập nhật đánh giá. Vui lòng thử lại.";
        }
        session.setAttribute("manageReviewMessage", message);
        session.setAttribute("manageReviewMessageType", type);
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
        if (!"Manager".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    private static void ensureToken(HttpSession session) {
        if (session.getAttribute("reviewCsrf") == null) {
            session.setAttribute("reviewCsrf", UUID.randomUUID().toString());
        }
    }

    private static boolean validToken(HttpSession session, String token) {
        Object expected = session == null ? null : session.getAttribute("reviewCsrf");
        return expected instanceof String && expected.equals(token);
    }

    private static int positiveInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ex) {
            return -1;
        }
    }
}
