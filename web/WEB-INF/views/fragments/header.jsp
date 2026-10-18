<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String currentURI = request.getRequestURI();
        String roleName = currentUser == null ? "" : currentUser.getRoleName();
        String displayName = currentUser == null ? "" : currentUser.getFullName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = currentUser == null ? "" : currentUser.getUsername();
        }
        boolean isAdmin = "Admin".equalsIgnoreCase(roleName);
        boolean isManager = "Manager".equalsIgnoreCase(roleName);
        boolean isChef = "Chef".equalsIgnoreCase(roleName);
        boolean hideHeader = isAdmin || isManager || "Server".equalsIgnoreCase(roleName)
            || "Receptionist".equalsIgnoreCase(roleName) || isChef;
        boolean canViewTables = isManager || "Server".equalsIgnoreCase(roleName)
            || "Receptionist".equalsIgnoreCase(roleName);
        boolean showStaffSidebar = isAdmin || isManager || canViewTables || isChef;
        java.util.Map cartForBadge = (java.util.Map) session.getAttribute("orderCart");
        int cartQuantity = 0;
        if (cartForBadge != null) for (Object value : cartForBadge.values()) {
            if (value instanceof Number) cartQuantity += ((Number) value).intValue();
        }
%>

<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/header.css?v=1.9">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

<% if (!hideHeader) { %>
<header class="site-header">
    <!-- Logo -->
    <a href="${pageContext.request.contextPath}/home" class="header-logo">
        Việt <span>À la carte</span>
    </a>

    <!-- Điều hướng -->
    <nav class="header-nav">
        <a href="${pageContext.request.contextPath}/home" class="<%= currentURI.contains("home") ? "active" : "" %>">Trang chủ</a>
        <a href="${pageContext.request.contextPath}/menu" class="<%= currentURI.contains("menu") ? "active" : "" %>">Thực đơn</a>
        <a href="${pageContext.request.contextPath}/booking" class="<%= currentURI.contains("booking") ? "active" : "" %>">Đặt bàn</a>
        <a href="${pageContext.request.contextPath}/news" class="<%= currentURI.contains("news") ? "active" : "" %>">Tin tức</a>
        <a href="${pageContext.request.contextPath}/review" class="<%= currentURI.contains("review") ? "active" : "" %>">Đánh giá</a>
    </nav>
    <% if (currentUser == null || "Customer".equalsIgnoreCase(roleName)) { %>
    <a class="cart-nav-link" href="${pageContext.request.contextPath}/cart" aria-label="Giỏ hàng, <%= cartQuantity %> món">
        <i class="fa-solid fa-cart-shopping" aria-hidden="true"></i><span>Giỏ hàng</span><b class="cart-badge" data-cart-count><%= cartQuantity %></b>
    </a>
    <% } %>

    <!-- Khu vực Tài khoản -->
    <div class="header-auth">
        <% if (currentUser == null) { %>
            <!-- GUEST: Chưa đăng nhập -->
            <a href="${pageContext.request.contextPath}/login" class="btn-login">Đăng nhập</a>
            <a href="${pageContext.request.contextPath}/register" class="btn-register">Đăng ký</a>
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
                    <% if ("Customer".equalsIgnoreCase(roleName)) { %>
                    <a href="${pageContext.request.contextPath}/my-orders"><i class="fa-solid fa-receipt"></i> Đơn hàng của tôi</a>
                    <% } %>
                    <% if (!"Server".equalsIgnoreCase(roleName)) { %>
                    <a href="${pageContext.request.contextPath}/my-bookings"><i class="fa-solid fa-utensils"></i> Lịch sử đặt bàn</a>
                    <% } %>
                    <hr>
                    <a href="${pageContext.request.contextPath}/logout" class="logout-item"><i class="fa-solid fa-right-from-bracket"></i> Đăng xuất</a>
                </div>
            </div>
        <% } %>
    </div>
</header>
<% } %>

