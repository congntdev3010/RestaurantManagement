package feature.menu.controller;

import feature.order.controller.CartServlet;
import feature.menu.dao.CategoryDAO;
import feature.menu.dao.DishDAO;
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

@WebServlet(name = "MenuServlet", urlPatterns = {"/menu"})
public class MenuServlet extends HttpServlet {

    private static final int PAGE_SIZE = 20; // Hiển thị 8 món / trang

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        jakarta.servlet.http.HttpSession session = request.getSession();
        CartServlet.ensureToken(session);

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
        final int pageSize = PAGE_SIZE;
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
        request.getRequestDispatcher("/WEB-INF/views/menu/menu.jsp").forward(request, response);
    }
}
