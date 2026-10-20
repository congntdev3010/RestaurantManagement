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

    private static final int PAGE_SIZE = 8; // Hiển thị 8 món / trang

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);

        // Lấy các tham số lọc từ URL
        // Lấy tham số keyword từ URL và xử lý giải mã chuẩn UTF-8
        String keyword = request.getParameter("keyword");
        keyword = fixUtf8GetParameter(keyword);
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

        DishDAO dishDAO = new DishDAO();
        CategoryDAO categoryDAO = new CategoryDAO();

        // Lấy dữ liệu
        List<Category> categories = categoryDAO.getAllCategories();
        List<Dish> dishes = dishDAO.getDishes(keyword, categoryId, sortBy, page, PAGE_SIZE);
        int totalDishes = dishDAO.getTotalDishesCount(keyword, categoryId);
        int totalPages = (int) Math.ceil((double) totalDishes / PAGE_SIZE);

        // Đẩy sang JSP
        request.setAttribute("categories", categories);
        request.setAttribute("dishes", dishes);
        request.setAttribute("totalDishes", totalDishes);
        request.setAttribute("totalPages", totalPages);

        // Giữ lại trạng thái lọc trên thanh công cụ
        request.setAttribute("keyword", keyword);
        request.setAttribute("categoryId", categoryId);
        request.setAttribute("sortBy", sortBy);
        request.setAttribute("currentPage", page);
        request.setAttribute("cartMessage", session.getAttribute("cartMessage"));
        session.removeAttribute("cartMessage");
        String returnTo = request.getContextPath() + "/menu?page=" + page + "&categoryId=" + categoryId
                + "&sortBy=" + URLEncoder.encode(sortBy == null ? "" : sortBy, StandardCharsets.UTF_8)
                + "&keyword=" + URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);
        request.setAttribute("orderReturnTo", returnTo.replace("/menu?", "/order?"));
        if ("/order".equals(request.getServletPath())) {
            request.getRequestDispatcher("/WEB-INF/views/customer/order.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/customer/menu.jsp").forward(request, response);
        }
    }
    /**
     * Hàm tự động phát hiện và sửa lỗi mã hóa UTF-8 cho phương thức GET
     */
    private String fixUtf8GetParameter(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        param = param.trim();

        // Check 1: Nếu chuỗi đã chứa ký tự Tiếng Việt chuẩn (Unicode > 255) -> Tomcat 10 đã giải mã đúng
        for (char c : param.toCharArray()) {
            if (c > 255) {
                return Normalizer.normalize(param, Normalizer.Form.NFC);
            }
        }

        // Check 2: Nếu Tomcat giải mã nhầm theo ISO-8859-1, chuyển lại về UTF-8
        try {
            byte[] bytes = param.getBytes(StandardCharsets.ISO_8859_1);
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            return Normalizer.normalize(decoded, Normalizer.Form.NFC);
        } catch (Exception e) {
            return Normalizer.normalize(param, Normalizer.Form.NFC);
        }
    }
}
