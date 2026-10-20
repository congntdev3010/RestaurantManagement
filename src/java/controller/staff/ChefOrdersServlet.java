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

@WebServlet(name = "ChefOrdersServlet", urlPatterns = {"/chef-orders"})
public class ChefOrdersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (requireChef(request, response) == null) return;
        HttpSession session = request.getSession();
        CartServlet.ensureToken(session);
        request.setAttribute("chefMessage", session.getAttribute("chefOrderMessage"));
        session.removeAttribute("chefOrderMessage");
        try (OrderDAO orders = new OrderDAO()) {
            request.setAttribute("orders", orders.getChefOrders());
        }
        request.getRequestDispatcher("/WEB-INF/views/staff/chef-orders.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User chef = requireChef(request, response);
        if (chef == null) return;
        HttpSession session = request.getSession(false);
        if (session == null || !CartServlet.validToken(session, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(request.getParameter("orderId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean updated = false;
        try (OrderDAO orders = new OrderDAO()) {
            String action = request.getParameter("action");
            if ("start".equals(action)) updated = orders.startChefPreparation(orderId);
            else if ("complete".equals(action)) updated = orders.completeChefPreparation(orderId);
        }
        session.setAttribute("chefOrderMessage", updated
                ? "Đã cập nhật trạng thái chế biến."
                : "Không thể cập nhật trạng thái đơn hàng.");
        response.sendRedirect(request.getContextPath() + "/chef-orders");
    }

    private User requireChef(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        if (!(value instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) value;
        if (!"Chef".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }
}