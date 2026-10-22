<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin khách - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/order-menu.css?v=1.0">
    <style>
        .qr-info{max-width:420px;margin:24px auto;padding:20px 18px;background:#fff;border:1px solid #e8e1d6;border-radius:14px}
        .qr-info h2{margin:0 0 6px;font-size:1.2rem}
        .qr-info p{margin:0 0 14px;color:#666;font-size:.92rem}
        .qr-info label{display:block;margin:0 0 12px;font-weight:600;font-size:.92rem}
        .qr-info input{display:block;width:100%;box-sizing:border-box;margin-top:6px;padding:12px;border:1px solid #ddd;border-radius:10px;font-size:1rem}
        .qr-info button{width:100%;padding:13px;border:0;border-radius:12px;background:#7a1f1f;color:#fff;font-weight:700;font-size:1rem;cursor:pointer}
        .qr-info .qr-err{color:#c0392b;margin:0 0 12px;font-weight:600}
    </style>
</head>
<body class="qr-order">
    <header class="qr-top">
        <small>Việt À la carte</small>
        <h1>Chào mừng bạn</h1>
        <c:if test="${not empty orderTableName}"><span class="qr-table">Bàn: <strong><c:out value="${orderTableName}"/></strong></span></c:if>
    </header>
    <form class="qr-info" action="${pageContext.request.contextPath}/order" method="post" accept-charset="UTF-8">
        <h2>Thông tin của bạn</h2>
        <p>Vui lòng nhập tên và số điện thoại để nhân viên phục vụ bạn tốt hơn.</p>
        <c:if test="${not empty infoError}"><p class="qr-err" role="alert"><c:out value="${infoError}"/></p></c:if>
        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
        <label>Tên của bạn
            <input name="customerName" maxlength="30" required autocomplete="name" value="<c:out value='${not empty infoName ? infoName : sessionScope.user.fullName}'/>">
        </label>
        <label>Số điện thoại
            <input name="customerPhone" type="tel" inputmode="numeric" maxlength="10" pattern="0[0-9]{9}" title="Số điện thoại bắt đầu bằng số 0 và có đúng 10 chữ số" required autocomplete="tel" value="<c:out value='${not empty infoPhone ? infoPhone : sessionScope.user.phone}'/>">
        </label>
        <button type="submit">Bắt đầu gọi món</button>
    </form>
</body>
</html>
