<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Dish" %>
<%@ page import="model.Category" %>
<%@ page import="java.text.DecimalFormat" %>
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
    <title>Thực Đơn - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/menu.css?v=1.0">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

    <!-- Header -->
    <jsp:include page="includes/header.jsp" />

    <div class="menu-container">
        
        <div class="menu-title-section">
            <span class="menu-subtitle">Mỹ vị Đất Việt</span>
            <h1 class="menu-main-title">Thực Đơn Á La Carte</h1>
        </div>

        <!-- Thanh Tìm Kiếm, Lọc & Sắp Xếp -->
        <form action="${pageContext.request.contextPath}/menu" method="GET" class="filter-bar">
            
            <div class="search-box">
                <input type="text" name="keyword" placeholder="Tìm tên món ăn (VD: Phở, Bún chả...)" value="<%= keyword %>">
                <button type="submit"><i class="fa-solid fa-magnifying-glass"></i></button>
            </div>

            <div class="filter-group">
                <!-- Filter Category Dropdown -->
                <select name="categoryId" onchange="this.form.submit()">
                    <option value="0">--- Tất cả danh mục ---</option>
                    <% if (categories != null) { 
                        for (Category c : categories) { %>
                            <option value="<%= c.getCategoryId() %>" <%= categoryId == c.getCategoryId() ? "selected" : "" %>>
                                <%= c.getCategoryName() %>
                            </option>
                    <%   } 
                       } %>
                </select>

                <!-- Sort Price Dropdown -->
                <select name="sortBy" onchange="this.form.submit()">
                    <option value="">Sắp xếp mặc định</option>
                    <option value="asc" <%= "asc".equals(sortBy) ? "selected" : "" %>>Giá: Thấp đến Cao</option>
                    <option value="desc" <%= "desc".equals(sortBy) ? "selected" : "" %>>Giá: Cao đến Thấp</option>
                </select>
            </div>
        </form>

        <!-- Category Quick Tabs -->
        <div class="category-tabs">
            <a href="${pageContext.request.contextPath}/menu?keyword=<%= keyword %>&sortBy=<%= sortBy %>" 
               class="tab-item <%= categoryId == 0 ? "active" : "" %>">Tất cả</a>
            <% if (categories != null) { 
                for (Category c : categories) { %>
                    <a href="${pageContext.request.contextPath}/menu?categoryId=<%= c.getCategoryId() %>&keyword=<%= keyword %>&sortBy=<%= sortBy %>" 
                       class="tab-item <%= categoryId == c.getCategoryId() ? "active" : "" %>">
                       <%= c.getCategoryName() %>
                    </a>
            <%   } 
               } %>
        </div>

        <!-- Danh Sách Món Ăn Grid -->
        <div class="dishes-grid">
            <% if (dishes != null && !dishes.isEmpty()) { 
                for (Dish d : dishes) { %>
                    <div class="dish-card">
                        <div class="dish-img-wrapper">
                            <img src="${pageContext.request.contextPath}/<%= d.getImage() %>" alt="<%= d.getDishName() %>">
                            <% if (d.isIsFeatured()) { %>
                                <span class="badge-featured"><i class="fa-solid fa-fire"></i> Best Seller</span>
                            <% } %>
                        </div>
                        <div class="dish-info">
                            <span class="dish-category"><%= d.getCategoryName() %></span>
                            <h3 class="dish-name"><%= d.getDishName() %></h3>
                            <p class="dish-desc"><%= d.getDescription() != null ? d.getDescription() : "" %></p>
                            <div class="dish-bottom">
                                <span class="dish-price">
                                    <%= formatter.format(d.getPrice()) %> đ 
                                    <span class="dish-unit">/ <%= d.getUnit() %></span>
                                </span>
                            </div>
                        </div>
                    </div>
            <%   } 
               } else { %>
                <div class="no-result">
                    <i class="fa-regular fa-face-frown fa-3x" style="margin-bottom: 10px; color: #d89b36;"></i>
                    <h3>Không tìm thấy món ăn phù hợp!</h3>
                    <p>Quý khách vui lòng thử tìm kiếm từ khóa khác.</p>
                </div>
            <% } %>
        </div>

        <!-- Phân Trang (Pagination) -->
        <% if (totalPages > 1) { %>
            <div class="pagination">
                <% for (int i = 1; i <= totalPages; i++) { %>
                    <a href="${pageContext.request.contextPath}/menu?page=<%= i %>&keyword=<%= keyword %>&categoryId=<%= categoryId %>&sortBy=<%= sortBy %>" 
                       class="page-link <%= i == currentPage ? "active" : "" %>">
                        <%= i %>
                    </a>
                <% } %>
            </div>
        <% } %>

    </div>

</body>
</html>