package controller.payment;

import dao.CustomerReviewDAO;
import dao.ServiceHistoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import model.PaidBill;
import model.User;

/**
 * Lịch sử thanh toán của khách thành viên đang đăng nhập: các hóa đơn đã gắn
 * tài khoản khi tích điểm. Chỉ đọc đơn của chính khách này (lọc theo
 * loyalty_customer_id); không hiện tổng doanh thu.
 */
@WebServlet(name = "MyPaymentsServlet", urlPatterns = {"/my-payments"})
public class MyPaymentsServlet extends HttpServlet {

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

        try (ServiceHistoryDAO dao = new ServiceHistoryDAO(); CustomerReviewDAO reviews = new CustomerReviewDAO()) {
            request.setAttribute("bills", dao.findForCustomer(user.getUserId()));
            Set<Integer> reviewedHistoryIds = reviews.findSubmittedHistoryIds(user.getUserId());
            request.setAttribute("reviewedHistoryIds", reviewedHistoryIds);
            String idParam = request.getParameter("id");
            if (idParam != null) {
                int id = parseInt(idParam);
                PaidBill selected = id > 0 ? dao.getForCustomer(id, user.getUserId()) : null;
                if (selected != null) {
                    request.setAttribute("selected", selected);
                } else {
                    request.setAttribute("detailMessage", "Không tìm thấy hóa đơn #" + idParam + " trong lịch sử của bạn.");
                }
            }
            HttpSession currentSession = request.getSession();
            request.setAttribute("reviewMessage", currentSession.getAttribute("reviewMessage"));
            request.setAttribute("reviewMessageType", currentSession.getAttribute("reviewMessageType"));
            currentSession.removeAttribute("reviewMessage");
            currentSession.removeAttribute("reviewMessageType");
        } catch (SQLException e) {
            getServletContext().log("Could not load customer payment and review history", e);
            request.setAttribute("loadFailed", true);
        }
        request.getRequestDispatcher("/WEB-INF/views/payment/my-payments.jsp").forward(request, response);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
