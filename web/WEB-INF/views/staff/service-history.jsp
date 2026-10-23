<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thanh toán thành công</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/service-history.css?v=1.0">
</head>
<body class="history-page">
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="receipt-shell">
        <section class="receipt-card">
            <div class="success-mark">✓</div>
            <p class="eyebrow">GIAO DỊCH HOÀN TẤT</p>
            <h1>Thanh toán thành công</h1>
            <p class="receipt-subtitle">Bill của bàn <strong><c:out value="${receipt.tableId}"/></strong> đã được đóng.</p>
            <div class="receipt-total">
                <span>Tổng tiền</span>
                <strong><c:out value="${receipt.totalAmount}"/> đ</strong>
            </div>
            <div class="receipt-info">
                <div><span>Bàn</span><strong><c:out value="${receipt.tableId}"/></strong></div>
                <div><span>Phương thức</span><strong><c:out value="${receipt.paymentMethod eq 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR'}"/></strong></div>
                <c:if test="${not empty receipt.customerName}"><div><span>Khách hàng</span><strong><c:out value="${receipt.customerName}"/></strong></div></c:if>
                <c:if test="${receipt.paymentMethod eq 'CASH' and not empty receipt.cashReceived}"><div><span>Số tiền đã đưa</span><strong><c:out value="${receipt.cashReceived}"/> đ</strong></div></c:if>
                <c:if test="${not empty receipt.cashReceived}"><div><span>Tiền thừa</span><strong><c:out value="${receipt.cashChange}"/> đ</strong></div></c:if>
            </div>
            <div class="receipt-lines">
                <div class="lines-heading"><h2>Các món đã phục vụ</h2><span><c:out value="${receipt.items.size()}"/> món</span></div>
                <c:forEach var="item" items="${receipt.items}">
                    <div class="receipt-line"><span><strong><c:out value="${item.dishName}"/></strong><small><c:out value="${item.quantity}"/> × <c:out value="${item.unitPrice}"/> đ</small></span><strong><c:out value="${item.lineTotal}"/> đ</strong></div>
                </c:forEach>
                <div class="receipt-total-bottom">
                    <span>Tổng bill</span>
                    <strong><c:out value="${receipt.totalAmount}"/> đ</strong>
                </div>
            </div>
            <div class="receipt-actions">
                <button type="button" onclick="window.print()">In hóa đơn</button>
                <a href="${pageContext.request.contextPath}/payment">Thanh toán bàn khác</a>
            </div>
        </section>
    </main>
</body>
</html>
