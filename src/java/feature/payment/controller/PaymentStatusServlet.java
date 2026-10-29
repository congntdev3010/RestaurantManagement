package feature.payment.controller;

import feature.order.dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.TableBill;
import model.User;

@WebServlet(name = "PaymentStatusServlet", urlPatterns = {"/payment-status"})
public class PaymentStatusServlet extends HttpServlet {
    private static final Pattern COMPACT_REFERENCE = Pattern.compile(
            "^RMTABLE(\\d{1,3})(\\d{13})$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User) || (!hasRole((User) sessionUser, "Receptionist")
                && !hasRole((User) sessionUser, "Manager"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int tableId;
        try {
            tableId = Integer.parseInt(request.getParameter("tableId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String reference = request.getParameter("reference");
        if (tableId < 1 || reference == null || reference.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        reference = normalizeReference(reference.trim());

        boolean paid;
        TableBill receipt = null;
        try (OrderDAO dao = new OrderDAO()) {
            paid = dao.isQrPaymentCompleted(tableId, reference.trim());
            if (paid) {
                receipt = dao.getQrReceipt(tableId, reference.trim());
            }
        } catch (SQLException e) {
            throw new ServletException("Không thể kiểm tra trạng thái thanh toán QR.", e);
        }
        if (paid && receipt != null) {
            receipt.setPaymentMethod("QR");
            session.setAttribute("oneTimeServiceReceipt", receipt);
        }
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print("{\"paid\":" + (paid && receipt != null) + "}");
    }

    private boolean hasRole(User user, String role) {
        return user.getRoleName() != null && role.equalsIgnoreCase(user.getRoleName());
    }

    private String normalizeReference(String reference) {
        Matcher compact = COMPACT_REFERENCE.matcher(reference);
        if (compact.matches()) {
            return "RM-TABLE-" + compact.group(1) + "-" + compact.group(2);
        }
        return reference;
    }
}
