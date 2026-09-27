-- 1. Tạo Database
CREATE DATABASE AlacarteRestaurantDB;
GO

USE AlacarteRestaurantDB;
GO

-- 2. Bảng Vai trò (Roles)
CREATE TABLE Roles (
    role_id INT IDENTITY(1,1) PRIMARY KEY,
    role_name NVARCHAR(50) NOT NULL UNIQUE
);
GO

-- 3. Bảng Người dùng / Tài khoản (Users)
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) UNIQUE NULL,      -- Dành cho nhân viên hoặc Customer dùng tên đăng nhập
    email NVARCHAR(100) NOT NULL UNIQUE,     -- Bắt buộc, dùng để nhận OTP & Đăng nhập bằng Gmail
    password NVARCHAR(255) NOT NULL,        -- Mật khẩu (Nên băm bằng BCrypt/SHA-256 khi code)
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20) NULL,
    role_id INT NOT NULL,
    is_active BIT DEFAULT 1,                 -- 0: Chờ xác nhận OTP / Khóa, 1: Đã kích hoạt
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);
GO

-- 4. Bảng Quản lý mã OTP xác thực Email (OTP_Verifications)
CREATE TABLE OTP_Verifications (
    otp_id INT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(100) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expired_at DATETIME NOT NULL,            -- Thời gian hết hạn của mã OTP (ví dụ: +5 phút)
    is_used BIT DEFAULT 0,                   -- 0: Chưa dùng, 1: Đã xác thực thành công
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- =========================================================
-- CHÈN DỮ LIỆU MẪU ĐỂ TEST CHỨC NĂNG (DEMO)
-- =========================================================

-- Chèn danh sách 5 vai trò
INSERT INTO Roles (role_name) VALUES
('Customer'),
('Manager'),
('Server'),
('Chef'),
('Receptionist');
GO

-- Chèn tài khoản mẫu cho từng vai trò (Mật khẩu mặc định: 123456)
INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) VALUES
-- 1. Customer đã kích hoạt
('customer1', 'customer1@gmail.com', '123456', N'Nguyễn Văn Customer', '0901123456', 1, 1),

-- 2. Customer vừa đăng ký (Chờ nhập OTP -> is_active = 0)
(NULL, 'pending_user@gmail.com', '123456', N'Trần Văn Chờ Xác Nhận', '0909876543', 1, 0),

-- 3. Manager
('manager1', 'manager@restaurant.com', '123456', N'Lê Quản Lý', '0912345678', 2, 1),

-- 4. Server (Phục vụ)
('server1', 'server@restaurant.com', '123456', N'Phạm Phục Vụ', '0922345678', 3, 1),

-- 5. Chef (Bếp)
('chef1', 'chef@restaurant.com', '123456', N'Hoàng Bếp Trưởng', '0932345678', 4, 1),

-- 6. Receptionist (Lễ tân)
('receptionist1', 'receptionist@restaurant.com', '123456', N'Đỗ Lễ Tân', '0942345678', 5, 1);
GO