package controller.staff;

import dao.RestaurantTableDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.RestaurantTable;
import model.User;

/** Trang in mã QR gọi món cho từng bàn (Manager, Server). */
@WebServlet(name = "TableQrServlet", urlPatterns = {"/table-qr"})
public class TableQrServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return; }
        String role = ((User) value).getRoleName();
        if (!"Manager".equalsIgnoreCase(role) && !"Server".equalsIgnoreCase(role)
                && !"Receptionist".equalsIgnoreCase(role)) { response.sendError(403); return; }
        List<RestaurantTable> tables = new ArrayList<>();
        try (RestaurantTableDAO dao = new RestaurantTableDAO()) {
            tables = dao.getAllTables();
        } catch (SQLException e) { throw new ServletException(e); }
        String onlyId = request.getParameter("id");
        if (onlyId != null) {
            tables.removeIf(t -> !String.valueOf(t.getTableId()).equals(onlyId));
        }
        // Địa chỉ gốc mặc định lấy từ yêu cầu; người in có thể sửa trên trang nếu máy chủ có tên miền/IP khác.
        String base = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort())
                + request.getContextPath();
        // Địa chỉ cố định cấu hình trong web.xml (qrBaseUrl) thắng địa chỉ suy ra từ yêu cầu.
        String configured = getServletContext().getInitParameter("qrBaseUrl");
        if (configured != null && !configured.trim().isEmpty()) {
            base = configured.trim().replaceAll("/+$", "");
        }
        request.setAttribute("tables", tables);
        request.setAttribute("baseUrl", base);
        request.getRequestDispatcher("/WEB-INF/views/staff/table-qr.jsp").forward(request, response);
    }
}
