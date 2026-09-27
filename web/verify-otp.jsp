<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head><title>Xác nhận OTP</title></head>
<body>
    <h2>Nhập Mã OTP Xác Thực Email</h2>
    <p style="color:red;">${error}</p>
    <p>Mã OTP đã được gửi đến email: <b>${sessionScope.emailRegister}</b></p>
    <form action="verify-otp" method="post">
        Mã OTP (6 chữ số): <input type="text" name="otp" required><br><br>
        <button type="submit">Xác minh</button>
    </form>
</body>
</html>