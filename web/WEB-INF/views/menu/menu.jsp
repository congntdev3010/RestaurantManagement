<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Dish" %>
<%@ page import="model.Category" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%!
    // Chống XSS khi in dữ liệu ra HTML
    private static String esc(Object value) {
        if (value == null) return "";
        String s = value.toString();
        StringBuilder out = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '&': out.append("&amp;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(ch);
            }
        }
        return out.toString();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
%>
<%
    String ctx = request.getContextPath();
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    List<Dish> dishes = (List<Dish>) request.getAttribute("dishes");

    String keyword = request.getAttribute("keyword") != null ? (String) request.getAttribute("keyword") : "";
    int categoryId = request.getAttribute("categoryId") != null ? (Integer) request.getAttribute("categoryId") : 0;
    String sortBy = (String) request.getAttribute("sortBy");
    if (!"asc".equals(sortBy) && !"desc".equals(sortBy)) sortBy = "";

    int currentPage = request.getAttribute("currentPage") != null ? (Integer) request.getAttribute("currentPage") : 1;
    int totalPages = request.getAttribute("totalPages") != null ? (Integer) request.getAttribute("totalPages") : 0;
    int totalDishes = request.getAttribute("totalDishes") != null ? (Integer) request.getAttribute("totalDishes") : 0;

    String currentCategoryName = null;
    if (categories != null) {
        for (Category c : categories) {
            if (c.getCategoryId() == categoryId) currentCategoryName = c.getCategoryName();
        }
    }

    // Đường dẫn giữ nguyên bộ lọc hiện tại
    String baseQuery = "keyword=" + enc(keyword) + "&amp;sortBy=" + enc(sortBy);
    String pageQuery = baseQuery + "&amp;categoryId=" + categoryId;

    DecimalFormat formatter = new DecimalFormat("#,###");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thực Đơn - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="<%= ctx %>/assets/css/menu.css?v=2.0">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body class="menu-page">

    <jsp:include page="/WEB-INF/views/fragments/header.jsp" />

    <!-- Banner -->
    <section class="menu-hero">
        <div class="menu-hero-inner">
            <span class="menu-subtitle">Mỹ vị Đất Việt</span>
            <h1 class="menu-main-title">Thực Đơn Á La Carte</h1>
            <p class="menu-hero-text">Tinh hoa ẩm thực ba miền, chế biến mỗi ngày từ nguyên liệu tươi.</p>

            <!-- Thanh tìm kiếm -->
            <form action="<%= ctx %>/menu" method="GET" accept-charset="UTF-8" class="hero-search" role="search">
                <i class="fa-solid fa-magnifying-glass hero-search-icon" aria-hidden="true"></i>
                <input type="search" name="keyword" id="menu-keyword" autocomplete="off"
                       placeholder="Tìm món — gõ có dấu hoặc không dấu (VD: canh ga chien)"
                       value="<%= esc(keyword) %>" aria-label="Tìm tên món ăn">
                <% if (categoryId > 0) { %><input type="hidden" name="categoryId" value="<%= categoryId %>"><% } %>
                <% if (!sortBy.isEmpty()) { %><input type="hidden" name="sortBy" value="<%= sortBy %>"><% } %>
                <button type="submit">Tìm kiếm</button>
            </form>
        </div>
    </section>

    <main class="menu-container">

        <!-- Thanh danh mục + sắp xếp -->
        <div class="menu-toolbar">
            <nav class="category-tabs" aria-label="Danh mục món ăn">
                <a href="<%= ctx %>/menu?<%= baseQuery %>" class="tab-item <%= categoryId == 0 ? "active" : "" %>">Tất cả</a>
                <% if (categories != null) {
                    for (Category c : categories) { %>
                        <a href="<%= ctx %>/menu?categoryId=<%= c.getCategoryId() %>&amp;<%= baseQuery %>"
                           class="tab-item <%= categoryId == c.getCategoryId() ? "active" : "" %>"><%= esc(c.getCategoryName()) %></a>
                <%   }
                   } %>
            </nav>

            <form action="<%= ctx %>/menu" method="GET" accept-charset="UTF-8" class="sort-form">
                <input type="hidden" name="keyword" value="<%= esc(keyword) %>">
                <input type="hidden" name="categoryId" value="<%= categoryId %>">
                <label for="sortBy"><i class="fa-solid fa-arrow-down-wide-short" aria-hidden="true"></i></label>
                <select name="sortBy" id="sortBy" onchange="this.form.submit()" aria-label="Sắp xếp theo giá">
                    <option value="">Mặc định</option>
                    <option value="asc" <%= "asc".equals(sortBy) ? "selected" : "" %>>Giá thấp → cao</option>
                    <option value="desc" <%= "desc".equals(sortBy) ? "selected" : "" %>>Giá cao → thấp</option>
                </select>
            </form>
        </div>

        <!-- Tóm tắt kết quả -->
        <div class="result-summary">
            <p>
                <% if (!keyword.isEmpty()) { %>
                    Tìm thấy <strong><%= totalDishes %></strong> món cho “<strong><%= esc(keyword) %></strong>”
                <% } else { %>
                    Có <strong><%= totalDishes %></strong> món
                <% } %>
                <% if (currentCategoryName != null) { %> trong <strong><%= esc(currentCategoryName) %></strong><% } %>
            </p>
            <% if (!keyword.isEmpty() || categoryId > 0 || !sortBy.isEmpty()) { %>
                <a class="clear-filter" href="<%= ctx %>/menu"><i class="fa-solid fa-xmark" aria-hidden="true"></i> Xoá bộ lọc</a>
            <% } %>
        </div>

        <!-- Danh sách món -->
        <section class="dishes-grid" aria-label="Danh sách món ăn">
            <% if (dishes != null && !dishes.isEmpty()) {
                for (Dish d : dishes) {
                    String img = d.getImage() != null && d.getImage().startsWith("images/") ? "assets/" + d.getImage() : d.getImage(); %>
                    <article class="dish-card">
                        <div class="dish-img-wrapper">
                            <img src="<%= ctx %>/<%= esc(img) %>" alt="<%= esc(d.getDishName()) %>" loading="lazy">
                            <% if (d.isIsFeatured()) { %>
                                <span class="badge-featured"><i class="fa-solid fa-fire" aria-hidden="true"></i> Bán chạy</span>
                            <% } %>
                        </div>
                        <div class="dish-info">
                            <span class="dish-category"><%= esc(d.getCategoryName()) %></span>
                            <h2 class="dish-name"><%= esc(d.getDishName()) %></h2>
                            <p class="dish-desc"><%= esc(d.getDescription()) %></p>
                            <div class="dish-bottom">
                                <span class="dish-price">
                                    <%= formatter.format(d.getPrice()) %>&nbsp;đ
                                    <span class="dish-unit">/ <%= esc(d.getUnit()) %></span>
                                </span>
                            </div>
                        </div>
                    </article>
            <%   }
               } else { %>
                <div class="no-result">
                    <i class="fa-solid fa-bowl-rice" aria-hidden="true"></i>
                    <h3>Không tìm thấy món ăn phù hợp</h3>
                    <p>Quý khách thử từ khoá ngắn hơn (VD: “ga”, “pho”) hoặc chọn danh mục khác.</p>
                    <a class="btn-outline" href="<%= ctx %>/menu">Xem toàn bộ thực đơn</a>
                </div>
            <% } %>
        </section>

        <!-- Phân trang -->
        <% if (totalPages > 1) { %>
            <nav class="pagination" aria-label="Phân trang">
                <% if (currentPage > 1) { %>
                    <a class="page-link" href="<%= ctx %>/menu?page=<%= currentPage - 1 %>&amp;<%= pageQuery %>" aria-label="Trang trước"><i class="fa-solid fa-chevron-left"></i></a>
                <% } %>
                <% for (int i = 1; i <= totalPages; i++) { %>
                    <a class="page-link <%= i == currentPage ? "active" : "" %>" href="<%= ctx %>/menu?page=<%= i %>&amp;<%= pageQuery %>"
                       <%= i == currentPage ? "aria-current=\"page\"" : "" %>><%= i %></a>
                <% } %>
                <% if (currentPage < totalPages) { %>
                    <a class="page-link" href="<%= ctx %>/menu?page=<%= currentPage + 1 %>&amp;<%= pageQuery %>" aria-label="Trang sau"><i class="fa-solid fa-chevron-right"></i></a>
                <% } %>
            </nav>
        <% } %>

    </main>

</body>
</html>
