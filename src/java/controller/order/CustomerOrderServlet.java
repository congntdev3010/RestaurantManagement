package controller.order;

import dao.CategoryDAO;
import dao.DishDAO;
import dao.OrderDAO;
import dao.RestaurantTableDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Category;
import model.Dish;

@WebServlet(name = "CustomerOrderServlet", urlPatterns = {"/order"})
public class CustomerOrderServlet extends HttpServlet {

    public static final String GUEST_NAME = "orderGuestName";
    public static final String GUEST_PHONE = "orderGuestPhone";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String name = request.getParameter("customerName") == null
                ? "" : request.getParameter("customerName").trim();
        String phone = request.getParameter("customerPhone") == null
                ? "" : request.getParameter("customerPhone").trim();
        session.setAttribute("infoName", name);
        session.setAttribute("infoPhone", phone);
        if (name.isEmpty() || name.length() > 30) {
            session.setAttribute("infoError", "Họ và tên là bắt buộc và tối đa 30 ký tự.");
        } else if (!phone.matches("0\\d{9}")) {
            session.setAttribute("infoError", "Số điện thoại phải bắt đầu bằng số 0 và có đúng 10 chữ số.");
        } else {
            session.setAttribute(GUEST_NAME, name);
            session.setAttribute(GUEST_PHONE, phone);
            session.removeAttribute("infoName");
            session.removeAttribute("infoPhone");
        }
        response.sendRedirect(request.getContextPath() + "/order");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);

        String tableToken = request.getParameter("table");
        if (tableToken != null) {
            RestaurantTableDAO tableDAO = new RestaurantTableDAO();
            model.RestaurantTable qrTable = tableDAO.getTableByQrToken(tableToken.trim());
            tableDAO.close();
            if (qrTable == null || "UNAVAILABLE".equals(qrTable.getStatus())) {
                session.removeAttribute(CartServlet.TABLE_ID);
                session.removeAttribute(CartServlet.TABLE_NAME);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Mã QR bàn không hợp lệ.");
                return;
            }
            Object oldTable = session.getAttribute(CartServlet.TABLE_ID);
            if (oldTable != null && !oldTable.equals(qrTable.getTableId())) {
                session.removeAttribute("orderCart");
            }
            session.setAttribute(CartServlet.TABLE_ID, qrTable.getTableId());
            session.setAttribute(CartServlet.TABLE_NAME, qrTable.getTableName());
        }

        if (session.getAttribute(CartServlet.TABLE_ID) == null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        CartServlet.refreshTableName(session);
        request.setAttribute("orderTableName", session.getAttribute(CartServlet.TABLE_NAME));

        if (session.getAttribute(GUEST_NAME) == null || session.getAttribute(GUEST_PHONE) == null) {
            request.setAttribute("infoError", session.getAttribute("infoError"));
            session.removeAttribute("infoError");
            request.setAttribute("infoName", session.getAttribute("infoName"));
            request.setAttribute("infoPhone", session.getAttribute("infoPhone"));
            session.removeAttribute("infoName");
            session.removeAttribute("infoPhone");
            request.getRequestDispatcher("/WEB-INF/views/order/customer-order-info.jsp").forward(request, response);
            return;
        }

        String keyword = request.getParameter("keyword");
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.length() > 50) {
                keyword = keyword.substring(0, 50);
            }
            keyword = keyword.isEmpty() ? null : Normalizer.normalize(keyword, Normalizer.Form.NFC);
        }
        int categoryId = parseInt(request.getParameter("categoryId"), 0);
        int page = parseInt(request.getParameter("page"), 1);
        String sortBy = request.getParameter("sortBy");

        try (DishDAO dishDAO = new DishDAO(); CategoryDAO categoryDAO = new CategoryDAO()) {
            List<Category> categories = categoryDAO.getAllCategories();
            List<Dish> dishes = dishDAO.getDishes(keyword, categoryId, sortBy, page, 200);
            int totalDishes = dishDAO.getTotalDishesCount(keyword, categoryId);
            int totalPages = (int) Math.ceil((double) totalDishes / 200);
            request.setAttribute("categories", categories);
            request.setAttribute("dishes", dishes);
            request.setAttribute("totalDishes", totalDishes);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("keyword", keyword);
            request.setAttribute("keywordUrlEncoded", URLEncoder.encode(
                    keyword == null ? "" : keyword, StandardCharsets.UTF_8));
            request.setAttribute("categoryId", categoryId);
            request.setAttribute("sortBy", sortBy);
            request.setAttribute("currentPage", page);
            request.setAttribute("cartMessage", session.getAttribute("cartMessage"));
            session.removeAttribute("cartMessage");

            String returnTo = request.getContextPath() + "/order?page=" + page
                    + "&categoryId=" + categoryId
                    + "&sortBy=" + URLEncoder.encode(sortBy == null ? "" : sortBy, StandardCharsets.UTF_8)
                    + "&keyword=" + URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);
            request.setAttribute("orderReturnTo", returnTo);

            Map<Integer, Integer> cart = CartServlet.getCart(session);
            List<Dish> cartDishes = new ArrayList<>();
            int cartCount = 0;
            BigDecimal cartTotal = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                cartCount += entry.getValue();
                Dish dish = dishDAO.getActiveDishById(entry.getKey());
                if (dish != null) {
                    cartDishes.add(dish);
                    cartTotal = cartTotal.add(dish.getPrice().multiply(
                            BigDecimal.valueOf(entry.getValue())));
                }
            }
            request.setAttribute("cartQuantities", cart);
            request.setAttribute("cartCount", cartCount);
            request.setAttribute("cartDishes", cartDishes);
            request.setAttribute("cartTotal", cartTotal);
            loadSentOrders(request, session, cartTotal);
        }
        request.getRequestDispatcher("/WEB-INF/views/order/customer-order.jsp").forward(request, response);
    }

    private void loadSentOrders(HttpServletRequest request, HttpSession session, BigDecimal cartTotal) {
        List<model.Order> sentOrders = new ArrayList<>();
        Object sentIds = session.getAttribute("myOrderIds");
        if (sentIds instanceof java.util.Set<?>) {
            try (OrderDAO orderDAO = new OrderDAO()) {
                for (Object idObj : (java.util.Set<?>) sentIds) {
                    if (!(idObj instanceof Integer)) {
                        continue;
                    }
                    model.Order order = orderDAO.getOrder((Integer) idObj, null);
                    if (order != null && ("PENDING".equals(order.getStatus())
                            || "OPEN".equals(order.getStatus()))) {
                        sentOrders.add(0, order);
                    }
                }
            }
        }
        BigDecimal sentTotal = BigDecimal.ZERO;
        for (model.Order order : sentOrders) {
            if (order.getTotalAmount() != null) {
                sentTotal = sentTotal.add(order.getTotalAmount());
            }
        }
        request.setAttribute("sentOrders", sentOrders);
        request.setAttribute("sentTotal", sentTotal);
        request.setAttribute("grandTotal", sentTotal.add(cartTotal));
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
