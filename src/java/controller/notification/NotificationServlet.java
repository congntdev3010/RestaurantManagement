package controller.notification;

import dao.NotificationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.Notification;
import model.User;

/**
 * Thông báo của người đang đăng nhập. GET trả JSON {unread, items} (mới nhất
 * trước); POST action=markAllRead đánh dấu đã đọc. Chỉ đọc/ghi thông báo của
 * chính người dùng trong session.
 */
@WebServlet(name = "NotificationServlet", urlPatterns = {"/notifications"})
public class NotificationServlet extends HttpServlet {

    private static final int LIMIT = 20;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User user = currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try (NotificationDAO dao = new NotificationDAO()) {
            int unread = dao.countUnread(user.getUserId());
            List<Notification> items = dao.listRecent(user.getUserId(), LIMIT);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"unread\":").append(unread).append(",\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                Notification n = items.get(i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append("{\"id\":").append(n.getNotificationId())
                        .append(",\"title\":\"").append(escape(n.getTitle())).append('"')
                        .append(",\"body\":\"").append(escape(n.getBody())).append('"')
                        .append(",\"read\":").append(n.isRead())
                        .append(",\"createdAt\":\"")
                        .append(n.getCreatedAt() == null ? "" : escape(n.getCreatedAt().format(FORMAT)))
                        .append("\"}");
            }
            sb.append("]}");
            out.print(sb);
        } catch (SQLException e) {
            throw new ServletException("Không tải được thông báo.", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!"markAllRead".equals(request.getParameter("action"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try (NotificationDAO dao = new NotificationDAO()) {
            dao.markAllRead(user.getUserId());
        } catch (SQLException e) {
            throw new ServletException("Không cập nhật được thông báo.", e);
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        return value instanceof User ? (User) value : null;
    }

    /** Escape để đưa chuỗi vào JSON. */
    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
