package controller.customer;

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

@WebServlet(name = "ReviewServlet", urlPatterns = {"/review"})
public class ReviewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        User user = customer(session);
        request.setAttribute("reviewMessage", session.getAttribute("reviewMessage"));
        request.setAttribute("reviewMessageType", session.getAttribute("reviewMessageType"));
        session.removeAttribute("reviewMessage");
        session.removeAttribute("reviewMessageType");
        try (ReviewDAO dao = new ReviewDAO()) {
            request.setAttribute("approvedReviews", dao.getApprovedReviews());
            int[] summary = dao.getApprovedRatingSummary();
            request.setAttribute("reviewCount", summary[0]);
            request.setAttribute("averageRating", summary[1] / 100.0);
        } catch (SQLException ex) {
            getServletContext().log("Could not load reviews", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải đánh giá.");
            return;
        }
        request.setAttribute("isCustomer", user != null);
        request.getRequestDispatcher("/WEB-INF/views/customer/reviews.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = customer(session);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ khách hàng đăng nhập mới có thể gửi đánh giá.");
            return;
        }
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int rating = positiveInt(request.getParameter("rating"));
        String text = trim(request.getParameter("reviewText"));
        String message;
        String type = "error";
        if (rating < 1 || rating > 5 || (text != null && text.length() > 1000)) {
            message = "Vui lòng chọn số sao từ 1 đến 5; nội dung tối đa 1.000 ký tự.";
        } else {
            try (ReviewDAO dao = new ReviewDAO()) {
                dao.submit(user.getUserId(), rating, text);
                message = "Đã gửi đánh giá. Đánh giá sẽ hiển thị sau khi Quản lý duyệt.";
                type = "success";
            } catch (SQLException ex) {
                getServletContext().log("Could not submit review for customer " + user.getUserId(), ex);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể lưu đánh giá.");
                return;
            }
        }
        session.setAttribute("reviewMessage", message);
        session.setAttribute("reviewMessageType", type);
        response.sendRedirect(request.getContextPath() + "/review");
    }

    private User customer(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("user");
        if (value instanceof User && ReviewAccess.isCustomer((User) value)) return (User) value;
        return null;
    }

    private int positiveInt(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return -1; }
    }

    private String trim(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
