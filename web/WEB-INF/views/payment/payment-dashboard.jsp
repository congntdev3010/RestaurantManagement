<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Thanh toán</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/payment.css?v=1.1">
    </head>
    <body class="payment-page">
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

        <main class="payment-shell payment-dashboard">
            <header class="dashboard-heading">
                <div>
                    <p class="eyebrow">QUẦY THANH TOÁN</p>
                    <h1>Bàn chưa thanh toán</h1>
                    <p class="dashboard-subtitle">Chọn một bàn để kiểm tra bill và hoàn tất thanh toán.</p>
                </div>
                <div class="unpaid-count">
                    <strong><c:out value="${unpaidTableCount}"/></strong>
                    <span>bàn</span>
                </div>
            </header>

            <form class="payment-filter" method="get" action="${pageContext.request.contextPath}/payment">
                <div>
                    <span class="filter-label">Bộ lọc</span>
                    <strong>Danh sách bàn chưa thanh toán</strong>
                </div>
                <select name="filter" onchange="this.form.submit()" aria-label="Lọc bàn thanh toán">
                    <option value="ALL" ${paymentFilter eq 'ALL' ? 'selected' : ''}>Tất cả bàn</option>
                    <option value="READY" ${paymentFilter eq 'READY' ? 'selected' : ''}>Có thể thanh toán ngay</option>
                </select>
            </form>

            <section class="payment-table-grid" aria-label="Danh sách bàn chưa thanh toán">
                <c:set var="shown" value="0"/>
                <c:forEach var="table" items="${paymentTables}">
                    <c:set var="bill" value="${paymentBills[table.tableId]}"/>
                    <c:if test="${not empty bill and (paymentFilter eq 'ALL' or bill.readyForPayment)}">
                        <c:set var="shown" value="${shown + 1}"/>
                        <article class="payment-table-card">
                            <div class="payment-table-top">
                                <div class="table-mark"><i class="fa-solid fa-receipt" aria-hidden="true"></i></div>
                                <span class="unpaid-pill">Chưa thanh toán</span>
                            </div>
                            <h2><c:out value="${table.tableName}"/></h2>
                            <p class="order-meta">
                                <c:out value="${bill.orderCount}"/> order · <c:out value="${bill.items.size()}"/> món
                            </p>
                            <div class="payment-table-total">
                                <span>Tổng bill</span>
                                <strong><fmt:formatNumber value="${bill.payableAmount}" pattern="#,##0"/> đ</strong>
                            </div>
                            <c:choose>
                                <c:when test="${bill.readyForPayment and bill.totalAmount gt 0}">
                                    <a class="payment-table-action"
                                       href="${pageContext.request.contextPath}/payment?tableId=${table.tableId}">
                                        Mở thanh toán <span>→</span>
                                    </a>
                                </c:when>
                                <c:when test="${bill.totalAmount le 0}">
                                    <a class="payment-table-action" style="background: #a855f7; color: white;"
                                       href="${pageContext.request.contextPath}/payment?tableId=${table.tableId}">
                                        Xử lý bàn 0 đ <span>→</span>
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <span class="payment-table-action is-disabled">Chưa đủ điều kiện thanh toán</span>
                                </c:otherwise>
                            </c:choose>
                        </article>
                    </c:if>
                </c:forEach>

                <c:if test="${shown eq 0}">
                    <div class="payment-empty">
                        <span class="empty-icon">✓</span>
                        <h2>Không có bàn phù hợp</h2>
                        <p>Danh sách thanh toán hiện không có bàn nào theo bộ lọc này.</p>
                    </div>
                </c:if>
            </section>
        </main>
    <script>
    (function () {
        // Tự tải lại danh sách bàn mỗi 3 giây; chỉ tải khi tab đang hiển thị.
        setInterval(function () {
            if (document.visibilityState === 'visible') {
                window.location.reload();
            }
        }, 8000);
    }());
</script>
</body>
</html>
