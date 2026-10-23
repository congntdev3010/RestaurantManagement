<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Sơ đồ bàn - Việt À la carte</title>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/manage-tables.css?v=1.2">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
        <main class="tables-page">
            <%-- Tổng quan và thông báo flash do servlet chuẩn bị trước khi forward. --%>
                        <header class="tables-heading">
                <div>
                    <p class="tables-eyebrow">VẬN HÀNH NHÀ HÀNG</p>
                    <h1>Sơ đồ bàn</h1>
                    <p class="tables-intro">Danh sách bàn được sắp xếp theo từng khu vực.</p>
                </div>
                <div class="tables-heading-actions">
                    <c:if test="${not tableLoadFailed}">
                        <p class="table-count"><strong><c:out value="${tableCount}" /></strong> bàn</p>
                    </c:if>
                </div>
            </header>
            <c:if test="${not tableLoadFailed and (canChangeStatus or canManage)}">
                <form class="table-payment-filter" method="get" action="${pageContext.request.contextPath}/manage-tables">
                    <label for="paymentFilter">Bàn chưa thanh toán</label>
                    <select id="paymentFilter" name="paymentFilter" onchange="this.form.submit()">
                        <option value="ALL" ${paymentFilter eq 'ALL' ? 'selected' : ''}>Tất cả bàn</option>
                        <option value="UNPAID" ${paymentFilter eq 'UNPAID' ? 'selected' : ''}>Chỉ bàn chưa thanh toán</option>
                    </select>
                    <strong><c:out value="${unpaidTableCount}" /></strong> bàn
                </form>
            </c:if>

            <c:if test="${not empty tableMessage}">
                <p class="table-message ${tableMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                    <c:out value="${tableMessage}" />
                </p>
            </c:if>

            <%-- Chỉ Manager thấy form tạo/sửa; servlet vẫn kiểm tra quyền khi nhận POST. --%>
            <c:if test="${canManage}">
                <section class="table-form-section" aria-label="Thao tác quản lý bàn">
                    <details class="table-form-disclosure" ${not empty editTable ? 'open' : ''}>
                        <summary class="button-primary table-form-toggle">
                            <i class="fa-solid ${empty editTable ? 'fa-plus' : 'fa-pen-to-square'}" aria-hidden="true"></i>
                            ${empty editTable ? 'Thêm bàn' : 'Đang sửa bàn'}
                            <i class="fa-solid fa-chevron-down table-form-chevron" aria-hidden="true"></i>
                        </summary>
                        <div class="table-form-content">
                            <%-- Khi sửa, nạp ID và trạng thái hiện tại; khi tạo, trạng thái mặc định do servlet gán. --%>
                            <c:if test="${not empty editTable}">
                                <div class="section-heading">
                                    <h2>Cập nhật bàn</h2>
                                    <a href="${pageContext.request.contextPath}/manage-tables">Hủy chỉnh sửa</a>
                                </div>
                            </c:if>
                            <form class="table-form" action="${pageContext.request.contextPath}/manage-tables" method="post">
                                <input type="hidden" name="action" value="${empty editTable ? 'create' : 'update'}">
                                <c:if test="${not empty editTable}">
                                    <input type="hidden" name="tableId" value="${editTable.tableId}">
                                </c:if>
                                <label>
                                    Mã bàn
                                    <input type="text" name="tableName" maxlength="50" required placeholder="Ví dụ: Bàn 01"
                                           value="${fn:escapeXml(editTable.tableName)}">
                                </label>
                                <label>
                                    Loại bàn
                                    <input type="text" name="tableType" maxlength="50" list="table-types" required
                                           placeholder="Ví dụ: Cửa sổ" value="${fn:escapeXml(editTable.tableType)}">
                                    <datalist id="table-types">
                                        <option value="Phòng riêng">
                                        <option value="Cửa sổ">
                                        <option value="Trong nhà">
                                        <option value="Ngoài trời">
                                    </datalist>
                                </label>
                                <label>
                                    Sức chứa
                                    <input type="number" name="capacity" min="1" max="30" required
                                           value="${not empty editTable ? editTable.capacity : 2}">
                                </label>
                                <c:if test="${not empty editTable}">
                                    <label>
                                        Trạng thái
                                        <select name="status" required>
                                            <option value="AVAILABLE" ${editTable.status eq 'AVAILABLE' ? 'selected' : ''}>Còn trống</option>
                                            <option value="RESERVED" ${editTable.status eq 'RESERVED' ? 'selected' : ''}>Đã đặt</option>
                                            <option value="OCCUPIED" ${editTable.status eq 'OCCUPIED' ? 'selected' : ''}>Đang phục vụ</option>
                                        </select>
                                    </label>
                                </c:if>
                                <label class="description-field">
                                    Ghi chú
                                    <input type="text" name="description" maxlength="255" placeholder="Vị trí hoặc đặc điểm của bàn"
                                           value="${fn:escapeXml(editTable.description)}">
                                </label>
                                <div class="form-actions">
                                    <button class="button-primary" type="submit">${empty editTable ? 'Thêm bàn' : 'Lưu thay đổi'}</button>
                                </div>
                            </form>
                        </div>
                    </details>
                </section>
            </c:if>

            <%-- Hiển thị lỗi tải dữ liệu, trạng thái rỗng hoặc danh sách bàn theo khu vực. --%>
            <section class="table-layout" aria-label="Bàn theo khu vực">
                <c:choose>
                    <c:when test="${tableLoadFailed}">
                        <p class="empty-state" role="alert">Không thể tải sơ đồ bàn do lỗi kết nối dữ liệu. Vui lòng thử lại sau.</p>
                    </c:when>
                    <c:when test="${empty tableGroups}">
                        <p class="empty-state">Chưa có bàn nào được thiết lập.</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="group" items="${tableGroups}">
                            <section class="table-group">
                                <header class="group-heading">
                                    <h2><c:out value="${group.key}" /></h2>
                                    <span><c:out value="${fn:length(group.value)}" /> bàn</span>
                                </header>
                                <div class="table-grid">
                                    <%-- Mỗi thẻ bàn hiển thị thông tin, trạng thái và thao tác theo quyền. --%>
                            <c:forEach var="table" items="${group.value}">
                                <article class="table-card status-${fn:escapeXml(fn:toLowerCase(table.status))}">
                                    <div class="card-topline">
                                        <span class="table-icon"><i class="fa-solid fa-table-cells-large" aria-hidden="true"></i></span>
                                        <span class="table-status">
                                            <c:choose>
                                                <c:when test="${table.status eq 'AVAILABLE'}">Còn trống</c:when>
                                                <c:when test="${table.status eq 'RESERVED'}">Đã đặt</c:when>
                                                <c:when test="${table.status eq 'OCCUPIED'}">Đang phục vụ</c:when>
                                                <c:otherwise>Đang phục vụ</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                    <div class="table-name-row">
                                        <h3><c:out value="${table.tableName}" /></h3>
                                    </div>
                                    <p class="capacity"><c:out value="${table.capacity}" /> khách</p>
                                    <c:if test="${not empty table.description}">
                                        <p class="table-description"><c:out value="${table.description}" /></p>
                                    </c:if>
                                    <div class="table-actions">
                                        <c:if test="${not empty tableBills[table.tableId] and (sessionScope.user.roleName eq 'Receptionist' or sessionScope.user.roleName eq 'Manager')}">
                                            <a href="${pageContext.request.contextPath}/payment?tableId=${table.tableId}">
                                                <i class="fa-solid fa-cash-register" aria-hidden="true"></i> Thanh toán
                                            </a>
                                        </c:if>
                                        <c:if test="${canOrder and table.status ne 'UNAVAILABLE'}">
                                            <a href="${pageContext.request.contextPath}/manage-orders?action=new&tableId=${table.tableId}">Order</a>
                                        </c:if>
                                        <c:if test="${not empty table.qrToken}">
                                            <a href="#" class="qr-open" data-name="${fn:escapeXml(table.tableName)}" data-token="${fn:escapeXml(table.qrToken)}">
                                                <i class="fa-solid fa-qrcode" aria-hidden="true"></i> QR bàn
                                            </a>
                                        </c:if>
                                    </div>
                                    <%-- Manager quản lý thông tin bàn; Server/Receptionist chỉ đổi trạng thái. --%>
                                    <c:choose>
                                        <c:when test="${canManage}">
                                            <div class="table-actions">
                                                <a href="${pageContext.request.contextPath}/manage-tables?edit=${table.tableId}">Sửa</a>
                                                <form action="${pageContext.request.contextPath}/manage-tables" method="post"
                                                      onsubmit="return confirm('Bạn chắc chắn muốn xóa bàn này?');">
                                                    <input type="hidden" name="action" value="delete">
                                                    <input type="hidden" name="tableId" value="${table.tableId}">
                                                    <button type="submit">Xóa</button>
                                                </form>
                                            </div>
                                        </c:when>
                                        <c:when test="${canChangeStatus}">
                                            <form class="table-status-form" action="${pageContext.request.contextPath}/manage-tables" method="post">
                                                <input type="hidden" name="action" value="changeStatus">
                                                <input type="hidden" name="tableId" value="${table.tableId}">
                                                <label>
                                                    Trạng thái
                                                    <select name="status" required>
                                                        <option value="AVAILABLE" ${table.status eq 'AVAILABLE' ? 'selected' : ''}>Còn trống</option>
                                                        <option value="RESERVED" ${table.status eq 'RESERVED' ? 'selected' : ''}>Đã đặt</option>
                                                        <option value="OCCUPIED" ${table.status eq 'OCCUPIED' ? 'selected' : ''}>Đang phục vụ</option>
                                                    </select>
                                                </label>
                                                <button type="submit">Cập nhật</button>
                                            </form>
                                        </c:when>
                                    </c:choose>
                                </article>
                            </c:forEach>
                                </div>
                            </section>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    <c:set var="qrDefaultBase" value="${pageContext.request.scheme}://${pageContext.request.serverName}${(pageContext.request.serverPort == 80 or pageContext.request.serverPort == 443) ? '' : ':'.concat(pageContext.request.serverPort)}${pageContext.request.contextPath}" />
        <c:set var="qrBase" value="${not empty fn:trim(initParam.qrBaseUrl) ? fn:trim(initParam.qrBaseUrl) : qrDefaultBase}" />
        <dialog id="qrDialog" data-base="${fn:escapeXml(qrBase)}" style="border:0;border-radius:12px;padding:20px;text-align:center;max-width:340px;width:90%">
            <h3 id="qrTitle" style="margin:0 0 10px"></h3>
            <img id="qrImg" alt="QR bàn" width="260" height="260">
            <p style="margin:8px 0 0">Quét để gọi món</p>
            <small id="qrUrl" style="display:block;word-break:break-all;color:#777;font-size:.7rem;margin:6px 0 12px"></small>
            <button type="button" id="qrPrint">In</button>
            <button type="button" id="qrClose">Đóng</button>
        </dialog>
        <script src="${pageContext.request.contextPath}/assets/js/qrcode.min.js"></script>
        <script>
            (function () {
                var dlg = document.getElementById('qrDialog');
                var base = dlg.getAttribute('data-base').replace(/\/+$/, '');
                var title = document.getElementById('qrTitle');
                var img = document.getElementById('qrImg');
                var preloaded = [];
                document.querySelectorAll('.qr-open').forEach(function (a) {
                    var url = base + '/order?table=' + encodeURIComponent(a.dataset.token);
                    var qr = qrcode(0, 'M'); qr.addData(url); qr.make();
                    var qrSrc = qr.createDataURL(8, 2); // sinh QR ngay trên trình duyệt, không cần mạng
                    // Tải sẵn ảnh QR ngay khi mở trang để popup hiện tức thì.
                    var pre = new Image();
                    pre.src = qrSrc;
                    preloaded.push(pre);
                    a.addEventListener('click', function (e) {
                        e.preventDefault();
                        title.textContent = a.dataset.name;
                        document.getElementById('qrUrl').textContent = url;
                        img.src = qrSrc;
                        dlg.showModal();
                    });
                });
                document.getElementById('qrClose').addEventListener('click', function () { dlg.close(); });
                var downOnBackdrop = false;
                dlg.addEventListener('mousedown', function (e) { downOnBackdrop = (e.target === dlg); });
                dlg.addEventListener('click', function (e) {
                    if (e.target === dlg && downOnBackdrop) dlg.close();
                    downOnBackdrop = false;
                });
                document.getElementById('qrPrint').addEventListener('click', function () {
                    var w = window.open('', '_blank', 'width=400,height=520');
                    var h = document.createElement('h2');
                    h.textContent = title.textContent;
                    w.document.write('<body style="text-align:center;font-family:sans-serif">' + h.outerHTML
                        + '<img src="' + img.src + '" width="300"><p>Quét để gọi món</p></body>');
                    w.document.close();
                    w.onload = function () { w.print(); };
                });
            })();
        </script>
    </body>
</html>
