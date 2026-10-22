<%@ page contentType="text/html;charset=UTF-8" language="java" %><%@ taglib prefix="c" uri="jakarta.tags.core" %><%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Quản lý order</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
<style>
.status-filter{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 18px}.status-filter a{padding:8px 18px;border-radius:999px;border:1px solid #e3ddd3;background:#fff;color:#444;text-decoration:none;font-weight:500}.status-filter a:hover{border-color:#7a1f1f;color:#7a1f1f}.status-filter a.active{background:#7a1f1f !important;border-color:#7a1f1f !important;color:#fff !important}
.so-card{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:6px 18px;background:#fff;border:1px solid #e8e1d6;border-left:6px solid #999;border-radius:12px;padding:12px 18px;margin:0 0 10px}
.so-card.st-PENDING{border-left-color:#e08a00;background:#fffaf0}
.so-card.st-OPEN{border-left-color:#1e8e4a}
.so-card.st-CANCELLED{border-left-color:#c0392b;background:#fdf5f4}
.so-table{font-size:1.5rem;font-weight:800;color:#7a1f1f;line-height:1.1;min-width:110px}
.so-meta{display:flex;flex-wrap:wrap;align-items:center;gap:6px 14px;color:#555;font-size:.9rem}
.so-meta strong{color:#2c2c2c}
.so-badge{display:inline-block;padding:2px 10px;border-radius:999px;font-size:.8rem;font-weight:700;color:#fff}
.so-badge.st-PENDING{background:#e08a00}.so-badge.st-OPEN{background:#1e8e4a}.so-badge.st-CANCELLED{background:#c0392b}
.so-time{white-space:nowrap}
.so-go{white-space:nowrap}
.so-right{display:flex;flex-direction:column;align-items:stretch;gap:8px;text-align:center}
.so-left{display:inline-block;padding:4px 10px;border-radius:8px;font-size:.85rem;font-weight:700;white-space:nowrap}
.so-left.todo{background:#fff3e0;color:#b34100}.so-left.done{background:#e6f4ea;color:#14532d}
</style></head><body><jsp:include page="/WEB-INF/views/fragments/header.jsp"/>
<main class="orders-page staff-orders"><header class="page-heading"><div><p>VẬN HÀNH NHÀ HÀNG</p><h1>Quản lý order</h1></div></header>
<c:if test="${not empty orderMessage}"><p class="notice"><c:out value="${orderMessage}"/></p></c:if>
<nav class="status-filter"><a href="?status=PENDING" class="${currentStatus eq 'PENDING' ? 'active' : ''}">Chờ duyệt</a><a href="?status=OPEN" class="${currentStatus eq 'OPEN' ? 'active' : ''}">Đang phục vụ</a><a href="?status=CANCELLED" class="${currentStatus eq 'CANCELLED' ? 'active' : ''}">Đã hủy</a><a href="?status=ALL" class="${currentStatus eq 'ALL' ? 'active' : ''}">Tất cả</a></nav>
<c:choose><c:when test="${empty orders}"><p>${currentStatus eq 'PENDING' ? 'Không có đơn nào đang chờ duyệt.' : 'Không có đơn hàng.'}</p></c:when><c:otherwise><div class="order-list">
<c:forEach var="order" items="${orders}"><article class="so-card st-${order.status}">
<div class="so-table"><c:out value="${empty order.tableName ? '—' : order.tableName}"/></div>
<div class="so-meta"><span class="so-badge st-${order.status}"><c:choose><c:when test="${order.status eq 'PENDING'}">Chờ duyệt</c:when><c:when test="${order.status eq 'OPEN'}">Đang phục vụ</c:when><c:when test="${order.status eq 'CANCELLED'}">Đã hủy</c:when><c:otherwise><c:out value="${order.status}"/></c:otherwise></c:choose></span>
<span>Đơn #<strong><c:out value="${order.orderId}"/></strong></span>
<span class="so-time"><fmt:formatDate value="${order.createdAt}" pattern="HH:mm"/> · <fmt:formatDate value="${order.createdAt}" pattern="dd/MM"/></span>
<span><strong><fmt:formatNumber value="${order.totalAmount}" pattern="#,###"/> đ</strong></span></div>
<div class="so-right"><c:if test="${order.status eq 'OPEN'}"><c:choose><c:when test="${order.cookingCount > 0}"><span class="so-left todo">Còn <c:out value="${order.cookingCount}"/> món</span></c:when><c:otherwise><span class="so-left done">Đủ món</span></c:otherwise></c:choose></c:if>
<a class="button so-go" href="${pageContext.request.contextPath}/manage-orders?id=${order.orderId}"><c:choose><c:when test="${order.status eq 'PENDING'}">Duyệt</c:when><c:otherwise>Chi tiết</c:otherwise></c:choose></a></div>
</article></c:forEach></div></c:otherwise></c:choose></main></body></html>
