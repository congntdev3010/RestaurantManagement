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
        boolean isReceptionist = "Receptionist".equalsIgnoreCase(roleName);
        boolean hideHeader = isAdmin || isManager || "Server".equalsIgnoreCase(roleName)
            || "Receptionist".equalsIgnoreCase(roleName) || isChef;
        boolean canViewTables = isManager || "Server".equalsIgnoreCase(roleName)
            || "Receptionist".equalsIgnoreCase(roleName);
        boolean showStaffSidebar = isAdmin || isManager || canViewTables || isChef;
%>

<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/header.css?v=2.0">
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
        <a href="${pageContext.request.contextPath}/review" class="<%= currentURI.endsWith("/review") ? "active" : "" %>">Đánh giá</a>
    </nav>
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
                    <a href="${pageContext.request.contextPath}/my-points"><i class="fa-solid fa-star"></i> Điểm tích lũy</a>
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
        <% if ("Server".equalsIgnoreCase(roleName)) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-tables") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-tables" title="Sơ đồ bàn" aria-label="Sơ đồ bàn">
            <i class="fa-solid fa-table-cells-large" aria-hidden="true"></i><span>Sơ đồ bàn</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("manage-orders") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-orders" title="Quản lý order" aria-label="Quản lý order">
            <i class="fa-solid fa-receipt" aria-hidden="true"></i><span>Quản lý order</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("kitchen") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/kitchen" title="Màn hình bếp" aria-label="Màn hình bếp">
            <i class="fa-solid fa-fire-burner" aria-hidden="true"></i><span>Màn hình bếp</span>
        </a>
        <% } else { %>
        <% if (!isAdmin && !isChef) { %>
        <% if (!isManager && !isReceptionist) { %>
        <a class="sidebar-link <%= currentURI.contains("home") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/home" title="Trang chủ" aria-label="Trang chủ">
            <i class="fa-solid fa-house" aria-hidden="true"></i><span>Trang chủ</span>
        </a>
        <% } %>
        <% if (!isReceptionist && !isManager) { %>
        <a class="sidebar-link <%= currentURI.contains("menu") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/menu" title="Thực đơn" aria-label="Thực đơn">
            <i class="fa-solid fa-book-open" aria-hidden="true"></i><span>Thực đơn</span>
        </a>
        <% } %>
        <% if (!isManager && !isReceptionist) { %>
        <a class="sidebar-link <%= currentURI.endsWith("/news") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/news" title="Tin tức" aria-label="Tin tức">
            <i class="fa-regular fa-newspaper" aria-hidden="true"></i><span>Tin tức</span>
        </a>
        <% } %>
        <% if (canViewTables) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-tables") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-tables" title="Sơ đồ bàn" aria-label="Sơ đồ bàn">
            <i class="fa-solid fa-table-cells-large" aria-hidden="true"></i><span>Sơ đồ bàn</span>
        </a>
        <% } %>
        <% } %>
        <% if (isReceptionist) { %>
        <a class="sidebar-link <%= currentURI.contains("checkin") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/checkin" title="Check-in khách" aria-label="Check-in khách">
            <i class="fa-solid fa-door-open" aria-hidden="true"></i><span>Check-in khách</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("manage-reservations") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-reservations" title="Quản lý đặt bàn" aria-label="Quản lý đặt bàn">
            <i class="fa-solid fa-calendar-check" aria-hidden="true"></i><span>Quản lý đặt bàn</span>
        </a>
        <% if (isReceptionist) { %>
        <a class="sidebar-link <%= currentURI.contains("service-history") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/service-history" title="Lịch sử thanh toán" aria-label="Lịch sử thanh toán">
            <i class="fa-solid fa-clock-rotate-left" aria-hidden="true"></i><span>Lịch sử thanh toán</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("payment") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/payment" title="Thanh toán theo bàn" aria-label="Thanh toán theo bàn">
            <i class="fa-solid fa-cash-register" aria-hidden="true"></i><span>Thanh toán</span>
        </a>
        <% } %>
        <% } %>
        <% if (isChef) { %>
        <a class="sidebar-link <%= currentURI.contains("kitchen") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/kitchen" title="Màn hình bếp" aria-label="Màn hình bếp">
            <i class="fa-solid fa-fire-burner" aria-hidden="true"></i><span>Màn hình bếp</span>
        </a>
        <% } %>
        <% if (isManager || isAdmin) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-users") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-users" title="Quản lý tài khoản" aria-label="Quản lý tài khoản">
            <i class="fa-solid fa-users-gear" aria-hidden="true"></i><span>Quản lý tài khoản</span>
        </a>
        <% } %>
        <% if (isManager) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-reviews") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-reviews" title="Duyệt đánh giá" aria-label="Duyệt đánh giá">
            <i class="fa-solid fa-star" aria-hidden="true"></i><span>Duyệt đánh giá</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("service-history") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/service-history" title="Lịch sử thanh toán" aria-label="Lịch sử thanh toán">
            <i class="fa-solid fa-clock-rotate-left" aria-hidden="true"></i><span>Lịch sử thanh toán</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("manage-news") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-news" title="Quản lý tin tức" aria-label="Quản lý tin tức">
            <i class="fa-regular fa-newspaper" aria-hidden="true"></i><span>Quản lý tin tức</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("loyalty-settings") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/loyalty-settings" title="Quy đổi điểm" aria-label="Quy đổi điểm">
            <i class="fa-solid fa-star" aria-hidden="true"></i><span>Quy đổi điểm</span>
        </a>
        <a class="sidebar-link <%= currentURI.contains("manage-dishes") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-dishes" title="Quản lý thực đơn" aria-label="Quản lý thực đơn">
            <i class="fa-solid fa-utensils" aria-hidden="true"></i><span>Quản lý thực đơn</span>
        </a>
        <% } %>
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
