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
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/my-points.css">
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
