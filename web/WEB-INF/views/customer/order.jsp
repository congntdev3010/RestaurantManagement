<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Dish" %>
<%@ page import="model.Category" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    List<Dish> dishes = (List<Dish>) request.getAttribute("dishes");
    String keyword = request.getAttribute("keyword") != null ? (String) request.getAttribute("keyword") : "";
    int categoryId = request.getAttribute("categoryId") != null ? (Integer) request.getAttribute("categoryId") : 0;
    String sortBy = request.getAttribute("sortBy") != null ? (String) request.getAttribute("sortBy") : "";
    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");
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
            <div>
                <span class="menu-subtitle">Chọn món yêu thích</span>
                <h1 class="menu-main-title">Gọi món</h1>
            </div>
            <a class="button" href="${pageContext.request.contextPath}/cart"><i class="fa-solid fa-cart-shopping" aria-hidden="true"></i> Xem giỏ hàng</a>
        </header>
        <form action="${pageContext.request.contextPath}/order" method="get" accept-charset="UTF-8" class="filter-bar">
            <div class="search-box">
                <input type="text" name="keyword" placeholder="Tìm món ăn" value="<%= keyword %>">
                <button type="submit" aria-label="Tìm kiếm"><i class="fa-solid fa-magnifying-glass"></i></button>
            </div>
            <div class="filter-group">
                <select name="categoryId" onchange="this.form.submit()">
                    <option value="0">Tất cả danh mục</option>
                    <% if (categories != null) for (Category category : categories) { %>
                        <option value="<%= category.getCategoryId() %>" <%= categoryId == category.getCategoryId() ? "selected" : "" %>><%= category.getCategoryName() %></option>
                    <% } %>
                </select>
                <select name="sortBy" onchange="this.form.submit()">
                    <option value="">Sắp xếp mặc định</option>
                    <option value="asc" <%= "asc".equals(sortBy) ? "selected" : "" %>>Giá thấp đến cao</option>
                    <option value="desc" <%= "desc".equals(sortBy) ? "selected" : "" %>>Giá cao đến thấp</option>
                </select>
            </div>
        </form>
        <nav class="category-tabs" aria-label="Danh mục món ăn">
            <a href="${pageContext.request.contextPath}/order?keyword=<%= keyword %>&sortBy=<%= sortBy %>" class="tab-item <%= categoryId == 0 ? "active" : "" %>">Tất cả</a>
            <% if (categories != null) for (Category category : categories) { %>
                <a href="${pageContext.request.contextPath}/order?categoryId=<%= category.getCategoryId() %>&keyword=<%= keyword %>&sortBy=<%= sortBy %>" class="tab-item <%= categoryId == category.getCategoryId() ? "active" : "" %>"><%= category.getCategoryName() %></a>
            <% } %>
        </nav>
        <section class="dishes-grid" aria-label="Danh sách món ăn">
            <% if (dishes != null && !dishes.isEmpty()) for (Dish dish : dishes) { %>
                <article class="dish-card">
                    <div class="dish-img-wrapper">
                        <img src="${pageContext.request.contextPath}/<%= dish.getImage() != null && dish.getImage().startsWith("images/") ? "assets/" + dish.getImage() : dish.getImage() %>" alt="<%= dish.getDishName() %>">
                        <% if (dish.isIsFeatured()) { %><span class="badge-featured"><i class="fa-solid fa-fire" aria-hidden="true"></i> Bán chạy</span><% } %>
                    </div>
                    <div class="dish-info">
                        <span class="dish-category"><%= dish.getCategoryName() %></span>
                        <h2 class="dish-name"><%= dish.getDishName() %></h2>
                        <p class="dish-desc"><%= dish.getDescription() != null ? dish.getDescription() : "" %></p>
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
                <div class="no-result"><h2>Không tìm thấy món phù hợp</h2><p>Thử đổi từ khóa hoặc danh mục.</p></div>
            <% } %>
        </section>
        <% if (totalPages > 1) { %>
            <nav class="pagination" aria-label="Phân trang">
                <% for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) { %>
                    <a href="${pageContext.request.contextPath}/order?page=<%= pageNumber %>&keyword=<%= keyword %>&categoryId=<%= categoryId %>&sortBy=<%= sortBy %>" class="page-link <%= pageNumber == currentPage ? "active" : "" %>"><%= pageNumber %></a>
                <% } %>
            </nav>
        <% } %>
    </main>
</body>
</html>
