<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng Ký - Việt À la carte</title>
    <link rel="stylesheet" href="css/register-style.css">
</head>
<body>
    <div class="register-container">
        <div class="register-wrapper">
            <!-- Form Container -->
            <div class="form-box">
                <h1 class="form-title">Đăng ký</h1>
                <p class="form-subtitle">Trở thành thực khách thân thiết</p>

                <!-- Error Messages -->
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-error">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <!-- Register Form -->
                <form action="register" method="post" class="register-form">
                    <!-- Full Name -->
                    <div class="form-group">
                        <label for="fullName">Họ và tên</label>
                        <input type="text" id="fullName" name="fullName" class="form-input" 
                               placeholder="Nhập họ và tên" required>
                    </div>

                    <!-- Username -->
                    <div class="form-group">
                        <label for="username">Tên tài khoản</label>
                        <input type="text" id="username" name="username" class="form-input" 
                               placeholder="Chọn tên tài khoản" required>
                    </div>

                    <!-- Email -->
                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" class="form-input" 
                               placeholder="Nhập email của bạn" required>
                    </div>

                    <!-- Phone -->
                    <div class="form-group">
                        <label for="phone">Số điện thoại</label>
                        <input type="tel" id="phone" name="phone" class="form-input" 
                               placeholder="Nhập số điện thoại" required>
                    </div>

                    <!-- Password -->
                    <div class="form-group">
                        <label for="password">Mật khẩu</label>
                        <input type="password" id="password" name="password" class="form-input" 
                               placeholder="Tạo mật khẩu mạnh" required>
                    </div>

                    <!-- Confirm Password -->
                    <div class="form-group">
                        <label for="confirmPassword">Nhập lại mật khẩu</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-input" 
                               placeholder="Xác nhận mật khẩu" required>
                    </div>

                    <!-- Submit Button -->
                    <button type="submit" class="btn-register">Tạo tài khoản</button>
                </form>

                <!-- Login Link -->
                <p class="login-prompt">
                    Đã có tài khoản? 
                    <a href="login" class="login-link">Đăng nhập</a>
                </p>
            </div>

            <!-- Decorative Elements -->
            <div class="register-decoration">
                <div class="decoration-circle circle-1"></div>
                <div class="decoration-circle circle-2"></div>
                <div class="decoration-circle circle-3"></div>
            </div>
        </div>
    </div>
</body>
</html>
