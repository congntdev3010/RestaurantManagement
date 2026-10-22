package controller.customer;

import dao.DishDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Dish;
import model.User;

@WebServlet(name = "CartServlet", urlPatterns = {"/cart"})
public class CartServlet extends HttpServlet {
    private static final String CART = "orderCart";
    /** Bàn gắn vào phiên khi khách quét QR (xem MenuServlet). */
    public static final String TABLE_ID = "orderTableId";
    public static final String TABLE_NAME = "orderTableName";

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        ensureToken(session);
        Map<Integer, Integer> cart = getCart(session);
        List<Dish> dishes = new ArrayList<>();
        List<Integer> unavailable = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        try (DishDAO dao = new DishDAO()) {
            for (Map.Entry<Integer,Integer> entry : cart.entrySet()) {
                Dish dish = dao.getActiveDishById(entry.getKey());
                if (dish != null) {
                    dishes.add(dish);
                    total = total.add(dish.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
                } else unavailable.add(entry.getKey());
            }
        }
        for (Integer dishId : unavailable) cart.remove(dishId);
        session.setAttribute(CART, cart);
        request.setAttribute("cartDishes", dishes);
        request.setAttribute("cartQuantities", cart);
        request.setAttribute("cartTotal", total);
        request.setAttribute("message", session.getAttribute("cartMessage"));
        session.removeAttribute("cartMessage");
        request.setAttribute("orderTableName", session.getAttribute(TABLE_NAME));
        // Bước xem lại: tóm tắt đơn rồi hỏi khách đã chắc chắn gửi chưa. Giỏ trống hoặc chưa có bàn thì quay về giỏ.
        if ("1".equals(request.getParameter("review")) && !dishes.isEmpty() && session.getAttribute(TABLE_ID) != null) {
            request.getRequestDispatcher("/WEB-INF/views/customer/order-review.jsp").forward(request, response);
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/customer/cart.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        String action = request.getParameter("action");
        boolean addAction = "add".equals(action);
        if (session == null && addAction) session = request.getSession(true);
        if (session == null || (!addAction && !validToken(session, request.getParameter("csrfToken")))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }
        if ("add".equals(action)) {
            int dishId = parseInt(request.getParameter("dishId"), -1);
            int quantity = parseInt(request.getParameter("quantity"), 1);
            if (dishId > 0 && quantity > 0 && quantity <= 99) {
                Map<Integer,Integer> cart = getCart(session);
                cart.put(dishId, Math.min(99, cart.getOrDefault(dishId, 0) + quantity));
                session.setAttribute(CART, cart);
                session.setAttribute("cartMessage", "Đã thêm món vào giỏ.");
            }
        } else if ("update".equals(action)) {
            Map<Integer,Integer> cart = getCart(session);
            for (String name : request.getParameterMap().keySet()) if (name.startsWith("quantity_")) {
                int dishId = parseInt(name.substring(9), -1), quantity = parseInt(request.getParameter(name), -1);
                if (dishId > 0 && cart.containsKey(dishId)) {
                    if (quantity == 0) cart.remove(dishId);
                    else if (quantity > 0 && quantity <= 99) cart.put(dishId, quantity);
                }
            }
            session.setAttribute(CART, cart);
        } else if ("remove".equals(action)) {
            int dishId = parseInt(request.getParameter("dishId"), -1);
            Map<Integer,Integer> cart = getCart(session);
            cart.remove(dishId);
            session.setAttribute(CART, cart);
        } else if ("clear".equals(action)) {
            session.removeAttribute(CART);
        }
        String returnTo = request.getParameter("returnTo");
        String menuPath = request.getContextPath() + "/menu";
        String orderPath = request.getContextPath() + "/order";
        if (("add".equals(action) || "update".equals(action)) && returnTo != null
            && (returnTo.equals(menuPath) || returnTo.startsWith(menuPath + "?")
            || returnTo.equals(orderPath) || returnTo.startsWith(orderPath + "?"))
                && !returnTo.contains("\r") && !returnTo.contains("\n")) {
            response.sendRedirect(returnTo);
        } else {
            response.sendRedirect(request.getContextPath() + "/cart");
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Integer,Integer> getCart(HttpSession session) {
        Object stored = session.getAttribute(CART);
        if (stored instanceof Map<?,?>) return (Map<Integer,Integer>) stored;
        Map<Integer,Integer> cart = new LinkedHashMap<>(); session.setAttribute(CART, cart); return cart;
    }
    public static void ensureToken(HttpSession session) {
        if (session.getAttribute("orderCsrf") == null) session.setAttribute("orderCsrf", UUID.randomUUID().toString());
    }
    public static boolean validToken(HttpSession session, String token) {
        Object expected = session.getAttribute("orderCsrf");
        return expected instanceof String && expected.equals(token);
    }
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception e) { return fallback; } }
}
