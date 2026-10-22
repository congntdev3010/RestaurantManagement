<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gọi món - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/order-menu.css?v=1.0">
</head>
<body class="qr-order">
    <c:set var="ctx" value="${pageContext.request.contextPath}"/>
    <header class="qr-top">
        <small>Việt À la carte</small>
        <h1>Gọi món</h1>
        <c:if test="${not empty orderTableName}"><span class="qr-table">Bàn: <strong><c:out value="${orderTableName}"/></strong></span></c:if>
    </header>

    <c:if test="${empty orderTableName}">
        <p class="qr-warn">Bạn chưa chọn bàn. Hãy quét mã QR đặt trên bàn để gọi món.</p>
    </c:if>
    <c:if test="${not empty cartMessage}"><p class="qr-flash" role="status"><c:out value="${cartMessage}"/></p></c:if>

    <form class="qr-search" action="${ctx}/order" method="get" accept-charset="UTF-8">
        <input type="search" name="keyword" placeholder="Tìm món ăn" value="<c:out value='${keyword}'/>">
        <c:if test="${categoryId > 0}"><input type="hidden" name="categoryId" value="${categoryId}"></c:if>
        <button type="submit">Tìm</button>
    </form>

    <nav class="qr-cats" aria-label="Danh mục món ăn">
        <c:url var="allUrl" value="/order"><c:param name="keyword" value="${keyword}"/></c:url>
        <a href="${allUrl}" class="${categoryId == 0 ? 'active' : ''}">Tất cả</a>
        <c:forEach var="cat" items="${categories}">
            <c:url var="catUrl" value="/order"><c:param name="categoryId" value="${cat.categoryId}"/><c:param name="keyword" value="${keyword}"/></c:url>
            <a href="${catUrl}" class="${categoryId == cat.categoryId ? 'active' : ''}"><c:out value="${cat.categoryName}"/></a>
        </c:forEach>
    </nav>

    <div class="qr-layout">
    <main class="qr-list">
        <c:choose>
            <c:when test="${empty dishes}"><p class="qr-empty">Không tìm thấy món phù hợp.</p></c:when>
            <c:otherwise>
                <c:forEach var="dish" items="${dishes}">
                    <c:set var="qty" value="${empty cartQuantities[dish.dishId] ? 0 : cartQuantities[dish.dishId]}"/>
                    <article class="qr-dish">
                        <div class="qr-dish-info">
                            <h2><c:out value="${dish.dishName}"/><c:if test="${dish.isFeatured}"><span class="qr-hot">Bán chạy</span></c:if></h2>
                            <p><c:out value="${dish.description}"/></p>
                            <span class="qr-price"><fmt:formatNumber value="${dish.price}" pattern="#,###"/> đ <small>/ <c:out value="${dish.unit}"/></small></span>
                        </div>
                        <div class="qr-dish-media">
                            <c:if test="${not empty dish.image}">
                                <img loading="lazy" alt="<c:out value='${dish.dishName}'/>"
                                     src="${ctx}/${fn:startsWith(dish.image, 'images/') ? 'assets/' : ''}${dish.image}">
                            </c:if>
                            <div class="qr-step">
                                <c:if test="${qty > 0}">
                                    <form action="${ctx}/cart" method="post">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                        <input type="hidden" name="action" value="update">
                                        <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                        <input type="hidden" name="quantity_${dish.dishId}" value="${qty - 1}">
                                        <button type="submit" aria-label="Bớt một ${dish.dishName}">−</button>
                                    </form>
                                    <span><c:out value="${qty}"/></span>
                                </c:if>
                                <form action="${ctx}/cart" method="post">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                    <input type="hidden" name="dishId" value="${dish.dishId}">
                                    <input type="hidden" name="quantity" value="1">
                                    <button type="submit" aria-label="Thêm một ${dish.dishName}">+</button>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </main>

    <aside class="qr-panel" id="cart-panel" aria-label="Các món đã chọn">
        <h2>Món đã chọn <span>(<c:out value="${cartCount}"/>)</span></h2>
        <c:choose>
            <c:when test="${empty cartDishes}"><p class="qr-empty">Chưa chọn món nào.</p></c:when>
            <c:otherwise>
                <ul class="qr-lines">
                    <c:forEach var="d" items="${cartDishes}">
                        <c:set var="q" value="${cartQuantities[d.dishId]}"/>
                        <li>
                            <div class="qr-line-name"><c:out value="${d.dishName}"/><small><fmt:formatNumber value="${d.price * q}" pattern="#,###"/> đ</small></div>
                            <div class="qr-step qr-step-inline">
                                <form action="${ctx}/cart" method="post">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                    <input type="hidden" name="action" value="update">
                                    <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                    <input type="hidden" name="quantity_${d.dishId}" value="${q - 1}">
                                    <button type="submit" aria-label="Bớt một ${d.dishName}">−</button>
                                </form>
                                <span><c:out value="${q}"/></span>
                                <form action="${ctx}/cart" method="post">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                                    <input type="hidden" name="action" value="add">
                                    <input type="hidden" name="returnTo" value="${orderReturnTo}">
                                    <input type="hidden" name="dishId" value="${d.dishId}">
                                    <input type="hidden" name="quantity" value="1">
                                    <button type="submit" aria-label="Thêm một ${d.dishName}">+</button>
                                </form>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
        <div class="qr-total">Tổng tiền <strong><fmt:formatNumber value="${cartTotal}" pattern="#,###"/> đ</strong></div>
        <c:choose>
            <c:when test="${cartCount > 0 and not empty orderTableName}"><a class="qr-send" href="${ctx}/cart?review=1">Gửi order chờ duyệt</a></c:when>
            <c:otherwise><span class="qr-send disabled"><c:out value="${empty orderTableName ? 'Quét QR bàn để gửi order' : 'Hãy chọn món'}"/></span></c:otherwise>
        </c:choose>
    </aside>
    </div>

</body>
</html>
