package controller.payment;

import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Webhook SePay: chỉ chốt QR khi đúng token, đúng mã bàn và đúng số tiền.
 * Token/user id đặt ở biến môi trường, không lưu trong source.
 */
@WebServlet(name = "SePayWebhookServlet", urlPatterns = {"/webhooks/sepay"})
public class SePayWebhookServlet extends HttpServlet {

    private static final Pattern TABLE = Pattern.compile("(RM-TABLE-(\\d+)-(\\d+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPACT_TABLE = Pattern.compile("(RMTABLE(\\d{1,3})O(\\d+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT = Pattern.compile(
            "\"(?:transferAmount|amount)\"\\s*:\\s*\"?([0-9]+(?:[.,][0-9]+)?)\"?",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = System.getenv("SEPAY_WEBHOOK_TOKEN");
        String actor = System.getenv("SEPAY_ACTOR_USER_ID");
        String supplied = request.getHeader("Authorization");
        if (supplied == null) {
            supplied = request.getHeader("X-SePay-Token");
        }
        if (token == null || token.trim().isEmpty() || supplied == null
                || !supplied.replaceFirst("(?i)^Apikey\\s+", "").equals(token)
                || actor == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Webhook token không hợp lệ.");
            return;
        }
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Matcher table = TABLE.matcher(body);
        Matcher compactTable = COMPACT_TABLE.matcher(body);
        Matcher amount = AMOUNT.matcher(body);
        boolean standardReference = table.find();
        boolean compactReference = !standardReference && compactTable.find();
        if (!standardReference && !compactReference) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Payload thiếu content chứa mã RM-TABLE-<tableId>-<billKey> hoặc RMTABLE<tableId>O<billKey>. Body=" + body);
            return;
        }
        if (!amount.find()) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Payload thiếu transferAmount hoặc amount dạng số. Body=" + body);
            return;
        }
        int tableId;
        int actorId;
        int billKey;
        try {
            tableId = Integer.parseInt(standardReference ? table.group(2) : compactTable.group(2));
            billKey = Integer.parseInt(standardReference ? table.group(3) : compactTable.group(3));
            actorId = Integer.parseInt(actor);
        } catch (NumberFormatException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Mã bàn hoặc actor không hợp lệ.");
            return;
        }
        // Mã phải khớp bill đang mở của bàn (kiểm trong DAO); chuyển khoản cũ không chốt được bill mới.
        String reference = OrderDAO.qrReference(tableId, billKey);
        boolean paid;
        String failureReason = null;
        try (OrderDAO dao = new OrderDAO()) {
            paid = dao.markQrTablePaid(tableId, actorId,
                    reference, new BigDecimal(amount.group(1).replace(',', '.')));
            failureReason = dao.getLastPaymentFailureReason();
        }
        response.setContentType("application/json;charset=UTF-8");
        if (paid) {
            response.getWriter().print("{\"success\":true}");
        } else {
            String safeReason = failureReason == null ? "Không xác định." : failureReason.replace("\\", "\\\\").replace("\"", "\\\"");
            response.getWriter().print("{\"success\":false,\"reason\":\"" + safeReason + "\"}");
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String safe = message.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
        response.getWriter().print("{\"success\":false,\"reason\":\"" + safe + "\"}");
    }
}
