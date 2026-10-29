package controller.voucher;

import dao.VoucherDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import model.User;
import model.Voucher;
import utils.EmailUtils;

/**
 * Màn gửi voucher giảm giá — chỉ Manager. Bước 1: cấu hình % giảm và tổng đã chi
 * tối thiểu rồi xem danh sách khách đủ điều kiện. Bước 2: chọn khách, tạo mã và
 * gửi email. Mọi giá trị đều được kiểm lại ở server, không tin form.
 */
@WebServlet(name = "VoucherServlet", urlPatterns = {"/vouchers"})
public class VoucherServlet extends HttpServlet {

    private static final BigDecimal MAX_PERCENT = new BigDecimal("100");
    private static final BigDecimal MAX_MONEY = new BigDecimal("1000000000");
    private static final int MAX_RECIPIENTS = 300;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        User actor = requireManager(request, response);
        if (actor == null) {
            return;
        }
        HttpSession session = request.getSession();
        request.setAttribute("voucherMessage", session.getAttribute("voucherMessage"));
        request.setAttribute("voucherMessageType", session.getAttribute("voucherMessageType"));
        session.removeAttribute("voucherMessage");
        session.removeAttribute("voucherMessageType");

        String percentParam = request.getParameter("percent");
        String minParam = request.getParameter("minSpent");
        if (percentParam != null || minParam != null) {
            BigDecimal percent = parsePercent(percentParam);
            BigDecimal minSpent = parseMoney(minParam);
            if (percent == null) {
                request.setAttribute("voucherMessage", "Phần trăm giảm phải từ 0,01 đến 100, tối đa 2 chữ số thập phân.");
                request.setAttribute("voucherMessageType", "error");
            } else if (minSpent == null) {
                request.setAttribute("voucherMessage", "Tổng đã chi tối thiểu phải là số nguyên từ 0 đến 1.000.000.000 đ.");
                request.setAttribute("voucherMessageType", "error");
            } else {
                try (VoucherDAO dao = new VoucherDAO()) {
                    request.setAttribute("recipients", dao.findEligibleCustomers(minSpent));
                } catch (SQLException e) {
                    throw new ServletException("Không tải được danh sách khách.", e);
                }
                request.setAttribute("previewPercent", percent);
                request.setAttribute("previewMinSpent", minSpent);
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/voucher/vouchers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User actor = requireManager(request, response);
        if (actor == null) {
            return;
        }
        BigDecimal percent = parsePercent(request.getParameter("percent"));
        BigDecimal minSpent = parseMoney(request.getParameter("minSpent"));
        List<Integer> userIds = parseUserIds(request.getParameterValues("userIds"));
        String message;
        String type = "error";
        if (percent == null) {
            message = "Phần trăm giảm phải từ 0,01 đến 100, tối đa 2 chữ số thập phân.";
        } else if (minSpent == null) {
            message = "Tổng đã chi tối thiểu phải là số nguyên từ 0 đến 1.000.000.000 đ.";
        } else if (userIds == null || userIds.isEmpty()) {
            message = "Chưa chọn khách nhận voucher.";
        } else if (userIds.size() > MAX_RECIPIENTS) {
            message = "Mỗi lần gửi tối đa " + MAX_RECIPIENTS + " khách.";
        } else {
            try (VoucherDAO dao = new VoucherDAO()) {
                List<Voucher> issued = dao.issueVouchers(percent, minSpent, userIds, actor.getUserId());
                // Mã đã lưu xong mới gửi mail; khách gửi lỗi thì in mã ra để Manager báo tay.
                int sent = 0;
                List<String> failed = new ArrayList<>();
                for (Voucher v : issued) {
                    if (EmailUtils.sendEmail(v.getCustomerEmail(), subject(percent), body(v, percent))) {
                        sent++;
                    } else {
                        failed.add(v.getCustomerName() + " (" + v.getCustomerEmail() + "): " + v.getCode());
                    }
                }
                if (failed.isEmpty()) {
                    message = "Đã tạo " + issued.size() + " voucher và gửi email thành công.";
                    type = "success";
                } else {
                    message = "Đã tạo " + issued.size() + " voucher, gửi email thành công " + sent
                            + ". Các khách sau chưa nhận được email, hãy báo mã tay (mã vẫn hợp lệ):\n"
                            + String.join("\n", failed);
                }
            } catch (IllegalArgumentException e) {
                message = e.getMessage();
            } catch (SQLException e) {
                e.printStackTrace();
                message = "Không thể tạo voucher, vui lòng thử lại.";
            }
        }
        HttpSession session = request.getSession();
        session.setAttribute("voucherMessage", message);
        session.setAttribute("voucherMessageType", type);
        response.sendRedirect(request.getContextPath() + "/vouchers");
    }

    private static String subject(BigDecimal percent) {
        return "Voucher giảm " + percent.stripTrailingZeros().toPlainString() + "% tổng hóa đơn - Việt À la carte";
    }

    private static String body(Voucher v, BigDecimal percent) {
        return "Xin chào " + v.getCustomerName() + ",\n\n"
                + "Việt À la carte gửi tặng bạn mã giảm giá " + percent.stripTrailingZeros().toPlainString()
                + "% tổng hóa đơn.\n\n"
                + "Mã voucher: " + v.getCode() + "\n\n"
                + "Cách dùng: đọc mã này cho lễ tân khi thanh toán. Mỗi mã chỉ dùng được một lần.\n\n"
                + "Cảm ơn bạn đã đồng hành cùng chúng tôi!\nViệt À la carte";
    }

    /** Từ 0,01 đến 100, tối đa 2 chữ số thập phân; null nếu không hợp lệ. */
    static BigDecimal parsePercent(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal p = new BigDecimal(raw.trim().replace(',', '.'));
            if (p.signum() <= 0 || p.compareTo(MAX_PERCENT) > 0) {
                return null;
            }
            if (p.stripTrailingZeros().scale() > 2) {
                return null;
            }
            return p.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /** Số nguyên đồng từ 0 đến 1.000.000.000; null nếu không hợp lệ. */
    static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal b = new BigDecimal(raw.trim());
            if (b.signum() < 0 || b.compareTo(MAX_MONEY) > 0) {
                return null;
            }
            if (b.stripTrailingZeros().scale() > 0) {
                return null;
            }
            return b.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /** Danh sách id khách, bỏ trùng; null nếu có giá trị không phải số nguyên dương. */
    private static List<Integer> parseUserIds(String[] raw) {
        if (raw == null) {
            return null;
        }
        Set<Integer> ids = new LinkedHashSet<>();
        for (String s : raw) {
            try {
                int id = Integer.parseInt(s.trim());
                if (id <= 0) {
                    return null;
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new ArrayList<>(ids);
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
}
