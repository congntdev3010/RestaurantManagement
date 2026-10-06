<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Xác minh OTP - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/auth.css?v=1.2">
</head>
<body>

    <div class="auth-container">
        <!-- Nửa trái: Banner thương hiệu -->
        <div class="auth-banner" style="background-image: url('${pageContext.request.contextPath}/images/login.jpg');">
            <div class="banner-content">
                <p class="banner-subtitle">ẨM THỰC BA MIỀN</p>
                <h1 class="banner-title">“Hương vị quê nhà,<br>trọn vẹn từng món.”</h1>
            </div>
        </div>

        <!-- Nửa phải: Form nhập OTP -->
        <div class="auth-content">
            <h1 class="brand-logo">Việt <span>À la carte</span></h1>

            <div class="auth-card">
                <div class="card-header">
                    <h2 class="card-title">Xác minh tài khoản</h2>
                    <p class="card-subtitle">
                        Mã OTP 6 chữ số đã được gửi đến email:<br>
                        <strong style="color: #2b2b2b;"><%= session.getAttribute("registerEmail") != null ? session.getAttribute("registerEmail") : "" %></strong>
                    </p>
                </div>

                <!-- Thông báo Lỗi -->
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-error">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <!-- Thông báo Thành công (Gửi lại mã thành công) -->
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert-success">
                        <%= request.getAttribute("success") %>
                    </div>
                <% } %>

                <form action="verify-otp" method="post">
                    <div class="form-group">
                        <label style="text-align: center; display: block;">Nhập mã OTP (6 chữ số)</label>
                        <input type="text" 
                               name="otp" 
                               maxlength="6" 
                               pattern="\d{6}" 
                               placeholder="******" 
                               class="form-input otp-input" 
                               required 
                               autofocus 
                               autocomplete="off" />
                    </div>

                    <button type="submit" class="btn-submit">Xác nhận kích hoạt</button>
                </form>

                <!-- KHU VỰC GỬI LẠI MÃ OTP & ĐẾM NGƯỢC -->
                <div class="resend-box">
                    <span>Bạn chưa nhận được mã? </span>
                    <button id="resendBtn" class="btn-resend" onclick="resendOTP()" disabled>
                        Gửi lại mã (<span id="timer">60</span>s)
                    </button>
                </div>

                <div class="auth-footer" style="margin-top: 15px;">
                    Nhập sai email? <a href="register.jsp">Đăng ký lại</a>
                </div>
            </div>
        </div>
    </div>

    <!-- SCRIPT ĐẾM NGƯỢC 60s TRÁNH SPAM GỬI LẠI -->
    <script>
        let timeLeft = 60;
        const timerElement = document.getElementById('timer');
        const resendBtn = document.getElementById('resendBtn');

        const countdown = setInterval(() => {
            timeLeft--;
            if (timeLeft <= 0) {
                clearInterval(countdown);
                resendBtn.disabled = false;
                resendBtn.innerText = "Gửi lại mã ngay";
            } else {
                timerElement.innerText = timeLeft;
            }
        }, 1000);

        function resendOTP() {
            // Chuyển hướng sang Servlet gửi lại OTP
            window.location.href = "${pageContext.request.contextPath}/resend-otp";
        }
    </script>

</body>
</html>