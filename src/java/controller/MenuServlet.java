package controller;

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

@WebServlet(name = "MenuServlet", urlPatterns = {"/menu"})
public class MenuServlet extends HttpServlet {

    private static final int PAGE_SIZE = 8; // Hiển thị 8 món / trang

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");

        // Lấy các tham số lọc từ URL
        String keyword = request.getParameter("keyword");
        String categoryIdRaw = request.getParameter("categoryId");
        String sortBy = request.getParameter("sortBy");
        String pageRaw = request.getParameter("page");

        int categoryId = 0;
        if (categoryIdRaw != null && !categoryIdRaw.isEmpty()) {
            try { categoryId = Integer.parseInt(categoryIdRaw); } catch (NumberFormatException ignored) {}
        }

        int page = 1;
        if (pageRaw != null && !pageRaw.isEmpty()) {
            try { page = Integer.parseInt(pageRaw); } catch (NumberFormatException ignored) {}
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

        request.getRequestDispatcher("menu.jsp").forward(request, response);
    }
}