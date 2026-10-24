package controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.TableBill;
import model.User;

/**
 * Trang bill dùng một lần sau thanh toán. Không có truy vấn xem lại cho lễ tân.
 */
@WebServlet(name = "ServiceHistoryServlet", urlPatterns = {"/service-history"})
public class ServiceHistoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object actor = session == null ? null : session.getAttribute("user");
        if (!(actor instanceof User)
                || (!"Receptionist".equalsIgnoreCase(((User) actor).getRoleName())
                && !"Manager".equalsIgnoreCase(((User) actor).getRoleName()))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Object receipt = session.getAttribute("oneTimeServiceReceipt");
        session.removeAttribute("oneTimeServiceReceipt");
        if (!(receipt instanceof TableBill)) {
            response.sendRedirect(request.getContextPath() + "/manage-tables");
            return;
        }
        request.setAttribute("receipt", receipt);
        request.getRequestDispatcher("/WEB-INF/views/staff/service-history.jsp").forward(request, response);
    }
}
