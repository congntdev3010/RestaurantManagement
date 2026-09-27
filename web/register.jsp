<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head><title>Register - Customer</title></head>
<body>
    <h2>Đăng Ký Tài Khoản Khách Hàng</h2>
    <p style="color:red;">${error}</p>
    <form action="register" method="post">
        Họ và tên: <input type="text" name="fullName" required><br><br>
        Email (Gmail): <input type="email" name="email" required><br><br>
        Số điện thoại: <input type="text" name="phone"><br><br>
        Mật khẩu: <input type="password" name="password" required><br><br>
        <button type="submit">Đăng ký & Nhận OTP</button>
    </form>
    <br>
    <a href="login">Đã có tài khoản? Đăng nhập</a>
</body>
</html>