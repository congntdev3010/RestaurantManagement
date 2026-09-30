<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng Nhập - Việt À la carte</title>
    <link rel="stylesheet" href="css/login-style.css">
</head>
<body>
    <div class="login-container">
        <!-- Left Section - Banner -->
        <div class="login-left">
            <div class="banner-content">
                <h1 class="tagline">Hương vị quê nhà, <span>trọn vẹn từng món.</span></h1>
                <p class="subtitle">Ẩm THỰC BA MIỀN</p>
                <div class="decoration"></div>
            </div>
            <div class="banner-image">
                <img src="images/login.jpg" alt="Vietnamese Cuisine" class="banner-img">
            </div>
        </div>

        <!-- Right Section - Login Form -->
        <div class="login-right">
            <div class="form-container">
                <h2 class="form-title">Việt À la carte</h2>
                <p class="form-subtitle">Mời quý khách vào bàn</p>

                <!-- Error/Success Messages -->
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-error">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>
                
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert alert-success">
                        <%= request.getAttribute("success") %>
                    </div>
                <% } %>

                <!-- Login Form -->
                <form action="login" method="post" class="login-form">
                    <div class="form-group">
                        <label for="account">Email hoặc tên tài khoản</label>
                        <input type="text" id="account" name="account" class="form-input" 
                               placeholder="Nhập email hoặc tài khoản" required>
                    </div>

                    <div class="form-group">
                        <div class="label-row">
                            <label for="password">Mật khẩu</label>
                            <a href="forgot-password" class="forgot-password">Quên mật khẩu?</a>
                        </div>
                        <input type="password" id="password" name="password" class="form-input" 
                               placeholder="Nhập mật khẩu" required>
                    </div>

                    <button type="submit" class="btn-login">Đăng nhập</button>
                </form>

                <!-- Divider -->
                <div class="divider">

                </div>

                <!-- Social Login (Optional) -->
                
                <!-- Register Link -->
                <p class="register-prompt">
                    Chưa có tài khoản? 
                    <a href="register" class="register-link">Đăng ký ngay</a>
                </p>
            </div>
        </div>
    </div>
</body>
</html>
