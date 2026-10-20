<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Dish" %>
<%@ page import="model.Category" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    List<Dish> dishes = (List<Dish>) request.getAttribute("dishes");
    String keyword = request.getAttribute("keyword") != null ? (String) request.getAttribute("keyword") : "";
    int categoryId = request.getAttribute("categoryId") != null ? (Integer) request.getAttribute("categoryId") : 0;
    String sortBy = request.getAttribute("sortBy") != null ? (String) request.getAttribute("sortBy") : "";
    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");
    int totalDishes = (Integer) request.getAttribute("totalDishes");
    String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    String encodedSortBy = URLEncoder.encode(sortBy, StandardCharsets.UTF_8);
    DecimalFormat formatter = new DecimalFormat("#,###");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gọi món - Việt À la carte</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/menu.css?v=1.0">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/orders.css?v=1.0">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />
    <main class="menu-container order-menu-page">
        <c:if test="${not empty cartMessage}">
            <div class="cart-feedback" role="status"><i class="fa-solid fa-check" aria-hidden="true"></i><span><c:out value="${cartMessage}"/></span></div>
        </c:if>
        <header class="order-menu-heading">
            <div class="order-menu-title-group">
                <span class="menu-subtitle">THỰC ĐƠN NHÀ HÀNG</span>
                <h1 class="menu-main-title">Gọi món</h1>
                <p class="order-menu-description">Khám phá món ngon và thêm lựa chọn của bạn vào giỏ hàng.</p>
            </div>
            <div class="order-menu-result-summary" aria-live="polite">
                <strong><%= formatter.format(totalDishes) %></strong>
                <span>món phù hợp</span>
            </div>
        </header>
        <form action="${pageContext.request.contextPath}/order" method="get" accept-charset="UTF-8" class="filter-bar order-filter-bar">
            <div class="search-box">
                <label class="order-search-label" for="order-keyword">Tìm trong thực đơn</label>
                <div class="order-search-control">
                    <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                    <input id="order-keyword" type="search" name="keyword" placeholder="Tên món ăn..." value="<c:out value='${keyword}'/>" autocomplete="off">
                    <button type="submit">Tìm món</button>
                </div>
            </div>
            <div class="filter-group">
                <label>Danh mục
                <select name="categoryId" onchange="this.form.submit()">
                    <option value="0">Tất cả danh mục</option>
                    <% if (categories != null) for (Category category : categories) { %>
                        <option value="<%= category.getCategoryId() %>" <%= categoryId == category.getCategoryId() ? "selected" : "" %>><c:out value="<%= category.getCategoryName() %>"/></option>
                    <% } %>
                </select>
                </label>
                <label>Sắp xếp
                <select name="sortBy" onchange="this.form.submit()">
                    <option value="">Sắp xếp mặc định</option>
                    <option value="asc" <%= "asc".equals(sortBy) ? "selected" : "" %>>Giá thấp đến cao</option>
                    <option value="desc" <%= "desc".equals(sortBy) ? "selected" : "" %>>Giá cao đến thấp</option>
                </select>
                </label>
            </div>
            <% if (!keyword.isEmpty() || categoryId != 0 || !sortBy.isEmpty()) { %>
                <a class="order-reset-link" href="${pageContext.request.contextPath}/order">Xóa bộ lọc</a>
            <% } %>
        </form>
        <nav class="category-tabs order-category-tabs" aria-label="Danh mục món ăn">
            <a href="${pageContext.request.contextPath}/order?keyword=<%= encodedKeyword %>&sortBy=<%= encodedSortBy %>" class="tab-item <%= categoryId == 0 ? "active" : "" %>">Tất cả</a>
            <% if (categories != null) for (Category category : categories) { %>
                <a href="${pageContext.request.contextPath}/order?categoryId=<%= category.getCategoryId() %>&keyword=<%= encodedKeyword %>&sortBy=<%= encodedSortBy %>" class="tab-item <%= categoryId == category.getCategoryId() ? "active" : "" %>"><c:out value="<%= category.getCategoryName() %>"/></a>
            <% } %>
        </nav>
        <section class="dishes-grid" aria-label="Danh sách món ăn">
            <% if (dishes != null && !dishes.isEmpty()) for (Dish dish : dishes) { %>
                <article class="dish-card">
                    <div class="dish-img-wrapper">
                        <img src="${pageContext.request.contextPath}/<%= dish.getImage() != null && dish.getImage().startsWith("images/") ? "assets/" + dish.getImage() : dish.getImage() %>" alt="<c:out value='<%= dish.getDishName() %>'/>" loading="lazy">
                        <% if (dish.isIsFeatured()) { %><span class="badge-featured"><i class="fa-solid fa-fire" aria-hidden="true"></i> Bán chạy</span><% } %>
                    </div>
                    <div class="dish-info">
                        <span class="dish-category"><c:out value="<%= dish.getCategoryName() %>"/></span>
                        <h2 class="dish-name"><c:out value="<%= dish.getDishName() %>"/></h2>
                        <p class="dish-desc"><c:out value="<%= dish.getDescription() %>"/></p>
                        <div class="dish-bottom"><span class="dish-price"><%= formatter.format(dish.getPrice()) %> đ <span class="dish-unit">/ <%= dish.getUnit() %></span></span></div>
                        <form action="${pageContext.request.contextPath}/cart" method="post" class="order-add-form">
                            <input type="hidden" name="csrfToken" value="${sessionScope.orderCsrf}">
                            <input type="hidden" name="action" value="add">
                            <input type="hidden" name="returnTo" value="${orderReturnTo}">
                            <input type="hidden" name="dishId" value="<%= dish.getDishId() %>">
                            <label>Số lượng <input type="number" name="quantity" min="1" max="99" value="1"></label>
                            <button type="submit"><i class="fa-solid fa-plus" aria-hidden="true"></i> Thêm món</button>
                        </form>
                    </div>
                </article>
            <% } else { %>
                <div class="no-result order-no-result"><span class="order-empty-icon"><i class="fa-solid fa-utensils" aria-hidden="true"></i></span><h2>Không tìm thấy món phù hợp</h2><p>Thử đổi từ khóa hoặc danh mục để xem thêm lựa chọn.</p><a href="${pageContext.request.contextPath}/order">Xóa bộ lọc</a></div>
            <% } %>
        </section>
        <% if (totalPages > 1) { %>
            <nav class="pagination" aria-label="Phân trang">
                <% for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) { %>
                    <a href="${pageContext.request.contextPath}/order?page=<%= pageNumber %>&keyword=<%= encodedKeyword %>&categoryId=<%= categoryId %>&sortBy=<%= encodedSortBy %>" class="page-link <%= pageNumber == currentPage ? "active" : "" %>" <%= pageNumber == currentPage ? "aria-current='page'" : "" %>><%= pageNumber %></a>
                <% } %>
            </nav>
        <% } %>
    </main>
</body>
</html>
