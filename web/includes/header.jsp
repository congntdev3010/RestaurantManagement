<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="model.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String currentURI = request.getRequestURI();
        String roleName = currentUser == null ? "" : currentUser.getRoleName();
        boolean isAdmin = "Admin".equalsIgnoreCase(roleName);
        boolean isManager = "Manager".equalsIgnoreCase(roleName);
        boolean hideHeader = isAdmin || isManager || "Server".equalsIgnoreCase(roleName)
            || "Receptionist".equalsIgnoreCase(roleName);
        boolean canViewTables = isManager || "Server".equalsIgnoreCase(roleName)
            || "Receptionist".equalsIgnoreCase(roleName);
        boolean showStaffSidebar = isAdmin || isManager || canViewTables
            || "Chef".equalsIgnoreCase(roleName);
%>

<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/header.css?v=1.7">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

<% if (!hideHeader) { %>
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
        <div>
            <strong>Vận hành nhà hàng</strong>
            <span><%= escapeHtmlSidebarRole(roleName) %></span>
        </div>
    </div>
    <nav class="staff-sidebar-nav">
        <a class="sidebar-link <%= currentURI.contains("home") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/home.jsp" title="Trang chủ" aria-label="Trang chủ">
            <i class="fa-solid fa-house" aria-hidden="true"></i><span>Trang chủ</span>
        </a>
        <% if (!isAdmin) { %>
        <a class="sidebar-link <%= currentURI.contains("menu") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/menu" title="Thực đơn" aria-label="Thực đơn">
            <i class="fa-solid fa-book-open" aria-hidden="true"></i><span>Thực đơn</span>
        </a>
        <% } %>
        <% if (canViewTables) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-tables") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-tables" title="Sơ đồ bàn" aria-label="Sơ đồ bàn">
            <i class="fa-solid fa-table-cells-large" aria-hidden="true"></i><span>Sơ đồ bàn</span>
        </a>
        <% } %>
        <% if (isManager || isAdmin) { %>
        <a class="sidebar-link <%= currentURI.contains("manage-users") ? "active" : "" %>"
           href="${pageContext.request.contextPath}/manage-users" title="Quản lý tài khoản" aria-label="Quản lý tài khoản">
            <i class="fa-solid fa-users-gear" aria-hidden="true"></i><span>Quản lý tài khoản</span>
        </a>
        <% } %>
    </nav>
    <% if (hideHeader && currentUser != null) { %>
    <div class="sidebar-account">
        <div class="sidebar-account-identity">
            <i class="fa-solid fa-user" aria-hidden="true"></i>
            <span title="<%= escapeHtmlSidebarRole(currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty() ? currentUser.getFullName() : currentUser.getUsername()) %>">
                <%= escapeHtmlSidebarRole(currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()
                    ? currentUser.getFullName() : currentUser.getUsername()) %>
            </span>
        </div>
        <a class="sidebar-account-link" href="${pageContext.request.contextPath}/profile" title="Thông tin cá nhân" aria-label="Thông tin cá nhân">
            <i class="fa-regular fa-id-card" aria-hidden="true"></i><span>Thông tin cá nhân</span>
        </a>
        <a class="sidebar-account-link sidebar-logout" href="${pageContext.request.contextPath}/logout" title="Đăng xuất" aria-label="Đăng xuất">
            <i class="fa-solid fa-right-from-bracket" aria-hidden="true"></i><span>Đăng xuất</span>
        </a>
    </div>
    <% } %>
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