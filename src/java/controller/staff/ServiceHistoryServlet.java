package controller.staff;

import dao.ServiceHistoryDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.PaidBill;
import model.TableBill;
import model.User;

/**
 * Lịch sử thanh toán — chỉ Manager. Tổng số đơn và doanh thu cũng chỉ Manager được xem.
 * - Ngay sau khi thanh toán (tiền mặt/QR) chuyển thẳng tới hóa đơn vừa trả.
 * - Danh sách lọc theo ngày; xem và in lại hóa đơn bất kỳ lúc nào.
 */
@WebServlet(name = "ServiceHistoryServlet", urlPatterns = {"/service-history"})
public class ServiceHistoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Object sessionUser = session == null ? null : session.getAttribute("user");
        if (!(sessionUser instanceof User)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String role = ((User) sessionUser).getRoleName();
        // Manager xem và lọc tất cả, thấy tổng. Lễ tân chỉ xem và in hóa đơn trong ngày: không lọc ngày, không thấy tổng.
        boolean manager = "Manager".equalsIgnoreCase(role);
        if (!manager && !"Receptionist".equalsIgnoreCase(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        LocalDate today = LocalDate.now();

        // Vừa thanh toán xong: mở luôn hóa đơn của bàn đó (đã lưu trong lịch sử, xem lại được sau này).
        Object justPaid = session.getAttribute("oneTimeServiceReceipt");
        session.removeAttribute("oneTimeServiceReceipt");
        String idParam = request.getParameter("id");
        if (idParam == null && justPaid instanceof TableBill) {
            try (ServiceHistoryDAO dao = new ServiceHistoryDAO()) {
                Integer latest = dao.findLatestHistoryId(((TableBill) justPaid).getTableId());
                if (latest != null) {
                    response.sendRedirect(request.getContextPath() + "/service-history?id=" + latest);
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LocalDate from = manager ? parseDate(request.getParameter("from")) : today;
        LocalDate to = manager ? parseDate(request.getParameter("to")) : today;
        if (from != null && to != null && from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }

        try (ServiceHistoryDAO dao = new ServiceHistoryDAO()) {
            List<PaidBill> bills = dao.search(from, to);
            request.setAttribute("bills", bills);
            request.setAttribute("isManager", manager);
            if (manager) {
                BigDecimal revenue = BigDecimal.ZERO;
                for (PaidBill b : bills) {
                    revenue = revenue.add(b.getPayableAmount());
                }
                request.setAttribute("revenue", revenue);
            }

            if (idParam != null) {
                int id = parseInt(idParam);
                PaidBill selected = id > 0 ? dao.getById(id) : null;
                // Lễ tân chỉ mở được hóa đơn của hôm nay.
                if (!manager && selected != null && (selected.getPaidAt() == null
                        || !selected.getPaidAt().toLocalDate().equals(today))) {
                    selected = null;
                }
                if (selected != null) {
                    request.setAttribute("selected", selected);
                } else {
                    request.setAttribute("detailMessage", "Không tìm thấy hóa đơn #" + idParam + ".");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("loadFailed", true);
        }

        request.setAttribute("from", from == null ? "" : from.toString());
        request.setAttribute("to", to == null ? "" : to.toString());
        request.getRequestDispatcher("/WEB-INF/views/staff/service-history.jsp").forward(request, response);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
