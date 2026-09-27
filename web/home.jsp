<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head><title>Trang chủ - À la carte Restaurant</title></head>
<body>
    <c:if test="${sessionScope.account == null}">
        <c:redirect url="login.jsp"/>
    </c:if>
    
    <h2>Chào mừng, ${sessionScope.account.fullName}!</h2>
    <p>Vai trò của bạn: <b>${sessionScope.account.roleName}</b></p>
    <p>Email: ${sessionScope.account.email}</p>
    
    <hr>
    <a href="logout">Đăng xuất</a>
</body>
</html>