<% if (showStaffSidebar) { %>
<aside class="staff-sidebar" aria-label="Điều hướng nhân viên">
    <div class="sidebar-brand">
        <span class="sidebar-brand-mark"><i class="fa-solid fa-leaf" aria-hidden="true"></i></span>
        <div class="sidebar-brand-title">
            <strong>Việt À la carte</strong>
            <span class="sidebar-brand-subtitle">Vận hành nhà hàng</span>
        </div>
    </div>
    <nav class="staff-sidebar-nav">
        <a class="sidebar-link <%= currentURI.contains("home") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/home" title="Trang chủ" aria-label="Trang chủ">
            <i class="fa-solid fa-house" aria-hidden="true"></i><span>Trang chủ</span>
        </a>
        <% if (!isAdmin) { %>
        <a class="sidebar-link <%= currentURI.contains("menu") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/menu" title="Thực đơn" aria-label="Thực đơn">
            <i class="fa-solid fa-book-open" aria-hidden="true"></i><span>Thực đơn</span>
        </a>
        <% } %>
        <a class="sidebar-link <%= currentURI.endsWith("/news") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/news" title="Tin tức" aria-label="Tin tức">
            <i class="fa-regular fa-newspaper" aria-hidden="true"></i><span>Tin tức</span>
        </a>
        <% if (canViewTables) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-tables") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-tables" title="Sơ đồ bàn" aria-label="Sơ đồ bàn">
            <i class="fa-solid fa-table-cells-large" aria-hidden="true"></i><span>Sơ đồ bàn</span>
        </a>
        <% } %>
        <% if ("Server".equalsIgnoreCase(roleName) || isManager) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-orders") ? "active" : "" %>" href="${pageContext.request.contextPath}/manage-orders" title="Quản lý đơn hàng" aria-label="Quản lý đơn hàng">
            <i class="fa-solid fa-receipt" aria-hidden="true"></i><span>Đơn hàng</span>
        </a>
        <% } %>
        <% if (isManager || isAdmin) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-users") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-users" title="Quản lý tài khoản" aria-label="Quản lý tài khoản">
            <i class="fa-solid fa-users-gear" aria-hidden="true"></i><span>Quản lý tài khoản</span>
        </a>
        <% } %>
        <% if (isAdmin) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-news") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-news" title="Quản lý tin tức" aria-label="Quản lý tin tức">
            <i class="fa-regular fa-newspaper" aria-hidden="true"></i><span>Quản lý tin tức</span>
        </a>
        <% } %>
    </nav>
    <div class="sidebar-profile">
        <div class="sidebar-profile-trigger" tabindex="0" role="group" aria-label="Thông tin tài khoản">
            <span class="sidebar-profile-avatar"><i class="fa-solid fa-user" aria-hidden="true"></i></span>
            <span class="sidebar-profile-identity">
                <strong title="<%= escapeHtmlSidebarRole(displayName) %>"><%= escapeHtmlSidebarRole(displayName) %></strong>
                <span class="sidebar-profile-role"><%= escapeHtmlSidebarRole(roleName) %></span>
            </span>
            <i class="fa-solid fa-chevron-up sidebar-profile-chevron" aria-hidden="true"></i>
        </div>
        <div class="sidebar-profile-menu">
            <a href="${pageContext.request.contextPath}/profile">
                <i class="fa-regular fa-id-card" aria-hidden="true"></i><span>Thông tin cá nhân</span>
            </a>
            <a href="${pageContext.request.contextPath}/logout" class="sidebar-profile-logout">
                <i class="fa-solid fa-right-from-bracket" aria-hidden="true"></i><span>Đăng xuất</span>
            </a>
        </div>
    </div>
</aside>
<script>
    document.body.classList.add("has-staff-sidebar");
</script>
<% } %>

<%!
    private String escapeHtmlSidebarRole(String value) {
        if (value == null) return "Nhân viên";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
