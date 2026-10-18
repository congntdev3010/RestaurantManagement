<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="model.User" %>
<%@ page import="model.Dish" %>
<%@ page import="java.text.DecimalFormat" %>
<%
    User current = (User) session.getAttribute("user");
    java.util.List<Dish> cartDishes = (java.util.List<Dish>) request.getAttribute("cartDishes");
    java.util.Map<Integer,Integer> cartQuantities = (java.util.Map<Integer,Integer>) request.getAttribute("cartQuantities");
    DecimalFormat fmt = new DecimalFormat("#,###");
%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Giỏ hàng</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css"></head><body>
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
<main class="orders-page"><h1>Giỏ hàng</h1><c:if test="${not empty message}"><p class="notice"><c:out value="${message}"/></p></c:if>
<% if (cartDishes == null || cartDishes.isEmpty()) { %><p>Giỏ hàng của bạn đang trống.</p><a class="button" href="${pageContext.request.contextPath}/menu">Xem thực đơn</a><% } else { %>
<form action="${pageContext.request.contextPath}/cart" method="post"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input id="cartAction" type="hidden" name="action" value="update">
<table><thead><tr><th>Món</th><th>Đơn giá</th><th>Số lượng</th><th>Tạm tính</th><th></th></tr></thead><tbody>
<% for (Dish dish : cartDishes) { int qty=cartQuantities.get(dish.getDishId()); %>
<tr><td><%= dish.getDishName() %></td><td><%= fmt.format(dish.getPrice()) %> đ</td><td><input type="number" name="quantity_<%=dish.getDishId()%>" min="1" max="99" value="<%=qty%>"></td><td><%=fmt.format(dish.getPrice().multiply(java.math.BigDecimal.valueOf(qty)))%> đ</td>
<td><button type="button" onclick="this.form.dishId.value='<%=dish.getDishId()%>';this.form.querySelector('input[name=action]').value='remove';this.form.submit();">Xóa</button></td></tr>
<% } %></tbody></table><input type="hidden" name="dishId" value="">
<div class="order-actions"><button type="submit">Cập nhật giỏ</button><strong>Tạm tính: <%=fmt.format((java.math.BigDecimal)request.getAttribute("cartTotal"))%> đ</strong></div></form>
<% if (current == null || !"Customer".equalsIgnoreCase(current.getRoleName())) { %><p>Vui lòng đăng nhập bằng tài khoản Customer để xác nhận đặt hàng.</p><% } else { %>
<form action="${pageContext.request.contextPath}/my-orders" method="post" class="checkout-form"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="action" value="checkout"><label>Ghi chú <textarea name="note" maxlength="500"></textarea></label><button type="submit">Xác nhận đặt hàng</button></form>
<% } } %></main></body></html>
