<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <meta http-equiv="refresh" content="3">
        <title>Màn hình bếp</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/kitchen.css">
    </head>
    <body class="kitchen-page">
        <jsp:include page="/WEB-INF/views/fragments/header.jsp"/>

        <main class="orders-page">
            <h1>Món cần nấu</h1>
            <p>
                <c:choose>
                    <c:when test="${canEdit}">Server có thể hủy món hoặc giảm số lượng khi bếp báo không đủ.</c:when>
                    <c:otherwise>Chỉ xem, tự làm mới mỗi 3 giây.</c:otherwise>
                </c:choose>
            </p>
            <c:if test="${not empty kitchenMessage}">
                <p class="notice"><c:out value="${kitchenMessage}"/></p>
            </c:if>

            <c:choose>
                <c:when test="${empty items}">
                    <p>Hiện chưa có món nào cần nấu.</p>
                </c:when>
                <c:otherwise>
                    <table class="k-table">
                        <tbody>
                            <c:set var="prev" value="0"/>
                            <c:forEach var="item" items="${items}">
                                <c:if test="${item.orderId ne prev}">
                                    <c:if test="${prev ne 0}">
                                        <tr class="k-sep"><td colspan="2"></td></tr>
                                        </c:if>
                                    <tr id="order-${item.orderId}" class="k-head">
                                        <td class="k-title" colspan="2">
                                            <span class="k-time">Duyệt lúc <fmt:formatDate value="${item.approvedAt}" pattern="HH:mm"/></span>
                                            <c:out value="${empty item.tableName ? '—' : item.tableName}"/>
                                            <span>· Đơn #<c:out value="${item.orderId}"/></span>
                                        </td>
                                    </tr>
                                </c:if>
                                <tr id="item-${item.orderItemId}">
                                    <td class="k-qtycell">
                                        <span class="k-qty"><c:out value="${item.quantity}"/></span> <c:out value="${item.unit}"/> ×
                                    </td>
                                    <td class="k-namecell">
                                        <c:out value="${item.dishName}"/>
                                        <c:if test="${not empty item.itemNote}"><span class="k-note">Note: <c:out value="${item.itemNote}"/></span></c:if>
                                        <c:if test="${canEdit}">
                                            <div class="k-actions">
                                                <form method="post" action="${pageContext.request.contextPath}/kitchen">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                                    <input type="hidden" name="itemId" value="${item.orderItemId}">
                                                    <input type="hidden" name="orderId" value="${item.orderId}">
                                                    <input type="number" name="quantity" min="1" max="${item.quantity}" value="${item.quantity}" aria-label="Số lượng ${item.dishName}">
                                                    <button type="submit">Cập nhật số lượng</button>
                                                </form>
                                                <form method="post" action="${pageContext.request.contextPath}/kitchen">
                                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                                    <input type="hidden" name="itemId" value="${item.orderItemId}">
                                                    <input type="hidden" name="orderId" value="${item.orderId}">
                                                    <input type="hidden" name="quantity" value="0">
                                                    <button class="k-cancel" type="submit">Hủy món</button>
                                                </form>
                                            </div>
                                        </c:if>
                                    </td>
                                </tr>
                                <c:set var="prev" value="${item.orderId}"/>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </main>
    </body>
</html>
