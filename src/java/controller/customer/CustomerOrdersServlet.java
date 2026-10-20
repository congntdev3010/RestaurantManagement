package controller.customer;

import dao.OrderDAO;
import dao.DishDAO;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Order;
import model.User;

@WebServlet(name = "CustomerOrdersServlet", urlPatterns = {"/my-orders"})
public class CustomerOrdersServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = requireCustomer(request, response); if (user == null) return;
        HttpSession session = request.getSession(); CartServlet.ensureToken(session);
        request.setAttribute("orderMessage", session.getAttribute("orderMessage")); session.removeAttribute("orderMessage");
        int id = parseInt(request.getParameter("id"), -1);
        try (OrderDAO dao = new OrderDAO()) {
            if (id > 0) {
                Order order = dao.getOrder(id, user.getUserId());
                if (order == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                request.setAttribute("order", order);
                if ("PENDING".equals(order.getStatus())) {
                    try (DishDAO dishDAO = new DishDAO()) {
                        request.setAttribute("availableDishes", dishDAO.getDishes(null, 0, null, 1, 1000));
                    }
                }
                request.getRequestDispatcher("/WEB-INF/views/customer/order-detail.jsp").forward(request, response);
            } else {
                List<Order> orders = dao.getOrders(user.getUserId(), request.getParameter("status"));
                request.setAttribute("orders", orders);
                request.getRequestDispatcher("/WEB-INF/views/customer/my-orders.jsp").forward(request, response);
            }
        }
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = requireCustomer(request, response); if (user == null) return;
        HttpSession session = request.getSession(false);
        if (session == null || !CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }
        String action = request.getParameter("action"); int orderId = parseInt(request.getParameter("orderId"), -1);
        boolean ok = false;
        try (OrderDAO dao = new OrderDAO()) {
            if ("checkout".equals(action)) {
                Map<Integer,Integer> cart = CartServlet.getCart(session);
                if (cart.isEmpty()) session.setAttribute("orderMessage", "Giỏ hàng đang trống.");
                else {
                    int createdId = dao.createOrder(user.getUserId(), null, null, null, "PENDING",
                            user.getFullName(), user.getPhone(), user.getEmail(), trim(request.getParameter("note")), cart);
                    if (createdId > 0) {
                        session.removeAttribute("orderCart"); session.setAttribute("orderMessage", "Đơn hàng đã gửi, đang chờ nhân viên duyệt.");
                        response.sendRedirect(request.getContextPath() + "/my-orders?id=" + createdId); return;
                    }
                }
            } else if ("cancel".equals(action)) {
                ok = dao.cancelCustomerOrder(orderId, user.getUserId(), trim(request.getParameter("reason")), user.getUserId());
            } else if ("updateItems".equals(action)) {
                Map<Integer,Integer> quantities = readQuantities(request);
                addNewDish(request, quantities);
                ok = !quantities.isEmpty() && dao.updateCustomerPendingOrder(orderId, user.getUserId(), quantities);
            }
        }
        if (!"checkout".equals(action)) session.setAttribute("orderMessage", ok ? "Đã cập nhật đơn hàng." : "Không thể thực hiện thao tác này với đơn hàng.");
        response.sendRedirect(request.getContextPath() + "/my-orders?id=" + orderId);
    }

    private Map<Integer,Integer> readQuantities(HttpServletRequest request) {
        Map<Integer,Integer> values = new LinkedHashMap<>();
        for (String name : request.getParameterMap().keySet()) if (name.startsWith("quantity_")) {
            int dishId = parseInt(name.substring(9), -1), quantity = parseInt(request.getParameter(name), -1);
            if (dishId > 0 && quantity > 0 && quantity <= 99) values.put(dishId, quantity);
        }
        return values;
    }
    private void addNewDish(HttpServletRequest request, Map<Integer,Integer> quantities) {
        int dishId=parseInt(request.getParameter("newDishId"),-1), quantity=parseInt(request.getParameter("newQuantity"),0);
        if(dishId>0 && quantity>0 && quantity<=99) quantities.merge(dishId,quantity,(a,b)->Math.min(99,a+b));
    }
    private User requireCustomer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false); Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        User user = (User)value;
        if (!"Customer".equalsIgnoreCase(user.getRoleName())) { response.sendError(403); return null; }
        return user;
    }
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception e) { return fallback; } }
    private String trim(String value) { if (value == null) return null; String s=value.trim(); return s.isEmpty()?null:s; }
}
