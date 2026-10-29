package feature.reservation.controller;

import feature.order.controller.CartServlet;
import feature.reservation.dao.ReservationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import model.Reservation;
import model.User;

/**
 * Form đặt bàn cho guest (chưa đăng nhập) và customer. Đơn luôn vào trạng thái PENDING
 * để lễ tân gọi lại xác nhận; khách không tự chọn bàn cụ thể hay ca.
 */
@WebServlet(name = "BookingServlet", urlPatterns = {"/booking"})
public class BookingServlet extends HttpServlet {

    private static final int MAX_DAYS_AHEAD = 7;
    private static final int MAX_PENDING_PER_PHONE = 3;
    private static final int MIN_LEAD_MINUTES = 60;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        request.setAttribute("bookingMessage", session.getAttribute("bookingMessage"));
        session.removeAttribute("bookingMessage");

        // Customer đã đăng nhập thì điền sẵn tên và số điện thoại từ tài khoản.
        User user = userOf(session);
        if (user != null) {
            request.setAttribute("formName", user.getFullName());
            request.setAttribute("formPhone", user.getPhone());
        }
        forwardForm(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || !CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        User user = userOf(session);

        String name = trim(request.getParameter("customerName"));
        String phone = normalizePhone(request.getParameter("customerPhone"));
        String tableType = trim(request.getParameter("tableType"));
        String dateText = trim(request.getParameter("reservationDate"));
        String timeText = trim(request.getParameter("reservationTime"));
        String note = trim(request.getParameter("note"));
        int guests = parseInt(request.getParameter("guestCount"), -1);

        // Giữ lại giá trị đã nhập để hiển thị lại khi có lỗi.
        request.setAttribute("formName", name);
        request.setAttribute("formPhone", request.getParameter("customerPhone"));
        request.setAttribute("formType", tableType);
        request.setAttribute("formDate", dateText);
        request.setAttribute("formTime", timeText);
        request.setAttribute("formGuests", request.getParameter("guestCount"));
        request.setAttribute("formNote", note);

        try (ReservationDAO dao = new ReservationDAO()) {
            List<String> types = dao.getTableTypes();
            Map<String, LocalTime[]> shifts = dao.getShifts();

            String error = validate(name, phone, tableType, dateText, timeText, note, guests, types, shifts);
            if (error == null && dao.countPendingByPhone(phone) >= MAX_PENDING_PER_PHONE) {
                error = "Số điện thoại này đang có " + MAX_PENDING_PER_PHONE
                        + " đơn chờ xác nhận. Vui lòng đợi nhà hàng gọi lại hoặc liên hệ trực tiếp.";
            }
            if (error != null) {
                request.setAttribute("bookingError", error);
                forwardForm(request, response);
                return;
            }

            Reservation r = new Reservation();
            r.setUserId(user == null ? null : user.getUserId());
            r.setCustomerName(name);
            r.setCustomerEmail(user == null ? null : user.getEmail());
            r.setCustomerPhone(phone);
            r.setReservationDate(LocalDate.parse(dateText));
            r.setReservationTime(LocalTime.parse(timeText));
            r.setGuestCount(guests);
            r.setNote(note == null || note.isEmpty() ? null : note);
            r.setTableTypeRequested(tableType == null || tableType.isEmpty() ? null : tableType);

            if (dao.createReservation(r) < 1) {
                request.setAttribute("bookingError", "Không thể gửi yêu cầu đặt bàn lúc này. Vui lòng thử lại sau.");
                forwardForm(request, response);
                return;
            }
        }
        session.setAttribute("bookingMessage", "Đã gửi yêu cầu đặt bàn. Lễ tân sẽ gọi đến số " + phone
                + " để xác nhận.");
        response.sendRedirect(request.getContextPath() + "/booking");
    }

    /**
     * Luật nhập liệu dùng chung cho đặt bàn của khách và của lễ tân (StaffBookingServlet),
     * để hai màn luôn kiểm tra giống nhau. Trả về null nếu hợp lệ, ngược lại là thông báo lỗi.
     */
    public static String validate(String name, String phone, String tableType, String dateText, String timeText,
            String note, int guests, List<String> types, Map<String, LocalTime[]> shifts) {
        if (name == null || name.length() < 2 || name.length() > 30) {
            return "Họ tên phải từ 2 đến 30 ký tự.";
        }
        if (phone == null) {
            return "Số điện thoại không hợp lệ (ví dụ 0901234567).";
        }
        if (guests < 1 || guests > 30) {
            return "Số người phải từ 1 đến 30.";
        }
        if (tableType != null && !tableType.isEmpty() && !types.contains(tableType)) {
            return "Loại bàn không hợp lệ.";
        }
        if (note != null && note.length() > 100) {
            return "Ghi chú tối đa 100 ký tự.";
        }
        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(dateText);
            time = LocalTime.parse(timeText);
        } catch (DateTimeParseException | NullPointerException e) {
            return "Vui lòng chọn ngày và giờ đến.";
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            return "Chỉ nhận đặt bàn từ hôm nay đến " + MAX_DAYS_AHEAD + " ngày tới.";
        }
        boolean inShift = false;
        for (LocalTime[] range : shifts.values()) {
            if (!time.isBefore(range[0]) && !time.isAfter(range[1])) {
                inShift = true;
            }
        }
        if (!inShift) {
            return "Giờ đến phải nằm trong giờ phục vụ: " + describeShifts(shifts) + ".";
        }
        if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now().plusMinutes(MIN_LEAD_MINUTES))) {
            return "Vui lòng đặt trước ít nhất " + MIN_LEAD_MINUTES + " phút.";
        }
        return null;
    }

    private static String describeShifts(Map<String, LocalTime[]> shifts) {
        StringBuilder sb = new StringBuilder();
        for (LocalTime[] range : shifts.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(range[0]).append("–").append(range[1]);
        }
        return sb.toString();
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (ReservationDAO dao = new ReservationDAO()) {
            request.setAttribute("tableTypes", dao.getTableTypes());
        }
        request.setAttribute("minDate", LocalDate.now().toString());
        request.setAttribute("maxDate", LocalDate.now().plusDays(MAX_DAYS_AHEAD).toString());
        request.getRequestDispatcher("/WEB-INF/views/reservation/booking.jsp").forward(request, response);
    }

    /** Chuẩn hoá số điện thoại VN; trả về null nếu sai định dạng. */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String p = raw.trim();
        return p.matches("0\\d{9}") ? p : null;
    }

    private User userOf(HttpSession session) {
        Object u = session.getAttribute("user");
        return u instanceof User ? (User) u : null;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
