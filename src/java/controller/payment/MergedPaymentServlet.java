package controller.payment;

import dao.MergedBillDAO;
import dao.LoyaltyDAO;
import dao.OrderDAO;
import dao.ServiceHistoryDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.TableBill;
import model.User;

@WebServlet(name = "MergedPaymentServlet", urlPatterns = {"/merged-payment"})
public class MergedPaymentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        User user = staff(req, resp);
        if (user == null) return;
        int id = number(req.getParameter("id"));
        if (id < 1) { resp.sendRedirect(req.getContextPath() + "/payment"); return; }
        try (MergedBillDAO dao = new MergedBillDAO()) {
            if ("leave".equals(req.getParameter("action"))) {
                dao.discardPaymentLinks(id);
                resp.sendRedirect(req.getContextPath() + "/payment");
                return;
            }
            TableBill bill = dao.bill(id);
            if (bill == null) { resp.sendRedirect(req.getContextPath() + "/payment"); return; }
            req.setAttribute("bill", bill);
            req.setAttribute("mergedId", id);
            try (LoyaltyDAO loyalty = new LoyaltyDAO()) {
                model.LoyaltySettings settings = loyalty.getSettings();
                req.setAttribute("loyaltySettings", settings);
                BigDecimal redeem = settings.getRedeemValuePerPoint();
                int max = redeem.signum() > 0
                        ? bill.getTotalAmount().subtract(bill.getVoucherDiscount()).divideToIntegralValue(redeem).intValue() : 0;
                req.setAttribute("maxLoyaltyPoints", Math.min(bill.getLoyaltyBalance(), max));
            }
            req.setAttribute("qrReference", bill.isReadyForPayment() ? dao.issueQr(id) : "");
            req.setAttribute("paymentQrImageUrl", getServletContext().getInitParameter("paymentQrImageUrl"));
            req.setAttribute("paymentBankCode", getServletContext().getInitParameter("paymentBankCode"));
            req.setAttribute("paymentAccountNumber", getServletContext().getInitParameter("paymentAccountNumber"));
            req.setAttribute("paymentAccountName", getServletContext().getInitParameter("paymentAccountName"));
            String token = System.getenv("SEPAY_WEBHOOK_TOKEN");
            String actor = System.getenv("SEPAY_ACTOR_USER_ID");
            req.setAttribute("sepayWebhookConfigured", token != null && !token.trim().isEmpty() && actor != null && !actor.trim().isEmpty());
            HttpSession session = req.getSession();
            req.setAttribute("paymentMessage", session.getAttribute("mergedPaymentMessage"));
            session.removeAttribute("mergedPaymentMessage");
            req.getRequestDispatcher("/WEB-INF/views/payment/merged-payment.jsp").forward(req, resp);
        } catch (SQLException e) { throw new ServletException("Không tải được bill gộp.", e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = staff(req, resp);
        if (user == null) return;
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        int id = number(req.getParameter("id"));
        try (MergedBillDAO dao = new MergedBillDAO()) {
            if ("create".equals(action)) {
                List<Integer> selected = new ArrayList<>();
                String[] raw = req.getParameterValues("tableId");
                if (raw != null) for (String s : raw) {
                    int n = number(s);
                    if (n < 1) { req.getSession().setAttribute("paymentMessage", "Mã bàn không hợp lệ."); resp.sendRedirect(req.getContextPath() + "/payment"); return; }
                    selected.add(n);
                }
                try {
                    id = dao.create(selected, user.getUserId());
                    resp.sendRedirect(req.getContextPath() + "/merged-payment?id=" + id);
                    return;
                } catch (SQLException e) {
                    req.getSession().setAttribute("paymentMessage", e.getMessage());
                    resp.sendRedirect(req.getContextPath() + "/payment"); return;
                }
            }
            if (id < 1) { resp.sendError(400); return; }
            if ("linkLoyalty".equals(action) || "setLoyaltyPoints".equals(action)
                    || "clearLoyalty".equals(action) || "applyVoucher".equals(action)
                    || "clearVoucher".equals(action)) {
                TableBill bill = dao.bill(id);
                if (bill == null) { resp.sendRedirect(req.getContextPath() + "/payment"); return; }
                String message;
                if ("linkLoyalty".equals(action)) {
                    try (LoyaltyDAO loyalty = new LoyaltyDAO()) {
                        LoyaltyDAO.Customer customer = loyalty.findCustomer(req.getParameter("customerKey"));
                        if (customer == null) message = "Không tìm thấy tài khoản customer.";
                        else {
                            dao.setLoyalty(id, customer.getUserId(), 0, user.getUserId());
                            message = "Đã gắn tài khoản khách hàng vào bill gộp.";
                        }
                    }
                } else if ("setLoyaltyPoints".equals(action)) {
                    int points = number(req.getParameter("points"));
                    try (LoyaltyDAO loyalty = new LoyaltyDAO()) {
                        if (!bill.isHasLoyaltyCustomer()) message = "Chưa gắn tài khoản customer cho bill.";
                        else if (points < 0 || points > loyalty.getBalance(bill.getLoyaltyCustomerId())) message = "Số điểm không hợp lệ.";
                        else {
                            dao.setLoyalty(id, bill.getLoyaltyCustomerId(), points, user.getUserId());
                            message = "Đã cập nhật số điểm sử dụng.";
                        }
                    }
                } else if ("clearLoyalty".equals(action)) {
                    dao.clearLoyalty(id); message = "Đã bỏ gắn tài khoản customer.";
                } else if ("applyVoucher".equals(action)) {
                    message = dao.applyVoucher(id, req.getParameter("voucherCode"), user.getUserId());
                    if (message == null) message = "Đã áp dụng voucher cho bill gộp.";
                } else {
                    dao.clearVoucher(id); message = "Đã bỏ mã voucher.";
                }
                req.getSession().setAttribute("mergedPaymentMessage", message);
                resp.sendRedirect(req.getContextPath() + "/merged-payment?id=" + id);
                return;
            }
            if ("cancel".equals(action)) {
                req.getSession().setAttribute("paymentMessage", dao.cancel(id) ? "Đã hủy gộp bill." : "Bill đã chốt hoặc không còn tồn tại.");
                resp.sendRedirect(req.getContextPath() + "/payment"); return;
            }
            if ("pay".equals(action)) {
                if (!"CASH".equals(req.getParameter("paymentMethod"))) { resp.sendError(400); return; }
                BigDecimal received = money(req.getParameter("cashReceived"));
                BigDecimal expected = money(req.getParameter("expectedAmount"));
                if (dao.pay(id, user.getUserId(), "CASH", null, received, expected)) {
                    try (ServiceHistoryDAO histories = new ServiceHistoryDAO()) {
                        Integer history = histories.findMergedHistoryId(id);
                        resp.sendRedirect(req.getContextPath() + "/service-history" + (history == null ? "" : "?id=" + history));
                    }
                    return;
                }
                req.getSession().setAttribute("mergedPaymentMessage", dao.getError());
                resp.sendRedirect(req.getContextPath() + "/merged-payment?id=" + id); return;
            }
            resp.sendError(400);
        } catch (SQLException e) { throw new ServletException("Không thể xử lý bill gộp.", e); }
    }

    private BigDecimal money(String s) {
        try { return s == null ? null : new BigDecimal(s); }
        catch (NumberFormatException e) { return null; }
    }
    private int number(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return -1; }
    }
    private User staff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if (!(u instanceof User)) { resp.sendRedirect(req.getContextPath() + "/login"); return null; }
        User user = (User) u;
        if (!"Manager".equalsIgnoreCase(user.getRoleName()) && !"Receptionist".equalsIgnoreCase(user.getRoleName())) {
            resp.sendError(403); return null;
        }
        return user;
    }
}
