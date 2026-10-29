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
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/my-payments.css?v=1.0">
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
                                    <p class="mp-sub">
                                        Bàn <strong><c:out value="${selected.tableName}"/></strong> · <c:out value="${selected.paidAtText}"/>
                                    </p>

                                    <div class="mp-facts">
                                        <div>
                                            <span>Phương thức</span>
                                            <strong><c:out value="${selected.paymentMethod eq 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR'}"/></strong>
                                        </div>
                                        <div>
                                            <span>Điểm đã dùng</span>
                                            <strong><c:out value="${selected.pointsUsed}"/></strong>
                                        </div>
                                        <div>
                                            <span>Điểm được cộng</span>
                                            <strong>+<c:out value="${selected.pointsEarned}"/></strong>
                                        </div>
                                    </div>

                                    <table class="mp-items">
                                        <thead>
                                            <tr>
                                                <th>Món</th>
                                                <th class="num">SL</th>
                                                <th class="num">Đơn giá</th>
                                                <th class="num">Thành tiền</th>
                                            </tr>
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
                                        <div>
                                            <span>Tổng bill</span>
                                            <strong><fmt:formatNumber value="${selected.totalAmount}" pattern="#,##0"/> đ</strong>
                                        </div>
                                        <c:if test="${selected.discountAmount gt 0}">
                                            <div class="minus">
                                                <span>Giảm giá bằng điểm</span>
                                                <strong>- <fmt:formatNumber value="${selected.discountAmount}" pattern="#,##0"/> đ</strong>
                                            </div>
                                        </c:if>
                                        <div class="pay">
                                            <span>Đã thanh toán</span>
                                            <strong><fmt:formatNumber value="${selected.payableAmount}" pattern="#,##0"/> đ</strong>
                                        </div>
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
                                                        <td class="num">
                                                            <c:if test="${b.discountAmount gt 0}">
                                                                -<fmt:formatNumber value="${b.discountAmount}" pattern="#,##0"/> đ
                                                            </c:if>
                                                        </td>
                                                        <td class="num"><fmt:formatNumber value="${b.payableAmount}" pattern="#,##0"/> đ</td>
                                                        <td class="num">
                                                            <c:if test="${b.pointsEarned gt 0}">+<c:out value="${b.pointsEarned}"/></c:if>
                                                            </td>
                                                            <td>
                                                                <a class="mp-link"
                                                                   href="${pageContext.request.contextPath}/my-payments?id=${b.historyId}">Xem chi tiết</a>
                                                        </td>
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
