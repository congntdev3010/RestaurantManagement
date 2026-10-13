<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="model.RestaurantTable" %>
<%@ page import="model.User" %>
<%!
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String statusLabel(String status) {
        if ("AVAILABLE".equals(status)) return "Còn trống";
        if ("RESERVED".equals(status)) return "Đã đặt";
        if ("OCCUPIED".equals(status)) return "Đang phục vụ";
        return "Tạm ngưng";
    }
%>
<%
    User currentUser = (User) session.getAttribute("user");
    List<RestaurantTable> tables = (List<RestaurantTable>) request.getAttribute("tables");
    Boolean canManage = (Boolean) request.getAttribute("canManage");
    Boolean canChangeStatus = (Boolean) request.getAttribute("canChangeStatus");
    RestaurantTable editTable = (RestaurantTable) request.getAttribute("editTable");
    String tableMessage = (String) request.getAttribute("tableMessage");
    String tableMessageType = (String) request.getAttribute("tableMessageType");
    if (currentUser == null || tables == null || canManage == null || canChangeStatus == null) {
        response.sendRedirect(request.getContextPath() + "/manage-tables");
        return;
    }
    String formAction = editTable == null ? "create" : "update";
%>
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
                <p class="table-count"><strong><%= tables.size() %></strong> bàn</p>
            </header>

            <% if (tableMessage != null) { %>
            <p class="table-message <%= "success".equals(tableMessageType) ? "is-success" : "is-error" %>" role="status">
                <%= escapeHtml(tableMessage) %>
            </p>
            <% } %>

            <% if (Boolean.TRUE.equals(canManage)) { %>
            <section class="table-form-section" aria-labelledby="table-form-title">
                <div class="section-heading">
                    <h2 id="table-form-title"><%= editTable == null ? "Thêm bàn" : "Cập nhật bàn" %></h2>
                    <% if (editTable != null) { %>
                    <a href="${pageContext.request.contextPath}/manage-tables">Hủy chỉnh sửa</a>
                    <% } %>
                </div>
                <form class="table-form" action="${pageContext.request.contextPath}/manage-tables" method="post">
                    <input type="hidden" name="action" value="<%= formAction %>">
                    <% if (editTable != null) { %>
                    <input type="hidden" name="tableId" value="<%= editTable.getTableId() %>">
                    <% } %>
                    <label>
                        Mã bàn
                        <input type="text" name="tableName" maxlength="50" required
                               placeholder="Ví dụ: Bàn 01"
                               value="<%= editTable == null ? "" : escapeHtml(editTable.getTableName()) %>">
                    </label>
                    <label>
                        Loại bàn
                        <input type="text" name="tableType" maxlength="50" list="table-types" required
                               placeholder="Ví dụ: Cửa sổ"
                               value="<%= editTable == null ? "" : escapeHtml(editTable.getTableType()) %>">
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
                               value="<%= editTable == null ? "2" : editTable.getCapacity() %>">
                    </label>
                    <% if (editTable != null) { %>
                    <label>
                        Trạng thái
                        <select name="status" required>
                            <option value="AVAILABLE" <%= "AVAILABLE".equals(editTable.getStatus()) ? "selected" : "" %>>Còn trống</option>
                            <option value="RESERVED" <%= "RESERVED".equals(editTable.getStatus()) ? "selected" : "" %>>Đã đặt</option>
                            <option value="OCCUPIED" <%= "OCCUPIED".equals(editTable.getStatus()) ? "selected" : "" %>>Đang phục vụ</option>
                            <option value="UNAVAILABLE" <%= "UNAVAILABLE".equals(editTable.getStatus()) ? "selected" : "" %>>Tạm ngưng</option>
                        </select>
                    </label>
                    <% } %>
                    <label class="description-field">
                        Ghi chú
                        <input type="text" name="description" maxlength="255"
                               placeholder="Vị trí hoặc đặc điểm của bàn"
                               value="<%= editTable == null ? "" : escapeHtml(editTable.getDescription()) %>">
                    </label>
                    <div class="form-actions">
                        <button class="button-primary" type="submit"><%= editTable == null ? "Thêm bàn" : "Lưu thay đổi" %></button>
                        <% if (editTable != null) { %>
                        <a class="button-secondary" href="${pageContext.request.contextPath}/manage-tables">Hủy</a>
                        <% } %>
                    </div>
                </form>
            </section>
            <% } %>

            <section class="table-layout" aria-label="Bàn theo khu vực">
                <% if (tables.isEmpty()) { %>
                <p class="empty-state">Chưa có bàn nào được thiết lập.</p>
                <% } else {
                    String currentType = null;
                    for (RestaurantTable table : tables) {
                        if (!table.getTableType().equals(currentType)) {
                            if (currentType != null) { %>
                </div>
            </section>
                            <% }
                            currentType = table.getTableType(); %>
            <section class="table-group">
                <header class="group-heading">
                    <h2><%= escapeHtml(currentType) %></h2>
                    <span><% int typeCount = 0; for (RestaurantTable item : tables) {
                        if (currentType.equals(item.getTableType())) typeCount++;
                    } %><%= typeCount %> bàn</span>
                </header>
                <div class="table-grid">
                        <% } %>
                    <article class="table-card status-<%= table.getStatus().toLowerCase() %>">
                        <div class="card-topline">
                            <span class="table-icon"><i class="fa-solid fa-table-cells-large" aria-hidden="true"></i></span>
                            <span class="table-status"><%= statusLabel(table.getStatus()) %></span>
                        </div>
                        <h3><%= escapeHtml(table.getTableName()) %></h3>
                        <p class="capacity"><%= table.getCapacity() %> khách</p>
                        <% if (table.getDescription() != null && !table.getDescription().isEmpty()) { %>
                        <p class="table-description"><%= escapeHtml(table.getDescription()) %></p>
                        <% } %>
                        <% if (Boolean.TRUE.equals(canManage)) { %>
                        <div class="table-actions">
                            <a href="${pageContext.request.contextPath}/manage-tables?edit=<%= table.getTableId() %>">Sửa</a>
                            <form action="${pageContext.request.contextPath}/manage-tables" method="post"
                                  onsubmit="return confirm('Bạn chắc chắn muốn xóa bàn này?');">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="tableId" value="<%= table.getTableId() %>">
                                <button type="submit">Xóa</button>
                            </form>
                        </div>
                        <% } else if (Boolean.TRUE.equals(canChangeStatus)) { %>
                        <form class="table-status-form" action="${pageContext.request.contextPath}/manage-tables" method="post">
                            <input type="hidden" name="action" value="changeStatus">
                            <input type="hidden" name="tableId" value="<%= table.getTableId() %>">
                            <label>
                                Trạng thái
                                <select name="status" required>
                                    <option value="AVAILABLE" <%= "AVAILABLE".equals(table.getStatus()) ? "selected" : "" %>>Còn trống</option>
                                    <option value="RESERVED" <%= "RESERVED".equals(table.getStatus()) ? "selected" : "" %>>Đã đặt</option>
                                    <option value="OCCUPIED" <%= "OCCUPIED".equals(table.getStatus()) ? "selected" : "" %>>Đang phục vụ</option>
                                    <option value="UNAVAILABLE" <%= "UNAVAILABLE".equals(table.getStatus()) ? "selected" : "" %>>Tạm ngưng</option>
                                </select>
                            </label>
                            <button type="submit">Cập nhật</button>
                        </form>
                        <% } %>
                    </article>
                    <% }
                    if (currentType != null) { %>
                </div>
            </section>
                    <% }
                } %>
            </section>
        </main>
    </body>
</html>
