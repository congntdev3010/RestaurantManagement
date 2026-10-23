package controller.staff;

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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Reservation;
import model.RestaurantTable;
import model.ShiftAvailability;
import model.TableAvailability;
import model.User;

/**
 * Màn hình lễ tân: xem đơn đặt bàn, gọi xác nhận, kiểm tra bàn còn trống theo ngày + ca,
 * rồi duyệt (chọn bàn cụ thể + ca) hoặc từ chối.
 */
@WebServlet(name = "ManageReservationsServlet", urlPatterns = {"/manage-reservations"})
public class ManageReservationsServlet extends HttpServlet {

    private static final List<String> STATUSES
            = List.of("PENDING", "APPROVED", "COMPLETED", "NO_SHOW", "REJECTED", "CANCELLED");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireStaff(request, response);
        if (actor == null) return;
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        request.setAttribute("reservationMessage", session.getAttribute("reservationMessage"));
        request.setAttribute("reservationMessageType", session.getAttribute("reservationMessageType"));
        session.removeAttribute("reservationMessage");
        session.removeAttribute("reservationMessageType");

        int id = parseInt(request.getParameter("id"), -1);
        try (ReservationDAO dao = new ReservationDAO()) {
            if (id > 0) {
                Reservation r = dao.getById(id);
                if (r == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                prepareDetail(request, dao, r);
                request.getRequestDispatcher("/WEB-INF/views/staff/reservation-detail.jsp").forward(request, response);
                return;
            }

            String status = request.getParameter("status");
            if (status == null) status = "PENDING";
            boolean all = "ALL".equals(status);
            if (!all && !STATUSES.contains(status)) status = "PENDING";
            LocalDate date = parseDate(request.getParameter("date"));

            request.setAttribute("reservations", dao.search(all ? null : status, date));
            request.setAttribute("statusFilter", status);
            request.setAttribute("dateFilter", date == null ? "" : date.toString());
            request.setAttribute("statusCounts", dao.countByStatus());
        }
        request.getRequestDispatcher("/WEB-INF/views/staff/manage-reservations.jsp").forward(request, response);
    }

