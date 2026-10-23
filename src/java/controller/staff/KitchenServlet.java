package controller.staff;

import dao.OrderDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

/**
 * Màn hình nấu của bếp: CHỈ XEM. Servlet này không có doPost nên mọi yêu cầu ghi bị từ chối (405);
 * việc đánh dấu "đã phục vụ" do server làm ở /manage-orders.
 */
@WebServlet(name = "KitchenServlet", urlPatterns = {"/kitchen"})
public class KitchenServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return; }
        String role = ((User) value).getRoleName();
        if (!"Chef".equalsIgnoreCase(role) && !"Manager".equalsIgnoreCase(role)) { response.sendError(403); return; }
        try (OrderDAO dao = new OrderDAO()) {
            request.setAttribute("items", dao.getKitchenItems());
        }
        request.getRequestDispatcher("/WEB-INF/views/staff/kitchen.jsp").forward(request, response);
    }
}
