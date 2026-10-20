package controller.admin;

import dao.NewsDAO;
import java.io.IOException;
import java.net.URI;
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

@WebServlet(name = "ManageNewsServlet", urlPatterns = {"/manage-news"})
public class ManageNewsServlet extends HttpServlet {
    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = requireNewsManager(request, response);
        if (actor == null) return;
        response.setHeader("Cache-Control", "no-store");
        String keyword = trim(request.getParameter("keyword"));
        String status = request.getParameter("status");
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status)) status = "";
        int page = positiveInt(request.getParameter("page"), 1);
        try (NewsDAO dao = new NewsDAO()) {
            int total = dao.countAdminNews(keyword, status);
            int pages = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
            page = Math.min(page, pages);
            List<News> records = dao.getAdminNews(keyword, status, page, PAGE_SIZE);
            request.setAttribute("newsList", records);
            request.setAttribute("keyword", keyword == null ? "" : keyword);
            request.setAttribute("statusFilter", status);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", pages);
            String editValue = request.getParameter("edit");
            if (editValue != null) {
                int id = positiveInt(editValue, -1);
                News editing = id > 0 ? dao.getById(id) : null;
                if (editing == null) flash(request.getSession(), "Không tìm thấy tin cần sửa.", "error");
                else request.setAttribute("editingNews", editing);
            }
            HttpSession session = request.getSession();
            News formNews = (News) session.getAttribute("newsForm");
            if (formNews != null) {
                request.setAttribute("formNews", formNews);
                request.setAttribute("formAction", session.getAttribute("newsFormAction"));
                request.setAttribute("formError", session.getAttribute("newsFormError"));
                request.setAttribute("openForm", Boolean.TRUE);
                session.removeAttribute("newsForm"); session.removeAttribute("newsFormAction"); session.removeAttribute("newsFormError");
            }
            request.setAttribute("manageMessage", session.getAttribute("newsMessage"));
            request.setAttribute("manageMessageType", session.getAttribute("newsMessageType"));
            session.removeAttribute("newsMessage"); session.removeAttribute("newsMessageType");
            request.getRequestDispatcher("/WEB-INF/views/admin/manage-news.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Could not load news administration", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải trang quản lý tin tức.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireNewsManager(request, response);
        if (actor == null) return;
        String action = request.getParameter("action");
        String message = "Thao tác không hợp lệ.";
        String type = "error";
        boolean hasFormError = false;
        try (NewsDAO dao = new NewsDAO()) {
            if ("delete".equals(action)) {
                int id = positiveInt(request.getParameter("newsId"), -1);
                if (id > 0 && dao.delete(id)) { message = "Đã xóa tin tức."; type = "success"; }
                else message = "Không tìm thấy tin tức cần xóa.";
            } else if ("toggleStatus".equals(action)) {
                int id = positiveInt(request.getParameter("newsId"), -1);
                String target = request.getParameter("targetStatus");
                if (id > 0 && ("DRAFT".equals(target) || "PUBLISHED".equals(target)) && dao.setStatus(id, target)) {
                    message = "Đã cập nhật trạng thái tin tức."; type = "success";
                } else message = "Không thể cập nhật trạng thái tin.";
            } else if ("create".equals(action) || "update".equals(action)) {
                News news = readForm(request);
                if ("update".equals(action)) news.setNewsId(positiveInt(request.getParameter("newsId"), -1));
                news.setCreatedBy(actor.getUserId());
                message = validate(news);
                if (message == null) {
                    boolean saved = "create".equals(action) ? dao.create(news) : dao.update(news);
                    if (saved) { message = "Đã lưu tin tức."; type = "success"; }
                    else message = "Không tìm thấy tin cần cập nhật.";
                } else {
                    hasFormError = true;
                    HttpSession session = request.getSession();
                    session.setAttribute("newsForm", news);
                    session.setAttribute("newsFormAction", action);
                    session.setAttribute("newsFormError", message);
                }
            }
        } catch (SQLException ex) {
            getServletContext().log("Could not save news administration change", ex);
            message = "Có lỗi khi lưu dữ liệu. Vui lòng thử lại.";
        }
        if (!hasFormError) flash(request.getSession(), message, type);
        response.sendRedirect(request.getContextPath() + "/manage-news");
    }

    private User requireNewsManager(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        User user = value instanceof User ? (User) value : null;
        if (user == null) { response.sendRedirect(request.getContextPath() + "/login"); return null; }
        String role = user.getRoleName();
        if (!"Manager".equalsIgnoreCase(role) && !"Admin".equalsIgnoreCase(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    private News readForm(HttpServletRequest request) {
        News news = new News();
        news.setTitle(trim(request.getParameter("title")));
        news.setSummary(trim(request.getParameter("summary")));
        news.setContent(trim(request.getParameter("content")));
        news.setImageUrl(trim(request.getParameter("imageUrl")));
        news.setStatus(request.getParameter("status"));
        return news;
    }

    private String validate(News news) {
        if (news.getTitle() == null || news.getTitle().isEmpty()) return "Tiêu đề là bắt buộc.";
        if (news.getTitle().length() > 200) return "Tiêu đề không được vượt quá 200 ký tự.";
        if (news.getSummary() == null || news.getSummary().isEmpty()) return "Tóm tắt là bắt buộc.";
        if (news.getSummary().length() > 500) return "Tóm tắt không được vượt quá 500 ký tự.";
        if (news.getContent() == null || news.getContent().isEmpty()) return "Nội dung là bắt buộc.";
        if (news.getImageUrl() != null && news.getImageUrl().length() > 1000) return "Đường dẫn ảnh quá dài.";
        if (news.getImageUrl() != null && !validImageUrl(news.getImageUrl())) return "Ảnh chỉ nhận URL HTTPS hoặc đường dẫn trong /assets/images/.";
        if (!"DRAFT".equals(news.getStatus()) && !"PUBLISHED".equals(news.getStatus())) return "Trạng thái không hợp lệ.";
        if (news.getNewsId() < 0) return "Mã tin không hợp lệ.";
        return null;
    }

    private boolean validImageUrl(String value) {
        if (value.isEmpty()) return true;
        if (value.startsWith("/assets/images/") && !value.contains("..") && !value.contains("\\")) return true;
        try { URI uri = URI.create(value); return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null; }
        catch (IllegalArgumentException ex) { return false; }
    }

    private int positiveInt(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed > 0 ? parsed : fallback; }
        catch (NumberFormatException ex) { return fallback; }
    }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private void flash(HttpSession session, String message, String type) {
        session.setAttribute("newsMessage", message); session.setAttribute("newsMessageType", type);
    }
}
