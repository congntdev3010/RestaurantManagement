<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chi tiết đơn</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css"></head><body><jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="orders-page"><c:if test="${not empty orderMessage}"><p class="notice"><c:out value="${orderMessage}"/></p></c:if><h1>Đơn #<c:out value="${order.orderId}"/></h1>
<div class="order-summary"><p>Trạng thái: <strong><c:choose><c:when test="${order.status eq 'PENDING'}">Chờ duyệt</c:when><c:when test="${order.status eq 'OPEN'}">Đang phục vụ</c:when><c:when test="${order.status eq 'PAID'}">Đã thanh toán</c:when><c:otherwise>Đã hủy</c:otherwise></c:choose></strong></p>
<p>Bàn: <c:out value="${order.tableName}"/></p><p>Khách: <c:out value="${order.customerName}"/> <c:out value="${order.customerPhone}"/></p><p>Note: <c:out value="${order.note}"/></p><p>Tạo lúc <c:out value="${order.createdAt}"/></p>
<c:if test="${not empty order.cancellationReason}"><p>Lý do hủy: <c:out value="${order.cancellationReason}"/></p></c:if></div>
<c:choose>
<c:when test="${order.status eq 'PENDING'}">
<p>Sau khi hỏi bếp, bỏ tick món đã hết hoặc giảm số lượng nếu chỉ phục vụ được ít hơn.</p>
<form method="post" action="${pageContext.request.contextPath}/manage-orders"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="orderId" value="${order.orderId}"><input type="hidden" name="action" value="approveItems"><input type="hidden" name="listStatus" value="<c:out value='${listStatus}'/>">
<table><thead><tr><th>Duyệt</th><th>Món</th><th>Khách gọi</th><th>Phục vụ được</th></tr></thead><tbody>
<c:forEach var="item" items="${order.items}"><c:if test="${item.itemStatus eq 'PENDING'}"><tr><td><input type="checkbox" name="approve_${item.orderItemId}" checked aria-label="Duyệt ${item.dishName}"></td><td><c:out value="${item.dishName}"/><c:if test="${not empty item.itemNote}"><br><em>Note: <c:out value="${item.itemNote}"/></em></c:if></td><td><c:out value="${item.requestedQuantity}"/></td><td><input type="number" name="qty_${item.orderItemId}" min="1" max="${item.requestedQuantity}" value="${item.requestedQuantity}"></td></tr></c:if></c:forEach>
</tbody></table><button type="submit">Duyệt và đẩy lên bếp</button></form>
</c:when>
<c:otherwise>
<table><thead><tr><th>Món</th><th>Đơn giá</th><th>Số lượng</th><th>Tình trạng</th><th></th></tr></thead><tbody>
<c:forEach var="item" items="${order.items}"><tr id="item-${item.orderItemId}"><td><c:out value="${item.dishName}"/><c:if test="${not empty item.itemNote}"><br><em>Note: <c:out value="${item.itemNote}"/></em></c:if></td><td><c:out value="${item.unitPrice}"/> đ</td><td><c:out value="${item.quantity}"/><c:if test="${item.quantity ne item.requestedQuantity}"> (gọi <c:out value="${item.requestedQuantity}"/>)</c:if></td>
<td><c:choose><c:when test="${order.status eq 'CANCELLED'}">Đã hủy</c:when><c:when test="${item.itemStatus eq 'COOKING'}">Đang nấu</c:when><c:when test="${item.itemStatus eq 'SERVED'}">Đã phục vụ</c:when><c:when test="${item.itemStatus eq 'REJECTED'}">Hết món</c:when><c:otherwise>Chờ duyệt</c:otherwise></c:choose></td>
<td><c:if test="${order.status eq 'OPEN' and item.itemStatus eq 'COOKING'}"><form method="post" action="${pageContext.request.contextPath}/manage-orders"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="orderId" value="${order.orderId}"><input type="hidden" name="itemId" value="${item.orderItemId}"><input type="hidden" name="action" value="serve"><input type="hidden" name="listStatus" value="<c:out value='${listStatus}'/>"><button type="submit">Đã phục vụ</button></form></c:if></td></tr></c:forEach>
</tbody></table></c:otherwise></c:choose>
<p class="total">Tổng cộng: <c:out value="${order.totalAmount}"/> đ</p>
<div class="order-actions">

<c:if test="${order.status eq 'PENDING'}"><form method="post" action="${pageContext.request.contextPath}/manage-orders"><input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}"><input type="hidden" name="orderId" value="${order.orderId}"><input type="hidden" name="action" value="cancel"><input type="hidden" name="listStatus" value="<c:out value='${listStatus}'/>"><input name="reason" maxlength="500" placeholder="Lý do hủy" required><button type="submit">Hủy đơn</button></form></c:if>
</div><a class="button secondary" href="${pageContext.request.contextPath}/manage-orders<c:if test='${not empty listStatus}'>?status=<c:out value='${listStatus}'/></c:if>">Quay lại danh sách</a></main></body></html>
