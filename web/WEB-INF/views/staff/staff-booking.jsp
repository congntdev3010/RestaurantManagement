<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đặt bàn cho khách - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservations.css?v=1.1">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <c:set var="base" value="${pageContext.request.contextPath}/staff-booking" />
        <main class="rsv-page rsv-narrow">
            <p><a href="${pageContext.request.contextPath}/manage-reservations">&larr; Danh sách đặt bàn</a></p>
            <p class="rsv-eyebrow">VẬN HÀNH NHÀ HÀNG</p>
            <h1>Đặt bàn cho khách</h1>
            <p class="rsv-intro">
                Khách gọi điện đặt bàn: nhập thông tin như form khách, bấm "Xem bàn trống" để chọn vị trí bàn (có thể gộp nhiều bàn).
                Đơn do lễ tân tạo được duyệt ngay.
            </p>

            <c:if test="${not empty bookingMessage}">
                <p class="rsv-message ${bookingMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                    <c:out value="${bookingMessage}" />
                </p>
            </c:if>
            <c:if test="${not empty slotError}"><p class="rsv-message is-error"><c:out value="${slotError}" /></p></c:if>

            <%-- Một form: nút "Xem bàn trống" gửi GET (không kèm token), nút "Đặt bàn" gửi POST --%>
            <form class="rsv-card rsv-form" action="${base}" method="post" id="staffBookingForm">
                <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">

                <label>Họ tên
                    <input type="text" name="customerName" maxlength="30" required value="<c:out value='${fName}'/>">
                </label>
                <label>Số điện thoại
                    <input type="tel" name="customerPhone" maxlength="10" pattern="0[0-9]{9}" title="Số điện thoại gồm 10 chữ số, bắt đầu bằng 0" inputmode="numeric" required placeholder="0901234567"
                           value="<c:out value='${fPhone}'/>">
                </label>

                <label>Loại bàn
                    <select name="tableType">
                        <option value="">Không yêu cầu</option>
                        <c:forEach var="t" items="${tableTypes}">
                            <option value="${fn:escapeXml(t)}" ${fType eq t ? 'selected' : ''}><c:out value="${t}" /></option>
                        </c:forEach>
                    </select>
                </label>
                <label>Số người
                    <input type="number" name="guestCount" min="1" max="30" required value="<c:out value='${empty fGuests ? 2 : fGuests}'/>">
                </label>

                <label>Ngày đến
                    <input type="date" name="reservationDate" required min="${minDate}" max="${maxDate}" value="${fDate}">
                </label>
                <label>Giờ đến
                    <select name="reservationTime" required>
                        <option value="">-- Chọn giờ --</option>
                        <optgroup label="Ca trưa">
                            <c:forTokens var="h" items="10:00,10:30,11:00,11:30,12:00,12:30,13:00,13:30,14:00,14:30" delims=",">
                                <option value="${h}" ${fTime eq h ? 'selected' : ''}>${h}</option>
                            </c:forTokens>
                        </optgroup>
                        <optgroup label="Ca tối">
                            <c:forTokens var="h" items="17:00,17:30,18:00,18:30,19:00,19:30,20:00,20:30,21:00,21:30,22:00,22:30" delims=",">
                                <option value="${h}" ${fTime eq h ? 'selected' : ''}>${h}</option>
                            </c:forTokens>
                        </optgroup>
                    </select>
                </label>

                <label class="full">Ghi chú
                    <textarea name="note" rows="3" maxlength="100"
                              placeholder="Ví dụ: tiệc sinh nhật, cần ghế trẻ em..."><c:out value="${fNote}" /></textarea>
                </label>

                <div class="full">
                    <button type="submit" name="xemBan" value="1" formmethod="get" formaction="${base}" formnovalidate class="rsv-btn secondary"
                            onclick="this.form.csrfToken.disabled = true;">Xem bàn trống</button>
                </div>

                <c:if test="${not empty tableGroups}">
                    <div class="full">
                        <h2>Chọn vị trí bàn — <c:out value="${shiftName}" />, ${fDate} lúc ${fTime}</h2>
                        <c:forEach var="g" items="${tableGroups}">
                            <div class="rsv-type-group">
                                <h3><c:out value="${g.key}" /></h3>
                                <div class="rsv-seats">
                                    <c:forEach var="slot" items="${g.value}">
                                        <label class="rsv-seat ${slot.booked ? 'disabled' : ''}">
                                            <input type="checkbox" name="tableId" value="${slot.table.tableId}"
                                                   data-cap="${slot.table.capacity}" ${slot.booked ? 'disabled' : ''}>
                                            <span>
                                                <c:out value="${slot.table.tableName}" />
                                                <small>${slot.table.capacity} chỗ · ${slot.booked ? 'đã có người đặt' : 'còn trống'}</small>
                                            </span>
                                        </label>
                                    </c:forEach>
                                </div>
                            </div>
                        </c:forEach>
                        <p class="rsv-hint" id="capSummary">Đã chọn 0 bàn</p>
                        <p class="rsv-hint">Nếu đổi ngày, giờ, số người hoặc loại bàn, hãy bấm "Xem bàn trống" lại trước khi đặt.</p>
                    </div>
                    <div class="full">
                        <button type="submit" class="rsv-btn" id="submitBtn" disabled>Đặt bàn &amp; duyệt luôn</button>
                    </div>
                    <script>
                        (function () {
                            var form = document.getElementById('staffBookingForm');
                            var out = document.getElementById('capSummary');
                            var btn = document.getElementById('submitBtn');
                            function refresh() {
                                var guests = parseInt(form.guestCount.value, 10) || 0;
                                var boxes = form.querySelectorAll('input[name=tableId]:checked');
                                var cap = 0;
                                boxes.forEach(function (b) { cap += parseInt(b.dataset.cap, 10); });
                                var enough = cap >= guests && boxes.length > 0;
                                out.textContent = 'Đã chọn ' + boxes.length + ' bàn · ' + cap + '/' + guests + ' chỗ'
                                        + (enough ? ' — đủ chỗ' : ' — chưa đủ, chọn thêm bàn để gộp');
                                btn.disabled = !enough;
                            }
                            form.addEventListener('change', refresh);
                            form.addEventListener('input', refresh);
                            refresh();
                        })();
                    </script>
                </c:if>
            </form>
        </main>
    </body>
</html>
