<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán - Bàn <c:out value="${not empty bill.tableName ? bill.tableName : bill.tableId}"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.0">
    <style>
        .bill-sum { margin-top: 18px; padding: 6px 4px 0; border-top: 1px solid #e6d9cf; }
        .bill-sum-row { display: flex; justify-content: space-between; align-items: baseline; padding: 9px 0; color: #6b5d57; font-size: .98rem; }
        .bill-sum-row strong { font-size: 1.05rem; font-weight: 600; color: #3f3330; }
        .bill-sum-row.discount strong { color: #1e7a3c; }
        .bill-payable { display: flex; justify-content: space-between; align-items: baseline; gap: 14px; margin-top: 6px;
                        padding: 14px 4px 0; border-top: 2px solid #3f3330; color: #000; }
        .bill-payable span { font-size: 1.15rem; font-weight: 700; color: #000; }
        .bill-payable strong { font-size: 2rem; line-height: 1.1; font-weight: 800; color: #000; }
        .pay-alert { margin: 0 0 16px; padding: 11px 14px; border-radius: 12px; font-size: .92rem; line-height: 1.4; }
        .pay-alert.success { background: #e6f4ea; color: #1e6b38; border: 1px solid #c5e5cf; }
        .pay-alert.error { background: #fdeaea; color: #9b2227; border: 1px solid #f3c6c8; }

        .loyalty-card { border: 1px solid #ecdcd0; background: #fffaf5; border-radius: 16px; padding: 18px; margin-bottom: 18px; }
        .loyalty-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px; }
        .loyalty-head h2 { margin: 0; font-size: 1.15rem; }
        .loyalty-tag { font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; color: #8a6f66; }

        .loyalty-customer { display: flex; align-items: center; justify-content: space-between; gap: 12px;
                            background: #fff; border: 1px solid #efe2d8; border-radius: 12px; padding: 12px 14px; margin-bottom: 14px; }
        .loyalty-customer .name { font-weight: 700; color: #3f1a1f; }
        .loyalty-customer .sub { font-size: .8rem; color: #8a7a73; }
        .loyalty-balance { text-align: right; }
        .loyalty-balance strong { display: block; font-size: 1.5rem; color: #7b1f28; line-height: 1.1; }
        .loyalty-balance span { font-size: .75rem; color: #8a7a73; }

        .loyalty-card label { display: block; font-size: .85rem; font-weight: 600; color: #4d3b35; margin-bottom: 6px; }
        .loyalty-hint { font-weight: 400; color: #8a7a73; }
        .loyalty-row { display: flex; gap: 8px; align-items: stretch; }
        .loyalty-row input { flex: 1; min-width: 0; }
        .loyalty-card input[type=number], .loyalty-card input[type=text], .loyalty-card input:not([type]) {
            width: 100%; box-sizing: border-box; padding: 11px 14px; border: 1px solid #e3d3c8; border-radius: 12px; background: #fff; font: inherit; }
        .loyalty-card input:focus { outline: 2px solid #c98a3d; outline-offset: 0; border-color: #c98a3d; }

        .btn-sm { border: 0; border-radius: 12px; padding: 0 18px; font: inherit; font-weight: 600; cursor: pointer; white-space: nowrap; min-height: 44px; }
        .btn-primary-sm { background: #7b1f28; color: #fff; }
        .btn-primary-sm:hover { background: #63171f; }
        .btn-ghost { background: #fff; color: #7b1f28; border: 1px solid #e3c9c3; }
        .btn-ghost:hover { background: #fbeeee; }
        .btn-link { background: none; border: 0; color: #9b2227; font: inherit; font-size: .85rem; cursor: pointer; padding: 6px 0 0; text-decoration: underline; }

        .loyalty-summary { margin-top: 14px; border-top: 1px dashed #e3d3c8; padding-top: 12px; }
        .loyalty-summary div { display: flex; justify-content: space-between; padding: 4px 0; font-size: .93rem; color: #5d4f49; }
        .loyalty-summary .minus strong { color: #a8242b; }
        .loyalty-summary .plus strong { color: #1e7a3c; }

        .payable-box { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 0 0 22px; padding: 16px 18px;
                       border-radius: 16px; background: linear-gradient(135deg, #4a1620, #7b1f28); color: #fff; }
        .payable-box span { font-size: .85rem; letter-spacing: .05em; text-transform: uppercase; opacity: .85; }
        .payable-box strong { font-size: 1.7rem; }
    </style>
</head>
<body class="payment-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="payment-shell">
        <a class="back-link" href="${pageContext.request.contextPath}/payment">← Về danh sách thanh toán</a>
        <section class="payment-card">
            <header class="payment-header">
                <div>
                    <p class="eyebrow">HOÀN TẤT PHỤC VỤ</p>
                    <h1>Thanh toán bàn <c:out value="${not empty bill.tableName ? bill.tableName : bill.tableId}"/></h1>
                    <p class="payment-subtitle"><c:out value="${bill.orderCount}"/> order trong phiên phục vụ</p>
                </div>
            </header>
            <div class="payment-grid">
                <section class="bill-panel">
                    <h2>Chi tiết bill</h2>
                    <c:if test="${not empty bill.customerName}">
                        <p class="customer"><strong><c:out value="${bill.customerName}"/></strong> · <c:out value="${bill.customerPhone}"/></p>
                    </c:if>
                    <c:if test="${not empty bill.checkedInAt}"><p class="muted">Mở bàn: <c:out value="${bill.checkedInAt}"/></p></c:if>
                    <table>
                        <thead><tr><th>Món</th><th>SL</th><th>Thành tiền</th></tr></thead>
                        <tbody><c:forEach var="line" items="${bill.items}">
                            <tr><td><c:out value="${line.dishName}"/></td><td><c:out value="${line.quantity}"/></td><td><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/> đ</td></tr>
                        </c:forEach></tbody>
                    </table>
                    <div class="bill-sum">
                        <div class="bill-sum-row">
                            <span>Tổng bill</span>
                            <strong><fmt:formatNumber value="${bill.totalAmount}" pattern="#,##0"/> đ</strong>
                        </div>
                        <c:if test="${bill.discountAmount gt 0}">
                        <div class="bill-sum-row discount">
                            <span>Giảm giá bằng điểm (<c:out value="${bill.pointsToUse}"/> điểm)</span>
                            <strong>- <fmt:formatNumber value="${bill.discountAmount}" pattern="#,##0"/> đ</strong>
                        </div>
                        </c:if>
                    </div>
                    <div class="bill-payable">
                        <span>Khách cần trả</span>
                        <strong><fmt:formatNumber value="${bill.payableAmount}" pattern="#,##0"/> đ</strong>
                    </div>
                </section>
                <section class="pay-panel">
                    <c:if test="${not empty paymentMessage}">
                        <div class="pay-alert ${paymentMessageType eq 'success' ? 'success' : 'error'}"><c:out value="${paymentMessage}"/></div>
                    </c:if>

                    <div class="loyalty-card">
                        <div class="loyalty-head">
                            <h2>Điểm tích lũy</h2>
                            <span class="loyalty-tag">1 điểm = <fmt:formatNumber value="${loyaltySettings.redeemValuePerPoint}" pattern="#,##0"/> đ</span>
                        </div>
                        <c:choose>
                            <c:when test="${not bill.hasLoyaltyCustomer}">
                                <form action="${pageContext.request.contextPath}/payment" method="post">
                                    <input type="hidden" name="action" value="linkLoyalty">
                                    <input type="hidden" name="tableId" value="${bill.tableId}">
                                    <label for="customerKey">Tài khoản khách hàng
                                        <span class="loyalty-hint">(tên đăng nhập, email hoặc số điện thoại)</span></label>
                                    <div class="loyalty-row">
                                        <input id="customerKey" name="customerKey" maxlength="100" placeholder="Ví dụ: 0901234567" required>
                                        <button class="btn-sm btn-primary-sm" type="submit">Gắn tài khoản</button>
                                    </div>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <c:set var="maxPoints" value="${maxLoyaltyPoints}"/>
                                <div class="loyalty-customer">
                                    <div>
                                        <div class="name"><c:out value="${bill.loyaltyCustomerName}"/></div>
                                        <div class="sub">Tài khoản đã gắn vào bill</div>
                                    </div>
                                    <div class="loyalty-balance">
                                        <strong><fmt:formatNumber value="${bill.loyaltyBalance}" pattern="#,##0"/></strong>
                                        <span>điểm hiện có</span>
                                    </div>
                                </div>
                                <form action="${pageContext.request.contextPath}/payment" method="post">
                                    <input type="hidden" name="action" value="setLoyaltyPoints">
                                    <input type="hidden" name="tableId" value="${bill.tableId}">
                                    <label for="loyaltyPoints">Số điểm muốn dùng
                                        <span class="loyalty-hint">(tối đa <c:out value="${maxPoints}"/> điểm)</span></label>
                                    <div class="loyalty-row">
                                        <input id="loyaltyPoints" name="points" type="number" min="0" max="${maxPoints}" step="1" value="${bill.pointsToUse}" required>
                                        <button class="btn-sm btn-ghost" type="button" onclick="document.getElementById('loyaltyPoints').value='${maxPoints}'">Tối đa</button>
                                        <button class="btn-sm btn-primary-sm" type="submit">Áp dụng</button>
                                    </div>
                                </form>
                                <div class="loyalty-summary">
                                    <div class="minus"><span>Giảm giá bằng điểm</span><strong>- <fmt:formatNumber value="${bill.discountAmount}" pattern="#,##0"/> đ</strong></div>
                                    <div class="plus"><span>Điểm được cộng sau thanh toán</span><strong>+ <fmt:formatNumber value="${bill.pointsToEarn}" pattern="#,##0"/> điểm</strong></div>
                                </div>
                                <form action="${pageContext.request.contextPath}/payment" method="post">
                                    <input type="hidden" name="action" value="clearLoyalty">
                                    <input type="hidden" name="tableId" value="${bill.tableId}">
                                    <button class="btn-link" type="submit">Bỏ gắn tài khoản</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <h2>Chọn phương thức</h2>
                    <c:choose>
                        <c:when test="${bill.readyForPayment and bill.totalAmount gt 0}">
                            <form action="${pageContext.request.contextPath}/manage-tables" method="post" id="paymentForm">
                                <input type="hidden" name="action" value="payTable">
                                <input type="hidden" name="tableId" value="${bill.tableId}">
                                <label>Phương thức
                                    <select name="paymentMethod" id="paymentMethod" data-total="${bill.payableAmount}" required>
                                        <option value="CASH">Tiền mặt</option>
                                        <option value="QR">QR chuyển khoản</option>
                                    </select>
                                </label>
                                <div id="cashFields">
                                    <label>Tiền khách đưa
                                        <input name="cashReceived" id="cashReceived" type="number" min="${bill.payableAmount}" step="1" inputmode="numeric" required>
                                    </label>
                                    <p class="change">Tiền trả lại <strong id="cashChange">0</strong> đ</p>
                                    <input type="hidden" name="cashChange" id="cashChangeValue" value="0">
                                </div>
                                <div id="qrFields" hidden data-bank="${fn:escapeXml(paymentBankCode)}" data-account="${fn:escapeXml(paymentAccountNumber)}" data-name="${fn:escapeXml(paymentAccountName)}" data-configured="${sepayWebhookConfigured}">
                                    <c:choose><c:when test="${sepayWebhookConfigured and not empty paymentAccountNumber and not empty paymentAccountName}">
                                        <img id="paymentQrImage" alt="QR thanh toán">
                                        <p class="waiting">Đang chờ hệ thống xác nhận giao dịch...</p>
                                    </c:when><c:otherwise>
                                        <p class="warning">QR chưa được cấu hình SePay/webhook, chưa thể xác nhận tự động.</p>
                                    </c:otherwise></c:choose>
                                    <input name="paymentReference" id="paymentReference" maxlength="100" placeholder="Mã giao dịch">
                                </div>
                                <button class="submit-button" type="submit">Xác nhận thanh toán</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="warning"><c:choose><c:when test="${bill.totalAmount le 0}">Bill 0 đ; order không phát sinh thanh toán.</c:when><c:otherwise>Bàn còn <strong><c:out value="${bill.unfinishedItemCount}"/> món</strong> chưa phục vụ.</c:otherwise></c:choose></p>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </section>
    </main>
    <script>
        (function () {
            var form = document.getElementById('paymentForm');
            if (!form) return;
            var method = document.getElementById('paymentMethod');
            var cashFields = document.getElementById('cashFields');
            var qrFields = document.getElementById('qrFields');
            var received = document.getElementById('cashReceived');
            var total = Number(method.dataset.total || 0);
            var timer = null;
            function refresh() {
                var cash = method.value === 'CASH';
                cashFields.hidden = !cash; qrFields.hidden = cash; received.required = cash;
                if (cash) {
                    var change = Math.max(0, Number(received.value || 0) - total);
                    document.getElementById('cashChange').textContent = change.toLocaleString('vi-VN');
                    document.getElementById('cashChangeValue').value = change;
                }
            }
            function stopPolling() { if (timer) { clearInterval(timer); timer = null; } }
            function poll() {
                stopPolling();
                var reference = document.getElementById('paymentReference').value;
                function check() {
                    fetch('${pageContext.request.contextPath}/payment-status?tableId=${bill.tableId}&reference=' + encodeURIComponent(reference) + '&_=' + Date.now(), {credentials:'same-origin', cache:'no-store'})
                        .then(function (r) { if (!r.ok) throw new Error('payment status failed'); return r.json(); })
                        .then(function (s) { if (s.paid) { stopPolling(); window.location.href = '${pageContext.request.contextPath}/service-history'; } });
                }
                check(); timer = setInterval(check, 2000);
            }
            received.addEventListener('input', refresh);
            method.addEventListener('change', function () {
                stopPolling();
                if (method.value === 'QR') {
                    if (qrFields.dataset.configured !== 'true' || !qrFields.dataset.account || !qrFields.dataset.name) {
                        method.value = 'CASH'; alert('QR SePay chưa được cấu hình đầy đủ.'); refresh(); return;
                    }
                    var reference = 'RMTABLE${bill.tableId}' + Date.now();
                    document.getElementById('paymentReference').value = reference;
                    document.getElementById('paymentQrImage').src = 'https://img.vietqr.io/image/' + encodeURIComponent(qrFields.dataset.bank) + '-' + encodeURIComponent(qrFields.dataset.account) + '-compact2.png?amount=' + Math.round(total) + '&addInfo=' + encodeURIComponent(reference) + '&accountName=' + encodeURIComponent(qrFields.dataset.name);
                    poll();
                }
                refresh();
            });
            form.addEventListener('submit', function (event) {
                if (method.value === 'CASH' && Number(received.value || 0) < total) { event.preventDefault(); alert('Tiền khách đưa chưa đủ.'); }
                if (method.value === 'QR') { event.preventDefault(); alert('Thanh toán QR sẽ tự động hoàn tất sau khi hệ thống nhận webhook.'); }
            });
            refresh();
        }());
    </script>
</body>
</html>
