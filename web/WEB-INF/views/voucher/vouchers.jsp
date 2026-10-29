<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gửi voucher giảm giá</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.1">
    <style>
        .vc-wrap { max-width: 900px; margin: 0 auto; }
        .vc-head { margin-bottom: 22px; }
        .vc-head .eyebrow { margin: 0 0 6px; }
        .vc-head h1 { margin: 0 0 6px; font-size: 1.9rem; color: #3f1a1f; }
        .vc-head p { margin: 0; color: #7a6a64; }

        .vc-alert { margin: 0 0 18px; padding: 12px 16px; border-radius: 12px; font-size: .94rem; white-space: pre-line; }
        .vc-alert.success { background: #e6f4ea; color: #1e6b38; border: 1px solid #c5e5cf; }
        .vc-alert.error { background: #fdeaea; color: #9b2227; border: 1px solid #f3c6c8; }

        .vc-card { background: #fff; border: 1px solid #ecdfd5; border-radius: 18px; padding: 22px 24px; margin-bottom: 18px; box-shadow: 0 6px 18px rgba(74, 22, 32, .05); }
        .vc-card h2 { margin: 0 0 14px; font-size: 1.15rem; color: #4a1620; }
        .vc-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; align-items: end; }
        .vc-grid label { display: block; font-size: .85rem; font-weight: 600; color: #4d3b35; margin-bottom: 6px; }
        .vc-grid input[type=text], .vc-grid input[type=number] { width: 100%; box-sizing: border-box; border: 1px solid #e3d3c8; border-radius: 12px; background: #fffaf6; padding: 12px 14px; font: inherit; }
        .vc-hint { display: block; font-weight: 400; color: #8a7a73; font-size: .8rem; margin-top: 4px; }
        .vc-btn { border: 0; border-radius: 12px; background: #7b1f28; color: #fff; padding: 12px 22px; font: inherit; font-weight: 700; cursor: pointer; }
        .vc-btn:hover { background: #63171f; }

        .vc-summary { margin: 0 0 12px; color: #4d3b35; }
        .vc-selectall { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 10px; font-size: .9rem; }
        .vc-table { width: 100%; border-collapse: collapse; font-size: .92rem; }
        .vc-table th, .vc-table td { padding: 10px 8px; border-bottom: 1px solid #f0e6df; text-align: left; }
        .vc-table th { color: #8a7a73; font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .05em; }
        .vc-table .num { text-align: right; white-space: nowrap; }
        .vc-empty { color: #8a7a73; margin: 0; }
        .vc-send-row { display: flex; justify-content: flex-end; margin-top: 16px; }
    </style>
</head>
<body class="payment-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="payment-shell">
        <div class="vc-wrap">
            <header class="vc-head">
                <p class="eyebrow">QUẢN LÝ</p>
                <h1>Gửi voucher giảm giá</h1>
                <p>Cấu hình % giảm và mức chi tiêu tối thiểu, chọn khách đủ điều kiện, rồi gửi mã 8 ký tự qua Gmail của khách.</p>
            </header>

            <c:if test="${not empty voucherMessage}">
                <div class="vc-alert ${voucherMessageType eq 'success' ? 'success' : 'error'}"><c:out value="${voucherMessage}"/></div>
            </c:if>

            <section class="vc-card">
                <h2>1. Cấu hình voucher</h2>
                <form method="get" action="${pageContext.request.contextPath}/vouchers">
                    <div class="vc-grid">
                        <div>
                            <label for="percent">Giảm (% tổng hóa đơn)
                                <span class="vc-hint">Từ 0,01 đến 100, tối đa 2 chữ số thập phân</span>
                            </label>
                            <input id="percent" name="percent" type="text" inputmode="decimal" maxlength="7" required
                                   placeholder="Ví dụ: 10"
                                   value="${empty previewPercent ? '' : previewPercent.toPlainString()}">
                        </div>
                        <div>
                            <label for="minSpent">Khách phải đã chi trên (đ)
                                <span class="vc-hint">Tổng tiền khách đã thanh toán, lớn hơn mức này mới nhận. Từ 0 đến 1.000.000.000</span>
                            </label>
                            <input id="minSpent" name="minSpent" type="number" min="0" max="1000000000" step="1" required
                                   placeholder="Ví dụ: 500000"
                                   value="${empty previewMinSpent ? '' : previewMinSpent.toPlainString()}">
                        </div>
                        <div>
                            <button class="vc-btn" type="submit">Tìm khách đủ điều kiện</button>
                        </div>
                    </div>
                </form>
            </section>

            <c:if test="${not empty recipients}">
                <section class="vc-card">
                    <h2>2. Chọn khách và gửi</h2>
                    <form method="post" action="${pageContext.request.contextPath}/vouchers" id="sendForm"
                          onsubmit="return confirmSend(this);">
                        <input type="hidden" name="percent" value="${previewPercent.toPlainString()}">
                        <input type="hidden" name="minSpent" value="${previewMinSpent.toPlainString()}">
                        <p class="vc-summary">
                            Giảm <strong><c:out value="${previewPercent.stripTrailingZeros().toPlainString()}"/>%</strong>
                            cho khách đã chi trên <strong><fmt:formatNumber value="${previewMinSpent}" pattern="#,##0"/> đ</strong>
                            · <strong>${recipients.size()}</strong> khách đủ điều kiện
                        </p>
                        <label class="vc-selectall">
                            <input type="checkbox" id="selectAll" checked> Chọn tất cả
                        </label>
                        <table class="vc-table">
                            <thead>
                                <tr>
                                    <th></th>
                                    <th>Khách hàng</th>
                                    <th>Gmail</th>
                                    <th class="num">Tổng đã chi</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="r" items="${recipients}">
                                    <tr>
                                        <td><input class="rcpt" type="checkbox" name="userIds" value="${r.userId}" checked></td>
                                        <td><c:out value="${r.fullName}"/></td>
                                        <td><c:out value="${r.email}"/></td>
                                        <td class="num"><fmt:formatNumber value="${r.totalSpent}" pattern="#,##0"/> đ</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        <div class="vc-send-row">
                            <button class="vc-btn" type="submit">Tạo mã và gửi email</button>
                        </div>
                    </form>
                </section>
            </c:if>

            <c:if test="${recipients != null and empty recipients}">
                <section class="vc-card">
                    <h2>2. Chọn khách và gửi</h2>
                    <p class="vc-empty">Không có khách nào đã chi trên
                        <fmt:formatNumber value="${previewMinSpent}" pattern="#,##0"/> đ. Thử giảm mức chi tiêu tối thiểu.</p>
                </section>
            </c:if>
        </div>
    </main>
    <script>
        (function () {
            var all = document.getElementById('selectAll');
            if (all) {
                all.addEventListener('change', function () {
                    document.querySelectorAll('.rcpt').forEach(function (cb) { cb.checked = all.checked; });
                });
            }
        })();

        function confirmSend(form) {
            var n = form.querySelectorAll('.rcpt:checked').length;
            if (n === 0) {
                alert('Chưa chọn khách nào.');
                return false;
            }
            return confirm('Tạo ' + n + ' mã voucher và gửi email cho các khách đã chọn?');
        }
    </script>
</body>
</html>
