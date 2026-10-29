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
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment-checkout.css">
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
                    <!-- ===== Cột trái: chi tiết bill ===== -->
                    <section class="bill-panel">
                        <h2>Chi tiết bill</h2>
                        <p class="customer">
                            <c:if test="${not empty bill.customerName}">
                                <strong><c:out value="${bill.customerName}"/></strong>
                                <c:if test="${not empty bill.customerPhone}"> · <c:out value="${bill.customerPhone}"/></c:if>
                                <c:if test="${not empty bill.customerEmail}"> · <c:out value="${bill.customerEmail}"/></c:if>
                            </c:if>
                            <span class="member-tag ${bill.hasLoyaltyCustomer ? 'is-member' : ''}">
                                <c:out value="${bill.hasLoyaltyCustomer ? 'Thành viên' : 'Vãng lai'}"/>
                            </span>
                        </p>
                        <c:if test="${not empty bill.checkedInAt}">
                            <p class="muted">Mở bàn: <c:out value="${bill.checkedInAt}"/></p>
                        </c:if>

                        <table>
                            <thead>
                                <tr><th>Món</th><th>SL</th><th>Thành tiền</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="line" items="${bill.items}">
                                    <tr>
                                        <td><c:out value="${line.dishName}"/></td>
                                        <td><c:out value="${line.quantity}"/></td>
                                        <td><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/> đ</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
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

                    <!-- ===== Cột phải: điểm tích lũy và phương thức thanh toán ===== -->
                    <section class="pay-panel">
                        <c:if test="${not empty paymentMessage}">
                            <div class="pay-alert ${paymentMessageType eq 'success' ? 'success' : 'error'}">
                                <c:out value="${paymentMessage}"/>
                            </div>
                        </c:if>

                        <div class="loyalty-card">
                            <div class="loyalty-head">
                                <h2>Điểm tích lũy</h2>
                                <span class="loyalty-tag">
                                    1 điểm = <fmt:formatNumber value="${loyaltySettings.redeemValuePerPoint}" pattern="#,##0"/> đ
                                </span>
                            </div>

                            <c:choose>
                                <c:when test="${not bill.hasLoyaltyCustomer}">
                                    <form action="${pageContext.request.contextPath}/payment" method="post">
                                        <input type="hidden" name="action" value="linkLoyalty">
                                        <input type="hidden" name="tableId" value="${bill.tableId}">
                                        <label for="customerKey">
                                            Tài khoản khách hàng
                                            <span class="loyalty-hint">(số điện thoại hoặc Gmail)</span>
                                        </label>
                                        <div class="loyalty-row">
                                            <input id="customerKey" name="customerKey" type="text" maxlength="30"
                                                   placeholder="Ví dụ: 0901234567" required>
                                            <button class="btn-sm btn-primary-sm" type="submit">Gắn tài khoản</button>
                                        </div>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <c:set var="maxPoints" value="${maxLoyaltyPoints}"/>
                                    <div class="loyalty-customer">
                                        <div>
                                            <div class="name"><c:out value="${bill.loyaltyCustomerName}"/></div>
                                            <div class="sub">
                                                Thành viên
                                                <c:if test="${not empty bill.loyaltyCustomerPhone}"> · <c:out value="${bill.loyaltyCustomerPhone}"/></c:if>
                                                <c:if test="${not empty bill.loyaltyCustomerEmail}"> · <c:out value="${bill.loyaltyCustomerEmail}"/></c:if>
                                                </div>
                                            </div>
                                            <div class="loyalty-balance">
                                                <strong><fmt:formatNumber value="${bill.loyaltyBalance}" pattern="#,##0"/></strong>
                                            <span>điểm hiện có</span>
                                        </div>
                                    </div>

                                    <form action="${pageContext.request.contextPath}/payment" method="post">
                                        <input type="hidden" name="action" value="setLoyaltyPoints">
                                        <input type="hidden" name="tableId" value="${bill.tableId}">
                                        <label for="loyaltyPoints">
                                            Số điểm muốn dùng
                                            <span class="loyalty-hint">(tối đa <c:out value="${maxPoints}"/> điểm)</span>
                                        </label>
                                        <div class="loyalty-row">
                                            <input id="loyaltyPoints" name="points" type="number" min="0"
                                                   max="${maxPoints}" step="1" value="${bill.pointsToUse}" required>
                                            <button class="btn-sm btn-ghost" type="button"
                                                    onclick="document.getElementById('loyaltyPoints').value = '${maxPoints}'">Tối đa</button>
                                            <button class="btn-sm btn-primary-sm" type="submit">Áp dụng</button>
                                        </div>
                                    </form>

                                    <div class="loyalty-summary">
                                        <div class="minus">
                                            <span>Giảm giá bằng điểm</span>
                                            <strong>- <fmt:formatNumber value="${bill.discountAmount}" pattern="#,##0"/> đ</strong>
                                        </div>
                                        <div class="plus">
                                            <span>Điểm được cộng sau thanh toán</span>
                                            <strong>+ <fmt:formatNumber value="${bill.pointsToEarn}" pattern="#,##0"/> điểm</strong>
                                        </div>
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
                                            <input name="cashReceived" id="cashReceived" type="number"
                                                   min="${bill.payableAmount}" step="1" inputmode="numeric" required>
                                        </label>
                                        <p class="change">Tiền trả lại <strong id="cashChange">0</strong> đ</p>
                                        <input type="hidden" name="cashChange" id="cashChangeValue" value="0">
                                    </div>

                                    <div id="qrFields" hidden
                                         data-bank="${fn:escapeXml(paymentBankCode)}"
                                         data-account="${fn:escapeXml(paymentAccountNumber)}"
                                         data-name="${fn:escapeXml(paymentAccountName)}"
                                         data-configured="${sepayWebhookConfigured}">
                                        <c:choose>
                                            <c:when test="${sepayWebhookConfigured and not empty paymentAccountNumber and not empty paymentAccountName}">
                                                <img id="paymentQrImage" alt="QR thanh toán">
                                                <p class="waiting">Đang chờ hệ thống xác nhận giao dịch...</p>
                                            </c:when>
                                            <c:otherwise>
                                                <p class="warning">QR chưa được cấu hình SePay/webhook, chưa thể xác nhận tự động.</p>
                                            </c:otherwise>
                                        </c:choose>
                                        <input name="paymentReference" id="paymentReference" maxlength="100" placeholder="Mã giao dịch">
                                    </div>

                                    <button class="submit-button" type="submit">Xác nhận thanh toán</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <p class="warning">
                                    <c:choose>
                                        <c:when test="${bill.totalAmount le 0}">Bill 0 đ; order không phát sinh thanh toán.</c:when>
                                        <c:otherwise>Bàn còn <strong><c:out value="${bill.unfinishedItemCount}"/> món</strong> chưa phục vụ.</c:otherwise>
                                    </c:choose>
                                </p>
                            </c:otherwise>
                        </c:choose>
                    </section>
                </div>
            </section>
        </main>

        <script>
            (function () {
                var form = document.getElementById('paymentForm');
                if (!form)
                    return;
                var method = document.getElementById('paymentMethod');
                var cashFields = document.getElementById('cashFields');
                var qrFields = document.getElementById('qrFields');
                var received = document.getElementById('cashReceived');
                var total = Number(method.dataset.total || 0);
                var timer = null;
                function refresh() {
                    var cash = method.value === 'CASH';
                    cashFields.hidden = !cash;
                    qrFields.hidden = cash;
                    received.required = cash;
                    if (cash) {
                        var change = Math.max(0, Number(received.value || 0) - total);
                        document.getElementById('cashChange').textContent = change.toLocaleString('vi-VN');
                        document.getElementById('cashChangeValue').value = change;
                    }
                }
                function stopPolling() {
                    if (timer) {
                        clearInterval(timer);
                        timer = null;
                    }
                }
                function poll() {
                    stopPolling();
                    var reference = document.getElementById('paymentReference').value;
                    function check() {
                        fetch('${pageContext.request.contextPath}/payment-status?tableId=${bill.tableId}&reference=' +
                                encodeURIComponent(reference) + '&_=' + Date.now(),
                                {credentials: 'same-origin', cache: 'no-store'})
                                .then(function (r) {
                                    if (!r.ok)
                                        throw new Error('payment status failed');
                                    return r.json();
                                })
                                .then(function (s) {
                                    if (s.paid) {
                                        stopPolling();
                                        window.location.href = '${pageContext.request.contextPath}/service-history';
                                    }
                                });
                    }
                    check();
                    timer = setInterval(check, 2000);
                }
                received.addEventListener('input', refresh);
                method.addEventListener('change', function () {
                    stopPolling();
                    if (method.value === 'QR') {
                        if (qrFields.dataset.configured !== 'true' || !qrFields.dataset.account || !qrFields.dataset.name) {
                            method.value = 'CASH';
                            alert('QR SePay chưa được cấu hình đầy đủ.');
                            refresh();
                            return;
                        }
                        var reference = 'RMTABLE${bill.tableId}' + Date.now();
                        document.getElementById('paymentReference').value = reference;
                        document.getElementById('paymentQrImage').src =
                                'https://img.vietqr.io/image/' + encodeURIComponent(qrFields.dataset.bank) + '-' + encodeURIComponent(qrFields.dataset.account)
                                + '-compact2.png?amount=' + Math.round(total) + '&addInfo=' + encodeURIComponent(reference)
                                + '&accountName=' + encodeURIComponent(qrFields.dataset.name);
                        poll();
                    }
                    refresh();
                });
                form.addEventListener('submit', function (event) {
                    if (method.value === 'CASH' && Number(received.value || 0) < total) {
                        event.preventDefault();
                        alert('Tiền khách đưa chưa đủ.');
                    }
                    if (method.value === 'QR') {
                        event.preventDefault();
                        alert('Thanh toán QR sẽ tự động hoàn tất sau khi hệ thống nhận webhook.');
                    }
                });
                refresh();
            }());
        </script>
    </body>
</html>
