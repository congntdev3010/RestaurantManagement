<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đặt bàn - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservations.css?v=1.0">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <main class="rsv-page rsv-narrow">
            <p class="rsv-eyebrow">ĐẶT BÀN TRỰC TUYẾN</p>
            <h1>Đặt bàn</h1>
            <p class="rsv-intro">
                Gửi thông tin bên dưới, lễ tân sẽ gọi lại cho bạn để xác nhận bàn. Đơn chỉ được giữ chỗ sau khi nhà hàng xác nhận.
            </p>

            <c:if test="${not empty bookingMessage}">
                <p class="rsv-message is-success" role="status"><c:out value="${bookingMessage}" /></p>
            </c:if>
            <c:if test="${not empty bookingError}">
                <p class="rsv-message is-error" role="alert"><c:out value="${bookingError}" /></p>
            </c:if>

            <form class="rsv-card rsv-form" action="${pageContext.request.contextPath}/booking" method="post">
                <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">

                <label>Họ tên
                    <input type="text" name="customerName" maxlength="30" required value="<c:out value='${formName}'/>">
                </label>
                <label>Số điện thoại
                    <input type="tel" name="customerPhone" maxlength="10" pattern="0[0-9]{9}" title="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0" inputmode="numeric" required placeholder="0901234567"
                           value="<c:out value='${formPhone}'/>">
                </label>

                <label>Loại bàn
                    <select name="tableType">
                        <option value="">Không yêu cầu</option>
                        <c:forEach var="t" items="${tableTypes}">
                            <option value="${fn:escapeXml(t)}" ${formType eq t ? 'selected' : ''}><c:out value="${t}" /></option>
                        </c:forEach>
                    </select>
                </label>
                <label>Số người
                    <input type="number" name="guestCount" min="1" max="30" required value="<c:out value='${empty formGuests ? 2 : formGuests}'/>">
                </label>

                <label>Ngày đến
                    <input type="date" name="reservationDate" required min="${minDate}" max="${maxDate}"
                           value="<c:out value='${formDate}'/>">
                </label>
                <label>Giờ đến
                    <select name="reservationTime" required>
                        <option value="">-- Chọn giờ --</option>
                        <optgroup label="Ca trưa">
                            <c:forTokens var="h" items="10:00,10:30,11:00,11:30,12:00,12:30,13:00,13:30,14:00,14:30" delims=",">
                                <option value="${h}" ${formTime eq h ? 'selected' : ''}>${h}</option>
                            </c:forTokens>
                        </optgroup>
                        <optgroup label="Ca tối">
                            <c:forTokens var="h" items="17:00,17:30,18:00,18:30,19:00,19:30,20:00,20:30,21:00,21:30,22:00,22:30" delims=",">
                                <option value="${h}" ${formTime eq h ? 'selected' : ''}>${h}</option>
                            </c:forTokens>
                        </optgroup>
                    </select>
                </label>

                <label class="full">Ghi chú
                    <textarea name="note" rows="3" maxlength="100"
                              placeholder="Ví dụ: tiệc sinh nhật, cần ghế trẻ em..."><c:out value="${formNote}" /></textarea>
                    <span class="rsv-hint">Cần đặt trước ít nhất 1 giờ và tối đa 1 tuần (7 ngày).</span>
                </label>

                <div class="full">
                    <button type="submit" class="rsv-btn">Gửi yêu cầu đặt bàn</button>
                    <c:if test="${empty sessionScope.user}">
                        <span class="rsv-hint">Có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a> để xem lịch sử đặt bàn.</span>
                    </c:if>
                </div>
            </form>
        </main>
    </body>
</html>
