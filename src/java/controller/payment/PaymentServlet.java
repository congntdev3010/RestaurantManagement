package controller.payment;

import dao.LoyaltyDAO;
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
                request.getRequestDispatcher("/WEB-INF/views/payment/payment-dashboard.jsp").forward(request, response);
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
            try (LoyaltyDAO loyaltyDao = new LoyaltyDAO()) {
                model.LoyaltySettings loyaltySettings = loyaltyDao.getSettings();
                request.setAttribute("loyaltySettings", loyaltySettings);
                // Số điểm tối đa dùng được: không quá số dư và không làm tiền giảm vượt tổng bill.
                int byTotal = bill.getTotalAmount().divideToIntegralValue(loyaltySettings.getRedeemValuePerPoint()).intValue();
                request.setAttribute("maxLoyaltyPoints", Math.min(bill.getLoyaltyBalance(), byTotal));
            }
            HttpSession flash = request.getSession();
            request.setAttribute("paymentMessage", flash.getAttribute("paymentMessage"));
            request.setAttribute("paymentMessageType", flash.getAttribute("paymentMessageType"));
            flash.removeAttribute("paymentMessage");
            flash.removeAttribute("paymentMessageType");
            request.setAttribute("paymentQrImageUrl", getServletContext().getInitParameter("paymentQrImageUrl"));
            request.setAttribute("paymentBankCode", getServletContext().getInitParameter("paymentBankCode"));
            request.setAttribute("paymentAccountNumber", getServletContext().getInitParameter("paymentAccountNumber"));
            request.setAttribute("paymentAccountName", getServletContext().getInitParameter("paymentAccountName"));
            request.setAttribute("sepayWebhookConfigured", isSePayConfigured());
            request.getRequestDispatcher("/WEB-INF/views/payment/payment.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Không thể tải bill thanh toán.", e);
        }
    }

    /**
     * Lễ tân gắn tài khoản customer / chọn số điểm dùng cho bill của bàn, trước
     * khi thanh toán.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = currentUser(request);
        if (actor == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!hasRole(actor, "Receptionist") && !hasRole(actor, "Manager")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int tableId = parseInt(request.getParameter("tableId"));
        String action = request.getParameter("action");
        String message;
        String type = "error";
        try (OrderDAO orderDao = new OrderDAO(); LoyaltyDAO loyalty = new LoyaltyDAO()) {
            TableBill bill = tableId > 0 ? orderDao.getTableBill(tableId) : null;
            model.LoyaltySettings settings = loyalty.getSettings();
            if (bill == null) {
                message = "Bàn không còn bill để thanh toán.";
            } else if ("linkLoyalty".equals(action)) {
                String customerKey = request.getParameter("customerKey");
                if (customerKey == null || customerKey.trim().isEmpty()) {
                    message = "Vui lòng nhập số điện thoại hoặc Gmail của khách hàng.";
                } else if (customerKey.length() > 30) {
                    message = "Tài khoản khách hàng không được vượt quá 30 ký tự.";
                } else {
                    LoyaltyDAO.Customer customer = loyalty.findCustomer(customerKey);
                    if (customer == null) {
                        message = "Không tìm thấy tài khoản customer (nhập số điện thoại hoặc Gmail).";
                    } else if (loyalty.setBillLink(tableId, customer.getUserId(), 0, actor.getUserId())) {
                        message = "Đã gắn tài khoản " + customer.getFullName() + " (" + customer.getPoints() + " điểm) vào bill; họ tên, số điện thoại và Gmail khách đã được tự điền.";
                        type = "success";
                    } else {
                        message = "Không thể gắn tài khoản vào bill.";
                    }
                }
            } else if ("setLoyaltyPoints".equals(action)) {
                int points = parseInt(request.getParameter("points"));
                if (!bill.isHasLoyaltyCustomer()) {
                    message = "Chưa gắn tài khoản customer cho bill.";
                } else if (points < 0) {
                    message = "Số điểm không hợp lệ.";
                } else if (points > loyalty.getBalance(bill.getLoyaltyCustomerId())) {
                    message = "Khách không đủ điểm (hiện có " + loyalty.getBalance(bill.getLoyaltyCustomerId()) + " điểm).";
                } else if (settings.getRedeemValuePerPoint().multiply(java.math.BigDecimal.valueOf(points))
                        .compareTo(bill.getTotalAmount()) > 0) {
                    message = "Số tiền giảm không được vượt quá tổng hóa đơn.";
                } else if (loyalty.setBillLink(tableId, bill.getLoyaltyCustomerId(), points, actor.getUserId())) {
                    message = points == 0 ? "Đã bỏ dùng điểm." : "Đã áp dụng " + points + " điểm cho bill.";
                    type = "success";
                } else {
                    message = "Không thể lưu số điểm sử dụng.";
                }
            } else if ("clearLoyalty".equals(action)) {
                loyalty.clearBillLink(tableId);
                message = "Đã bỏ gắn tài khoản customer khỏi bill.";
                type = "success";
            } else {
                message = "Thao tác không hợp lệ.";
            }
        }
        request.getSession().setAttribute("paymentMessage", message);
        request.getSession().setAttribute("paymentMessageType", type);
        response.sendRedirect(request.getContextPath() + (tableId > 0 ? "/payment?tableId=" + tableId : "/payment"));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return -1;
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

    private boolean isSePayConfigured() {
        String token = System.getenv("SEPAY_WEBHOOK_TOKEN");
        String actor = System.getenv("SEPAY_ACTOR_USER_ID");
        return token != null && !token.trim().isEmpty()
                && actor != null && !actor.trim().isEmpty();
    }
}
