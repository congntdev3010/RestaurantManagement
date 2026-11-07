package controller.review;

import dao.CustomerReviewDAO;
import dao.ServiceHistoryDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.CustomerReview;
import model.PaidBill;
import model.User;

@WebServlet(name = "ReviewServlet", urlPatterns = {"/review", "/submit-review"})
public class ReviewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String path = request.getServletPath();
        if ("/submit-review".equals(path)) {
            showReviewForm(request, response);
            return;
        }

        try (CustomerReviewDAO dao = new CustomerReviewDAO()) {
            List<CustomerReview> reviews = dao.findApproved();
            double average = 0;
            for (CustomerReview review : reviews) {
                average += review.getRating();
            }
            request.setAttribute("reviews", reviews);
            request.setAttribute("reviewCount", reviews.size());
            request.setAttribute("averageRating", reviews.isEmpty() ? 0 : average / reviews.size());
        } catch (SQLException ex) {
            getServletContext().log("Could not load approved customer reviews", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải đánh giá.");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/review/reviews.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/submit-review".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        request.setCharacterEncoding("UTF-8");
        User customer = requireCustomer(request, response);
        if (customer == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (!validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int historyId = positiveInt(request.getParameter("historyId"));
        int rating = positiveInt(request.getParameter("rating"));
        String comment = trim(request.getParameter("comment"));
        if (historyId < 1 || rating < 1 || rating > 5 || comment == null || comment.isEmpty()
                || comment.length() > 1000) {
            session.setAttribute("reviewMessage", "Vui lòng chọn số sao và nhập nhận xét tối đa 1.000 ký tự.");
            session.setAttribute("reviewMessageType", "error");
            response.sendRedirect(request.getContextPath() + "/submit-review?billId=" + historyId);
            return;
        }

        try (CustomerReviewDAO dao = new CustomerReviewDAO()) {
            if (dao.submit(historyId, customer.getUserId(), rating, comment)) {
                session.setAttribute("reviewMessage", "Cảm ơn bạn đã gửi đánh giá.");
                session.setAttribute("reviewMessageType", "success");
            } else {
                session.setAttribute("reviewMessage", "Không thể gửi đánh giá cho hóa đơn này.");
                session.setAttribute("reviewMessageType", "error");
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not save customer review", ex);
            session.setAttribute("reviewMessage", "Có lỗi khi gửi đánh giá. Vui lòng thử lại.");
            session.setAttribute("reviewMessageType", "error");
        }
        response.sendRedirect(request.getContextPath() + "/my-payments");
    }

    private void showReviewForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User customer = requireCustomer(request, response);
        if (customer == null) {
            return;
        }
        int billId = positiveInt(request.getParameter("billId"));
        if (billId < 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        HttpSession session = request.getSession();
        ensureToken(session);
        try (ServiceHistoryDAO bills = new ServiceHistoryDAO(); CustomerReviewDAO reviews = new CustomerReviewDAO()) {
            PaidBill bill = bills.getForCustomer(billId, customer.getUserId());
            if (bill == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (reviews.hasSubmittedReview(billId, customer.getUserId())) {
                request.setAttribute("alreadySubmitted", true);
            }
            request.setAttribute("bill", bill);
            request.setAttribute("reviewMessage", session.getAttribute("reviewMessage"));
            request.setAttribute("reviewMessageType", session.getAttribute("reviewMessageType"));
            session.removeAttribute("reviewMessage");
            session.removeAttribute("reviewMessageType");
        } catch (SQLException ex) {
            getServletContext().log("Could not load customer review form", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải màn hình đánh giá.");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/review/submit-review.jsp").forward(request, response);
    }

    private User requireCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) value;
        if (!"Customer".equalsIgnoreCase(user.getRoleName())) {
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

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
