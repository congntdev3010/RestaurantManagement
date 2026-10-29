<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <meta http-equiv="refresh" content="20">
        <title>Chi tiết đơn hàng</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
    </head>
    <body>
        <main class="orders-page">
            <c:if test="${not empty orderMessage}">
                <p class="notice"><c:out value="${orderMessage}"/></p>
            </c:if>

            <h1>Đơn hàng #<c:out value="${order.orderId}"/></h1>
            <p>
                Bàn: <c:out value="${order.tableName}"/> ·
                Trạng thái:
                <strong>
                    <c:choose>
                        <c:when test="${order.status eq 'PENDING'}">Chờ nhân viên duyệt</c:when>
                        <c:when test="${order.status eq 'OPEN'}">Đang phục vụ</c:when>
                        <c:when test="${order.status eq 'PAID'}">Đã thanh toán</c:when>
                        <c:otherwise>Đã hủy</c:otherwise>
                    </c:choose>
                </strong>
                · Tạo lúc <c:out value="${order.createdAt}"/>
            </p>

            <c:if test="${not empty order.cancellationReason}">
                <p>Lý do hủy: <c:out value="${order.cancellationReason}"/></p>
            </c:if>

            <table class="od-table">
                <thead>
                    <tr>
                        <th>Món</th>
                        <th>Đơn giá</th>
                        <th>Số lượng</th>
                        <th>Tình trạng</th>
                        <th>Thành tiền</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="item" items="${order.items}">
                        <tr>
                            <td class="od-name">
                                <c:out value="${item.dishName}"/>
                                <c:if test="${not empty item.itemNote}">
                                    <br><em>Ghi chú: <c:out value="${item.itemNote}"/></em>
                                </c:if>
                            </td>
                            <td data-label="Đơn giá">
                                <c:out value="${item.unitPrice}"/> đ / <c:out value="${item.unit}"/>
                            </td>
                            <td data-label="Số lượng">
                                <c:out value="${item.quantity}"/>
                                <c:if test="${item.quantity ne item.requestedQuantity}">
                                    (bạn gọi <c:out value="${item.requestedQuantity}"/>)
                                </c:if>
                            </td>
                            <td data-label="Tình trạng">
                                <c:choose>
                                    <c:when test="${order.status eq 'CANCELLED'}">Đã hủy</c:when>
                                    <c:when test="${item.itemStatus eq 'PENDING'}">Chờ duyệt</c:when>
                                    <c:when test="${item.itemStatus eq 'COOKING'}">Đang nấu</c:when>
                                    <c:when test="${item.itemStatus eq 'SERVED'}">Đã phục vụ</c:when>
                                    <c:otherwise>Hết món</c:otherwise>
                                </c:choose>
                            </td>
                            <td data-label="Thành tiền"><c:out value="${item.lineTotal}"/> đ</td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>

            <p class="total">Tổng cộng: <c:out value="${order.totalAmount}"/> đ</p>

            <c:if test="${canCancel}">
                <form action="${pageContext.request.contextPath}/my-orders" method="post" class="order-form">
                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                    <input type="hidden" name="action" value="cancel">
                    <input type="hidden" name="orderId" value="${order.orderId}">
                    <label>Lý do hủy
                        <input name="reason" maxlength="500" required>
                    </label>
                    <button type="submit">Hủy đơn</button>
                </form>
            </c:if>

            <a class="button" href="${pageContext.request.contextPath}/order">Gọi thêm món (order mới)</a>
        </main>
    </body>
</html>
