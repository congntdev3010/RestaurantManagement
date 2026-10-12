<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
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
%>
<%
    List<User> users = (List<User>) request.getAttribute("users");
    Map<Integer, String> roles = (Map<Integer, String>) request.getAttribute("roles");
    User editUser = (User) request.getAttribute("editUser");
    String manageMessage = (String) request.getAttribute("manageMessage");
    String manageMessageType = (String) request.getAttribute("manageMessageType");
    Integer selectedRoleId = (Integer) request.getAttribute("selectedRoleId");
    String searchName = (String) request.getAttribute("searchName");
    String selectedStatus = (String) request.getAttribute("selectedStatus");
    if (users == null || roles == null) {
        response.sendRedirect(request.getContextPath() + "/manage-users");
        return;
    }
    String formAction = editUser == null ? "create" : "update";
    boolean isAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý tài khoản - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/manage-users.css">
</head>
<body>
    <jsp:include page="includes/header.jsp" />

    <main class="manage-page">
        <header class="manage-heading">
            <div>
                <p class="manage-eyebrow">QUẢN TRỊ HỆ THỐNG</p>
                <h1>Quản lý tài khoản</h1>
            </div>
            <p><%= users.size() %> tài khoản trong phạm vi quản lý</p>
        </header>

        <% if (manageMessage != null) { %>
            <p class="manage-message <%= "success".equals(manageMessageType) ? "is-success" : "is-error" %>" role="status">
                <%= escapeHtml(manageMessage) %>
            </p>
        <% } %>

        <section class="account-form-section" aria-labelledby="account-form-title">
            <div class="section-heading">
                <h2 id="account-form-title"><%= editUser == null ? "Thêm tài khoản" : "Sửa tài khoản" %></h2>
                <% if (editUser != null) { %>
                    <a href="${pageContext.request.contextPath}/manage-users">Hủy chỉnh sửa</a>
                <% } %>
            </div>

            <form class="account-form" action="${pageContext.request.contextPath}/manage-users" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="<%= formAction %>">
                <% if (editUser != null) { %>
                    <input type="hidden" name="userId" value="<%= editUser.getUserId() %>">
                <% } %>
                <label>
                    Tên đăng nhập
                    <input type="text" name="username" maxlength="50" required
                           value="<%= editUser == null ? "" : escapeHtml(editUser.getUsername()) %>">
                </label>
                <label>
                    Họ và tên
                    <input type="text" name="fullName" maxlength="100" required
                           value="<%= editUser == null ? "" : escapeHtml(editUser.getFullName()) %>">
                </label>
                <label>
                    Email
                    <input type="email" name="email" maxlength="100" required
                           value="<%= editUser == null ? "" : escapeHtml(editUser.getEmail()) %>">
                </label>
                <label>
                    Số điện thoại
                    <input type="tel" name="phone" maxlength="10" pattern="0[0-9]{9}"
                           title="Số điện thoại gồm 10 chữ số và bắt đầu bằng 0" required
                           value="<%= editUser == null ? "" : escapeHtml(editUser.getPhone()) %>">
                </label>
                <label>
                    Vai trò
                    <select name="roleId" required>
                        <option value="">Chọn vai trò</option>
                        <% for (Map.Entry<Integer, String> role : roles.entrySet()) { %>
                            <option value="<%= role.getKey() %>" <%= editUser != null && editUser.getRoleId() == role.getKey() ? "selected" : "" %>>
                                <%= escapeHtml(role.getValue()) %>
                            </option>
                        <% } %>
                    </select>
                </label>
                <label>
                    <%= editUser == null ? "Mật khẩu khởi tạo" : "Mật khẩu mới (để trống nếu không đổi)" %>
                    <input type="password" name="password" minlength="8" maxlength="255"
                           autocomplete="new-password" <%= editUser == null ? "required" : "" %>>
                </label>
                <div class="form-actions">
                    <button class="button-primary" type="submit"><%= editUser == null ? "Tạo tài khoản" : "Lưu thay đổi" %></button>
                    <% if (editUser != null) { %>
                        <a class="button-secondary" href="${pageContext.request.contextPath}/manage-users">Hủy</a>
                    <% } %>
                </div>
            </form>
        </section>

        <section class="users-section" aria-labelledby="users-title">
            <div class="section-heading">
                <h2 id="users-title">Danh sách tài khoản</h2>
                <form class="user-filters" action="${pageContext.request.contextPath}/manage-users" method="get">
                    <label for="name-filter">Tên</label>
                    <input id="name-filter" type="search" name="name" maxlength="100"
                           placeholder="Tìm theo họ tên" value="<%= escapeHtml(searchName) %>">
                    <label for="role-filter">Vai trò</label>
                    <select id="role-filter" name="roleId">
                        <option value="">Tất cả vai trò</option>
                        <% for (Map.Entry<Integer, String> role : roles.entrySet()) { %>
                            <option value="<%= role.getKey() %>" <%= selectedRoleId != null && selectedRoleId.equals(role.getKey()) ? "selected" : "" %>>
                                <%= escapeHtml(role.getValue()) %>
                            </option>
                        <% } %>
                    </select>
                        <label for="status-filter">Trạng thái</label>
                        <select id="status-filter" name="status">
                            <option value="" <%= selectedStatus == null || selectedStatus.isEmpty() ? "selected" : "" %>>Tất cả trạng thái</option>
                            <option value="active" <%= "active".equals(selectedStatus) ? "selected" : "" %>>Đang hoạt động</option>
                            <option value="inactive" <%= "inactive".equals(selectedStatus) ? "selected" : "" %>>Đã khóa</option>
                        </select>
                    <button type="submit">Lọc</button>
                </form>
            </div>

            <div class="table-scroll">
                <table class="users-table">
                    <thead>
                        <tr>
                            <th>Tài khoản</th>
                            <th>Liên hệ</th>
                            <th>Vai trò</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (User user : users) { %>
                            <tr>
                                <td>
                                    <strong><%= escapeHtml(user.getFullName()) %></strong>
                                    <span class="table-subtext"><%= escapeHtml(user.getUsername()) %></span>
                                </td>
                                <td>
                                    <span><%= escapeHtml(user.getEmail()) %></span>
                                    <span class="table-subtext"><%= escapeHtml(user.getPhone()) %></span>
                                </td>
                                <td><%= escapeHtml(user.getRoleName()) %></td>
                                <td>
                                    <span class="status <%= user.isIsActive() ? "status-active" : "status-inactive" %>">
                                        <%= user.isIsActive() ? "Đang hoạt động" : "Đã khóa" %>
                                    </span>
                                </td>
                                <td>
                                    <div class="row-actions">
                                        <a class="action-link" href="${pageContext.request.contextPath}/manage-users?edit=<%= user.getUserId() %>">Sửa</a>
                                        <form action="${pageContext.request.contextPath}/manage-users" method="post">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="action" value="toggle">
                                            <input type="hidden" name="userId" value="<%= user.getUserId() %>">
                                            <button class="action-button" type="submit"><%= user.isIsActive() ? "Khóa" : "Mở khóa" %></button>
                                        </form>
                                        <form action="${pageContext.request.contextPath}/manage-users" method="post"
                                              onsubmit="return confirm('Bạn chắc chắn muốn xóa tài khoản này?');">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="userId" value="<%= user.getUserId() %>">
                                            <button class="action-button action-danger" type="submit">Xóa</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                        <% } %>
                        <% if (users.isEmpty()) { %>
                            <tr><td class="empty-state" colspan="5">Chưa có tài khoản nào trong phạm vi quản lý.</td></tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</body>
</html>