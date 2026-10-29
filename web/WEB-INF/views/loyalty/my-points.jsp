<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Điểm tích lũy</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.1">
    <style>
        .points-body { padding: 28px 32px 32px; }
        .points-rules { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 28px; }
        .points-rule { background: #fbf3ee; border: 1px solid #efdfd5; border-radius: 14px; padding: 14px 18px; }
        .points-rule span { display: block; font-size: .78rem; letter-spacing: .06em; text-transform: uppercase; color: #8a6f66; margin-bottom: 4px; }
        .points-rule strong { font-size: 1.05rem; color: #5a1b22; }
        .points-note { margin: 0 0 26px; color: #7a6a64; font-size: .92rem; }
        .points-title { margin: 0 0 14px; font-size: 1.25rem; color: #4a1620; }
        .points-table-wrap { overflow-x: auto; border: 1px solid #efe4dc; border-radius: 14px; }
        .points-table { width: 100%; border-collapse: collapse; background: #fff; }
        .points-table th { text-align: left; padding: 12px 16px; font-size: .72rem; letter-spacing: .07em; text-transform: uppercase;
                           color: #8a7a73; background: #faf5f1; border-bottom: 1px solid #efe4dc; white-space: nowrap; }
        .points-table td { padding: 14px 16px; border-bottom: 1px solid #f3ebe5; vertical-align: middle; }
        .points-table tr:last-child td { border-bottom: 0; }
        .points-table tbody tr:hover { background: #fffaf6; }
        .points-table .num { text-align: right; white-space: nowrap; font-variant-numeric: tabular-nums; }
        .points-table th.num { text-align: right; }
        .points-time { white-space: nowrap; color: #5d4f49; }
        .points-badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-size: .8rem; font-weight: 600; white-space: nowrap; }
        .points-badge.earn { background: #e6f4ea; color: #1e7a3c; }
        .points-badge.redeem { background: #fdeaea; color: #a8242b; }
        .points-badge.adjust { background: #eef0f6; color: #46506b; }
        .points-delta { font-weight: 700; font-size: 1.02rem; }
        .points-delta.plus { color: #1e7a3c; }
        .points-delta.minus { color: #a8242b; }
        .points-note-cell { color: #6b5d57; }
        .points-empty { text-align: center; padding: 36px 12px; color: #8a7a73; background: #fffaf6; border: 1px dashed #e5d3c8; border-radius: 14px; }
        @media (max-width: 640px) { .points-body { padding: 20px 14px 24px; } }
    </style>
</head>
<body class="payment-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="payment-shell">
        <section class="payment-card">
            <header class="payment-header">
                <div>
                    <p class="eyebrow">THÀNH VIÊN</p>
                    <h1>Điểm tích lũy của tôi</h1>
                    <p class="payment-subtitle">Hãy đưa tài khoản cho lễ tân khi thanh toán để tích điểm hoặc dùng điểm giảm giá.</p>
                </div>
                <div class="unpaid-count"><strong><fmt:formatNumber value="${points}" pattern="#,##0"/></strong><span>điểm</span></div>
            </header>

            <div class="points-body">
                <div class="points-rules">
                    <div class="points-rule">
                        <span>Tích điểm</span>
                        <strong><fmt:formatNumber value="${settings.earnSpendAmount}" pattern="#,##0"/> đ = <c:out value="${settings.earnPoints}"/> điểm</strong>
                    </div>
                    <div class="points-rule">
                        <span>Dùng điểm</span>
                        <strong>1 điểm = giảm <fmt:formatNumber value="${settings.redeemValuePerPoint}" pattern="#,##0"/> đ</strong>
                    </div>
                </div>

                <h2 class="points-title">Lịch sử điểm</h2>
                <c:choose>
                    <c:when test="${empty history}">
                        <div class="points-empty">Bạn chưa có giao dịch điểm nào.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="points-table-wrap">
                            <table class="points-table">
                                <thead>
                                    <tr>
                                        <th>Thời gian</th>
                                        <th>Loại</th>
                                        <th class="num">Điểm</th>
                                        <th class="num">Giá trị</th>
                                        <th class="num">Số dư sau</th>
                                        <th>Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="t" items="${history}">
                                    <tr>
                                        <td class="points-time"><c:out value="${t.createdAtText}"/></td>
                                        <td>
                                            <span class="points-badge ${t.txnType eq 'EARN' ? 'earn' : (t.txnType eq 'REDEEM' ? 'redeem' : 'adjust')}">
                                                <c:out value="${t.typeLabel}"/>
                                            </span>
                                        </td>
                                        <td class="num">
                                            <span class="points-delta ${t.points gt 0 ? 'plus' : 'minus'}">
                                                <c:if test="${t.points gt 0}">+</c:if><fmt:formatNumber value="${t.points}" pattern="#,##0"/>
                                            </span>
                                        </td>
                                        <td class="num"><c:if test="${not empty t.moneyValue}"><fmt:formatNumber value="${t.moneyValue}" pattern="#,##0"/> đ</c:if></td>
                                        <td class="num"><fmt:formatNumber value="${t.balanceAfter}" pattern="#,##0"/></td>
                                        <td class="points-note-cell"><c:out value="${t.note}"/></td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </main>
</body>
</html>
