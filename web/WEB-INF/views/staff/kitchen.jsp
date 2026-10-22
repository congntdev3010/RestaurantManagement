<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="refresh" content="15"><title>Màn hình bếp</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
<style>
.k-table{width:100%;border-collapse:collapse;background:#fff}
.k-table td{padding:9px 16px;border-bottom:1px solid #eee;font-size:1.05rem}
.k-sep td{padding:0;border:0;height:6px;background:#7a1f1f}
.k-head td{padding:14px 16px 6px;border-bottom:0}
.k-head .k-title{font-size:1.5rem;font-weight:800;color:#2c2c2c}
.k-head .k-title span{color:#7a1f1f}
.k-head .k-time{float:right;color:#666;font-size:.9rem;font-weight:400;line-height:2}
.k-qtycell{width:1%;white-space:nowrap;text-align:right;padding-right:6px !important}
.k-namecell{padding-left:6px !important}
.k-qty{font-size:1.3rem;font-weight:800}
.k-note{display:inline-block;margin-left:14px;padding:1px 10px;border-radius:6px;background:#fff3e0;color:#b34100;font-weight:700;font-size:.95rem}
</style></head><body><jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="orders-page"><h1>Món cần nấu</h1><p>Chỉ xem, tự làm mới mỗi 15 giây.</p>
<c:choose><c:when test="${empty items}"><p>Hiện chưa có món nào cần nấu.</p></c:when><c:otherwise>
<table class="k-table"><tbody>
<c:set var="prev" value="0"/>
<c:forEach var="item" items="${items}">
<c:if test="${item.orderId ne prev}">
<c:if test="${prev ne 0}"><tr class="k-sep"><td colspan="2"></td></tr></c:if>
<tr class="k-head"><td class="k-title" colspan="2"><span class="k-time">Duyệt lúc <fmt:formatDate value="${item.approvedAt}" pattern="HH:mm"/></span><c:out value="${empty item.tableName ? '—' : item.tableName}"/> <span>· Đơn #<c:out value="${item.orderId}"/></span></td></tr>
</c:if>
<tr><td class="k-qtycell"><span class="k-qty"><c:out value="${item.quantity}"/></span> <c:out value="${item.unit}"/> ×</td><td class="k-namecell"><c:out value="${item.dishName}"/><c:if test="${not empty item.itemNote}"><span class="k-note">Note: <c:out value="${item.itemNote}"/></span></c:if></td></tr>
<c:set var="prev" value="${item.orderId}"/>
</c:forEach>
</tbody></table></c:otherwise></c:choose></main></body></html>
