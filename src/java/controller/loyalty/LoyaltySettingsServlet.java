package controller.loyalty;

import dao.LoyaltyDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import model.User;

/** Màn hình thiết lập quy tắc quy đổi điểm tích lũy — chỉ Manager. */
@WebServlet(name = "LoyaltySettingsServlet", urlPatterns = {"/loyalty-settings"})
public class LoyaltySettingsServlet extends HttpServlet {
    private static final BigDecimal MAX_MONEY = new BigDecimal("1000000000");
    private static final int MAX_POINTS = 100000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireManager(request, response);
        if (actor == null) return;
        HttpSession session = request.getSession();
        request.setAttribute("loyaltyMessage", session.getAttribute("loyaltyMessage"));
        request.setAttribute("loyaltyMessageType", session.getAttribute("loyaltyMessageType"));
        session.removeAttribute("loyaltyMessage");
        session.removeAttribute("loyaltyMessageType");
        try (LoyaltyDAO dao = new LoyaltyDAO()) {
            request.setAttribute("settings", dao.getSettings());
            request.setAttribute("lastUpdate", dao.getLastUpdateText());
        }
        request.getRequestDispatcher("/WEB-INF/views/loyalty/loyalty-settings.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireManager(request, response);
        if (actor == null) return;
        String message;
        String type = "error";
        BigDecimal spend = money(request.getParameter("earnSpendAmount"));
        BigDecimal redeem = money(request.getParameter("redeemValuePerPoint"));
        int points = parseInt(request.getParameter("earnPoints"));
        if (spend == null || spend.signum() <= 0 || spend.compareTo(MAX_MONEY) > 0) {
            message = "Số tiền để được tích điểm phải là số nguyên dương (tối đa 1.000.000.000).";
        } else if (points < 1 || points > MAX_POINTS) {
            message = "Số điểm được cộng phải từ 1 đến " + MAX_POINTS + ".";
        } else if (redeem == null || redeem.signum() <= 0 || redeem.compareTo(MAX_MONEY) > 0) {
            message = "Số tiền giảm cho mỗi điểm phải là số nguyên dương (tối đa 1.000.000.000).";
        } else {
            try (LoyaltyDAO dao = new LoyaltyDAO()) {
                if (dao.updateSettings(spend, points, redeem, actor.getUserId())) {
                    message = "Đã lưu quy tắc quy đổi điểm. Áp dụng cho các hóa đơn thanh toán từ bây giờ.";
                    type = "success";
                } else {
                    message = "Không thể lưu quy tắc quy đổi điểm.";
                }
            }
        }
        request.getSession().setAttribute("loyaltyMessage", message);
        request.getSession().setAttribute("loyaltyMessageType", type);
        response.sendRedirect(request.getContextPath() + "/loyalty-settings");
    }

    private User requireManager(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object u = session == null ? null : session.getAttribute("user");
        if (!(u instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        User user = (User) u;
        if (!"Manager".equalsIgnoreCase(user.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }

    private BigDecimal money(String value) {
        try {
            return new BigDecimal(value.trim()).setScale(0, java.math.RoundingMode.UNNECESSARY);
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
