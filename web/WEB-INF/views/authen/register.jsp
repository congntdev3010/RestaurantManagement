<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Đăng ký - Việt À la carte</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/auth.css">
</head>
<body>

    <div class="auth-container">
        <!-- Nửa trái: Ảnh món ăn & Slogan -->
        <div class="auth-banner" style="background-image: url('${pageContext.request.contextPath}/assets/images/login.jpg');">
            <div class="banner-content">
                <p class="banner-subtitle">ẨM THỰC BA MIỀN</p>
                <h1 class="banner-title">“Hương vị quê nhà,<br>trọn vẹn từng món.”</h1>
            </div>
        </div>

        <!-- Nửa phải: Form đăng ký -->
        <div class="auth-content">
            <h1 class="brand-logo">Việt <span>À la carte</span></h1>

            <div class="auth-card">
                <div class="card-header">
                    <h2 class="card-title">Đăng ký</h2>
                    <p class="card-subtitle">Trở thành thực khách thân thiết</p>
                </div>

                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-error">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <form action="register" method="post">
                    <div class="form-group">
                        <label>Họ và tên</label>
                        <input type="text" name="fullName" value="${param.fullName}" class="form-input" maxlength="30" required />
                    </div>

                    <div class="form-group">
                        <label>Tên tài khoản</label>
                        <input type="text" name="username" value="${param.username}" class="form-input" maxlength="30" required />
                    </div>

                    <div class="form-group">
                        <label>Email</label>
                        <input type="email" name="email" value="${param.email}" class="form-input" maxlength="50" required />
                    </div>

                    <div class="form-group">
                        <label>Số điện thoại</label>
                        <input type="tel" name="phone" value="${param.phone}" class="form-input"
                               pattern="0[0-9]{9}" maxlength="10"
                               title="Số điện thoại gồm 10 chữ số và bắt đầu bằng 0" required />
                    </div>

                    <div class="form-group">
                        <label>Mật khẩu</label>
                        <div style="position:relative;">
                            <input type="password" name="password" id="regPassword" class="form-input" style="padding-right:64px;box-sizing:border-box;" minlength="8" maxlength="30" required />
                            <button type="button" onclick="togglePw('regPassword', this)" aria-label="Hiện mật khẩu"
                                    style="position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:0;cursor:pointer;color:#666;font-size:13px;">Hiện</button>
                        </div>
                    </div>

                    <div class="form-group">
                        <label>Nhập lại mật khẩu</label>
                        <div style="position:relative;">
                            <input type="password" name="confirmPassword" id="regConfirmPassword" class="form-input" style="padding-right:64px;box-sizing:border-box;" minlength="8" maxlength="30" required />
                            <button type="button" onclick="togglePw('regConfirmPassword', this)" aria-label="Hiện mật khẩu"
                                    style="position:absolute;right:10px;top:50%;transform:translateY(-50%);background:none;border:0;cursor:pointer;color:#666;font-size:13px;">Hiện</button>
                        </div>
                    </div>

                    <button type="submit" class="btn-submit">Tạo tài khoản</button>
                </form>

                <div class="auth-footer">
                    Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
                </div>
            </div>
        </div>
    </div>

<script>
    function togglePw(id, btn) {
        var input = document.getElementById(id);
        var show = input.type === "password";
        input.type = show ? "text" : "password";
        btn.textContent = show ? "Ẩn" : "Hiện";
        btn.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
    }
</script>
</body>
</html>