    /** Chuẩn bị sơ đồ bàn còn trống của ngày khách đặt theo ca đang xem. */
    private void prepareDetail(HttpServletRequest request, ReservationDAO dao, Reservation r) {
        Map<String, LocalTime[]> shifts = dao.getShifts();
        // Ca đang xem: tham số ?shift, rồi ca đã chốt của đơn, rồi suy ra từ giờ khách chọn.
        String shift = request.getParameter("shift");
        if (shift == null || !shifts.containsKey(shift)) shift = r.getMealShift();
        if (shift == null || !shifts.containsKey(shift)) shift = shiftOf(r.getReservationTime(), shifts);

        List<RestaurantTable> tables = new ArrayList<>();
        try (RestaurantTableDAO tableDao = new RestaurantTableDAO()) {
            tables = tableDao.getAllTables();
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("tableLoadFailed", true);
        }

        String wanted = r.getTableTypeRequested();
        // Tóm tắt từng ca cho loại bàn khách muốn: còn bao nhiêu bàn đủ chỗ.
        List<ShiftAvailability> summaries = new ArrayList<>();
        for (Map.Entry<String, LocalTime[]> entry : shifts.entrySet()) {
            Set<Integer> booked = dao.getBookedTableIds(r.getReservationDate(), entry.getKey());
            int total = 0;
            int free = 0;
            for (RestaurantTable t : tables) {
                if ((wanted == null || wanted.equals(t.getTableType())) && t.getCapacity() >= r.getGuestCount()) {
                    total++;
                    if (!booked.contains(t.getTableId())) free++;
                }
            }
            summaries.add(new ShiftAvailability(entry.getKey(), shiftName(entry.getKey()), free, total));
        }

        // Sơ đồ bàn của ca đang xem, gom theo loại; đếm bàn trống mỗi loại để gợi ý đổi loại bàn.
        Set<Integer> booked = dao.getBookedTableIds(r.getReservationDate(), shift);
        Map<String, List<TableAvailability>> groups = new LinkedHashMap<>();
        Map<String, Integer> freeByType = new LinkedHashMap<>();
        for (RestaurantTable t : tables) {
            boolean isBooked = booked.contains(t.getTableId());
            boolean fits = t.getCapacity() >= r.getGuestCount();
            TableAvailability slot = new TableAvailability(t, isBooked, fits);
            groups.computeIfAbsent(t.getTableType(), k -> new ArrayList<>()).add(slot);
            freeByType.merge(t.getTableType(), slot.isSelectable() ? 1 : 0, Integer::sum);
        }

        request.setAttribute("reservation", r);
        request.setAttribute("selectedShift", shift);
        request.setAttribute("selectedShiftName", shiftName(shift));
        request.setAttribute("shiftSummaries", summaries);
        request.setAttribute("tableGroups", groups);
        request.setAttribute("freeByType", freeByType);
        request.setAttribute("canDecide", "PENDING".equals(r.getStatus()));
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

        int id = parseInt(request.getParameter("reservationId"), -1);
        String action = request.getParameter("action");
        String note = trimToNull(request.getParameter("note"), 500);
        String message = "Thao tác không hợp lệ.";
        boolean success = false;
        boolean backToDetail = true;

        if (id > 0 && action != null) {
            try (ReservationDAO dao = new ReservationDAO()) {
                switch (action) {
                    case "contacted":
                        success = dao.markContacted(id, note);
                        message = success ? "Đã ghi nhận cuộc gọi với khách." : "Không thể cập nhật đơn (đơn đã được xử lý).";
                        break;
                    case "approve": {
                        int tableId = parseInt(request.getParameter("tableId"), -1);
                        String shift = request.getParameter("shift");
                        if (tableId < 1 || shift == null || !dao.getShifts().containsKey(shift)) {
                            message = "Vui lòng chọn bàn và ca (trưa/tối) trước khi duyệt.";
                            break;
                        }
                        String result = dao.approve(id, tableId, shift, actor.getUserId(), note);
                        success = ReservationDAO.OK.equals(result);
                        message = approveMessage(result);
                        backToDetail = !success;
                        break;
                    }
                    case "reject": {
                        String reason = trimToNull(request.getParameter("reason"), 255);
                        success = dao.reject(id, actor.getUserId(), reason == null ? "Nhà hàng không thể phục vụ" : reason);
                        message = success ? "Đã từ chối đơn đặt bàn." : "Không thể từ chối (đơn đã được xử lý).";
                        backToDetail = !success;
                        break;
                    }
                    case "checkin": {
                        String result = dao.checkIn(id, actor.getUserId());
                        success = ReservationDAO.OK.equals(result);
                        message = CheckinServlet.checkinMessage(result);
                        break;
                    }
                    case "complete":
                        success = dao.finishApproved(id, "COMPLETED", null);
                        message = success ? "Đã hoàn tất bữa ăn của khách." : "Chỉ hoàn tất được đơn khách đã check-in.";
                        break;
                    case "noshow":
                        success = dao.finishApproved(id, "NO_SHOW", null);
                        message = success ? "Đã đánh dấu khách không đến; bàn được mở lại." : "Chỉ đánh dấu được đơn đã duyệt mà khách chưa check-in.";
                        break;
                    case "cancel": {
                        String reason = trimToNull(request.getParameter("reason"), 255);
                        success = dao.finishApproved(id, "CANCELLED", reason == null ? "Lễ tân huỷ theo yêu cầu khách" : reason);
                        message = success ? "Đã huỷ đơn; bàn được mở lại." : "Chỉ huỷ được đơn đã duyệt mà khách chưa check-in.";
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        session.setAttribute("reservationMessage", message);
        session.setAttribute("reservationMessageType", success ? "success" : "error");
        String target = backToDetail ? "?id=" + id + shiftQuery(request) : "";
        response.sendRedirect(request.getContextPath() + "/manage-reservations" + target);
    }

    private String approveMessage(String result) {
        switch (result) {
            case ReservationDAO.OK: return "Đã duyệt đơn và giữ bàn cho khách.";
            case ReservationDAO.CONFLICT: return "Bàn vừa được giữ cho đơn khác trong ca này. Vui lòng chọn bàn khác.";
            case ReservationDAO.CAPACITY: return "Bàn không đủ chỗ cho số khách của đơn.";
            case ReservationDAO.NO_TABLE: return "Bàn đã chọn không tồn tại.";
            case ReservationDAO.NOT_PENDING: return "Đơn đã được xử lý trước đó.";
            default: return "Không thể duyệt đơn do lỗi dữ liệu.";
        }
    }

    private String shiftQuery(HttpServletRequest request) {
        String shift = request.getParameter("shift");
        return shift != null && shift.matches("[A-Z]{1,10}") ? "&shift=" + shift : "";
    }

    /** Receptionist và Manager được quản lý đặt bàn. */
    private User requireStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if (!(u instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) u;
        String role = user.getRoleName();
        if (!"Receptionist".equalsIgnoreCase(role) && !"Manager".equalsIgnoreCase(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    /** Ca chứa giờ khách chọn; không khớp thì lấy ca gần nhất sau đó, cuối cùng là ca đầu tiên. */
    private String shiftOf(LocalTime time, Map<String, LocalTime[]> shifts) {
        String fallback = null;
        for (Map.Entry<String, LocalTime[]> e : shifts.entrySet()) {
            if (fallback == null) fallback = e.getKey();
            if (!time.isBefore(e.getValue()[0]) && !time.isAfter(e.getValue()[1])) return e.getKey();
            if (time.isBefore(e.getValue()[0])) return e.getKey();
        }
        return fallback;
    }

    private String shiftName(String code) {
        return "LUNCH".equals(code) ? "Ca trưa" : "DINNER".equals(code) ? "Ca tối" : code;
    }

    private LocalDate parseDate(String text) {
        try {
            return text == null || text.isEmpty() ? null : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trimToNull(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        return v.length() > max ? v.substring(0, max) : v;
    }
}
