package controller.staff;

import dao.OrderDAO;
import dao.RestaurantTableDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import model.TableBill;
import model.RestaurantTable;
import model.User;

@WebServlet(name = "PaymentServlet", urlPatterns = {"/payment"})
public class PaymentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = currentUser(request);
        if (actor == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!hasRole(actor, "Receptionist") && !hasRole(actor, "Manager")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (OrderDAO dao = new OrderDAO()) {
            String tableIdParameter = request.getParameter("tableId");
            if (tableIdParameter == null || tableIdParameter.trim().isEmpty()) {
                loadPaymentDashboard(request, dao);
                request.getRequestDispatcher("/WEB-INF/views/staff/payment-dashboard.jsp").forward(request, response);
                return;
            }

            int tableId;
            try {
                tableId = Integer.parseInt(tableIdParameter);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (tableId < 1) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            TableBill bill = dao.getTableBill(tableId);
            if (bill == null) {
                response.sendRedirect(request.getContextPath() + "/manage-tables");
                return;
            }
            request.setAttribute("bill", bill);
            request.setAttribute("paymentQrImageUrl", getServletContext().getInitParameter("paymentQrImageUrl"));
            request.setAttribute("paymentBankCode", getServletContext().getInitParameter("paymentBankCode"));
            request.setAttribute("paymentAccountNumber", getServletContext().getInitParameter("paymentAccountNumber"));
            request.setAttribute("paymentAccountName", getServletContext().getInitParameter("paymentAccountName"));
            request.setAttribute("sepayWebhookConfigured",
                    System.getenv("SEPAY_WEBHOOK_TOKEN") != null
                    && !System.getenv("SEPAY_WEBHOOK_TOKEN").trim().isEmpty());
            request.getRequestDispatcher("/WEB-INF/views/staff/payment.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Không thể tải bill thanh toán.", e);
        }
    }

    private void loadPaymentDashboard(HttpServletRequest request, OrderDAO orderDao)
            throws SQLException {
        String filter = "READY".equalsIgnoreCase(request.getParameter("filter"))
                ? "READY" : "ALL";
        java.util.List<RestaurantTable> tables;
        java.util.Map<Integer, TableBill> bills = new java.util.LinkedHashMap<>();
        try (RestaurantTableDAO tableDao = new RestaurantTableDAO()) {
            tables = tableDao.getAllTables();
        }
        for (RestaurantTable table : tables) {
            TableBill bill = orderDao.getTableBill(table.getTableId());
            if (bill != null) {
                bills.put(table.getTableId(), bill);
            }
        }
        request.setAttribute("paymentTables", tables);
        request.setAttribute("paymentBills", bills);
        request.setAttribute("paymentFilter", filter);
        request.setAttribute("unpaidTableCount", bills.size());
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("user");
        return value instanceof User ? (User) value : null;
    }

    private boolean hasRole(User user, String role) {
        return user.getRoleName() != null && role.equalsIgnoreCase(user.getRoleName());
    }
}
