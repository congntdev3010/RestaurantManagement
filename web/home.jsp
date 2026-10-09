<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.User" %>
<%
    User user = (User) session.getAttribute("user");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Trang chủ - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/home.css?v=1.0">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

    <!-- Nhúng Header -->
    <jsp:include page="includes/header.jsp" />

    <!-- Hero Banner -->
    <section class="hero-section">
        <div class="hero-content">
            <% if (user != null) { %>
                <div class="welcome-badge">
                    <i class="fa-solid fa-crown"></i>
                    <span>Xin chào quý khách, <strong><%= (user.getFullName() != null && !user.getFullName().trim().isEmpty()) ? user.getFullName() : user.getUsername() %></strong>!</span>
                </div>
            <% } %>

            <p class="hero-subtitle">Mỹ vị Tinh hoa Việt</p>
            <h1 class="hero-title">Trải Nghiệm Ẩm Thực Á la carte Đẳng Cấp</h1>
            <p class="hero-desc">
                Hương vị truyền thống ba miền hòa quyện cùng nghệ thuật chế biến hiện đại. 
                Thưởng thức những món ăn được phục vụ tận tâm trong không gian ấm cúng.
            </p>
            
            <div class="hero-buttons">
                <a href="${pageContext.request.contextPath}/booking" class="btn-hero-primary">
                    <i class="fa-regular fa-calendar-check"></i> Đặt bàn ngay
                </a>
                <a href="${pageContext.request.contextPath}/menu" class="btn-hero-secondary">
                    <i class="fa-solid fa-utensils"></i> Xem Thực Đơn
                </a>
            </div>
        </div>
    </section>

</body>
</html>