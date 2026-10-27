<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Check-in khách - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservations.css?v=1.1">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <c:set var="base" value="${pageContext.request.contextPath}/checkin" />
        <main class="rsv-page">
            <p class="rsv-eyebrow">VẬN HÀNH NHÀ HÀNG</p>
            <h1>Check-in khách</h1>
            <p class="rsv-intro">Khách đến nhà hàng: chọn khách đã đặt bàn hoặc mở bàn cho khách chưa đặt.</p>

            <c:if test="${not empty checkinMessage}">
                <p class="rsv-message ${checkinMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                    <c:out value="${checkinMessage}" />
                </p>
            </c:if>

            <nav class="rsv-tabs" aria-label="Loại khách">
                <a class="rsv-tab ${tab eq 'booked' ? 'active' : ''}" href="${base}?tab=booked">Khách đã đặt bàn<b>${todayArrivalCount}</b></a>
                <a class="rsv-tab ${tab eq 'walkin' ? 'active' : ''}" href="${base}?tab=walkin">Khách chưa đặt<b>${freeCount} bàn trống</b></a>
            </nav>

            <c:choose>
                <%-- ===== Khách đã đặt: tìm đơn rồi bấm "Khách đã đến" ===== --%>
                <c:when test="${tab eq 'booked'}">
                    <form class="rsv-filter" method="get" action="${base}">
                        <input type="hidden" name="tab" value="booked">
                        <input class="rsv-input" type="search" name="q" maxlength="100" style="flex:1 1 380px;min-width:280px"
                               placeholder="Tìm theo tên người đặt hoặc số điện thoại" value="<c:out value='${keyword}'/>" autofocus>
                        <button type="submit" class="rsv-btn small">Tìm</button>
                        <c:if test="${not empty keyword}"><a class="rsv-btn secondary small" href="${base}?tab=booked">Xem đơn hôm nay</a></c:if>
                    </form>
                    <p class="rsv-hint">
                        <c:choose>
                            <c:when test="${empty keyword}">Đang hiện các đơn đã duyệt có ngày đến là hôm nay và khách chưa đến.</c:when>
                            <c:otherwise>Kết quả cho "<c:out value="${keyword}" />" — các đơn đã duyệt từ hôm nay trở đi.</c:otherwise>
                        </c:choose>
                    </p>

                    <c:choose>
                        <c:when test="${empty reservations}">
                            <p class="rsv-empty">Không tìm thấy đơn đặt bàn phù hợp.</p>
                        </c:when>
                        <c:otherwise>
                            <table class="rsv-table">
                                <thead>
                                    <tr><th>Khách</th><th>Điện thoại</th><th>Giờ đến</th><th>Số người</th><th>Bàn đã giữ</th><th></th></tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="r" items="${reservations}">
                                        <tr>
                                            <td>
                                                <c:out value="${r.customerName}" />
                                                <c:if test="${not empty r.note}"><br><small class="rsv-hint">Ghi chú: <c:out value="${r.note}" /></small></c:if>
                                            </td>
                                            <td><c:out value="${r.customerPhone}" /></td>
                                            <td><c:out value="${r.timeText}" /> · <c:out value="${r.dateText}" /><br><span class="rsv-tag"><c:out value="${r.mealShiftText}" /></span></td>
                                            <td>${r.guestCount}</td>
                                            <td><strong><c:out value="${r.tableName}" /></strong></td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${r.today}">
                                                        <form action="${base}" method="post" class="rsv-inline"
                                                              onsubmit="return confirm(this.dataset.msg);"
                                                              data-msg="<c:out value='Xác nhận khách ${r.customerName} đã đến và mở ${r.tableName}?'/>">
                                                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                                            <input type="hidden" name="action" value="checkin">
                                                            <input type="hidden" name="reservationId" value="${r.reservationId}">
                                                            <input type="hidden" name="q" value="<c:out value='${keyword}'/>">
                                                            <button type="submit" class="rsv-btn small">Khách đã đến</button>
                                                        </form>
                                                    </c:when>
                                                    <c:otherwise><span class="rsv-hint">Chưa đến ngày</span></c:otherwise>
                                                </c:choose>
                                                <a class="rsv-btn secondary small" href="${pageContext.request.contextPath}/manage-reservations?id=${r.reservationId}">Chi tiết</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:otherwise>
                    </c:choose>
                </c:when>

                <%-- ===== Khách chưa đặt: bật bàn trống sang đang phục vụ ===== --%>
                <c:otherwise>
                    <c:if test="${tableLoadFailed}"><p class="rsv-message is-error">Không tải được danh sách bàn.</p></c:if>
                    <p class="rsv-hint">Chọn một bàn đang trống để mở cho khách. Bàn có nhãn vàng là bàn hôm nay đã có người đặt nhưng chưa đến.
                        Đoàn đông người: tích "gộp bàn" ở nhiều bàn trống rồi bấm nút bên dưới.</p>
                    <form id="mergeForm" action="${base}" method="post" class="rsv-actions"
                          onsubmit="var n = this.ownerDocument.querySelectorAll('input[form=mergeForm]:checked').length; if (n < 2) { alert('Hãy tích chọn ít nhất 2 bàn để gộp.'); return false; } return confirm('Mở ' + n + ' bàn gộp cho khách chưa đặt?');">
                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                        <input type="hidden" name="action" value="seat">
                        <button type="submit" class="rsv-btn small">Mở các bàn đã tích (gộp bàn)</button>
                    </form>

                    <c:forEach var="g" items="${tableGroups}">
                        <section class="rsv-card">
                            <h2><c:out value="${g.key}" /></h2>
                            <div class="rsv-seats">
                                <c:forEach var="t" items="${g.value}">
                                    <c:set var="arr" value="${arrivalsByTable[t.tableId]}" />
                                    <c:set var="confirmText" value="Mở ${t.tableName} cho khách chưa đặt?" />
                                    <c:forEach var="a" items="${arr}">
                                        <c:set var="confirmText" value="${confirmText} CẢNH BÁO: bàn này đã được ${a.customerName} đặt lúc ${a.timeText} (${a.mealShiftText}) hôm nay." />
                                    </c:forEach>
                                    <div class="rsv-seat ${t.status eq 'AVAILABLE' ? '' : 'disabled'}">
                                        <span>
                                            <c:out value="${t.tableName}" />
                                            <small>${t.capacity} chỗ ·
                                                <c:choose>
                                                    <c:when test="${t.status eq 'AVAILABLE'}">đang trống</c:when>
                                                    <c:when test="${t.status eq 'OCCUPIED'}">đang phục vụ</c:when>
                                                    <c:otherwise>đã đặt trước</c:otherwise>
                                                </c:choose>
                                            </small>
                                            <c:forEach var="a" items="${arr}">
                                                <small class="rsv-tag">Đặt: <c:out value="${a.customerName}" /> lúc <c:out value="${a.timeText}" /></small>
                                            </c:forEach>
                                            <c:if test="${t.status eq 'AVAILABLE'}">
                                                <label class="rsv-hint" style="display:block;margin-top:6px">
                                                    <input type="checkbox" form="mergeForm" name="tableId" value="${t.tableId}"> gộp bàn
                                                </label>
                                                <form action="${base}" method="post" style="margin-top:8px"
                                                      onsubmit="return confirm(this.dataset.msg);" data-msg="<c:out value='${confirmText}'/>">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                                    <input type="hidden" name="action" value="seat">
                                                    <input type="hidden" name="tableId" value="${t.tableId}">
                                                    <button type="submit" class="rsv-btn small">Mở bàn</button>
                                                </form>
                                            </c:if>
                                        </span>
                                    </div>
                                </c:forEach>
                            </div>
                        </section>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </main>
    </body>
</html>
