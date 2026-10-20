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

@WebServlet(name = "ChefOrdersServlet", urlPatterns = {"/chef-orders"})
public class ChefOrdersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (requireChef(request, response) == null) return;
        try (OrderDAO orders = new OrderDAO()) {
            request.setAttribute("orders", orders.getChefOrders());
        }
        request.getRequestDispatcher("/WEB-INF/views/staff/chef-orders.jsp").forward(request, response);
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
