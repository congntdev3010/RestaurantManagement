<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Bill phục vụ</title>
<style>
body{font-family:Arial,sans-serif;background:#f7f3ed;color:#29231f}.receipt{max-width:720px;margin:32px auto;background:#fff;padding:28px;box-shadow:0 4px 20px #0001}.receipt h1{text-align:center}.receipt table{width:100%;border-collapse:collapse;margin:20px 0}.receipt th,.receipt td{padding:9px;border-bottom:1px solid #eee;text-align:left}.receipt .total{text-align:right;font-size:1.2rem;font-weight:700}.actions{text-align:center;margin-top:24px}@media print{.actions{display:none}.receipt{box-shadow:none;margin:0}}
</style></head><body>
<main class="receipt">
<h1>HÓA ĐƠN THANH TOÁN</h1>
<p>Bàn: <strong><c:out value="${receipt.tableId}"/></strong></p>
<c:if test="${not empty receipt.customerName}"><p>Khách: <c:out value="${receipt.customerName}"/> <c:out value="${receipt.customerPhone}"/></p></c:if>
<c:if test="${not empty receipt.reservationTime}"><p>Giờ đặt: <c:out value="${receipt.reservationTime}"/></p></c:if>
<c:if test="${not empty receipt.checkedInAt}"><p>Giờ mở bàn: <c:out value="${receipt.checkedInAt}"/></p></c:if>
<table><thead><tr><th>Món</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>
<c:forEach var="item" items="${receipt.items}"><tr><td><c:out value="${item.dishName}"/></td><td><c:out value="${item.quantity}"/></td><td><c:out value="${item.unitPrice}"/></td><td><c:out value="${item.lineTotal}"/></td></tr></c:forEach>
</tbody></table>
<p class="total">Tổng bill: <c:out value="${receipt.totalAmount}"/> đ</p>
<p>Phương thức: <c:out value="${receipt.paymentMethod eq 'CASH' ? 'Tiền mặt' : 'Chuyển khoản QR'}"/></p>
<c:if test="${not empty receipt.cashReceived}"><p>Khách đưa: <c:out value="${receipt.cashReceived}"/> đ · Trả lại: <c:out value="${receipt.cashChange}"/> đ</p></c:if>
<div class="actions"><button type="button" onclick="window.print()">In bill</button><a href="${pageContext.request.contextPath}/manage-tables">Về sơ đồ bàn</a></div>
</main></body></html>
