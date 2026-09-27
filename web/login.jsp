<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head><title>Login - À la carte Restaurant</title></head>
<body>
    <h2>Đăng Nhập System</h2>
    <p style="color:red;">${error}</p>
    <p style="color:green;">${success}</p>
    <form action="login" method="post">
        Tài khoản / Email: <input type="text" name="account" required><br><br>
        Mật khẩu: <input type="password" name="password" required><br><br>
        <button type="submit">Đăng nhập</button>
    </form>
    <br>
    <a href="register">Đăng ký tài khoản Customer</a>
</body>
</html>