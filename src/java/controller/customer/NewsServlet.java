package controller.customer;

import dao.NewsDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.News;
import model.User;

@WebServlet(name = "NewsServlet", urlPatterns = {"/news"})
public class NewsServlet extends HttpServlet {
    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (value instanceof User && !hasNewsAccess(((User) value).getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String id = request.getParameter("id");
        try (NewsDAO dao = new NewsDAO()) {
            if (id != null && !id.trim().isEmpty()) {
                int newsId;
                try { newsId = Integer.parseInt(id); }
                catch (NumberFormatException ex) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                News news = dao.getPublishedById(newsId);
                if (news == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                request.setAttribute("news", news);
                request.getRequestDispatcher("/WEB-INF/views/customer/news-detail.jsp").forward(request, response);
                return;
            }
            String keyword = request.getParameter("keyword");
            keyword = keyword == null ? "" : keyword.trim();
            int page = positiveInt(request.getParameter("page"), 1);
            int total = dao.countPublished(keyword);
            int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            page = Math.min(page, pages);
            List<News> articles = dao.getPublished(keyword, page, PAGE_SIZE);
            request.setAttribute("newsList", articles);
            request.setAttribute("keyword", keyword);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", pages);
            request.getRequestDispatcher("/WEB-INF/views/customer/news.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Could not load published news", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải tin tức lúc này.");
        }
    }

    private boolean hasNewsAccess(String roleName) {
        return "User".equalsIgnoreCase(roleName)
                || "Customer".equalsIgnoreCase(roleName)
                || "Manager".equalsIgnoreCase(roleName);
    }

    private int positiveInt(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed > 0 ? parsed : fallback; }
        catch (NumberFormatException ex) { return fallback; }
    }
}
