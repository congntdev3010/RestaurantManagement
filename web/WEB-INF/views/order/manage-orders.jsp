<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <c:if test="${empty param.id}"><meta http-equiv="refresh" content="15"></c:if>
            <title>Quản lý order</title>
            <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/manage-orders.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/views/fragments/header.jsp"/>

        <main class="orders-page staff-orders">
            <header class="page-heading">
                <div>
                    <p>VẬN HÀNH NHÀ HÀNG</p>
                    <h1>Quản lý order</h1>
                </div>
            </header>

            <c:if test="${not empty orderMessage}">
                <p class="notice"><c:out value="${orderMessage}"/></p>
            </c:if>

            <nav class="status-filter">
                <a href="?status=PENDING" class="${currentStatus eq 'PENDING' ? 'active' : ''}">Chờ duyệt</a>
                <a href="?status=OPEN" class="${currentStatus eq 'OPEN' ? 'active' : ''}">Đang phục vụ</a>
                <c:if test="${sessionScope.user.roleName ne 'Server'}">
                    <a href="?status=PAID" class="${currentStatus eq 'PAID' ? 'active' : ''}">Đã thanh toán</a>
                </c:if>
                <a href="?status=CANCELLED" class="${currentStatus eq 'CANCELLED' ? 'active' : ''}">Đã hủy</a>
                <a href="?status=ALL" class="${currentStatus eq 'ALL' ? 'active' : ''}">Tất cả</a>
            </nav>

            <c:choose>
                <c:when test="${empty orders}">
                    <p>${currentStatus eq 'PENDING' ? 'Không có đơn nào đang chờ duyệt.' : 'Không có đơn hàng.'}</p>
                </c:when>
                <c:otherwise>
                    <div class="order-list">
                        <c:forEach var="order" items="${orders}">
                            <article class="so-card st-${order.status}">
                                <div class="so-table"><c:out value="${empty order.tableName ? '—' : order.tableName}"/></div>
                                <div class="so-meta">
                                    <span class="so-badge st-${order.status}">
                                        <c:choose>
                                            <c:when test="${order.status eq 'PENDING'}">Chờ duyệt</c:when>
                                            <c:when test="${order.status eq 'OPEN'}">Đang phục vụ</c:when>
                                            <c:when test="${order.status eq 'CANCELLED'}">Đã hủy</c:when>
                                            <c:otherwise><c:out value="${order.status}"/></c:otherwise>
                                        </c:choose>
                                    </span>
                                    <span>Đơn #<strong><c:out value="${order.orderId}"/></strong></span>
                                    <span class="so-time"><fmt:formatDate value="${order.createdAt}" pattern="HH:mm"/> · <fmt:formatDate value="${order.createdAt}" pattern="dd/MM"/></span>
                                    <span><strong><fmt:formatNumber value="${order.totalAmount}" pattern="#,###"/> đ</strong></span>
                                </div>
                                <div class="so-right">
                                    <c:if test="${order.status eq 'OPEN'}">
                                        <c:choose>
                                            <c:when test="${order.cookingCount > 0}"><span class="so-left todo">Còn <c:out value="${order.cookingCount}"/> món</span></c:when>
                                            <c:otherwise><span class="so-left done">Đủ món</span></c:otherwise>
                                        </c:choose>
                                    </c:if>
                                    <a class="button so-go" href="${pageContext.request.contextPath}/manage-orders?id=${order.orderId}&amp;status=${currentStatus}">
                                        <c:choose>
                                            <c:when test="${order.status eq 'PENDING'}">Duyệt</c:when>
                                            <c:otherwise>Chi tiết</c:otherwise>
                                        </c:choose>
                                    </a>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </main>
    </body>
</html>
