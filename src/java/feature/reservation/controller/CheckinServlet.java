package feature.reservation.controller;

import feature.order.controller.CartServlet;
import feature.reservation.dao.ReservationDAO;
import feature.table.dao.RestaurantTableDAO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Reservation;
import model.RestaurantTable;
import model.User;

/**
 * Màn hình check-in của lễ tân khi khách đến:
 * - Khách chưa đặt (walk-in): bật bàn trống sang "đang phục vụ".
 * - Khách đã đặt: tìm đơn theo tên/số điện thoại rồi bấm "Khách đã đến" để mở bàn đã giữ.
 */
@WebServlet(name = "CheckinServlet", urlPatterns = {"/checkin"})
public class CheckinServlet extends HttpServlet {

    private static final int MAX_KEYWORD = 100;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireStaff(request, response);
        if (actor == null) return;
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        request.setAttribute("checkinMessage", session.getAttribute("checkinMessage"));
        request.setAttribute("checkinMessageType", session.getAttribute("checkinMessageType"));
        session.removeAttribute("checkinMessage");
        session.removeAttribute("checkinMessageType");

        String tab = "walkin".equals(request.getParameter("tab")) ? "walkin" : "booked";
        String keyword = cleanKeyword(request.getParameter("q"));

        try (ReservationDAO dao = new ReservationDAO()) {
            request.setAttribute("reservations", dao.searchForCheckin(keyword));
            // Bàn nào hôm nay đã có người đặt mà chưa đến: cảnh báo khi định mở cho khách vãng lai.
            Map<Integer, List<Reservation>> arrivals = new LinkedHashMap<>();
            for (Reservation r : dao.getTodayArrivals()) {
                if (r.getTableId() != null) {
                    arrivals.computeIfAbsent(r.getTableId(), k -> new ArrayList<>()).add(r);
                }
            }
            request.setAttribute("arrivalsByTable", arrivals);
            request.setAttribute("todayArrivalCount", arrivals.values().stream().mapToInt(List::size).sum());
        }

        Map<String, List<RestaurantTable>> groups = new LinkedHashMap<>();
        int free = 0;
        try (RestaurantTableDAO tableDao = new RestaurantTableDAO()) {
            for (RestaurantTable t : tableDao.getAllTables()) {
                groups.computeIfAbsent(t.getTableType(), k -> new ArrayList<>()).add(t);
                if ("AVAILABLE".equals(t.getStatus())) free++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("tableLoadFailed", true);
        }
        request.setAttribute("tableGroups", groups);
        request.setAttribute("freeCount", free);
        request.setAttribute("tab", tab);
        request.setAttribute("keyword", keyword == null ? "" : keyword);
        request.getRequestDispatcher("/WEB-INF/views/reservation/checkin.jsp").forward(request, response);
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

        String action = request.getParameter("action");
        String tab = "booked";
        String message = "Thao tác không hợp lệ.";
        boolean success = false;

        if ("seat".equals(action)) {
            // Khách chưa đặt: bật bàn sang đang phục vụ.
            tab = "walkin";
            // Nhiều tableId = gộp bàn cho đoàn đông người.
            List<Integer> tableIds = new ArrayList<>();
            String[] rawIds = request.getParameterValues("tableId");
            if (rawIds != null) {
                for (String raw : rawIds) {
                    int tid = parseInt(raw, -1);
                    if (tid > 0) tableIds.add(tid);
                }
            }
            try (RestaurantTableDAO dao = new RestaurantTableDAO()) {
                success = !tableIds.isEmpty() && tableIds.size() <= 10 && dao.occupyAllIfFree(tableIds);
            }
            message = success ? (tableIds.size() > 1 ? "Đã gộp và mở " + tableIds.size() + " bàn, khách đang được phục vụ."
                            : "Đã mở bàn, khách vãng lai đang được phục vụ.")
                    : "Không mở được bàn: có bàn đang có khách hoặc không tồn tại. Không bàn nào được mở.";
        } else if ("checkin".equals(action)) {
            int id = parseInt(request.getParameter("reservationId"), -1);
            try (ReservationDAO dao = new ReservationDAO()) {
                String result = id > 0 ? dao.checkIn(id, actor.getUserId()) : ReservationDAO.NOT_PENDING;
                success = ReservationDAO.OK.equals(result);
                message = checkinMessage(result);
            }
        }

        session.setAttribute("checkinMessage", message);
        session.setAttribute("checkinMessageType", success ? "success" : "error");
        String keyword = cleanKeyword(request.getParameter("q"));
        String query = "?tab=" + tab + (keyword == null ? "" : "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        response.sendRedirect(request.getContextPath() + "/checkin" + query);
    }

    /** Dùng chung cho màn chi tiết đơn của ManageReservationsServlet. */
    public static String checkinMessage(String result) {
        switch (result) {
            case ReservationDAO.OK: return "Khách đã đến: bàn đã chuyển sang đang phục vụ và đơn đặt bàn đã hoàn thành.";
            case ReservationDAO.ALREADY_IN: return "Đơn này đã được check-in trước đó.";
            case ReservationDAO.NOT_TODAY: return "Chỉ check-in được đơn có ngày đến là hôm nay.";
            case ReservationDAO.TABLE_BUSY: return "Bàn của đơn này đang có khách. Kiểm tra sơ đồ bàn hoặc đổi bàn cho khách.";
            case ReservationDAO.NO_TABLE: return "Đơn chưa được gán bàn.";
            case ReservationDAO.NOT_PENDING: return "Đơn không còn ở trạng thái đã duyệt.";
            default: return "Không thể check-in do lỗi dữ liệu.";
        }
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

    private String cleanKeyword(String raw) {
        if (raw == null) return null;
        String k = raw.trim();
        if (k.isEmpty()) return null;
        return k.length() > MAX_KEYWORD ? k.substring(0, MAX_KEYWORD) : k;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
