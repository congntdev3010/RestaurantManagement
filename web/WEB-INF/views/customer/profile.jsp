<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Tài khoản được ProfileServlet xác thực trước khi forward tới view. --%>
<c:set var="profileUser" value="${sessionScope.user}" />
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Thông tin cá nhân - Việt À la carte</title>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/profile.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

        <main class="profile-page">
            <header class="profile-heading">
                <p class="profile-eyebrow">TÀI KHOẢN CỦA BẠN</p>
                <h1>Thông tin cá nhân</h1>
            </header>

            <%-- Thông báo flash đã được servlet chuyển từ session sang request. --%>
            <c:if test="${not empty profileMessage}">
                <p class="profile-message ${profileMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                    <c:out value="${profileMessage}" />
                </p>
            </c:if>

            <section class="profile-summary" aria-label="Thông tin tài khoản">
                <div class="profile-avatar" aria-hidden="true">
                    <i class="fa-solid fa-user"></i>
                </div>
                <div class="profile-identity">
                    <h2><c:out value="${profileUser.fullName}" /></h2>
                    <p>@<c:out value="${profileUser.username}" /></p>
                </div>
                <dl class="profile-details">
                    <div>
                        <dt>Email</dt>
                        <dd><c:out value="${profileUser.email}" /></dd>
                    </div>
                    <div>
                        <dt>Số điện thoại</dt>
                        <dd><c:out value="${profileUser.phone}" /></dd>
                    </div>
                </dl>
            </section>

            <%-- Hai form dùng chung route; action xác định thao tác cần xử lý trong servlet. --%>
            <section class="profile-editors" aria-label="Cập nhật tài khoản">
                <details class="profile-editor" ${profileOpenSection eq 'profile' ? 'open' : ''}>
                    <summary>
                        <span><i class="fa-regular fa-pen-to-square"></i> Thay đổi thông tin</span>
                        <i class="fa-solid fa-chevron-down editor-chevron" aria-hidden="true"></i>
                    </summary>
                    <form action="${pageContext.request.contextPath}/profile" method="post" class="profile-form">
                        <input type="hidden" name="action" value="updateProfile">
                        <label>
                            Họ và tên
                            <input type="text" name="fullName" maxlength="30" autocomplete="name" required
                                value="<c:out value='${profileUser.fullName}' />">
                        </label>
                        <label>
                            Số điện thoại
                            <input type="tel" name="phone" maxlength="10" pattern="0[0-9]{9}"
                                   title="Số điện thoại gồm 10 chữ số và bắt đầu bằng 0"
                                   autocomplete="tel" required value="<c:out value='${profileUser.phone}' />">
                        </label>
                        <button class="profile-submit" type="submit">Lưu thông tin</button>
                    </form>
                </details>

                <details class="profile-editor" ${profileOpenSection eq 'password' ? 'open' : ''}>
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
                            <input type="password" name="newPassword" minlength="8" maxlength="30"
                                   autocomplete="new-password" required>
                        </label>
                        <label>
                            Xác nhận mật khẩu mới
                            <input type="password" name="confirmPassword" minlength="8" maxlength="30"
                                   autocomplete="new-password" required>
                        </label>
                        <button class="profile-submit" type="submit">Cập nhật mật khẩu</button>
                    </form>
                </details>
            </section>
        </main>
    </body>
</html>