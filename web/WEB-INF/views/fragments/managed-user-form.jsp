<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="formUser" value="${not empty editUser ? editUser : createUserForm}" />
<form class="account-form" action="${pageContext.request.contextPath}/manage-users" method="post">
    <input type="hidden" name="action" value="${empty editUser ? 'create' : 'update'}">
    <input type="hidden" name="filterRoleId" value="${roleFilterId}">
    <input type="hidden" name="filterName" value="<c:out value='${nameQuery}' />">
    <c:if test="${not empty editUser}">
        <input type="hidden" name="userId" value="${editUser.userId}">
    </c:if>
    <label>
        Tên đăng nhập
        <input type="text" name="username" maxlength="30" required value="<c:out value='${formUser.username}' />">
    </label>
    <label>
        Họ và tên
        <input type="text" name="fullName" maxlength="30" required value="<c:out value='${formUser.fullName}' />">
    </label>
    <label>
        Email
        <input type="email" name="email" maxlength="50" required value="<c:out value='${formUser.email}' />">
    </label>
    <label>
        Số điện thoại
        <input type="tel" name="phone" maxlength="10" pattern="0[0-9]{9}"
               title="Số điện thoại gồm 10 chữ số và bắt đầu bằng 0" required
               value="<c:out value='${formUser.phone}' />">
    </label>
    <c:choose>
        <c:when test="${not empty editUser and editUser.roleName eq 'Admin'}">
            <input type="hidden" name="roleId" value="${editUser.roleId}">
            <label>
                Vai trò
                <input type="text" value="Admin" readonly>
            </label>
        </c:when>
        <c:otherwise>
            <label>
                Vai trò
                <select name="roleId" required>
                    <option value="">Chọn vai trò</option>
                    <c:forEach var="role" items="${roles}">
                        <option value="${role.key}" ${formUser.roleId eq role.key ? 'selected' : ''}>
                            <c:out value="${role.value}" />
                        </option>
                    </c:forEach>
                </select>
            </label>
        </c:otherwise>
    </c:choose>
    <label>
        ${empty editUser ? 'Mật khẩu khởi tạo' : 'Mật khẩu mới (để trống nếu không đổi)'}
        <input type="password" name="password" minlength="8" maxlength="30"
               autocomplete="new-password" ${empty editUser ? 'required' : ''}>
    </label>
    <div class="form-actions">
        <button class="button-primary" type="submit">${empty editUser ? 'Tạo tài khoản' : 'Lưu thay đổi'}</button>
    </div>
</form>