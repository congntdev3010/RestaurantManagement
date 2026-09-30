<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đăng nhập - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/auth.css">
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

                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-error">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <form action="login" method="post">
                    <div class="form-group">
                        <label>Email hoặc tên tài khoản</label>
                        <input type="text" name="account" class="form-input" required />
                    </div>

                    <div class="form-group">
                        <div class="form-label-row">
                            <label style="margin-bottom:0;">Mật khẩu</label>
                            <a href="#" class="forgot-link">Quên mật khẩu?</a>
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

</body>
</html>