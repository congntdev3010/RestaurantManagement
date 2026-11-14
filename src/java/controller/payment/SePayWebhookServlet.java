package controller.payment;

import dao.PaymentDAO;
import dao.MergedBillDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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
    private static final Pattern MERGED = Pattern.compile("(RMG(\\d+)V(\\d+))", Pattern.CASE_INSENSITIVE);
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
        Matcher merged = MERGED.matcher(body);
        Matcher amount = AMOUNT.matcher(body);
        boolean standardReference = table.find();
        boolean compactReference = !standardReference && compactTable.find();
        boolean mergedReference = !standardReference && !compactReference && merged.find();
        if (!standardReference && !compactReference && !mergedReference) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Payload thiếu mã QR hợp lệ. Body=" + body);
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
            if (mergedReference) {
                int mergedId = Integer.parseInt(merged.group(2));
                int version = Integer.parseInt(merged.group(3));
                actorId = Integer.parseInt(actor);
                boolean paid;
                try (MergedBillDAO dao = new MergedBillDAO()) {
                    try {
                        paid = dao.receiveQr(mergedId, version, providerId(body),
                                new BigDecimal(amount.group(1).replace(',', '.')), actorId);
                    } catch (SQLException e) {
                        getServletContext().log("Không thể xử lý QR bill gộp.", e);
                        writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                "Không thể xử lý thanh toán QR bill gộp.");
                        return;
                    }
                    String reason = dao.getError();
                    response.setContentType("application/json;charset=UTF-8");
                    if (paid) response.getWriter().print("{\"success\":true}");
                    else response.getWriter().print("{\"success\":false,\"reason\":\""
                            + (reason == null ? "Khoản chuyển cần đối soát." : reason).replace("\"", "\\\"") + "\"}");
                    return;
                }
            }
            tableId = Integer.parseInt(standardReference ? table.group(2) : compactTable.group(2));
            billKey = Integer.parseInt(standardReference ? table.group(3) : compactTable.group(3));
            actorId = Integer.parseInt(actor);
        } catch (NumberFormatException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Mã bàn hoặc actor không hợp lệ.");
            return;
        }

        // Mã phải khớp bill đang mở của bàn (kiểm trong DAO); chuyển khoản cũ không chốt được bill mới.
        String reference = PaymentDAO.qrReference(tableId, billKey);
        boolean paid;
        String failureReason = null;
        try (PaymentDAO dao = new PaymentDAO()) {
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

    private String providerId(String body) {
        Matcher m = Pattern.compile("\"(?:id|transactionId|referenceCode)\"\\s*:\\s*\"?([^\",}]+)\"?",
                Pattern.CASE_INSENSITIVE).matcher(body);
        return m.find() ? m.group(1).trim() : "SEPAY-" + Integer.toHexString(body.hashCode());
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String safe = message.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
        response.getWriter().print("{\"success\":false,\"reason\":\"" + safe + "\"}");
    }
}
