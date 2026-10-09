<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String currentURI = request.getRequestURI();
%>

<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/header.css?v=1.2">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

<header class="site-header">
    <!-- Logo -->
    <a href="${pageContext.request.contextPath}/home.jsp" class="header-logo">
        Việt <span>À la carte</span>
    </a>

    <!-- Điều hướng -->
    <nav class="header-nav">
        <a href="${pageContext.request.contextPath}/home.jsp" class="<%= currentURI.contains("home") ? "active" : "" %>">Trang chủ</a>
        <a href="${pageContext.request.contextPath}/menu" class="<%= currentURI.contains("menu") ? "active" : "" %>">Thực đơn</a>
        <a href="${pageContext.request.contextPath}/booking" class="<%= currentURI.contains("booking") ? "active" : "" %>">Đặt bàn</a>
        <a href="${pageContext.request.contextPath}/news" class="<%= currentURI.contains("news") ? "active" : "" %>">Tin tức</a>
        <a href="${pageContext.request.contextPath}/review" class="<%= currentURI.contains("review") ? "active" : "" %>">Đánh giá</a>
    </nav>

    <!-- Khu vực Tài khoản -->
    <div class="header-auth">
        <% if (currentUser == null) { %>
            <!-- GUEST: Chưa đăng nhập -->
            <a href="${pageContext.request.contextPath}/login.jsp" class="btn-login">Đăng nhập</a>
            <a href="${pageContext.request.contextPath}/register.jsp" class="btn-register">Đăng ký</a>
        <% } else { %>
            <!-- CUSTOMER: Đã đăng nhập -> Hiển thị icon user + Tên khách hàng -->
            <div class="user-account">
                <div class="user-profile-btn">
                    <i class="fa-solid fa-user user-icon"></i>
                    <span class="user-name">
                        <%= (currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()) 
                                ? currentUser.getFullName() 
                                : currentUser.getUsername() %>
                    </span>
                    <i class="fa-solid fa-chevron-down arrow-icon"></i>
                </div>

                <!-- Dropdown Menu khi di chuột -->
                <div class="dropdown-menu">
                    <a href="${pageContext.request.contextPath}/profile"><i class="fa-regular fa-id-card"></i> Thông tin cá nhân</a>
                    <a href="${pageContext.request.contextPath}/my-bookings"><i class="fa-solid fa-utensils"></i> Lịch sử đặt bàn</a>
                    <hr>
                    <a href="${pageContext.request.contextPath}/logout" class="logout-item"><i class="fa-solid fa-right-from-bracket"></i> Đăng xuất</a>
                </div>
            </div>
        <% } %>
    </div>
</header>