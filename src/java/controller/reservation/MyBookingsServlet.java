package controller.reservation;

import controller.order.CartServlet;
import dao.ReservationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;

/** Lịch sử đặt bàn của customer đang đăng nhập, kèm nút huỷ đơn. */
@WebServlet(name = "MyBookingsServlet", urlPatterns = {"/my-bookings"})
public class MyBookingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = userOf(session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        CartServlet.ensureToken(session);
        request.setAttribute("bookingMessage", session.getAttribute("bookingMessage"));
        session.removeAttribute("bookingMessage");
        try (ReservationDAO dao = new ReservationDAO()) {
            request.setAttribute("reservations", dao.getByUser(user.getUserId()));
        }
        request.getRequestDispatcher("/WEB-INF/views/reservation/my-bookings.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User user = session == null ? null : userOf(session);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String message = "Thao tác không hợp lệ.";
        if ("cancel".equals(request.getParameter("action"))) {
            int id;
            try {
                id = Integer.parseInt(request.getParameter("reservationId"));
            } catch (NumberFormatException e) {
                id = -1;
            }
            try (ReservationDAO dao = new ReservationDAO()) {
                message = id > 0 && dao.cancelByUser(id, user.getUserId())
                        ? "Đã huỷ đơn đặt bàn."
                        : "Không thể huỷ đơn này (đơn không tồn tại hoặc đã được xử lý xong).";
            }
        }
        session.setAttribute("bookingMessage", message);
        response.sendRedirect(request.getContextPath() + "/my-bookings");
    }

    private User userOf(HttpSession session) {
        Object u = session.getAttribute("user");
        return u instanceof User ? (User) u : null;
    }
}
