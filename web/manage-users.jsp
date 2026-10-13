<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:if test="${requestScope.users == null or requestScope.roles == null}">
    <c:redirect url="/manage-users" />
</c:if>
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
                <p><c:out value="${fn:length(users)}" /> tài khoản phù hợp</p>
            </header>

            <c:if test="${not empty manageMessage}">
            <p class="manage-message ${manageMessageType eq 'success' ? 'is-success' : 'is-error'}" role="status">
                <c:out value="${manageMessage}" />
            </p>
            </c:if>

            <c:choose>
                <c:when test="${empty editUser}">
            <details class="account-form-section account-create-section">
                <summary class="button-primary">Thêm tài khoản</summary>
                <div class="account-form-content">
                <div class="section-heading">
                    <h2 id="account-form-title">Thông tin tài khoản mới</h2>
                </div>
                    <jsp:include page="/WEB-INF/fragments/managed-user-form.jsp" />
                </div>
            </details>
                </c:when>
                <c:otherwise>
            <section class="account-form-section" aria-labelledby="account-form-title">
                <div class="section-heading">
                    <h2 id="account-form-title">Sửa tài khoản</h2>
                    <c:url var="cancelEditUrl" value="/manage-users">
                        <c:param name="roleId" value="${roleFilterId}" />
                        <c:param name="name" value="${nameQuery}" />
                    </c:url>
                    <a href="${cancelEditUrl}">Hủy chỉnh sửa</a>
                </div>
                <jsp:include page="/WEB-INF/fragments/managed-user-form.jsp" />
            </section>
                </c:otherwise>
            </c:choose>

            <section class="users-section" aria-labelledby="users-title">
                <div class="section-heading">
                    <h2 id="users-title">Danh sách tài khoản</h2>
                    <form class="user-filters" action="${pageContext.request.contextPath}/manage-users" method="get">
                        <div class="filter-field name-filter">
                            <label for="name-filter">Tìm theo tên</label>
                            <input id="name-filter" type="search" name="name" value="<c:out value='${nameQuery}' />" placeholder="Nhập họ tên">
                        </div>
                        <div class="filter-field role-filter">
                            <label for="role-filter">Vai trò</label>
                            <select id="role-filter" name="roleId" onchange="this.form.submit()">
                                <option value="0" ${roleFilterId == 0 ? 'selected' : ''}>Tất cả vai trò</option>
                                <c:forEach var="role" items="${roles}">
                                <option value="${role.key}" ${roleFilterId eq role.key ? 'selected' : ''}>
                                    <c:out value="${role.value}" />
                                </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="filter-actions">
                            <button class="filter-submit" type="submit">Tìm</button>
                            <c:if test="${not empty nameQuery}">
                                <c:url var="resetFilterUrl" value="/manage-users">
                                    <c:param name="roleId" value="${roleFilterId}" />
                                </c:url>
                                <a class="filter-reset" href="${resetFilterUrl}">Xem tất cả</a>
                            </c:if>
                        </div>
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
                            <c:forEach var="user" items="${users}">
                            <tr>
                                <td>
                                    <strong><c:out value="${user.fullName}" /></strong>
                                    <span class="table-subtext"><c:out value="${user.username}" /></span>
                                </td>
                                <td>
                                    <span><c:out value="${user.email}" /></span>
                                    <span class="table-subtext"><c:out value="${user.phone}" /></span>
                                </td>
                                <td><c:out value="${user.roleName}" /></td>
                                <td>
                                    <span class="status ${user.isActive ? 'status-active' : 'status-inactive'}">
                                        ${user.isActive ? 'Đang hoạt động' : 'Đã khóa'}
                                    </span>
                                </td>
                                <td>
                                    <div class="row-actions">
                                        <c:url var="editUserUrl" value="/manage-users">
                                            <c:param name="edit" value="${user.userId}" />
                                            <c:param name="roleId" value="${roleFilterId}" />
                                            <c:param name="name" value="${nameQuery}" />
                                        </c:url>
                                        <a class="action-link" href="${editUserUrl}">Sửa</a>
                                        <form action="${pageContext.request.contextPath}/manage-users" method="post">
                                            <input type="hidden" name="action" value="toggle">
                                            <input type="hidden" name="userId" value="${user.userId}">
                                            <input type="hidden" name="filterRoleId" value="${roleFilterId}">
                                            <input type="hidden" name="filterName" value="<c:out value='${nameQuery}' />">
                                            <button class="action-button" type="submit">${user.isActive ? 'Khóa' : 'Mở khóa'}</button>
                                        </form>
                                        <form action="${pageContext.request.contextPath}/manage-users" method="post"
                                              onsubmit="return confirm('Bạn chắc chắn muốn xóa tài khoản này?');">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="userId" value="${user.userId}">
                                            <input type="hidden" name="filterRoleId" value="${roleFilterId}">
                                            <input type="hidden" name="filterName" value="<c:out value='${nameQuery}' />">
                                            <button class="action-button action-danger" type="submit">Xóa</button>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                            </c:forEach>
                            <c:if test="${empty users}">
                            <tr><td class="empty-state" colspan="5">Không tìm thấy tài khoản phù hợp.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </body>
</html>