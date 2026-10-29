<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Đơn đặt bàn #${reservation.reservationId} - Việt À la carte</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/reservations.css?v=1.0">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <c:set var="base" value="${pageContext.request.contextPath}/manage-reservations" />
        <c:set var="r" value="${reservation}" />
        <main class="rsv-page">
            <p><a href="${base}">&larr; Danh sách đặt bàn</a></p>
            <p class="rsv-eyebrow">ĐƠN ĐẶT BÀN</p>
            <h1>Đơn #${r.reservationId} <span class="rsv-status st-${r.status}"><c:out value="${r.statusText}" /></span></h1>
            <p class="rsv-intro">Gửi lúc <c:out value="${r.createdAtText}" /></p>

            <c:if test="${not empty reservationMessage}">
                <p class="rsv-message ${reservationMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                    <c:out value="${reservationMessage}" />
                </p>
            </c:if>

            <div class="rsv-grid" style="${canDecide ? '' : 'grid-template-columns:1fr;max-width:720px'}">
                <section class="rsv-card">
                    <h2>Thông tin khách</h2>
                    <dl class="rsv-dl">
                        <dt>Họ tên</dt><dd><c:out value="${r.customerName}" />
                            <c:if test="${not empty r.userId}"><span class="rsv-tag">· Thành viên</span></c:if></dd>
                        <dt>Điện thoại</dt><dd><a href="tel:${r.customerPhone}"><c:out value="${r.customerPhone}" /></a></dd>
                        <dt>Ngày giờ đến</dt><dd><c:out value="${r.timeText}" /> · <c:out value="${r.dateText}" /></dd>
                        <dt>Số người</dt><dd>${r.guestCount}</dd>
                        <c:if test="${canDecide}">
                            <dt>Loại bàn muốn</dt><dd><c:out value="${empty r.tableTypeRequested ? 'Không yêu cầu' : r.tableTypeRequested}" /></dd>
                        </c:if>
                        <c:if test="${canDecide or not empty r.note}">
                            <dt>Ghi chú khách</dt><dd><c:out value="${empty r.note ? '—' : r.note}" /></dd>
                        </c:if>
                        <c:if test="${canDecide and not empty r.contactedAt}">
                            <dt>Đã gọi lúc</dt><dd><c:out value="${r.contactedAtText}" /></dd>
                        </c:if>
                        <c:if test="${not empty r.staffNote}">
                            <dt>Ghi chú lễ tân</dt><dd><c:out value="${r.staffNote}" /></dd>
                        </c:if>
                        <c:if test="${not empty r.tableName}">
                            <dt>Bàn đã giữ</dt><dd><c:out value="${r.tableName}" /> (<c:out value="${r.mealShiftText}" />)</dd>
                        </c:if>
                        <c:if test="${not empty r.cancelReason}">
                            <dt>Lý do</dt><dd><c:out value="${r.cancelReason}" /></dd>
                        </c:if>
                        <c:if test="${not empty r.confirmedByName}">
                            <dt>Xử lý bởi</dt><dd><c:out value="${r.confirmedByName}" /></dd>
                        </c:if>
                    </dl>
                </section>

                <%-- Chỉ đơn đang chờ duyệt mới cần xem còn bao nhiêu bàn trống --%>
                <c:if test="${canDecide}">
                <section class="rsv-card">
                    <h2>Tình trạng bàn ngày <c:out value="${r.dateText}" /></h2>
                    <p class="rsv-hint">
                        Bàn <c:out value="${empty r.tableTypeRequested ? 'mọi loại' : r.tableTypeRequested}" />
                        đủ chỗ cho ${r.guestCount} người:
                    </p>
                    <div class="rsv-shift-summary">
                        <c:forEach var="s" items="${shiftSummaries}">
                            <a class="rsv-shift-box ${s.free gt 0 ? 'ok' : 'full'} ${s.code eq selectedShift ? 'current' : ''}"
                               href="${base}?id=${r.reservationId}&shift=${s.code}" style="text-decoration:none;color:inherit">
                                <span><c:out value="${s.name}" /></span><br>
                                <strong>${s.free}/${s.total}</strong> bàn trống
                            </a>
                        </c:forEach>
                    </div>
                    <c:forEach var="s" items="${shiftSummaries}">
                        <c:if test="${s.code eq selectedShift and s.free eq 0}">
                            <p class="rsv-alert">
                                Hết bàn <c:out value="${empty r.tableTypeRequested ? '' : r.tableTypeRequested}" /> trong
                                <c:out value="${s.name}" />. Có thể gọi hỏi khách đổi ca hoặc loại bàn. Loại khác còn trống:
                                <c:set var="anyOther" value="false" />
                                <c:forEach var="e" items="${freeByType}">
                                    <c:if test="${e.key ne r.tableTypeRequested and e.value gt 0}">
                                        <strong><c:out value="${e.key}" /></strong> (${e.value})&nbsp;
                                        <c:set var="anyOther" value="true" />
                                    </c:if>
                                </c:forEach>
                                <c:if test="${not anyOther}">không còn.</c:if>
                            </p>
                        </c:if>
                    </c:forEach>

                    <c:if test="${canDecide}">
                        <h2>Ghi nhận cuộc gọi</h2>
                        <form action="${base}" method="post">
                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                            <input type="hidden" name="action" value="contacted">
                            <input type="hidden" name="reservationId" value="${r.reservationId}">
                            <input type="hidden" name="shift" value="${selectedShift}">
                            <input class="rsv-input" style="width:100%" type="text" name="note" maxlength="500"
                                   placeholder="Kết quả cuộc gọi (khách đồng ý đổi giờ, không nghe máy...)">
                            <div class="rsv-actions"><button type="submit" class="rsv-btn secondary small">Đã gọi khách</button></div>
                        </form>
                    </c:if>
                </section>
                </c:if>
            </div>

            <c:if test="${canDecide or r.status eq 'APPROVED'}">
            <section class="rsv-card">
                <c:if test="${canDecide}">
                <h2>Sơ đồ bàn — <c:out value="${selectedShiftName}" />, <c:out value="${r.dateText}" /></h2>

                <c:if test="${tableLoadFailed}">
                    <p class="rsv-message is-error">Không tải được danh sách bàn.</p>
                </c:if>

                <form action="${base}" method="post" id="approveForm">
                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                    <input type="hidden" name="reservationId" value="${r.reservationId}">
                    <input type="hidden" name="shift" value="${selectedShift}">

                    <c:forEach var="g" items="${tableGroups}">
                        <div class="rsv-type-group">
                            <h3><c:out value="${g.key}" />
                                <c:if test="${g.key eq r.tableTypeRequested}"><span class="rsv-tag">· khách yêu cầu</span></c:if></h3>
                            <div class="rsv-seats">
                                <c:forEach var="slot" items="${g.value}">
                                    <label class="rsv-seat ${(slot.selectable and canDecide) ? '' : 'disabled'}">
                                        <input type="checkbox" name="tableId" value="${slot.table.tableId}"
                                               ${(slot.selectable and canDecide) ? '' : 'disabled'}
                                               data-cap="${slot.table.capacity}">
                                        <span>
                                            <c:out value="${slot.table.tableName}" />
                                            <small>
                                                ${slot.table.capacity} chỗ ·
                                                <c:choose>
                                                    <c:when test="${slot.serving}">đang phục vụ</c:when>
                                                <c:when test="${slot.booked}">đã có người đặt</c:when>
                                                    <c:when test="${not slot.fits}">còn trống (cần gộp bàn)</c:when>
                                                    <c:otherwise>còn trống</c:otherwise>
                                                </c:choose>
                                            </small>
                                        </span>
                                    </label>
                                </c:forEach>
                            </div>
                        </div>
                    </c:forEach>

                    <c:if test="${canDecide}">
                        <p class="rsv-hint">Ca đang chọn: <strong><c:out value="${selectedShiftName}" /></strong>
                            (đổi ca bằng các ô tóm tắt ở trên rồi chọn lại bàn).
                            <strong>Gộp bàn:</strong> tích chọn nhiều bàn khi một bàn không đủ chỗ (vd 16 khách = 2 bàn 8 chỗ).</p>
                        <p class="rsv-hint" id="capSummary" data-guests="${r.guestCount}">Đã chọn 0 bàn · 0/${r.guestCount} chỗ</p>
                        <script>
                            (function () {
                                var form = document.getElementById('approveForm');
                                var out = document.getElementById('capSummary');
                                var guests = parseInt(out.dataset.guests, 10);
                                function refresh() {
                                    var boxes = form.querySelectorAll('input[name=tableId]:checked');
                                    var cap = 0;
                                    boxes.forEach(function (b) { cap += parseInt(b.dataset.cap, 10); });
                                    out.textContent = 'Đã chọn ' + boxes.length + ' bàn · ' + cap + '/' + guests + ' chỗ'
                                            + (cap >= guests ? ' — đủ chỗ' : ' — chưa đủ, chọn thêm bàn');
                                }
                                form.addEventListener('change', refresh);
                                refresh();
                            })();
                        </script>
                        <input class="rsv-input" style="width:100%" type="text" name="note" maxlength="500"
                               placeholder="Ghi chú lễ tân (tuỳ chọn)">
                        <div class="rsv-actions">
                            <button type="submit" name="action" value="approve" class="rsv-btn">Duyệt đơn &amp; giữ bàn</button>
                        </div>
                    </c:if>
                </form>
                </c:if>

                <c:if test="${canDecide}">
                    <form action="${base}" method="post" class="rsv-actions"
                          onsubmit="return confirm('Từ chối đơn đặt bàn này?');">
                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                        <input type="hidden" name="action" value="reject">
                        <input type="hidden" name="reservationId" value="${r.reservationId}">
                        <input class="rsv-input" type="text" name="reason" maxlength="255" style="flex:1 1 360px;min-width:260px" placeholder="Lý do từ chối (hết bàn, khách yêu cầu...)">
                        <button type="submit" class="rsv-btn danger small">Từ chối đơn</button>
                    </form>
                </c:if>

                <c:if test="${r.status eq 'APPROVED'}">
                    <h2>Khi khách đến</h2>
                    <c:if test="${not r.today}">
                        <p class="rsv-hint">Chỉ check-in được vào đúng ngày đến (<c:out value="${r.dateText}" />).</p>
                    </c:if>
                    <div class="rsv-actions">
                        <form action="${base}" method="post" class="rsv-inline">
                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                            <input type="hidden" name="reservationId" value="${r.reservationId}">
                            <c:if test="${r.today}">
                                        <button type="submit" name="action" value="checkin" class="rsv-btn small"
                                                onclick="return confirm('Khách đã đến? Bàn sẽ chuyển sang đang phục vụ và đơn đặt bàn được đánh dấu hoàn thành.');">Khách đã đến (mở bàn)</button>
                                    </c:if>
                                    <button type="submit" name="action" value="noshow" class="rsv-btn secondary small"
                                            onclick="return confirm('Đánh dấu khách không đến?');">Khách không đến</button>
                                    <button type="submit" name="action" value="cancel" class="rsv-btn danger small"
                                            onclick="return confirm('Huỷ đơn đã duyệt này?');">Huỷ đơn</button>
                        </form>
                    </div>
                </c:if>
            </section>
            </c:if>
        </main>
    </body>
</html>
