<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.User" %>
<%
    User user = (User) session.getAttribute("user");
    String profileMessage = (String) request.getAttribute("profileMessage");
    String profileMessageType = (String) request.getAttribute("profileMessageType");
    String openSection = (String) request.getAttribute("profileOpenSection");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/profile");
        return;
    }
%>
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
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin cá nhân - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/profile.css">
</head>
<body>
    <jsp:include page="includes/header.jsp" />

    <main class="profile-page">
        <header class="profile-heading">
            <p class="profile-eyebrow">TÀI KHOẢN CỦA BẠN</p>
            <h1>Thông tin cá nhân</h1>
        </header>

        <% if (profileMessage != null) { %>
            <p class="profile-message <%= "success".equals(profileMessageType) ? "is-success" : "is-error" %>" role="status">
                <%= escapeHtml(profileMessage) %>
            </p>
        <% } %>

        <section class="profile-summary" aria-label="Thông tin tài khoản">
            <div class="profile-avatar" aria-hidden="true">
                <i class="fa-solid fa-user"></i>
            </div>
            <div class="profile-identity">
                <h2><%= escapeHtml(user.getFullName()) %></h2>
                <p>@<%= escapeHtml(user.getUsername()) %></p>
            </div>
            <dl class="profile-details">
                <div>
                    <dt>Email</dt>
                    <dd><%= escapeHtml(user.getEmail()) %></dd>
                </div>
                <div>
                    <dt>Số điện thoại</dt>
                    <dd><%= escapeHtml(user.getPhone()) %></dd>
                </div>
            </dl>
        </section>

        <section class="profile-editors" aria-label="Cập nhật tài khoản">
            <details class="profile-editor" <%= "profile".equals(openSection) ? "open" : "" %>>
                <summary>
                    <span><i class="fa-regular fa-pen-to-square"></i> Thay đổi thông tin</span>
                    <i class="fa-solid fa-chevron-down editor-chevron" aria-hidden="true"></i>
                </summary>
                <form action="${pageContext.request.contextPath}/profile" method="post" class="profile-form">
                    <input type="hidden" name="action" value="updateProfile">
                    <label>
                        Họ và tên
                        <input type="text" name="fullName" maxlength="100" autocomplete="name" required
                               value="<%= escapeHtml(user.getFullName()) %>">
                    </label>
                    <label>
                        Số điện thoại
                        <input type="tel" name="phone" maxlength="10" pattern="0[0-9]{9}"
                               title="Số điện thoại gồm 10 chữ số và bắt đầu bằng 0"
                               autocomplete="tel" required value="<%= escapeHtml(user.getPhone()) %>">
                    </label>
                    <button class="profile-submit" type="submit">Lưu thông tin</button>
                </form>
            </details>

            <details class="profile-editor" <%= "password".equals(openSection) ? "open" : "" %>>
                <summary>
                    <span><i class="fa-solid fa-lock"></i> Đổi mật khẩu</span>
                    <i class="fa-solid fa-chevron-down editor-chevron" aria-hidden="true"></i>
                </summary>
                <form action="${pageContext.request.contextPath}/profile" method="post" class="profile-form password-form">
                    <input type="hidden" name="action" value="changePassword">
                    <label>
                        Mật khẩu hiện tại
                        <input type="password" name="currentPassword" autocomplete="current-password" required>
                    </label>
                    <label>
                        Mật khẩu mới
                        <input type="password" name="newPassword" minlength="8" maxlength="255"
                               autocomplete="new-password" required>
                    </label>
                    <label>
                        Xác nhận mật khẩu mới
                        <input type="password" name="confirmPassword" minlength="8" maxlength="255"
                               autocomplete="new-password" required>
                    </label>
                    <button class="profile-submit" type="submit">Cập nhật mật khẩu</button>
                </form>
            </details>
        </section>
    </main>
</body>
</html>