package controller.home;

import dao.ReviewDAO;
import java.io.IOException;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;

@WebServlet(name = "ReviewServlet", urlPatterns = {"/review"})
public class ReviewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (ReviewDAO dao = new ReviewDAO()) {
            request.setAttribute("reviews", dao.findApproved());
            request.setAttribute("averageRating", dao.averageRating());
            request.setAttribute("reviewCount", dao.approvedCount());
            request.getRequestDispatcher("/WEB-INF/views/review/reviews.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Could not load approved restaurant reviews", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute("user");
        if (!(value instanceof User) || !"Customer".equalsIgnoreCase(((User) value).getRoleName())) {
            response.sendRedirect(request.getContextPath() + "/login"); return;
        }
        User customer = (User) value;
        int historyId = positiveInt(request.getParameter("historyId"));
        int rating = positiveInt(request.getParameter("rating"));
        String reviewText = request.getParameter("reviewText");
        if (reviewText != null) reviewText = reviewText.trim();
        String message = "";
        try (ReviewDAO dao = new ReviewDAO()) {
            if (historyId < 1 || rating < 1 || rating > 5 || reviewText == null || reviewText.isEmpty() || reviewText.length() > 1000) {
                message = "Vui lòng chọn số sao và viết đánh giá tối đa 1.000 ký tự.";
            } else if (dao.hasReview(historyId, customer.getUserId())) {
                message = "Hóa đơn này đã có đánh giá.";
            } else if (dao.submit(historyId, customer.getUserId(), rating, reviewText)) {
                message = "Cảm ơn bạn! Đánh giá đang chờ quản lý duyệt.";
            } else {
                message = "Không tìm thấy hóa đơn hợp lệ của tài khoản này.";
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not save restaurant review", ex);
            message = "Chưa thể gửi đánh giá lúc này. Vui lòng thử lại.";
        }
        request.getSession().setAttribute("reviewMessage", message);
        response.sendRedirect(request.getContextPath() + "/my-payments");
    }

    private int positiveInt(String value) { try { return Integer.parseInt(value); } catch (Exception e) { return -1; } }
}
