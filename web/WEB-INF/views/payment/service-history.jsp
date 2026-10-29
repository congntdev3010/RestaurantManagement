<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch sử thanh toán - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/service-history.css?v=1.1">
    <style>
        /* Danh sách lịch sử */
        .sh-filter { display: flex; flex-wrap: wrap; align-items: end; gap: 12px; margin: 22px 0 14px; }
        .sh-filter label { display: flex; flex-direction: column; font-size: .8rem; color: #6b5d5d; gap: 4px; }
        .sh-filter input[type=date] { padding: 8px 10px; border: 1px solid #e5d3c8; border-radius: 8px; }
        .sh-filter button, .sh-filter a { padding: 9px 16px; border-radius: 8px; border: 1px solid #7c2028; background: #7c2028; color: #fff; text-decoration: none; cursor: pointer; font: inherit; }
        .sh-filter a { background: transparent; color: #7c2028; }
        .sh-summary { display: flex; gap: 14px; flex-wrap: wrap; margin-bottom: 14px; }
        .sh-summary div { background: #fbf3ee; border: 1px solid #efdfd5; border-radius: 12px; padding: 12px 16px; }
        .sh-summary span { display: block; font-size: .75rem; color: #8a6f66; text-transform: uppercase; letter-spacing: .05em; }
        .sh-summary strong { font-size: 1.1rem; color: #5a1b22; }
        .sh-table-wrap { overflow-x: auto; border: 1px solid #efe4dc; border-radius: 14px; background: #fff; }
        .sh-table { width: 100%; border-collapse: collapse; }
        .sh-table th { text-align: left; padding: 11px 14px; font-size: .72rem; letter-spacing: .06em; text-transform: uppercase; color: #8a7a73; background: #faf5f1; border-bottom: 1px solid #efe4dc; white-space: nowrap; }
        .sh-table td { padding: 11px 14px; border-bottom: 1px solid #f3ebe5; white-space: nowrap; }
        .sh-table tr:last-child td { border-bottom: 0; }
        .sh-table .num { text-align: right; font-variant-numeric: tabular-nums; }
        .sh-table tr.is-selected { background: #fff4e6; }
        .sh-empty { text-align: center; padding: 30px; color: #8a7a73; background: #fffaf6; border: 1px dashed #e5d3c8; border-radius: 14px; }
        .sh-message { padding: 12px 16px; border-radius: 10px; background: #fdeaea; color: #a8242b; }
        .sh-back { display: inline-block; margin-bottom: 12px; color: #7c2028; }
        .receipt-card .receipt-line span { text-align: left; }
        .sh-page-head { margin: 8px 0 22px; }
        .sh-page-head h1 { margin: 0; }
        .sh-intro { color: #71666a; font-size: 14px; margin: 8px 0 0; }

        @media print {
            /* header.css gắn padding-left 248px cho body có sidebar; selector dưới đây thắng để bỏ lệch. */
            body.history-page.has-staff-sidebar { padding: 0 !important; margin: 0 !important; }
            .site-header, .staff-sidebar, .no-print { display: none !important; }
            .receipt-shell { padding: 0 !important; margin: 0 auto !important; max-width: 100% !important; width: 100% !important; }
            .receipt-card { box-shadow: none !important; border: 0 !important; margin: 0 !important; padding: 0 !important; background: #fff !important; }
            .receipt-card .success-mark, .receipt-card .eyebrow, .receipt-card h1, .receipt-card .receipt-total { display: none !important; }
            .receipt-subtitle { font-size: 1.1rem; text-align: center; margin: 0 0 14px; }
            .receipt-card, .receipt-lines, .receipt-info { page-break-inside: avoid; }
        }
    </style>
</head>
<body class="history-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="receipt-shell">
        <header class="sh-page-head no-print">
            <p class="eyebrow">QUẦY THANH TOÁN</p>
            <h1>Lịch sử thanh toán</h1>
            <p class="sh-intro"><c:choose><c:when test="${isManager}">Tra cứu và in lại hóa đơn các bàn đã thanh toán.</c:when><c:otherwise>Hóa đơn các bàn đã thanh toán hôm nay. Bấm Xem / In để in lại.</c:otherwise></c:choose></p>
        </header>

        <c:if test="${not empty detailMessage}">
            <p class="sh-message" role="status"><c:out value="${detailMessage}"/></p>
        </c:if>

        <%-- ===== Hóa đơn chi tiết (xem lại / in) ===== --%>
        <c:if test="${not empty selected}">
            <a class="sh-back no-print" href="${pageContext.request.contextPath}/service-history?from=${from}&to=${to}">&larr; Danh sách thanh toán</a>
            <section class="receipt-card">
                <div class="success-mark">✓</div>
                <p class="eyebrow">HÓA ĐƠN</p>
                <h1>Hóa đơn #<c:out value="${selected.historyId}"/></h1>
                <p class="receipt-subtitle">Bill của bàn <strong><c:out value="${selected.tableName}"/></strong> · <c:out value="${selected.paidAtText}"/></p>
                <div class="receipt-total">
                    <span>Khách đã trả</span>
                    <strong><fmt:formatNumber value="${selected.payableAmount}" pattern="#,##0"/> đ</strong>
                </div>
                <div class="receipt-info">
                    <div><span>Bàn</span><strong><c:out value="${selected.tableName}"/></strong></div>
                    <div><span>Phương thức</span><strong><c:out value="${selected.paymentMethod eq 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR'}"/></strong></div>
                    <c:if test="${not empty selected.customerName}"><div><span>Khách hàng</span><strong><c:out value="${selected.customerName}"/></strong></div></c:if><div><span>Loại khách</span><strong><c:out value="${selected.memberTypeLabel}"/></strong></div><c:if test="${not empty selected.customerPhone}"><div><span>Số điện thoại</span><strong><c:out value="${isManager ? selected.customerPhone : selected.maskedPhone}"/></strong></div></c:if><c:if test="${not empty selected.customerEmail}"><div><span>Gmail</span><strong><c:out value="${isManager ? selected.customerEmail : selected.maskedEmail}"/></strong></div></c:if>
                    <div><span>Thu ngân</span><strong><c:out value="${selected.paidByName}"/></strong></div>
                    <c:if test="${selected.paymentMethod eq 'CASH' and not empty selected.cashReceived}"><div><span>Số tiền đã đưa</span><strong><fmt:formatNumber value="${selected.cashReceived}" pattern="#,##0"/> đ</strong></div></c:if>
                    <c:if test="${not empty selected.cashChange}"><div><span>Tiền thừa</span><strong><fmt:formatNumber value="${selected.cashChange}" pattern="#,##0"/> đ</strong></div></c:if>
                </div>
                <div class="receipt-lines">
                    <div class="lines-heading"><h2>Các món đã phục vụ</h2><span><c:out value="${fn:length(selected.items)}"/> món</span></div>
                    <c:forEach var="item" items="${selected.items}">
                        <div class="receipt-line"><span><strong><c:out value="${item.dishName}"/></strong><small><c:out value="${item.quantity}"/> × <fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/> đ</small></span><strong><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0"/> đ</strong></div>
                    </c:forEach>
                    <div class="receipt-total-bottom">
                        <span>Tổng bill (trước giảm)</span>
                        <strong><fmt:formatNumber value="${selected.totalAmount}" pattern="#,##0"/> đ</strong>
                    </div>
                    <c:if test="${selected.discountAmount gt 0}">
                        <div class="receipt-total-bottom">
                            <span>Giảm giá bằng điểm (<c:out value="${selected.pointsUsed}"/> điểm)</span>
                            <strong>- <fmt:formatNumber value="${selected.discountAmount}" pattern="#,##0"/> đ</strong>
                        </div>
                    </c:if>
                    <div class="receipt-total-bottom">
                        <span>Khách đã trả</span>
                        <strong><fmt:formatNumber value="${selected.payableAmount}" pattern="#,##0"/> đ</strong>
                    </div>
                </div>
                <div class="receipt-actions no-print">
                    <button type="button" onclick="window.print()">In hóa đơn</button>
                </div>
            </section>
        </c:if>

        <%-- ===== Danh sách lịch sử ===== --%>
        <section class="no-print" aria-label="Danh sách thanh toán">
            <c:choose>
                <c:when test="${loadFailed}">
                    <p class="sh-message" role="alert">Không tải được lịch sử thanh toán.</p>
                </c:when>
                <c:otherwise>
                    <c:if test="${isManager}">
                    <form class="sh-filter" method="get" action="${pageContext.request.contextPath}/service-history">
                        <label>Từ ngày
                            <input type="date" name="from" value="${from}">
                        </label>
                        <label>Đến ngày
                            <input type="date" name="to" value="${to}">
                        </label>
                        <button type="submit">Lọc</button>
                        <a href="${pageContext.request.contextPath}/service-history">Xóa lọc</a>
                    </form>
                    </c:if>

                    <c:if test="${isManager}">
                    <div class="sh-summary">
                        <div><span>Số đơn</span><strong><c:out value="${fn:length(bills)}"/></strong></div>
                        <div><span>Doanh thu (khách đã trả)</span><strong><fmt:formatNumber value="${revenue}" pattern="#,##0"/> đ</strong></div>
                    </div>
                    </c:if>

                    <c:choose>
                        <c:when test="${empty bills}">
                            <div class="sh-empty">Chưa có đơn thanh toán nào trong khoảng thời gian này.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="sh-table-wrap">
                                <table class="sh-table">
                                    <thead>
                                        <tr>
                                            <th>Mã</th>
                                            <th>Thời gian</th>
                                            <th>Khách</th>
                                            <th class="num">Khách trả</th>
                                            <th></th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="b" items="${bills}">
                                            <tr class="${selected.historyId eq b.historyId ? 'is-selected' : ''}">
                                                <td>#<c:out value="${b.historyId}"/></td>
                                                <td><c:out value="${b.paidAtText}"/></td>
                                                <td><c:out value="${b.customerName}"/></td>
                                                <td class="num"><fmt:formatNumber value="${b.payableAmount}" pattern="#,##0"/> đ</td>
                                                <td><a href="${pageContext.request.contextPath}/service-history?id=${b.historyId}&from=${from}&to=${to}">Xem / In</a></td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:otherwise>
            </c:choose>
        </section>
    </main>
</body>
</html>
