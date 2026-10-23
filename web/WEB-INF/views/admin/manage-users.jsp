<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý tài khoản - Việt À la carte</title>
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/manage-users.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

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
            <details class="account-form-section account-create-section" ${not empty createUserForm ? 'open' : ''}>
                <summary class="button-primary">Thêm tài khoản</summary>
                <div class="account-form-content">
                <div class="section-heading">
                    <h2 id="account-form-title">Thông tin tài khoản mới</h2>
                </div>
                    <jsp:include page="/WEB-INF/views/fragments/managed-user-form.jsp" />
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
                        <c:param name="archivedRoleId" value="${archivedRoleFilterId}" />
                        <c:param name="archivedName" value="${archivedNameQuery}" />
                    </c:url>
                    <a href="${cancelEditUrl}">Hủy chỉnh sửa</a>
                </div>
                <jsp:include page="/WEB-INF/views/fragments/managed-user-form.jsp" />
            </section>
                </c:otherwise>
            </c:choose>

            <section class="users-section" aria-labelledby="users-title">
                <div class="section-heading">
                    <h2 id="users-title">Danh sách tài khoản</h2>
                    <form class="user-filters" action="${pageContext.request.contextPath}/manage-users" method="get">
                        <input type="hidden" name="archivedName" value="<c:out value='${archivedNameQuery}' />">
                        <input type="hidden" name="archivedRoleId" value="${archivedRoleFilterId}">
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
                                    <c:param name="archivedRoleId" value="${archivedRoleFilterId}" />
                                    <c:param name="archivedName" value="${archivedNameQuery}" />
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
                                            <c:param name="archivedRoleId" value="${archivedRoleFilterId}" />
                                            <c:param name="archivedName" value="${archivedNameQuery}" />
                                        </c:url>
                                        <a class="action-link" href="${editUserUrl}">Sửa</a>
                                        <form action="${pageContext.request.contextPath}/manage-users" method="post">
                                            <input type="hidden" name="action" value="toggle">
                                            <input type="hidden" name="userId" value="${user.userId}">
                                            <input type="hidden" name="filterRoleId" value="${roleFilterId}">
                                            <input type="hidden" name="filterName" value="<c:out value='${nameQuery}' />">
                                            <input type="hidden" name="filterArchivedRoleId" value="${archivedRoleFilterId}">
                                            <input type="hidden" name="filterArchivedName" value="<c:out value='${archivedNameQuery}' />">
                                            <button class="action-button" type="submit">${user.isActive ? 'Khóa' : 'Mở khóa'}</button>
                                        </form>
                                        <form action="${pageContext.request.contextPath}/manage-users" method="post"
                                                onsubmit="return confirm('Xóa tài khoản này? Tài khoản sẽ bị vô hiệu và dữ liệu lịch sử được giữ lại.');">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="userId" value="${user.userId}">
                                            <input type="hidden" name="filterRoleId" value="${roleFilterId}">
                                            <input type="hidden" name="filterName" value="<c:out value='${nameQuery}' />">
                                            <input type="hidden" name="filterArchivedRoleId" value="${archivedRoleFilterId}">
                                            <input type="hidden" name="filterArchivedName" value="<c:out value='${archivedNameQuery}' />">
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

            <section class="users-section" aria-labelledby="archived-users-title">
                <div class="section-heading">
                    <h2 id="archived-users-title">Tài khoản đã xóa</h2>
                    <form class="user-filters" action="${pageContext.request.contextPath}/manage-users" method="get">
                        <input type="hidden" name="name" value="<c:out value='${nameQuery}' />">
                        <input type="hidden" name="roleId" value="${roleFilterId}">
                        <div class="filter-field name-filter">
                            <label for="archived-name-filter">Tìm theo tên</label>
                            <input id="archived-name-filter" type="search" name="archivedName"
                                   value="<c:out value='${archivedNameQuery}' />" placeholder="Nhập họ tên">
                        </div>
                        <div class="filter-field role-filter">
                            <label for="archived-role-filter">Vai trò</label>
                            <select id="archived-role-filter" name="archivedRoleId" onchange="this.form.submit()">
                                <option value="0" ${archivedRoleFilterId == 0 ? 'selected' : ''}>Tất cả vai trò</option>
                                <c:forEach var="role" items="${roles}">
                                <option value="${role.key}" ${archivedRoleFilterId eq role.key ? 'selected' : ''}>
                                    <c:out value="${role.value}" />
                                </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="filter-actions">
                            <button class="filter-submit" type="submit">Tìm</button>
                            <c:if test="${not empty archivedNameQuery}">
                                <c:url var="resetArchivedFilterUrl" value="/manage-users">
                                    <c:param name="roleId" value="${roleFilterId}" />
                                    <c:param name="name" value="${nameQuery}" />
                                    <c:param name="archivedRoleId" value="${archivedRoleFilterId}" />
                                </c:url>
                                <a class="filter-reset" href="${resetArchivedFilterUrl}">Xem tất cả</a>
                            </c:if>
                        </div>
                    </form>
                </div>
                <c:choose>
                    <c:when test="${empty archivedUsers}">
                        <p class="empty-state">Không có tài khoản đã xóa.</p>
                    </c:when>
                    <c:otherwise>
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
                                    <c:forEach var="user" items="${archivedUsers}">
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
                                        <td><span class="status status-inactive">Đã xóa</span></td>
                                        <td>
                                            <form action="${pageContext.request.contextPath}/manage-users" method="post"
                                                  onsubmit="return confirm('Khôi phục tài khoản này ở trạng thái khóa?');">
                                                <input type="hidden" name="action" value="restore">
                                                <input type="hidden" name="userId" value="${user.userId}">
                                                <input type="hidden" name="filterRoleId" value="${roleFilterId}">
                                                <input type="hidden" name="filterName" value="<c:out value='${nameQuery}' />">
                                                <input type="hidden" name="filterArchivedRoleId" value="${archivedRoleFilterId}">
                                                <input type="hidden" name="filterArchivedName" value="<c:out value='${archivedNameQuery}' />">
                                                <button class="action-button" type="submit">Khôi phục</button>
                                            </form>
                                        </td>
                                    </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </body>
</html>