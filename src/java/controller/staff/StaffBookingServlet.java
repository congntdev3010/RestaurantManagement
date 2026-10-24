package controller.staff;

import controller.customer.BookingServlet;
import controller.customer.CartServlet;
import dao.ReservationDAO;
import dao.RestaurantTableDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Reservation;
import model.RestaurantTable;
import model.TableAvailability;
import model.User;

/**
 * Lễ tân đặt bàn hộ khách gọi điện. Đơn do lễ tân tự tạo nên được duyệt ngay (APPROVED),
 * có thể gộp nhiều bàn. Bước 1: nhập ngày/giờ/số người để xem bàn trống; bước 2: chọn bàn và lưu.
 */
@WebServlet(name = "StaffBookingServlet", urlPatterns = {"/staff-booking"})
public class StaffBookingServlet extends HttpServlet {

    private static final int MAX_DAYS_AHEAD = 7;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (requireStaff(request, response) == null) return;
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        request.setAttribute("bookingMessage", session.getAttribute("staffBookingMessage"));
        request.setAttribute("bookingMessageType", session.getAttribute("staffBookingMessageType"));
        session.removeAttribute("staffBookingMessage");
        session.removeAttribute("staffBookingMessageType");
        prepareForm(request);
        request.getRequestDispatcher("/WEB-INF/views/staff/staff-booking.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireStaff(request, response);
        if (actor == null) return;
        HttpSession session = request.getSession(false);
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String name = trim(request.getParameter("customerName"));
        String phone = normalizePhone(request.getParameter("customerPhone"));
        String note = trim(request.getParameter("note"));
        int guests = parseInt(request.getParameter("guestCount"), -1);
        List<Integer> tableIds = new ArrayList<>();
        String[] raw = request.getParameterValues("tableId");
        if (raw != null) {
            for (String s : raw) {
                int id = parseInt(s, -1);
                if (id > 0) tableIds.add(id);
            }
        }

        String error = null;
        LocalDate date = parseDate(request.getParameter("reservationDate"));
        LocalTime time = parseTime(request.getParameter("reservationTime"));
        if (name == null || name.length() < 2 || name.length() > 100) error = "Họ tên phải từ 2 đến 100 ký tự.";
        else if (phone == null) error = "Số điện thoại không hợp lệ (ví dụ 0901234567).";
        else if (guests < 1 || guests > 60) error = "Số người phải từ 1 đến 60.";
        else if (note != null && note.length() > 500) error = "Ghi chú tối đa 500 ký tự.";
        else if (date == null || time == null) error = "Vui lòng chọn ngày và giờ đến.";
        else if (tableIds.isEmpty() || tableIds.size() > 10) error = "Vui lòng chọn từ 1 đến 10 bàn.";
        else {
            LocalDate today = LocalDate.now();
            if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
                error = "Chỉ đặt từ hôm nay đến " + MAX_DAYS_AHEAD + " ngày tới.";
            } else if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
                error = "Giờ đến đã qua.";
            }
        }

        if (error == null) {
            try (ReservationDAO dao = new ReservationDAO()) {
                String shift = shiftOf(time, dao.getShifts());
                if (shift == null) {
                    error = "Giờ đến phải nằm trong giờ phục vụ.";
                } else {
                    Reservation r = new Reservation();
                    r.setCustomerName(name);
                    r.setCustomerPhone(phone);
                    r.setReservationDate(date);
                    r.setReservationTime(time);
                    r.setGuestCount(guests);
                    r.setNote(note == null || note.isEmpty() ? null : note);
                    String result = dao.createApproved(r, tableIds, shift, actor.getUserId());
                    if (!ReservationDAO.OK.equals(result)) error = errorText(result);
                }
            }
        }

        if (error != null) {
            session.setAttribute("staffBookingMessage", error);
            session.setAttribute("staffBookingMessageType", "error");
            response.sendRedirect(request.getContextPath() + "/staff-booking?" + keep(request));
            return;
        }
        session.setAttribute("staffBookingMessage", "Đã đặt bàn cho " + name + " (" + phone + "), đơn được duyệt luôn.");
        session.setAttribute("staffBookingMessageType", "success");
        response.sendRedirect(request.getContextPath() + "/staff-booking");
    }

    /** Nếu đã có ngày + giờ hợp lệ thì tính ca và sơ đồ bàn còn trống. */
    private void prepareForm(HttpServletRequest request) {
        LocalDate date = parseDate(request.getParameter("reservationDate"));
        LocalTime time = parseTime(request.getParameter("reservationTime"));
        int guests = parseInt(request.getParameter("guestCount"), -1);
        request.setAttribute("minDate", LocalDate.now().toString());
        request.setAttribute("maxDate", LocalDate.now().plusDays(MAX_DAYS_AHEAD).toString());
        request.setAttribute("fName", request.getParameter("customerName"));
        request.setAttribute("fPhone", request.getParameter("customerPhone"));
        request.setAttribute("fDate", date == null ? "" : date.toString());
        request.setAttribute("fTime", time == null ? "" : time.toString());
        request.setAttribute("fGuests", guests < 1 ? "" : String.valueOf(guests));
        request.setAttribute("fNote", request.getParameter("note"));
        String type = trim(request.getParameter("tableType"));
        request.setAttribute("fType", type == null ? "" : type);
        try (ReservationDAO typeDao = new ReservationDAO()) {
            request.setAttribute("tableTypes", typeDao.getTableTypes());
        }
        if (date == null || time == null || guests < 1) return;

        try (ReservationDAO dao = new ReservationDAO()) {
            String shift = shiftOf(time, dao.getShifts());
            if (shift == null) {
                request.setAttribute("slotError", "Giờ đến phải nằm trong giờ phục vụ.");
                return;
            }
            Set<Integer> booked = dao.getBookedTableIds(date, shift);
            Map<String, List<TableAvailability>> groups = new LinkedHashMap<>();
            try (RestaurantTableDAO tableDao = new RestaurantTableDAO()) {
                for (RestaurantTable t : tableDao.getAllTables()) {
                    if (type != null && !type.isEmpty() && !type.equals(t.getTableType())) continue;
                    groups.computeIfAbsent(t.getTableType(), k -> new ArrayList<>())
                            .add(new TableAvailability(t, booked.contains(t.getTableId()), true));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                request.setAttribute("slotError", "Không tải được danh sách bàn.");
            }
            request.setAttribute("tableGroups", groups);
            request.setAttribute("shiftName", "LUNCH".equals(shift) ? "Ca trưa" : "DINNER".equals(shift) ? "Ca tối" : shift);
        }
    }

    private String errorText(String result) {
        switch (result) {
            case ReservationDAO.CONFLICT: return "Có bàn vừa được giữ cho đơn khác trong ca này. Vui lòng chọn lại bàn.";
            case ReservationDAO.CAPACITY: return "Tổng sức chứa các bàn đã chọn chưa đủ cho số khách. Hãy chọn thêm bàn để gộp.";
            case ReservationDAO.NO_TABLE: return "Bàn đã chọn không tồn tại.";
            default: return "Không thể tạo đơn do lỗi dữ liệu.";
        }
    }

    /** Giữ lại dữ liệu khách đã nhập khi quay về form sau lỗi. */
    private String keep(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        for (String key : new String[]{"customerName", "customerPhone", "tableType", "reservationDate", "reservationTime", "guestCount", "note"}) {
            String v = request.getParameter(key);
            if (v != null) {
                if (sb.length() > 0) sb.append('&');
                sb.append(key).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private String shiftOf(LocalTime time, Map<String, LocalTime[]> shifts) {
        for (Map.Entry<String, LocalTime[]> e : shifts.entrySet()) {
            if (!time.isBefore(e.getValue()[0]) && !time.isAfter(e.getValue()[1])) return e.getKey();
        }
        return null;
    }

    private User requireStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if (!(u instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) u;
        if (!"Receptionist".equalsIgnoreCase(user.getRoleName()) && !"Manager".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    private LocalDate parseDate(String s) {
        try {
            return s == null || s.isEmpty() ? null : LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private LocalTime parseTime(String s) {
        try {
            return s == null || s.isEmpty() ? null : LocalTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String trim(String v) {
        return v == null ? null : v.trim();
    }

    private String normalizePhone(String value) {
        if (value == null) return null;
        String phone = value.trim().replaceAll("[\\s().-]", "");
        if (phone.startsWith("+84")) phone = "0" + phone.substring(3);
        else if (phone.startsWith("84")) phone = "0" + phone.substring(2);
        return phone.matches("0\\d{9}") ? phone : null;
    }

    private int parseInt(String v, int fallback) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
