package controller.customer;

import dao.CategoryDAO;
import dao.DishDAO;
import model.Category;
import model.Dish;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.Normalizer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "MenuServlet", urlPatterns = {"/menu", "/order"})
public class MenuServlet extends HttpServlet {

    private static final int PAGE_SIZE = 20; // Hiển thị 8 món / trang
    /** Tên + SĐT khách nhập khi vào bàn (lưu trong phiên). */
    public static final String GUEST_NAME = "orderGuestName";
    public static final String GUEST_PHONE = "orderGuestPhone";

    /** Khách gửi tên + SĐT ở bước vào bàn. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }
        String name = request.getParameter("customerName") == null ? "" : request.getParameter("customerName").trim();
        String phone = request.getParameter("customerPhone") == null ? "" : request.getParameter("customerPhone").trim();
        // Quy tắc giống trang hồ sơ (ProfileServlet): tên ≤ 30 ký tự, SĐT bắt đầu bằng 0 và đúng 10 chữ số.
        // Giữ lại những gì khách đã gõ để form không bị xoá khi nhập sai.
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

        // Khách quét QR của bàn: /order?table=<token>. Token hợp lệ thì gắn bàn vào phiên; sai thì báo lỗi.
        String tableToken = request.getParameter("table");
        if (tableToken != null) {
            dao.RestaurantTableDAO tableDAO = new dao.RestaurantTableDAO();
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
                session.removeAttribute("orderCart"); // đổi bàn thì bỏ giỏ cũ
            }
            session.setAttribute(CartServlet.TABLE_ID, qrTable.getTableId());
            session.setAttribute(CartServlet.TABLE_NAME, qrTable.getTableName());
        }
        // Màn gọi món tại bàn chỉ mở được khi đã quét QR hợp lệ (phiên có bàn).
        // Khách tự gõ /order trên thanh địa chỉ thì đưa về trang chủ.
        if ("/order".equals(request.getServletPath()) && session.getAttribute(CartServlet.TABLE_ID) == null) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        CartServlet.refreshTableName(session);
        request.setAttribute("orderTableName", session.getAttribute(CartServlet.TABLE_NAME));

        // Quét QR vào bàn: phải điền tên + SĐT một lần trước khi sang màn gọi món.
        if ("/order".equals(request.getServletPath()) && session.getAttribute(CartServlet.TABLE_ID) != null
                && (session.getAttribute(GUEST_NAME) == null || session.getAttribute(GUEST_PHONE) == null)) {
            request.setAttribute("infoError", session.getAttribute("infoError"));
            session.removeAttribute("infoError");
            request.setAttribute("infoName", session.getAttribute("infoName"));
            request.setAttribute("infoPhone", session.getAttribute("infoPhone"));
            session.removeAttribute("infoName");
            session.removeAttribute("infoPhone");
            request.getRequestDispatcher("/WEB-INF/views/customer/order-info.jsp").forward(request, response);
            return;
        }

        // Lấy các tham số lọc từ URL
        // Servlet container đã giải mã query string theo UTF-8.
        String keyword = request.getParameter("keyword");
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.length() > 50) keyword = keyword.substring(0, 50);
            keyword = keyword.isEmpty() ? null : Normalizer.normalize(keyword, Normalizer.Form.NFC);
        }
        String categoryIdRaw = request.getParameter("categoryId");
        String sortBy = request.getParameter("sortBy");
        String pageRaw = request.getParameter("page");

        int categoryId = 0;
        if (categoryIdRaw != null && !categoryIdRaw.isEmpty()) {
            try {
                categoryId = Integer.parseInt(categoryIdRaw);
            } catch (NumberFormatException ignored) {
            }
        }

        int page = 1;
        if (pageRaw != null && !pageRaw.isEmpty()) {
            try {
                page = Integer.parseInt(pageRaw);
            } catch (NumberFormatException ignored) {
            }
        }

        // Trang gọi món hiển thị cả thực đơn trên một trang cuộn; trang /menu vẫn phân trang.
        final int pageSize = "/order".equals(request.getServletPath()) ? 200 : PAGE_SIZE;
        DishDAO dishDAO = new DishDAO();
        CategoryDAO categoryDAO = new CategoryDAO();

        // Lấy dữ liệu
        List<Category> categories = categoryDAO.getAllCategories();
        List<Dish> dishes = dishDAO.getDishes(keyword, categoryId, sortBy, page, pageSize);
        int totalDishes = dishDAO.getTotalDishesCount(keyword, categoryId);
        int totalPages = (int) Math.ceil((double) totalDishes / pageSize);

        // Đẩy sang JSP
        request.setAttribute("categories", categories);
        request.setAttribute("dishes", dishes);
        request.setAttribute("totalDishes", totalDishes);
        request.setAttribute("totalPages", totalPages);

        // Giữ lại trạng thái lọc trên thanh công cụ
        request.setAttribute("keyword", keyword);
        request.setAttribute("keywordUrlEncoded", URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8));
        request.setAttribute("categoryId", categoryId);
        request.setAttribute("sortBy", sortBy);
        request.setAttribute("currentPage", page);
        request.setAttribute("cartMessage", session.getAttribute("cartMessage"));
        session.removeAttribute("cartMessage");
        String returnTo = request.getContextPath() + "/menu?page=" + page + "&categoryId=" + categoryId
                + "&sortBy=" + URLEncoder.encode(sortBy == null ? "" : sortBy, StandardCharsets.UTF_8)
                + "&keyword=" + URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);
        request.setAttribute("orderReturnTo", returnTo.replace("/menu?", "/order?"));
        // Món khách đã chọn: số lượng từng món + tổng, để vẽ nút +/- và panel "Món đã chọn".
        java.util.Map<Integer, Integer> cart = CartServlet.getCart(session);
        java.util.List<model.Dish> cartDishes = new java.util.ArrayList<>();
        int cartCount = 0;
        java.math.BigDecimal cartTotal = java.math.BigDecimal.ZERO;
        for (java.util.Map.Entry<Integer, Integer> e : cart.entrySet()) {
            cartCount += e.getValue();
            model.Dish d = dishDAO.getActiveDishById(e.getKey());
            if (d != null) {
                cartDishes.add(d);
                cartTotal = cartTotal.add(d.getPrice().multiply(java.math.BigDecimal.valueOf(e.getValue())));
            }
        }
        request.setAttribute("cartQuantities", cart);
        request.setAttribute("cartCount", cartCount);
        request.setAttribute("cartDishes", cartDishes);
        request.setAttribute("cartTotal", cartTotal);
        if ("/order".equals(request.getServletPath())) {
            // Các order khách đã gửi trong phiên này (đang chờ duyệt / đang phục vụ), mới nhất trước.
            java.util.List<model.Order> sentOrders = new java.util.ArrayList<>();
            Object sentIds = session.getAttribute("myOrderIds");
            if (sentIds instanceof java.util.Set<?>) {
                try (dao.OrderDAO orderDao = new dao.OrderDAO()) {
                    for (Object idObj : (java.util.Set<?>) sentIds) {
                        if (!(idObj instanceof Integer)) continue;
                        model.Order o = orderDao.getOrder((Integer) idObj, null);
                        if (o != null && ("PENDING".equals(o.getStatus()) || "OPEN".equals(o.getStatus()))) {
                            sentOrders.add(0, o);
                        }
                    }
                }
            }
            java.math.BigDecimal sentTotal = java.math.BigDecimal.ZERO;
            for (model.Order so : sentOrders) {
                if (so.getTotalAmount() != null) sentTotal = sentTotal.add(so.getTotalAmount());
            }
            request.setAttribute("sentOrders", sentOrders);
            request.setAttribute("sentTotal", sentTotal);
            request.setAttribute("grandTotal", sentTotal.add(cartTotal));
        }
        if ("/order".equals(request.getServletPath())) {
            request.getRequestDispatcher("/WEB-INF/views/customer/order.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/customer/menu.jsp").forward(request, response);
        }
    }
}
