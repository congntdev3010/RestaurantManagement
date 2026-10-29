package feature.order.controller;

import feature.order.dao.OrderDAO;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Order;
import model.User;

/**
 * Khách (kể cả chưa đăng nhập) gửi order từ bàn đã quét QR và theo dõi đơn của
 * mình. Order luôn vào trạng thái PENDING để server duyệt; không sửa món sau
 * khi gửi — muốn thêm món thì tạo order mới.
 */
@WebServlet(name = "CustomerOrdersServlet", urlPatterns = {"/my-orders"})
public class CustomerOrdersServlet extends HttpServlet {

    private static final String MY_ORDER_IDS = "myOrderIds";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        User user = customerOf(session);
        request.setAttribute("orderMessage", session.getAttribute("orderMessage"));
        session.removeAttribute("orderMessage");
        int id = parseInt(request.getParameter("id"), -1);
        try (OrderDAO dao = new OrderDAO()) {
            if (id > 0) {
                Order order = dao.getOrder(id, null);
                // Chỉ xem được đơn mình vừa gửi trong phiên này, hoặc đơn thuộc tài khoản đang đăng nhập.
                boolean mine = order != null && (myOrderIds(session).contains(id)
                        || (user != null && order.getUserId() != null && order.getUserId() == user.getUserId()));
                if (!mine) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("order", order);
                request.setAttribute("canCancel", user != null && order.getUserId() != null
                        && order.getUserId() == user.getUserId() && "PENDING".equals(order.getStatus()));
                request.getRequestDispatcher("/WEB-INF/views/order/customer-order-detail.jsp").forward(request, response);
            } else {
                // Đã bỏ màn danh sách "Đơn hàng của tôi"; chỉ còn xem chi tiết đơn vừa gửi (?id=...).
                response.sendRedirect(request.getContextPath() + "/menu");
                return;
            }
        }
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
        User user = customerOf(session);
        String action = request.getParameter("action");
        int orderId = parseInt(request.getParameter("orderId"), -1);
        try (OrderDAO dao = new OrderDAO()) {
            if ("checkout".equals(action)) {
                Object tableId = session.getAttribute(CartServlet.TABLE_ID);
                Map<Integer, Integer> cart = CartServlet.getCart(session);
                if (!(tableId instanceof Integer)) {
                    session.setAttribute("cartMessage", "Vui lòng quét mã QR trên bàn để gọi món.");
                    response.sendRedirect(request.getContextPath() + "/order");
                    return;
                }
                if (cart.isEmpty()) {
                    session.setAttribute("cartMessage", "Bạn chưa chọn món nào.");
                    response.sendRedirect(request.getContextPath() + "/order");
                    return;
                }
                if (dao.hasPendingOrder((Integer) tableId)) {
                    session.setAttribute("cartMessage", "Bàn đang có order chờ nhân viên duyệt, vui lòng đợi duyệt xong rồi gọi thêm.");
                    response.sendRedirect(request.getContextPath() + "/order");
                    return;
                }
                Object guestName = session.getAttribute(CustomerOrderServlet.GUEST_NAME);
                Object guestPhone = session.getAttribute(CustomerOrderServlet.GUEST_PHONE);
                if (guestName == null || guestPhone == null) {
                    // Chưa điền tên + SĐT: đưa về bước vào bàn.
                    response.sendRedirect(request.getContextPath() + "/order");
                    return;
                }
                String name = guestName.toString();
                String phone = guestPhone.toString();
                String email = user != null ? user.getEmail() : null;
                Map<Integer, String> itemNotes = new java.util.LinkedHashMap<>();
                for (Integer dishId : cart.keySet()) {
                    String n = trim(request.getParameter("note_" + dishId));
                    if (n != null) {
                        itemNotes.put(dishId, n.length() > 100 ? n.substring(0, 100) : n);
                    }
                }
                int createdId = dao.createOrder(user == null ? null : user.getUserId(), null, (Integer) tableId, null,
                        "PENDING", name, phone, email, null, cart, itemNotes);
                if (createdId > 0) {
                    myOrderIds(session).add(createdId);
                    session.removeAttribute("orderCart");
                    session.setAttribute("orderMessage", "Order đã gửi, đang chờ nhân viên duyệt.");
                    response.sendRedirect(request.getContextPath() + "/my-orders?id=" + createdId);
                    return;
                }
                session.setAttribute("cartMessage", "Không thể gửi order. Món có thể đã ngừng phục vụ hoặc bàn không khả dụng.");
                response.sendRedirect(request.getContextPath() + "/order");
                return;
            }
            boolean ok = false;
            if ("cancel".equals(action) && user != null) {
                ok = dao.cancelCustomerOrder(orderId, user.getUserId(), trim(request.getParameter("reason")), user.getUserId());
            }
            session.setAttribute("orderMessage", ok ? "Đã hủy đơn hàng." : "Không thể thực hiện thao tác này với đơn hàng.");
        }
        response.sendRedirect(request.getContextPath() + "/my-orders?id=" + orderId);
    }

    @SuppressWarnings("unchecked")
    private Set<Integer> myOrderIds(HttpSession session) {
        Object stored = session.getAttribute(MY_ORDER_IDS);
        if (stored instanceof Set<?>) {
            return (Set<Integer>) stored;
        }
        Set<Integer> ids = new LinkedHashSet<>();
        session.setAttribute(MY_ORDER_IDS, ids);
        return ids;
    }

    /**
     * Trả về user nếu đang đăng nhập bằng tài khoản Customer, ngược lại null
     * (khách vãng lai).
     */
    private User customerOf(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("user");
        return value instanceof User && "Customer".equalsIgnoreCase(((User) value).getRoleName()) ? (User) value : null;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.isEmpty() ? null : s;
    }
}
