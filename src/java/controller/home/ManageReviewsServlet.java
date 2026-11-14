package controller.home;

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

@WebServlet(name = "ManageReviewsServlet", urlPatterns = {"/manage-reviews"})
public class ManageReviewsServlet extends HttpServlet {
    private User manager(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        User user = (User) value;
        if (!"Manager".equalsIgnoreCase(user.getRoleName())) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return null; }
        return user;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (manager(request, response) == null) return;
        try (ReviewDAO dao = new ReviewDAO()) {
            request.setAttribute("reviews", dao.findAll());
            request.setAttribute("averageRating", dao.averageRating());
            request.setAttribute("totalReviews", dao.totalCount());
            request.setAttribute("pendingCount", dao.pendingCount());
            request.getRequestDispatcher("/WEB-INF/views/review/manage-reviews.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Could not load review moderation", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User actor = manager(request, response); if (actor == null) return;
        int id; try { id = Integer.parseInt(request.getParameter("reviewId")); } catch (Exception ex) { id = -1; }
        String action = request.getParameter("action");
        String status = "approve".equals(action) ? "APPROVED" : "reject".equals(action) ? "REJECTED" : "";
        String message = "Thao tác không hợp lệ.";
        try (ReviewDAO dao = new ReviewDAO()) {
            if (id < 1) {
                message = "Mã đánh giá không hợp lệ.";
            } else if ("delete".equals(action)) {
                message = dao.delete(id) ? "Đã xóa đánh giá." : "Không tìm thấy đánh giá cần xóa.";
            } else if (!status.isEmpty()) {
                message = dao.setStatus(id, status, actor.getUserId())
                        ? ("APPROVED".equals(status) ? "Đã duyệt đánh giá." : "Đã từ chối đánh giá.")
                        : "Đánh giá không còn ở trạng thái chờ duyệt.";
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not moderate restaurant review", ex);
            message = "Chưa thể cập nhật đánh giá lúc này.";
        }
        request.getSession().setAttribute("reviewManageMessage", message);
        response.sendRedirect(request.getContextPath() + "/manage-reviews");
    }
}
