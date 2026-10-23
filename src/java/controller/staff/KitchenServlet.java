package controller.staff;

import controller.customer.CartServlet;
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
 * Màn hình nấu của bếp. Chef chỉ xem; Server/Manager có thể điều chỉnh món đang nấu.
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
        if (!canAccess(role)) { response.sendError(403); return; }
        CartServlet.ensureToken(session);
        request.setAttribute("canEdit", canEdit(role));
        request.setAttribute("kitchenMessage", session.getAttribute("kitchenMessage"));
        session.removeAttribute("kitchenMessage");
        try (OrderDAO dao = new OrderDAO()) {
            request.setAttribute("items", dao.getKitchenItems());
        }
        request.getRequestDispatcher("/WEB-INF/views/staff/kitchen.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) { response.sendRedirect(request.getContextPath() + "/login"); return; }
        String role = ((User) value).getRoleName();
        if (!canEdit(role)) { response.sendError(403); return; }
        if (!CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN); return;
        }
        int itemId = parseInt(request.getParameter("itemId"), -1);
        int orderId = parseInt(request.getParameter("orderId"), -1);
        int quantity = parseInt(request.getParameter("quantity"), -1);
        String message = "Không thể cập nhật món. Món có thể đã được phục vụ hoặc đã thay đổi.";
        if (itemId > 0 && quantity >= 0) {
            try (OrderDAO dao = new OrderDAO()) {
                if (dao.updateKitchenItem(itemId, quantity)) {
                    message = quantity == 0 ? "Đã hủy món khỏi order." : "Đã cập nhật số lượng món.";
                }
            }
        }
        session.setAttribute("kitchenMessage", message);
        String anchor = orderId > 0 && quantity == 0 ? "#order-" + orderId : "#item-" + itemId;
        response.sendRedirect(request.getContextPath() + "/kitchen" + anchor);
    }

    private boolean canAccess(String role) {
        return "Chef".equalsIgnoreCase(role) || canEdit(role);
    }

    private boolean canEdit(String role) {
        return "Server".equalsIgnoreCase(role) || "Manager".equalsIgnoreCase(role);
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }
}
