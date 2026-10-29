<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý đặt bàn - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservations.css?v=1.0">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <main class="rsv-page">
            <p class="rsv-eyebrow">VẬN HÀNH NHÀ HÀNG</p>
            <h1>Quản lý đặt bàn</h1>
            <p class="rsv-intro">Gọi lại khách để chốt đơn, kiểm tra bàn còn trống rồi duyệt.</p>
            <p><a class="rsv-btn small" href="${pageContext.request.contextPath}/staff-booking">+ Đặt bàn cho khách (gọi điện)</a></p>

            <c:if test="${not empty reservationMessage}">
                <p class="rsv-message ${reservationMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                    <c:out value="${reservationMessage}" />
                </p>
            </c:if>

            <c:set var="base" value="${pageContext.request.contextPath}/manage-reservations" />
            <nav class="rsv-tabs" aria-label="Lọc theo trạng thái">
                <a class="rsv-tab ${statusFilter eq 'PENDING' ? 'active' : ''}" href="${base}?status=PENDING&date=${dateFilter}">Chờ xác nhận<b>${empty statusCounts['PENDING'] ? 0 : statusCounts['PENDING']}</b></a>
                <a class="rsv-tab ${statusFilter eq 'APPROVED' ? 'active' : ''}" href="${base}?status=APPROVED&date=${dateFilter}">Đã duyệt<b>${empty statusCounts['APPROVED'] ? 0 : statusCounts['APPROVED']}</b></a>
                <a class="rsv-tab ${statusFilter eq 'COMPLETED' ? 'active' : ''}" href="${base}?status=COMPLETED&date=${dateFilter}">Hoàn thành</a>
                <a class="rsv-tab ${statusFilter eq 'NO_SHOW' ? 'active' : ''}" href="${base}?status=NO_SHOW&date=${dateFilter}">Không đến</a>
                <a class="rsv-tab ${statusFilter eq 'REJECTED' ? 'active' : ''}" href="${base}?status=REJECTED&date=${dateFilter}">Từ chối</a>
                <a class="rsv-tab ${statusFilter eq 'CANCELLED' ? 'active' : ''}" href="${base}?status=CANCELLED&date=${dateFilter}">Đã huỷ</a>
                <a class="rsv-tab ${statusFilter eq 'ALL' ? 'active' : ''}" href="${base}?status=ALL&date=${dateFilter}">Tất cả</a>
            </nav>

            <form class="rsv-filter" method="get" action="${base}">
                <input type="hidden" name="status" value="${statusFilter}">
                <label>Ngày đến: <input class="rsv-input" type="date" name="date" value="${dateFilter}"></label>
                <button type="submit" class="rsv-btn small">Lọc</button>
                <c:if test="${not empty dateFilter}">
                    <a class="rsv-btn secondary small" href="${base}?status=${statusFilter}">Bỏ lọc ngày</a>
                </c:if>
            </form>

            <c:choose>
                <c:when test="${empty reservations}">
                    <p class="rsv-empty">Không có đơn đặt bàn nào.</p>
                </c:when>
                <c:otherwise>
                    <table class="rsv-table">
                        <thead>
                            <tr>
                                <th>Mã</th><th>Khách</th><th>Điện thoại</th><th>Ngày giờ đến</th>
                                <th>Số người</th><th>Loại bàn</th><th>Trạng thái</th><th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="r" items="${reservations}">
                                <tr>
                                    <td>#${r.reservationId}</td>
                                    <td>
                                        <c:out value="${r.customerName}" />
                                        <c:if test="${not empty r.userId}"><br><span class="rsv-tag">Thành viên</span></c:if>
                                    </td>
                                    <td><c:out value="${r.customerPhone}" /></td>
                                    <td><c:out value="${r.timeText}" /> · <c:out value="${r.dateText}" /></td>
                                    <td>${r.guestCount}</td>
                                    <td><c:out value="${empty r.tableTypeRequested ? 'Không yêu cầu' : r.tableTypeRequested}" /></td>
                                    <td>
                                        <span class="rsv-status st-${r.status}"><c:out value="${r.statusText}" /></span>
                                        <c:if test="${r.status eq 'PENDING' and not empty r.contactedAt}"><br><span class="rsv-tag">Đã gọi</span></c:if>
                                        <c:if test="${not empty r.tableName}"><br><small><c:out value="${r.tableName}" /> · <c:out value="${r.mealShiftText}" /></small></c:if>
                                    </td>
                                    <td><a class="rsv-btn small" href="${base}?id=${r.reservationId}">Chi tiết</a></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </main>
    <script>
    // Tự tải lại mỗi 10 giây để thấy đơn đặt bàn mới (bỏ qua khi tab đang ẩn).
    setInterval(function () { if (!document.hidden) location.reload(); }, 10000);
</script>
</body>
</html>
