<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/auth.css?v=1.1">
</head>
<body>

    <div class="auth-container">
        <!-- Nửa trái: Ảnh món ăn & Slogan -->
        <div class="auth-banner" style="background-image: url('${pageContext.request.contextPath}/images/login.jpg');">
            <div class="banner-content">
                <p class="banner-subtitle">ẨM THỰC BA MIỀN</p>
                <h1 class="banner-title">“Hương vị quê nhà,<br>trọn vẹn từng món.”</h1>
            </div>
        </div>

        <!-- Nửa phải: Form đăng nhập -->
        <div class="auth-content">
            <h1 class="brand-logo">Việt <span>À la carte</span></h1>

            <div class="auth-card">
                <div class="card-header">
                    <h2 class="card-title">Đăng nhập</h2>
                    <p class="card-subtitle">Mời quý khách vào bàn</p>
                </div>

                <!-- Hiển thị lỗi đăng nhập -->
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-error">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <!-- Hiển thị thông báo khi đổi mật khẩu thành công -->
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert-success">
                        <%= request.getAttribute("success") %>
                    </div>
                <% } %>

                <form action="login" method="post">
                    <div class="form-group">
                        <label>Email hoặc tên tài khoản</label>
                        <input type="text" name="account" value="${param.account}" class="form-input" required />
                    </div>

                    <div class="form-group">
                        <div class="form-label-row">
                            <label style="margin-bottom:0;">Mật khẩu</label>
                            <!-- Nút mở Modal Quên Mật Khẩu -->
                            <a href="javascript:void(0)" onclick="openForgotModal()" class="forgot-link">Quên mật khẩu?</a>
                        </div>
                        <input type="password" name="password" class="form-input" required />
                    </div>

                    <button type="submit" class="btn-submit">Đăng nhập</button>
                </form>

                <div class="auth-footer">
                    Chưa có tài khoản? <a href="register.jsp">Đăng ký ngay</a>
                </div>
            </div>
        </div>
    </div>

    <!-- MÀN HÌNH NỔI (MODAL) QUÊN MẬT KHẨU -->
    <div id="forgotModal" class="modal-overlay <%= request.getAttribute("showForgotModal") != null ? "active" : "" %>">
        <div class="modal-card">
            <button class="modal-close" onclick="closeForgotModal()">&times;</button>
            
            <div class="card-header">
                <h2 class="card-title" style="font-size:22px;">Lấy lại mật khẩu</h2>
                <p class="card-subtitle">Nhập email đăng ký để nhận mật khẩu mới</p>
            </div>

            <% if (request.getAttribute("forgotError") != null) { %>
                <div class="alert-error">
                    <%= request.getAttribute("forgotError") %>
                </div>
            <% } %>

            <form action="forgot-password" method="post">
                <div class="form-group">
                    <label>Email tài khoản</label>
                    <input type="email" name="email" value="${param.email}" class="form-input" placeholder="nhapemail@example.com" required />
                </div>

                <button type="submit" class="btn-submit">Nhận mật khẩu mới</button>
            </form>
        </div>
    </div>

    <script>
        function openForgotModal() {
            document.getElementById("forgotModal").classList.add("active");
        }

        function closeForgotModal() {
            document.getElementById("forgotModal").classList.remove("active");
        }

        // Đóng modal khi click ra ngoài khung trắng
        window.onclick = function(event) {
            var modal = document.getElementById("forgotModal");
            if (event.target === modal) {
                closeForgotModal();
            }
        };
    </script>

</body>
</html>