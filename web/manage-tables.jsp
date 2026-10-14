<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:if test="${requestScope.tableGroups == null or requestScope.canManage == null or requestScope.canChangeStatus == null}">
    <c:redirect url="/manage-tables" />
</c:if>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Sơ đồ bàn - Việt À la carte</title>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/manage-tables.css">
    </head>
    <body>
        <jsp:include page="includes/header.jsp" />
        <main class="tables-page">
            <header class="tables-heading">
                <div>
                    <p class="tables-eyebrow">VẬN HÀNH NHÀ HÀNG</p>
                    <h1>Sơ đồ bàn</h1>
                    <p class="tables-intro">Danh sách bàn được sắp xếp theo từng khu vực.</p>
                </div>
                <c:if test="${not tableLoadFailed}">
                    <p class="table-count"><strong><c:out value="${tableCount}" /></strong> bàn</p>
                </c:if>
            </header>

            <c:if test="${not empty tableMessage}">
            <p class="table-message ${tableMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                <c:out value="${tableMessage}" />
            </p>
            </c:if>

            <c:if test="${canManage}">
            <section class="table-form-section" aria-label="Thao tác quản lý bàn">
                <details class="table-form-disclosure" ${not empty editTable ? 'open' : ''}>
                    <summary class="button-primary table-form-toggle">
                        <i class="fa-solid ${empty editTable ? 'fa-plus' : 'fa-pen-to-square'}" aria-hidden="true"></i>
                        ${empty editTable ? 'Thêm bàn' : 'Đang sửa bàn'}
                        <i class="fa-solid fa-chevron-down table-form-chevron" aria-hidden="true"></i>
                    </summary>
                    <div class="table-form-content">
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
                                <c:if test="${not empty editTable}">
                                    <a class="button-secondary" href="${pageContext.request.contextPath}/manage-tables">Hủy</a>
                                </c:if>
                            </div>
                        </form>
                    </div>
                </details>
            </section>
            </c:if>

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
                        <h3><c:out value="${table.tableName}" /></h3>
                        <p class="capacity"><c:out value="${table.capacity}" /> khách</p>
                        <c:if test="${not empty table.description}">
                            <p class="table-description"><c:out value="${table.description}" /></p>
                        </c:if>
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
    </body>
</html>
