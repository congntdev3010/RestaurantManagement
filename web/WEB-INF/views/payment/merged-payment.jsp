<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán bill gộp</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment-checkout.css">
</head>
<body class="payment-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="payment-shell">
        <a class="back-link" href="${pageContext.request.contextPath}/merged-payment?id=${mergedId}&action=leave">← Về danh sách thanh toán</a>
        <section class="payment-card">
            <header class="payment-header">
                <div>
                    <p class="eyebrow">HOÀN TẤT PHỤC VỤ</p>
                    <h1>Thanh toán bill gộp</h1>
                    <p class="payment-subtitle"><c:out value="${bill.tableName}"/> · <c:out value="${bill.orderCount}"/> order</p>
                </div>
            </header>

            <div class="payment-grid">
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
                    <p class="muted">Các bàn: <strong><c:out value="${bill.tableName}"/></strong></p>
                    <c:if test="${not empty bill.checkedInAt}">
                        <p class="muted">Mở bàn: <c:out value="${bill.checkedInAt}"/></p>
                    </c:if>
                    <table>
                        <thead><tr><th>Món</th><th>SL</th><th>Thành tiền</th></tr></thead>
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
                        <c:if test="${bill.voucherDiscount gt 0}">
                            <div class="bill-sum-row discount">
                                <span>Giảm giá voucher (<fmt:formatNumber value="${bill.voucherPercent}" maxFractionDigits="2"/>%)</span>
                                <strong>- <fmt:formatNumber value="${bill.voucherDiscount}" pattern="#,##0"/> đ</strong>
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
                    <c:if test="${bill.pendingOrder}">
                        <p class="warning">Có order mới chờ duyệt. Order này sẽ bị hủy khi thanh toán.</p>
                    </c:if>
                    <c:if test="${bill.unfinishedItemCount gt 0}">
                        <p class="warning">Còn món chưa phục vụ; chưa thể thanh toán.</p>
                    </c:if>
                    <div class="loyalty-card">
                        <div class="loyalty-head"><h2>Voucher giảm giá</h2><span class="loyalty-tag">Mã 8 ký tự</span></div>
                        <c:choose>
                            <c:when test="${bill.hasVoucher}">
                                <div class="loyalty-customer"><div><div class="name">Mã <c:out value="${bill.voucherCode}"/></div>
                                    <div class="sub">Giảm <fmt:formatNumber value="${bill.voucherPercent}" maxFractionDigits="2"/>% tổng hóa đơn</div></div>
                                    <div class="loyalty-balance"><strong>- <fmt:formatNumber value="${bill.voucherDiscount}" pattern="#,##0"/> đ</strong></div></div>
                                <form action="${pageContext.request.contextPath}/merged-payment" method="post">
                                    <input type="hidden" name="action" value="clearVoucher"><input type="hidden" name="id" value="${mergedId}">
                                    <button class="btn-link" type="submit">Bỏ mã voucher</button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <form action="${pageContext.request.contextPath}/merged-payment" method="post">
                                    <input type="hidden" name="action" value="applyVoucher"><input type="hidden" name="id" value="${mergedId}">
                                    <label for="voucherCode">Nhập mã voucher <span class="loyalty-hint">(8 ký tự, khách nhận qua Gmail)</span></label>
                                    <div class="loyalty-row"><input id="voucherCode" name="voucherCode" type="text" maxlength="8" pattern="[A-Za-z0-9]{8}" autocomplete="off" placeholder="Ví dụ: K7M2QX9P" required style="text-transform: uppercase">
                                        <button class="btn-sm btn-primary-sm" type="submit">Áp dụng</button></div>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="loyalty-card">
                        <div class="loyalty-head"><h2>Điểm tích lũy</h2>
                            <span class="loyalty-tag">1 điểm = <fmt:formatNumber value="${loyaltySettings.redeemValuePerPoint}" pattern="#,##0"/> đ</span>
                        </div>
                        <c:choose>
                            <c:when test="${not bill.hasLoyaltyCustomer}">
                                <form action="${pageContext.request.contextPath}/merged-payment" method="post">
                                    <input type="hidden" name="action" value="linkLoyalty"><input type="hidden" name="id" value="${mergedId}">
                                    <label for="customerKey">Tài khoản khách hàng</label>
                                    <div class="loyalty-row"><input id="customerKey" name="customerKey" maxlength="30" required>
                                        <button class="btn-sm btn-primary-sm" type="submit">Gắn tài khoản</button></div>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <div class="loyalty-customer"><div><div class="name"><c:out value="${bill.loyaltyCustomerName}"/></div>
                                    <div class="sub">Thành viên
                                        <c:if test="${not empty bill.loyaltyCustomerPhone}"> · <c:out value="${bill.loyaltyCustomerPhone}"/></c:if>
                                        <c:if test="${not empty bill.loyaltyCustomerEmail}"> · <c:out value="${bill.loyaltyCustomerEmail}"/></c:if>
                                    </div></div>
                                    <div class="loyalty-balance"><strong><fmt:formatNumber value="${bill.loyaltyBalance}" pattern="#,##0"/></strong><span>điểm hiện có</span></div></div>
                                <form action="${pageContext.request.contextPath}/merged-payment" method="post">
                                    <input type="hidden" name="action" value="setLoyaltyPoints"><input type="hidden" name="id" value="${mergedId}">
                                    <label for="loyaltyPoints">Số điểm muốn dùng <span class="loyalty-hint">(tối đa <c:out value="${maxLoyaltyPoints}"/> điểm)</span></label>
                                    <div class="loyalty-row"><input id="loyaltyPoints" name="points" type="number" min="0" max="${maxLoyaltyPoints}" value="${bill.pointsToUse}" required>
                                        <button class="btn-sm btn-ghost" type="button" onclick="document.getElementById('loyaltyPoints').value='${maxLoyaltyPoints}'">Tối đa</button>
                                        <button class="btn-sm btn-primary-sm" type="submit">Áp dụng</button></div>
                                </form>
                                <div class="loyalty-summary">
                                    <div class="minus"><span>Giảm giá bằng điểm</span>
                                        <strong>- <fmt:formatNumber value="${bill.discountAmount}" pattern="#,##0"/> đ</strong>
                                    </div>
                                    <div class="plus"><span>Điểm được cộng sau thanh toán</span>
                                        <strong>+ <fmt:formatNumber value="${bill.pointsToEarn}" pattern="#,##0"/> điểm</strong>
                                    </div>
                                </div>
                                <form action="${pageContext.request.contextPath}/merged-payment" method="post">
                                    <input type="hidden" name="action" value="clearLoyalty"><input type="hidden" name="id" value="${mergedId}">
                                    <button class="btn-link" type="submit">Bỏ gắn tài khoản</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <h2>Chọn phương thức</h2>
                    <c:choose>
                        <c:when test="${bill.readyForPayment}">
                            <form action="${pageContext.request.contextPath}/merged-payment" method="post" id="paymentForm">
                                <input type="hidden" name="action" value="pay">
                                <input type="hidden" name="id" value="${mergedId}">
                                <input type="hidden" name="expectedAmount" value="${bill.payableAmount}">
                                <label for="paymentMethod">Phương thức
                                    <select name="paymentMethod" id="paymentMethod" data-total="${bill.payableAmount}" required>
                                        <option value="CASH">Tiền mặt</option>
                                        <c:if test="${bill.payableAmount ge 2000}">
                                            <option value="QR">QR chuyển khoản</option>
                                        </c:if>
                                    </select>
                                </label>
                                <div id="cashFields">
                                    <label for="cashReceived">Tiền khách đưa
                                        <input id="cashReceived" name="cashReceived" type="number" min="${bill.payableAmount}" step="1" required>
                                    </label>
                                    <p class="change">Tiền trả lại <strong id="cashChange">0</strong></p>
                                </div>
                                <div id="qrFields" hidden data-bank="${fn:escapeXml(paymentBankCode)}"
                                     data-account="${fn:escapeXml(paymentAccountNumber)}"
                                     data-name="${fn:escapeXml(paymentAccountName)}"
                                     data-configured="${sepayWebhookConfigured}">
                                    <img id="paymentQrImage" alt="QR chuyển khoản">
                                    <p>Nội dung chuyển khoản: <strong id="paymentReferenceText"></strong></p>
                                    <p class="waiting">Chờ hệ thống xác nhận; không chuyển tiền lần nữa nếu đã trừ tiền.</p>
                                </div>
                                <button id="submitButton" class="submit-button" type="submit">Xác nhận thanh toán</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <p class="warning">Bill chưa sẵn sàng thanh toán.</p>
                        </c:otherwise>
                    </c:choose>
                    <form action="${pageContext.request.contextPath}/merged-payment" method="post"
                          onsubmit="return confirm('Hủy gộp bill và quay lại thanh toán từng bàn?')">
                        <input type="hidden" name="action" value="cancel">
                        <input type="hidden" name="id" value="${mergedId}">
                        <button class="btn-link" type="submit">Hủy gộp bill</button>
                    </form>
                </section>
            </div>
        </section>
    </main>
    <script>
        (function () {
            var leaveUrl = '${pageContext.request.contextPath}/merged-payment?id=${mergedId}&action=leave';
            document.querySelectorAll('a[href*="/manage-tables"], a[href*="/payment"]').forEach(function (link) {
                link.addEventListener('click', function (event) {
                    if (!confirm('Rời màn thanh toán sẽ bỏ voucher và tích điểm đang gắn. Bạn có muốn tiếp tục?')) {
                        event.preventDefault();
                        return;
                    }
                    event.preventDefault();
                    window.location.href = leaveUrl;
                });
            });
            var form = document.getElementById('paymentForm');
            if (!form) return;
            var method = document.getElementById('paymentMethod');
            var cash = document.getElementById('cashFields');
            var qr = document.getElementById('qrFields');
            var received = document.getElementById('cashReceived');
            var amount = Number(method.dataset.total);
            var timer = null;
            function stopPolling() {
                if (timer) {
                    clearInterval(timer);
                    timer = null;
                }
            }
            function refresh() {
                var isCash = method.value === 'CASH';
                cash.hidden = !isCash;
                qr.hidden = isCash;
                received.required = isCash;
                document.getElementById('submitButton').hidden = !isCash;
                document.getElementById('cashChange').textContent =
                    Math.max(0, Number(received.value || 0) - amount).toLocaleString('vi-VN') + ' đ';
            }
            received.addEventListener('input', refresh);
            method.addEventListener('change', function () {
                stopPolling();
                if (method.value === 'QR') {
                    if (qr.dataset.configured !== 'true' || !qr.dataset.account || !qr.dataset.name) {
                        method.value = 'CASH';
                        alert('QR chưa được cấu hình.');
                        refresh();
                        return;
                    }
                    var reference = '${qrReference}';
                    if (!reference) {
                        method.value = 'CASH';
                        alert('Bill chưa sẵn sàng tạo QR.');
                        refresh();
                        return;
                    }
                    document.getElementById('paymentReferenceText').textContent = reference;
                    document.getElementById('paymentQrImage').src =
                        'https://img.vietqr.io/image/' + encodeURIComponent(qr.dataset.bank) + '-'
                        + encodeURIComponent(qr.dataset.account) + '-compact2.png?amount='
                        + Math.round(amount) + '&addInfo=' + encodeURIComponent(reference)
                        + '&accountName=' + encodeURIComponent(qr.dataset.name);
                    function poll() {
                        fetch('${pageContext.request.contextPath}/payment-status?mergedId=${mergedId}&reference='
                                + encodeURIComponent(reference), {credentials: 'same-origin', cache: 'no-store'})
                            .then(function (response) {
                                if (!response.ok) throw new Error('status');
                                return response.json();
                            })
                            .then(function (status) {
                                if (status.paid) {
                                    stopPolling();
                                    window.location.href = '${pageContext.request.contextPath}/service-history?id=' + status.historyId;
                                } else if (status.review) {
                                    stopPolling();
                                    alert('Đã nhận chuyển khoản nhưng cần đối soát. Không chuyển lại tiền.');
                                } else if (status.payable !== null && Number(status.payable) !== amount) {
                                    stopPolling();
                                    alert('Bill đã thay đổi. Vui lòng tải lại màn hình.');
                                    window.location.reload();
                                }
                            }).catch(function () {});
                    }
                    poll();
                    timer = setInterval(poll, 2000);
                }
                refresh();
            });
            refresh();
        }());
    </script>
</body>
</html>
