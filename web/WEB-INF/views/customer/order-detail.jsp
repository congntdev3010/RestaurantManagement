
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Chi tiết đơn hàng</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/orders.css">
</head>

<body>

    <!-- Header -->
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

    <main class="orders-page">

        <!-- Thông báo -->
        <c:if test="${not empty orderMessage}">
            <p class="notice">
                <c:out value="${orderMessage}" />
            </p>
        </c:if>

        <!-- Thông tin đơn hàng -->
        <h1>
            Đơn hàng #<c:out value="${order.orderId}" />
        </h1>

        <p>
            Trạng thái:
            <strong>
                <c:out value="${order.status}" />
            </strong>

            · Tạo lúc
            <c:out value="${order.createdAt}" />
        </p>

        <!-- Lý do hủy đơn -->
        <c:if test="${not empty order.cancellationReason}">
            <p>
                Lý do hủy:
                <c:out value="${order.cancellationReason}" />
            </p>
        </c:if>

        <!-- Bảng chi tiết món ăn -->
        <table>
            <thead>
                <tr>
                    <th>Món</th>
                    <th>Đơn giá</th>
                    <th>Số lượng</th>
                    <th>Thành tiền</th>
                </tr>
            </thead>

            <tbody>
                <c:forEach var="item" items="${order.items}">
                    <tr>
                        <td>
                            <c:out value="${item.dishName}" />
                        </td>

                        <td>
                            <c:out value="${item.unitPrice}" />
                            đ /
                            <c:out value="${item.unit}" />
                        </td>

                        <td>
                            <c:out value="${item.quantity}" />
                        </td>

                        <td>
                            <c:out value="${item.lineTotal}" />
                            đ
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>

        <!-- Tổng tiền -->
        <p class="total">
            Tổng cộng:
            <c:out value="${order.totalAmount}" /> đ
        </p>

        <!-- Chỉ cho phép chỉnh sửa khi đơn hàng đang PENDING -->
        <c:if test="${order.status eq 'PENDING'}">

            <!-- Form điều chỉnh món ăn -->
            <form action="${pageContext.request.contextPath}/my-orders"
                  method="post"
                  class="order-form">

                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.orderCsrf}">

                <input type="hidden"
                       name="action"
                       value="updateItems">

                <input type="hidden"
                       name="orderId"
                       value="${order.orderId}">

                <h2>Điều chỉnh món</h2>

                <!-- Cập nhật số lượng món hiện có -->
                <c:forEach var="item" items="${order.items}">
                    <label>
                        <c:out value="${item.dishName}" />

                        <input type="number"
                               name="quantity_${item.dishId}"
                               min="0"
                               max="99"
                               value="${item.quantity}">
                    </label>
                </c:forEach>

                <p>Nhập 0 để bỏ món.</p>

                <!-- Chọn món mới -->
                <label>
                    Thêm món

                    <select name="newDishId">
                        <option value="">Chọn món</option>

                        <c:forEach var="dish" items="${availableDishes}">
                            <option value="${dish.dishId}">
                                <c:out value="${dish.dishName}" />
                                —
                                <c:out value="${dish.price}" /> đ
                            </option>
                        </c:forEach>
                    </select>
                </label>

                <!-- Số lượng món mới -->
                <label>
                    Số lượng thêm

                    <input type="number"
                           name="newQuantity"
                           min="0"
                           max="99"
                           value="0">
                </label>

                <button type="submit">
                    Lưu món
                </button>

            </form>

            <!-- Form hủy đơn hàng -->
            <form action="${pageContext.request.contextPath}/my-orders"
                  method="post"
                  class="order-form">

                <input type="hidden"
                       name="csrfToken"
                       value="${sessionScope.orderCsrf}">

                <input type="hidden"
                       name="action"
                       value="cancel">

                <input type="hidden"
                       name="orderId"
                       value="${order.orderId}">

                <label>
                    Lý do hủy

                    <input type="text"
                           name="reason"
                           maxlength="500"
                           required>
                </label>

                <button class="danger" type="submit">
                    Hủy đơn
                </button>

            </form>

        </c:if>

        <!-- Quay lại danh sách đơn hàng -->
        <a class="button secondary"
           href="${pageContext.request.contextPath}/my-orders">
            Quay lại đơn hàng
        </a>

    </main>

</body>
</html>
