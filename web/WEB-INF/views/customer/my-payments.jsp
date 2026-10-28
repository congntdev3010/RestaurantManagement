<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lịch sử thanh toán</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.1">
    <style>
        .mp-body { padding: 28px 32px 32px; }
        .mp-message { padding: 12px 16px; border-radius: 10px; background: #fdeaea; color: #a8242b; margin: 0 0 16px; }
        .mp-empty { text-align: center; padding: 36px 12px; color: #8a7a73; background: #fffaf6; border: 1px dashed #e5d3c8; border-radius: 14px; }
        .mp-table-wrap { overflow-x: auto; border: 1px solid #efe4dc; border-radius: 14px; }
        .mp-table { width: 100%; border-collapse: collapse; background: #fff; }
        .mp-table th { text-align: left; padding: 12px 16px; font-size: .72rem; letter-spacing: .07em; text-transform: uppercase;
                       color: #8a7a73; background: #faf5f1; border-bottom: 1px solid #efe4dc; white-space: nowrap; }
        .mp-table td { padding: 14px 16px; border-bottom: 1px solid #f3ebe5; vertical-align: middle; white-space: nowrap; }
        .mp-table tr:last-child td { border-bottom: 0; }
        .mp-table tbody tr:hover { background: #fffaf6; }
        .mp-table .num, .mp-table th.num { text-align: right; font-variant-numeric: tabular-nums; }
        .mp-table tr.is-selected { background: #fff4e6; }
        .mp-link { color: #7b1f28; font-weight: 600; text-decoration: none; }
        .mp-link:hover { text-decoration: underline; }
        .mp-back { display: inline-block; margin-bottom: 12px; color: #7b1f28; text-decoration: none; }
        .mp-detail { margin-top: 26px; border: 1px solid #efe4dc; border-radius: 16px; padding: 22px; background: #fff; }
        .mp-detail h2 { margin: 0 0 4px; font-size: 1.2rem; color: #4a1620; }
        .mp-sub { margin: 0 0 16px; color: #7a6a64; font-size: .92rem; }
        .mp-facts { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 18px; }
        .mp-facts div { background: #fbf3ee; border: 1px solid #efdfd5; border-radius: 12px; padding: 10px 14px; }
        .mp-facts span { display: block; font-size: .72rem; color: #8a6f66; text-transform: uppercase; letter-spacing: .05em; }
        .mp-facts strong { font-size: 1rem; color: #5a1b22; }
        .mp-items { width: 100%; border-collapse: collapse; }
        .mp-items th, .mp-items td { padding: 9px 4px; border-bottom: 1px solid #f3ebe5; text-align: left; }
        .mp-items .num { text-align: right; font-variant-numeric: tabular-nums; }
        .mp-totals { margin-top: 12px; }
        .mp-totals div { display: flex; justify-content: space-between; gap: 12px; padding: 6px 0; color: #5d4f49; }
        .mp-totals .minus strong { color: #a8242b; }
        .mp-totals .pay { border-top: 2px solid #3f3330; margin-top: 6px; padding-top: 12px; color: #000; }
        .mp-totals .pay strong { font-size: 1.4rem; color: #000; }
        @media (max-width: 640px) { .mp-body { padding: 20px 14px 24px; } .mp-detail { padding: 16px; } }
    </style>
</head>
<body class="payment-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="payment-shell">
        <section class="payment-card">
            <header class="payment-header">
                <div>
                    <p class="eyebrow">THÀNH VIÊN</p>
                    <h1>Lịch sử thanh toán</h1>
                    <p class="payment-subtitle">Các hóa đơn bạn đã thanh toán khi tài khoản được gắn vào bill.</p>
                </div>
            </header>

            <div class="mp-body">
                <c:choose>
                    <c:when test="${loadFailed}">
                        <p class="mp-message" role="alert">Không tải được lịch sử thanh toán. Vui lòng thử lại sau.</p>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty detailMessage}">
                            <p class="mp-message" role="status"><c:out value="${detailMessage}"/></p>
                        </c:if>

                        <%-- ===== Chi tiết một hóa đơn ===== --%>
                        <c:if test="${not empty selected}">
                            <a class="mp-back" href="${pageContext.request.contextPath}/my-payments">&larr; Danh sách hóa đơn</a>
                            <section class="mp-detail" aria-label="Chi tiết hóa đơn">
                                <h2>Hóa đơn #<c:out value="${selected.historyId}"/></h2>
                                <p class="mp-sub">Bàn <strong><c:out value="${selected.tableName}"/></strong> · <c:out value="${selected.paidAtText}"/></p>
                                <div class="mp-facts">
                                    <div><span>Phương thức</span><strong><c:out value="${selected.paymentMethod eq 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR'}"/></strong></div>
                                    <div><span>Điểm đã dùng</span><strong><c:out value="${selected.pointsUsed}"/></strong></div>
                                    <div><span>Điểm được cộng</span><strong>+<c:out value="${selected.pointsEarned}"/></strong></div>
                                </div>
                                <table class="mp-items">
                                    <thead>
                                        <tr><th>Món</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="item" items="${selected.items}">
                                            <tr>
                                                <td><c:out value="${item.dishName}"/></td>
                                                <td class="num"><c:out value="${item.quantity}"/></td>
                                                <td class="num"><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/> đ</td>
                                                <td class="num"><fmt:formatNumber value="${item.lineTotal}" pattern="#,##0"/> đ</td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                                <div class="mp-totals">
                                    <div><span>Tổng bill</span><strong><fmt:formatNumber value="${selected.totalAmount}" pattern="#,##0"/> đ</strong></div>
                                    <c:if test="${selected.discountAmount gt 0}">
                                        <div class="minus"><span>Giảm giá bằng điểm</span><strong>- <fmt:formatNumber value="${selected.discountAmount}" pattern="#,##0"/> đ</strong></div>
                                    </c:if>
                                    <div class="pay"><span>Đã thanh toán</span><strong><fmt:formatNumber value="${selected.payableAmount}" pattern="#,##0"/> đ</strong></div>
                                </div>
                            </section>
                        </c:if>

                        <%-- ===== Danh sách hóa đơn ===== --%>
                        <c:choose>
                            <c:when test="${empty bills}">
                                <div class="mp-empty">Bạn chưa có hóa đơn thanh toán nào.</div>
                            </c:when>
                            <c:otherwise>
                                <div class="mp-table-wrap">
                                    <table class="mp-table">
                                        <thead>
                                            <tr>
                                                <th>Mã</th>
                                                <th>Thời gian</th>
                                                <th>Bàn</th>
                                                <th>Thanh toán</th>
                                                <th class="num">Tổng bill</th>
                                                <th class="num">Giảm điểm</th>
                                                <th class="num">Đã trả</th>
                                                <th class="num">Điểm cộng</th>
                                                <th></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="b" items="${bills}">
                                                <tr class="${selected.historyId eq b.historyId ? 'is-selected' : ''}">
                                                    <td>#<c:out value="${b.historyId}"/></td>
                                                    <td><c:out value="${b.paidAtText}"/></td>
                                                    <td><c:out value="${b.tableName}"/></td>
                                                    <td><c:out value="${b.paymentMethod eq 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR'}"/></td>
                                                    <td class="num"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> đ</td>
                                                    <td class="num"><c:if test="${b.discountAmount gt 0}">-<fmt:formatNumber value="${b.discountAmount}" pattern="#,##0"/> đ</c:if></td>
                                                    <td class="num"><fmt:formatNumber value="${b.payableAmount}" pattern="#,##0"/> đ</td>
                                                    <td class="num"><c:if test="${b.pointsEarned gt 0}">+<c:out value="${b.pointsEarned}"/></c:if></td>
                                                    <td><a class="mp-link" href="${pageContext.request.contextPath}/my-payments?id=${b.historyId}">Xem chi tiết</a></td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </main>
</body>
</html>
