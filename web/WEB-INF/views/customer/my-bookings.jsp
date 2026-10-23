<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Lịch sử đặt bàn - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservations.css?v=1.0">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <main class="rsv-page rsv-narrow">
            <p class="rsv-eyebrow">TÀI KHOẢN</p>
            <h1>Lịch sử đặt bàn</h1>
            <p class="rsv-intro">Theo dõi trạng thái các đơn đặt bàn của bạn.</p>

            <c:if test="${not empty bookingMessage}">
                <p class="rsv-message is-success" role="status"><c:out value="${bookingMessage}" /></p>
            </c:if>

            <c:choose>
                <c:when test="${empty reservations}">
                    <p class="rsv-empty">Bạn chưa có đơn đặt bàn nào.</p>
                </c:when>
                <c:otherwise>
                    <div class="rsv-list">
                        <c:forEach var="r" items="${reservations}">
                            <article class="rsv-card rsv-item">
                                <div>
                                    <strong>Đơn #<c:out value="${r.reservationId}" /></strong>
                                    <span class="rsv-status st-${r.status}"><c:out value="${r.statusText}" /></span>
                                    <p>🗓 <c:out value="${r.timeText}" /> ngày <c:out value="${r.dateText}" /> · <c:out value="${r.guestCount}" /> người</p>
                                    <c:if test="${not empty r.tableTypeRequested}">
                                        <p>Loại bàn yêu cầu: <c:out value="${r.tableTypeRequested}" /></p>
                                    </c:if>
                                    <c:if test="${r.status eq 'APPROVED' or r.status eq 'COMPLETED'}">
                                        <p>Bàn được giữ: <strong><c:out value="${r.tableName}" /></strong> (<c:out value="${r.mealShiftText}" />)</p>
                                    </c:if>
                                    <c:if test="${r.status eq 'PENDING'}">
                                        <p class="rsv-hint">Đang chờ lễ tân gọi xác nhận qua số <c:out value="${r.customerPhone}" />.</p>
                                    </c:if>
                                    <c:if test="${not empty r.cancelReason and (r.status eq 'REJECTED' or r.status eq 'CANCELLED')}">
                                        <p class="rsv-hint">Lý do: <c:out value="${r.cancelReason}" /></p>
                                    </c:if>
                                    <p class="rsv-hint">Gửi lúc <c:out value="${r.createdAtText}" /></p>
                                </div>
                                <c:if test="${r.status eq 'PENDING' or r.status eq 'APPROVED'}">
                                    <form action="${pageContext.request.contextPath}/my-bookings" method="post"
                                          onsubmit="return confirm('Bạn chắc chắn muốn huỷ đơn đặt bàn này?');">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                        <input type="hidden" name="action" value="cancel">
                                        <input type="hidden" name="reservationId" value="${r.reservationId}">
                                        <button type="submit" class="rsv-btn danger small">Huỷ đơn</button>
                                    </form>
                                </c:if>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
            <p><a class="rsv-btn secondary" href="${pageContext.request.contextPath}/booking">Đặt bàn mới</a></p>
        </main>
    </body>
</html>
