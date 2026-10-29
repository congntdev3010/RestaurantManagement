package feature.order.controller;

import feature.order.controller.CartServlet;
import feature.menu.dao.CategoryDAO;
import feature.menu.dao.DishDAO;
import feature.order.dao.OrderDAO;
import feature.table.dao.RestaurantTableDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Category;
import model.Dish;
import model.RestaurantTable;
import model.User;

/**
 * Màn "Order hộ khách" tại bàn: chỉ Server được chọn bàn trên sơ đồ
 * rồi tạo order chờ duyệt thay khách. Tách khỏi ManageOrdersServlet (danh sách/duyệt/thanh toán order)
 * và ManageTablesServlet (sơ đồ bàn) để dễ quản lý.
 */
@WebServlet(name = "TableOrderServlet", urlPatterns = {"/table-order"})
public class TableOrderServlet extends HttpServlet {
    private static final Pattern PHONE_PATTERN = Pattern.compile("0\\d{9}");
    /** Dùng chung key với ManageOrdersServlet cũ để món đã lưu tạm không bị mất khi chuyển màn. */
    private static final String STAFF_ORDER_DRAFTS = "staffOrderDrafts";

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireServer(request, response); if (actor == null) return;
        HttpSession session = request.getSession(); CartServlet.ensureToken(session);
        request.setAttribute("orderMessage", session.getAttribute("staffOrderMessage"));
        session.removeAttribute("staffOrderMessage");

        // Phải chọn bàn trên sơ đồ trước.
        Integer tableId = parsePositive(request.getParameter("tableId"));
        RestaurantTable table;
        try (RestaurantTableDAO tableDAO = new RestaurantTableDAO()) {
            table = tableId == null ? null : tableDAO.getTableById(tableId);
        } catch (SQLException e) { throw new ServletException(e); }
        if (table == null || "UNAVAILABLE".equals(table.getStatus())) {
            session.setAttribute("tableMessage", "Hãy chọn một bàn trên sơ đồ để bắt đầu order.");
            session.setAttribute("tableMessageType", "error");
            response.sendRedirect(request.getContextPath() + "/manage-tables"); return;
        }

        loadCreateForm(request);
        request.setAttribute("table", table);
        try (OrderDAO orders = new OrderDAO()) {
            request.setAttribute("pendingOrder", orders.getPendingOrderForTable(table.getTableId()));
            // Order lần sau của bàn: hiện kèm các món đã được duyệt ở những lần trước.
            request.setAttribute("approvedItems", orders.getApprovedItemsForTable(table.getTableId()));
        }
        request.setAttribute("draftQuantities", getDrafts(session).get(table.getTableId()));
        request.getRequestDispatcher("/WEB-INF/views/order/table-order.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireServer(request, response); if (actor == null) return;
        HttpSession session = request.getSession(false);
        if (session == null || !CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(403); return;
        }
        Integer tableId = parsePositive(request.getParameter("tableId"));
        Map<Integer, Integer> quantities = readQuantities(request);
        String message = "Không thể thực hiện thao tác với đơn hàng.";
        String customerName = trim(request.getParameter("customerName"));
        String customerPhone = trim(request.getParameter("customerPhone"));
        String orderNote = trim(request.getParameter("note"));
        if (customerName != null && customerName.length() > 30) {
            message = "Tên khách tối đa 30 ký tự.";
        } else if (customerPhone != null && !PHONE_PATTERN.matcher(customerPhone).matches()) {
            message = "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số.";
        } else if (orderNote != null && orderNote.length() > 100) {
            message = "Ghi chú tối đa 100 ký tự.";
        } else {
        try (OrderDAO dao = new OrderDAO()) {
            if (tableId != null && dao.hasPendingOrder(tableId)) {
                // Bàn đã có order chờ duyệt: chỉ lưu tạm món mới, không đụng order cũ.
                getDrafts(session).put(tableId, quantities);
                session.setAttribute("staffOrderMessage", "Đã lưu tạm món mới. Order đang chờ duyệt chưa bị thay đổi.");
                response.sendRedirect(request.getContextPath() + "/table-order?tableId=" + tableId); return;
            }
            if (tableId != null && !quantities.isEmpty()) {
                // Order của server cũng phải qua bước chờ duyệt, giống order của khách.
                int created = dao.createOrder(null, actor.getUserId(), tableId, null, "PENDING",
                    customerName, customerPhone, null, orderNote, quantities, readItemNotes(request, quantities));
                if (created > 0) {
                    getDrafts(session).remove(tableId);
                    // Gửi xong quay về sơ đồ bàn.
                    session.setAttribute("tableMessage", "Đã gửi order #" + created + ", đang chờ duyệt.");
                    session.setAttribute("tableMessageType", "success");
                    response.sendRedirect(request.getContextPath() + "/manage-tables"); return;
                }
                message = "Không thể tạo order. Hãy kiểm tra bàn và các món đã chọn.";
            } else {
                message = "Hãy chọn bàn và ít nhất một món.";
            }
        }
        }
        session.setAttribute("staffOrderMessage", message);
        response.sendRedirect(request.getContextPath() + "/table-order" + (tableId == null ? "" : "?tableId=" + tableId));
    }

    private void loadCreateForm(HttpServletRequest request) throws ServletException, IOException {
        List<Category> categories; List<Dish> dishes;
        try (DishDAO dishDAO = new DishDAO(); CategoryDAO categoryDAO = new CategoryDAO()) {
            categories = categoryDAO.getAllCategories();
            dishes = dishDAO.getDishes(null, 0, null, 1, 1000);
        }
        request.setAttribute("categories", categories); request.setAttribute("dishes", dishes);
    }

    /** Ghi chú từng món (note_<dishId>), chỉ lấy cho món có số lượng, tối đa 100 ký tự. */
    private Map<Integer, String> readItemNotes(HttpServletRequest request, Map<Integer, Integer> quantities) {
        Map<Integer, String> notes = new LinkedHashMap<>();
        for (Integer dishId : quantities.keySet()) {
            String n = trim(request.getParameter("note_" + dishId));
            if (n != null) notes.put(dishId, n.length() > 100 ? n.substring(0, 100) : n);
        }
        return notes;
    }

    private Map<Integer, Integer> readQuantities(HttpServletRequest request) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (String key : request.getParameterMap().keySet()) if (key.startsWith("quantity_")) {
            int dishId = parseInt(key.substring(9), -1), qty = parseInt(request.getParameter(key), -1);
            if (dishId > 0 && qty > 0 && qty <= 99) quantities.put(dishId, qty);
        }
        return quantities;
    }

    private User requireServer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false); Object v = s == null ? null : s.getAttribute("user");
        if (!(v instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        User u = (User) v; String role = u.getRoleName();
        if (!"Server".equalsIgnoreCase(role)) { response.sendError(403); return null; }
        return u;
    }

    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception e) { return fallback; } }
    private Integer parsePositive(String value) { int i = parseInt(value, -1); return i > 0 ? i : null; }
    private String trim(String value) { if (value == null) return null; String s = value.trim(); return s.isEmpty() ? null : s; }

    @SuppressWarnings("unchecked")
    private Map<Integer, Map<Integer, Integer>> getDrafts(HttpSession session) {
        Object value = session.getAttribute(STAFF_ORDER_DRAFTS);
        if (value instanceof Map<?, ?>) return (Map<Integer, Map<Integer, Integer>>) value;
        Map<Integer, Map<Integer, Integer>> drafts = new HashMap<>();
        session.setAttribute(STAFF_ORDER_DRAFTS, drafts);
        return drafts;
    }
}
