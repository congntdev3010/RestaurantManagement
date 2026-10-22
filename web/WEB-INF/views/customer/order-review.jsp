<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Xác nhận order</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css"></head><body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="orders-page"><h1>Xem lại order</h1>
<p>Bàn: <strong><c:out value="${orderTableName}"/></strong></p>
<table><thead><tr><th>Món</th><th>Số lượng</th><th>Tạm tính</th></tr></thead><tbody>
<c:forEach var="dish" items="${cartDishes}"><tr><td><c:out value="${dish.dishName}"/></td><td><c:out value="${cartQuantities[dish.dishId]}"/></td><td><c:out value="${dish.price * cartQuantities[dish.dishId]}"/> đ</td></tr></c:forEach>
</tbody></table>
<p class="total">Tạm tính: <c:out value="${cartTotal}"/> đ</p>
<p>Order sau khi gửi sẽ chờ nhân viên duyệt. Món nào hết sẽ bị bỏ hoặc giảm số lượng. Muốn gọi thêm món, bạn tạo một order mới.</p>
<form action="${pageContext.request.contextPath}/my-orders" method="post" class="checkout-form">
<input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="action" value="checkout">
<c:if test="${empty sessionScope.user}"><label>Tên của bạn (tùy chọn) <input name="customerName" maxlength="100"></label></c:if>
<label>Ghi chú <textarea name="note" maxlength="500"></textarea></label>
<p><strong>Bạn đã chắc chắn gửi order này chưa?</strong></p>
<div class="order-actions"><a class="button secondary" href="${pageContext.request.contextPath}/cart">Quay lại chỉnh sửa</a><button type="submit">Chắc chắn, gửi order</button></div>
</form></main></body></html>
