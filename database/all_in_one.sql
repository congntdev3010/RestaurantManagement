-- Chạy MỘT LẦN trên SQL Server bằng SSMS/Azure Data Studio (hỗ trợ GO).
-- DB AlacarteRestaurantDB phải chưa tồn tại. Không chạy lại vì script (1), (7) tạo dữ liệu/bảng không idempotent.
-- Gộp theo thứ tự 1..23; bỏ file có nhãn "chưa dùng" (16, 20).
-- 1: createdb_user(1).sql
CREATE DATABASE AlacarteRestaurantDB;
GO
USE AlacarteRestaurantDB;
GO
CREATE TABLE Roles (
    role_id INT IDENTITY(1,1) PRIMARY KEY,
    role_name NVARCHAR(50) NOT NULL UNIQUE
);
GO
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) UNIQUE NULL,
    email NVARCHAR(100) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20) NULL,
    role_id INT NOT NULL,
    is_active BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);
GO
CREATE TABLE OTP_Verifications (
    otp_id INT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(100) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expired_at DATETIME NOT NULL,
    is_used BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE()
);
GO
INSERT INTO Roles (role_name) VALUES
('Customer'), ('Manager'), ('Server'), ('Chef'), ('Receptionist');
GO
INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active) VALUES
('customer1', 'customer1@gmail.com', '123456', N'Nguyễn Văn Customer', '0901123456', 1, 1),
(NULL, 'pending_user@gmail.com', '123456', N'Trần Văn Chờ Xác Nhận', '0909876543', 1, 0),
('manager1', 'manager@restaurant.com', '123456', N'Lê Quản Lý', '0912345678', 2, 1),
('server1', 'server@restaurant.com', '123456', N'Phạm Phục Vụ', '0922345678', 3, 1),
('chef1', 'chef@restaurant.com', '123456', N'Hoàng Bếp Trưởng', '0932345678', 4, 1),
('receptionist1', 'receptionist@restaurant.com', '123456', N'Đỗ Lễ Tân', '0942345678', 5, 1);
GO

-- 2: add_admin_role(2).sql
USE AlacarteRestaurantDB;
GO
IF NOT EXISTS (SELECT 1 FROM Roles WHERE role_name = 'Admin')
BEGIN
    INSERT INTO Roles (role_name) VALUES ('Admin');
END;
GO
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'admin1@restaurant.com')
BEGIN
    INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active)
    VALUES ('admin1', 'admin1@restaurant.com', '123456', N'Admin One', NULL,
            (SELECT role_id FROM Roles WHERE role_name = 'Admin'), 1);
END;
GO
IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'admin2@restaurant.com')
BEGIN
    INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active)
    VALUES ('admin2', 'admin2@restaurant.com', '123456', N'Admin Two', NULL,
            (SELECT role_id FROM Roles WHERE role_name = 'Admin'), 1);
END;
GO

-- 3: add_user_soft_delete(3).sql
USE AlacarteRestaurantDB;
GO
IF COL_LENGTH('dbo.Users', 'is_deleted') IS NULL
BEGIN
    ALTER TABLE dbo.Users ADD is_deleted BIT NOT NULL
        CONSTRAINT DF_Users_IsDeleted DEFAULT (0) WITH VALUES;
END;
GO

-- 4: add_restaurant_tables(4).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.RestaurantTables (
        table_id INT IDENTITY(1,1) PRIMARY KEY,
        table_name NVARCHAR(50) NOT NULL UNIQUE,
        table_type NVARCHAR(50) NOT NULL,
        capacity INT NOT NULL,
        table_status VARCHAR(20) NOT NULL CONSTRAINT DF_RestaurantTables_Status DEFAULT 'AVAILABLE',
        description NVARCHAR(255) NULL,
        created_at DATETIME NOT NULL CONSTRAINT DF_RestaurantTables_CreatedAt DEFAULT GETDATE(),
        CONSTRAINT CK_RestaurantTables_Capacity CHECK (capacity BETWEEN 1 AND 30),
        CONSTRAINT CK_RestaurantTables_Status CHECK (table_status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED', 'UNAVAILABLE'))
    );
END;
GO
IF NOT EXISTS (SELECT 1 FROM dbo.RestaurantTables)
BEGIN
    INSERT INTO dbo.RestaurantTables (table_name, table_type, capacity, table_status, description) VALUES
    (N'Bàn 01', N'Cửa sổ', 2, 'AVAILABLE', N'Gần cửa sổ'),
    (N'Bàn 02', N'Cửa sổ', 4, 'AVAILABLE', N'Gần cửa sổ'),
    (N'Bàn 03', N'Trong nhà', 4, 'AVAILABLE', NULL),
    (N'Bàn 04', N'Trong nhà', 6, 'AVAILABLE', NULL),
    (N'Phòng riêng 01', N'Phòng riêng', 8, 'AVAILABLE', N'Phòng riêng, tối đa 8 khách'),
    (N'Bàn sân vườn 01', N'Ngoài trời', 4, 'AVAILABLE', N'Khu vực ngoài trời');
END;
GO

-- 5: remove_unavailable_table_status(5).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
BEGIN
    THROW 50001, 'RestaurantTables does not exist.', 1;
END;
GO
SET XACT_ABORT ON;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    UPDATE dbo.RestaurantTables SET table_status = 'AVAILABLE' WHERE table_status = 'UNAVAILABLE';
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.RestaurantTables') AND name = 'CK_RestaurantTables_Status')
        ALTER TABLE dbo.RestaurantTables DROP CONSTRAINT CK_RestaurantTables_Status;
    ALTER TABLE dbo.RestaurantTables ADD CONSTRAINT CK_RestaurantTables_Status
        CHECK (table_status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED'));
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- 6: add_reservation_order_tables(6).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50001, 'Users does not exist. Run the user schema script first.', 1;
IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
    THROW 50002, 'RestaurantTables does not exist. Run the table schema script first.', 1;
GO
IF OBJECT_ID('dbo.Reservations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reservations (
        reservation_id INT IDENTITY(1,1) NOT NULL,
        user_id INT NULL, table_id INT NULL,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        customer_phone NVARCHAR(20) NOT NULL,
        reservation_date DATE NOT NULL,
        reservation_time TIME(0) NOT NULL,
        guest_count TINYINT NOT NULL,
        reservation_status VARCHAR(20) NOT NULL CONSTRAINT DF_Reservations_Status DEFAULT 'PENDING',
        note NVARCHAR(500) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_Reservations_CreatedAt DEFAULT SYSDATETIME(),
        confirmed_by INT NULL, confirmed_at DATETIME2(0) NULL,
        CONSTRAINT PK_Reservations PRIMARY KEY (reservation_id),
        CONSTRAINT FK_Reservations_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_Reservations_Tables FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_Reservations_ConfirmedBy FOREIGN KEY (confirmed_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Reservations_GuestCount CHECK (guest_count BETWEEN 1 AND 30),
        CONSTRAINT CK_Reservations_Status CHECK (reservation_status IN ('PENDING', 'APPROVED', 'CANCELLED'))
    );
    CREATE INDEX IX_Reservations_StatusDate ON dbo.Reservations (reservation_status, reservation_date, reservation_time);
    CREATE INDEX IX_Reservations_TableDate ON dbo.Reservations (table_id, reservation_date, reservation_time);
END;
GO
IF OBJECT_ID('dbo.Orders', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Orders (
        order_id INT IDENTITY(1,1) NOT NULL,
        reservation_id INT NULL, table_id INT NULL, user_id INT NULL, created_by INT NULL,
        order_status VARCHAR(20) NOT NULL CONSTRAINT DF_Orders_Status DEFAULT 'OPEN',
        total_amount DECIMAL(12,0) NOT NULL CONSTRAINT DF_Orders_TotalAmount DEFAULT 0,
        note NVARCHAR(500) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_Orders_CreatedAt DEFAULT SYSDATETIME(),
        closed_at DATETIME2(0) NULL,
        CONSTRAINT PK_Orders PRIMARY KEY (order_id),
        CONSTRAINT FK_Orders_Reservations FOREIGN KEY (reservation_id) REFERENCES dbo.Reservations(reservation_id),
        CONSTRAINT FK_Orders_Tables FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_Orders_Users FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_Orders_CreatedBy FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Orders_Status CHECK (order_status IN ('OPEN', 'PAID', 'CANCELLED')),
        CONSTRAINT CK_Orders_TotalAmount CHECK (total_amount >= 0)
    );
    CREATE INDEX IX_Orders_StatusCreatedAt ON dbo.Orders (order_status, created_at);
    CREATE INDEX IX_Orders_TableStatus ON dbo.Orders (table_id, order_status);
END;
GO

-- 7: add_dishmenu(7).sql
CREATE TABLE Category (
    category_id INT PRIMARY KEY IDENTITY(1,1),
    category_name NVARCHAR(100) NOT NULL,
    description NVARCHAR(255),
    status BIT DEFAULT 1
);
GO
CREATE TABLE Dish (
    dish_id INT PRIMARY KEY IDENTITY(1,1),
    category_id INT NOT NULL,
    dish_name NVARCHAR(150) NOT NULL,
    description NVARCHAR(MAX),
    price DECIMAL(12, 0) NOT NULL,
    unit NVARCHAR(50) DEFAULT N'Phần',
    image NVARCHAR(255),
    is_featured BIT DEFAULT 0,
    status BIT DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Dish_Category FOREIGN KEY (category_id) REFERENCES Category(category_id)
);
GO
INSERT INTO Category (category_name, description, status) VALUES
(N'Món Khai Vị', N'Các món gỏi, chả giò nhẹ nhàng kích thích vị giác', 1),
(N'Món Chính', N'Các món ăn đậm đà bản sắc ba miền kết hợp cùng cơm, bún', 1),
(N'Lẩu & Súp', N'Nước lẩu đậm đà và các loại canh truyền thống ấm nóng', 1),
(N'Tráng Miệng', N'Chè Việt, bánh dân gian và đồ ngọt thanh mát', 1),
(N'Đồ Uống & Trà', N'Trà thảo mộc, cà phê phin và nước ép tươi nguyên chất', 1);
GO
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(1, N'Gỏi Ngó Sen Tôm Thịt', N'Ngó sen giòn sần sật kết hợp tôm sú tươi, thịt ba chỉ và nước mắm chua ngọt chuẩn vị.', 145000, N'Đĩa', N'images/dishes/goi-ngo-sen.jpg', 1, 1),
(1, N'Chả Giò Cua Bể', N'Vỏ bánh tráng giòn rụm, nhân thịt cua bể tươi ngọt hòa quyện cùng mộc nhĩ và thịt heo.', 165000, N'Phần', N'images/dishes/cha-gio-cua-be.jpg', 1, 1),
(1, N'Bánh Khọt Miền Tây', N'Bánh khọt vỏ giòn đùm nhân tôm tươi, ăn kèm rau rừng và nước mắm tỏi ớt.', 120000, N'Đĩa', N'images/dishes/banh-khot.jpg', 0, 1),
(2, N'Phở Bò Đặc Biệt', N'Bánh phở tươi, nước dùng ninh xương 24h đượm hương thảo quả kèm tái, nạm, gầu, bò viên.', 135000, N'Tô', N'images/dishes/pho-bo.jpg', 1, 1),
(2, N'Bún Chả Hà Nội Truyền Thống', N'Chả miếng và chả viên nướng than hoa thơm lừng, nước chấm đu đủ tỏi ớt ấm nóng.', 140000, N'Phần', N'images/dishes/bun-cha.jpg', 1, 1),
(2, N'Cơm Niêu Cá Kho Tộ', N'Cá kho riềng ớt đậm đà mặn ngọt, ăn kèm cơm niêu cháy giòn đượm hương.', 185000, N'Phần', N'images/dishes/ca-kho-to.jpg', 0, 1),
(2, N'Bò Lúc Lắc Sốt Tiêu Đen', N'Thịt bò Mông Cổ cắt khối xào lửa lớn cùng ớt chuông, hành tây và sốt tiêu đen đậm vị.', 240000, N'Đĩa', N'images/dishes/bo-luc-lac.jpg', 0, 1),
(3, N'Lẩu Cua Đồng Hải Sản', N'Nước lẩu riêu cua đồng ngọt thanh, gạch cua béo ngậy ăn kèm hải sản tươi sống và rau đồng quê.', 480000, N'Nồi', N'images/dishes/lau-cua-dong.jpg', 1, 1),
(3, N'Canh Chua Cá Lóc Nam Bộ', N'Cá lóc đồng tươi ngon nấu cùng dọc mùng, giá đỗ, dứa và mầm ngổ thơm lừng.', 175000, N'Tô', N'images/dishes/canh-chua-ca-loc.jpg', 0, 1),
(4, N'Chè Hạt Sen Long Nhãn', N'Hạt sen Huế ninh mềm thanh mát lồng bên trong nhãn lồng giòn rụm, nước đường thốt nốt.', 65000, N'Bát', N'images/dishes/che-hat-sen.jpg', 0, 1),
(4, N'Bánh Flan Sữa Dừa', N'Bánh flan mềm mịn thơm ngậy béo ngậy từ sữa dừa, ăn cùng cà phê phin đắng nhẹ và đá bào.', 55000, N'Phần', N'images/dishes/banh-flan.jpg', 0, 1),
(5, N'Trà Sen Tây Hồ', N'Trà mạn ướp hoa sen bách diệp Tây Hồ, hương thơm thanh khiết, vị chát dịu hậu ngọt.', 85000, N'Ấm', N'images/dishes/tra-sen.jpg', 0, 1),
(5, N'Cà Phê Trứng Hà Nội', N'Lớp kem trứng đánh bông mịn ngậy béo phủ trên nền cà phê phin đậm đà truyền thống.', 60000, N'Ly', N'images/dishes/ca-phe-trung.jpg', 1, 1);
GO
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(1, N'Nộm Bò Khô Hà Thành', N'Đu đủ bào giòn, bò khô cay nồng, rau thơm và lạc rang quyện nước gỏi chua ngọt.', 115000, N'Đĩa', N'images/dishes/nom-bo-kho.jpg', 0, 1),
(1, N'Nem Nướng Nha Trang', N'Nem nướng than thơm phức, ăn kèm bánh tráng chiên giòn, rau sống và sốt chấm tương nếp đặc chế.', 155000, N'Phần', N'images/dishes/nem-nuong.jpg', 1, 1),
(1, N'Chạo Tôm Bọc Mía', N'Tôm quết nhẵn bọc thân mía ngọt, nướng xém cạnh thơm nức mũi.', 160000, N'Đĩa', N'images/dishes/chao-tom.jpg', 0, 1),
(1, N'Phở Cuốn Hà Nội', N'Bánh phở tráng mỏng cuốn thịt bò xào tái lăn, rau thơm và nước mắm tỏi ớt.', 125000, N'Đĩa', N'images/dishes/pho-cuon.jpg', 1, 1),
(1, N'Bánh Bột Lọc Huế', N'Vỏ bánh trong suốt dai giòn, nhân tôm thịt đậm đà gói lá chuối hấp nóng.', 95000, N'Đĩa', N'images/dishes/banh-bot-loc.jpg', 0, 1);
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(2, N'Bún Bò Huế Cố Đô', N'Nước dùng đậm đà thơm mùi sả mắm ruốc, kèm tiết, chả cua, nạm bò và giò heo.', 145000, N'Tô', N'images/dishes/bun-bo-hue.jpg', 1, 1),
(2, N'Cơm Gà Hội An', N'Cơm xé xé luộc nước luộc gà vàng ươm, thịt gà ta xé phay trộn rau răm hành tây.', 150000, N'Đĩa', N'images/dishes/com-ga-hoi-an.jpg', 1, 1),
(2, N'Bún Đậu Mắm Tôm Đầy Đủ', N'Bún lá, đậu hũ chiên lướt ván, thịt chân giò, chả cốm và mắm tôm Thanh Hóa dậy mùi.', 135000, N'Mẹt', N'images/dishes/bun-dau.jpg', 1, 1),
(2, N'Vịt Quay Lạng Sơn', N'Vịt tẩm mắc mật quay da giòn bóng, thịt mềm ngọt đượm hương rừng Tây Bắc.', 290000, N'Đĩa', N'images/dishes/vit-quay.jpg', 0, 1),
(2, N'Cánh Gà Chiên Nước Mắm', N'Cánh gà ta đậm vị nước mắm Phú Quốc, áo lớp sốt tỏi ớt kẹo ngọt thơm lừng.', 165000, N'Đĩa', N'images/dishes/canh-ga-chiên-mam.jpg', 0, 1),
(2, N'Cá Lóc Nướng Trui Miền Tây', N'Cá lóc đồng nướng rơm nguyên con, cuốn bánh tráng, rau rừng và mắm nêm đậm đà.', 230000, N'Con', N'images/dishes/ca-loc-nuong.jpg', 0, 1),
(2, N'Heo Quay Bánh Hỏi', N'Thịt heo giòn rụm bì ăn kèm bánh hỏi thoa mỡ hành và nước mắm chua ngọt.', 180000, N'Phần', N'images/dishes/heo-quay-banh-hoi.jpg', 0, 1),
(2, N'Tôm Hùm Sốt Bơ Tỏi', N'Tôm hùm Nha Trang bỏ lò sốt bơ tỏi béo ngậy, ăn kèm bánh mì giòn nóng.', 650000, N'Con', N'images/dishes/tom-hum-bo-toi.jpg', 1, 1);
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(3, N'Lẩu Gà Lá É Đà Lạt', N'Nước lẩu vịt cay nhẹ vị ớt hiểm, thơm nức mùi lá é tươi và nấm ớt.', 420000, N'Nồi', N'images/dishes/lau-ga-la-e.jpg', 1, 1),
(3, N'Lẩu Mắm Miền Tây', N'Nước lẩu nấu từ mắm cá linh, cá sặc đượm vị, ăn kèm cá lóc, tôm, mực và 10 loại rau dại.', 490000, N'Nồi', N'images/dishes/lau-mam.jpg', 0, 1),
(3, N'Lẩu Vịt Om Sấu', N'Vịt béo om cùng sấu tươi Hà Nội chua thanh, khoai môn dẻo quánh.', 390000, N'Nồi', N'images/dishes/lau-vit-sau.jpg', 0, 1),
(3, N'Súp Hải Sản Tổ Yến', N'Súp sánh mịn kết hợp tôm, điệp, bào ngư và tổ yến bổ dưỡng.', 195000, N'Thố', N'images/dishes/sup-to-yen.jpg', 1, 1),
(3, N'Canh Nghêu Nấu Chua', N'Nghêu béo nấu cùng cà chua, khế chua và dứa, vị thanh mát giải nhiệt.', 120000, N'Tô', N'images/dishes/canh-ngheu.jpg', 0, 1);
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(4, N'Chè Bưởi An Giang', N'Cùi bưởi giòn sần sật không đắng, đậu xanh ninh mềm quyện cốt dừa béo ngậy.', 50000, N'Cốc', N'images/dishes/che-buoi.jpg', 0, 1),
(4, N'Kem Xôi Tràng Tiền', N'Xôi nếp dẻo thơm hương lá nếp ăn cùng kem dừa béo lạnh và dừa dừa sấy giòn.', 55000, N'Ly', N'images/dishes/kem-xoi.jpg', 1, 1),
(4, N'Bánh Da Lợn Đậu Xanh', N'Bánh dẻo dai nhiều lớp lá đắng lá nếp và đậu xanh cốt dừa truyền thống.', 45000, N'Đĩa', N'images/dishes/banh-da-lon.jpg', 0, 1),
(4, N'Sương Sa Hạt Lựu', N'Thạch sương sa thanh mát kết hợp hạt lựu củ năng giòn rụm và nước cốt dừa.', 48000, N'Cốc', N'images/dishes/suong-sa.jpg', 0, 1);
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(5, N'Nước Sấu Ngâm Đường Hà Nội', N'Sấu ngâm đường gừng thơm nức, chua ngọt hài hòa đập đá mát lạnh.', 45000, N'Ly', N'images/dishes/nuoc-sau.jpg', 0, 1),
(5, N'Sinh Tố Bơ Sáp Bến Tre', N'Bơ sáp dẻo quánh xay cùng sữa đặc nguyên chất béo ngậy.', 65000, N'Ly', N'images/dishes/sinh-to-bo.jpg', 1, 1),
(5, N'Trà Cúc Mật Ong Tây Bắc', N'Trà hoa cúc nguyên bông pha cùng mật ong rừng ngọt dịu thanh nhiệt.', 70000, N'Ấm', N'images/dishes/tra-cuc.jpg', 0, 1);
GO

-- 8: add_order_items_and_workflow(8).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Users', 'U') IS NULL THROW 50001, 'Users does not exist.', 1;
IF OBJECT_ID('dbo.Dish', 'U') IS NULL THROW 50002, 'Dish does not exist. Run the dish schema script first.', 1;
IF OBJECT_ID('dbo.Orders', 'U') IS NULL THROW 50003, 'Orders does not exist. Run the reservation/order schema script first.', 1;
GO
IF COL_LENGTH('dbo.Orders', 'customer_name') IS NULL ALTER TABLE dbo.Orders ADD customer_name NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders', 'customer_phone') IS NULL ALTER TABLE dbo.Orders ADD customer_phone NVARCHAR(20) NULL;
IF COL_LENGTH('dbo.Orders', 'customer_email') IS NULL ALTER TABLE dbo.Orders ADD customer_email NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders', 'approved_by') IS NULL ALTER TABLE dbo.Orders ADD approved_by INT NULL;
IF COL_LENGTH('dbo.Orders', 'approved_at') IS NULL ALTER TABLE dbo.Orders ADD approved_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.Orders', 'cancelled_by') IS NULL ALTER TABLE dbo.Orders ADD cancelled_by INT NULL;
IF COL_LENGTH('dbo.Orders', 'cancelled_at') IS NULL ALTER TABLE dbo.Orders ADD cancelled_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.Orders', 'cancellation_reason') IS NULL ALTER TABLE dbo.Orders ADD cancellation_reason NVARCHAR(500) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_ApprovedBy')
    ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_ApprovedBy FOREIGN KEY (approved_by) REFERENCES dbo.Users(user_id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_CancelledBy')
    ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_CancelledBy FOREIGN KEY (cancelled_by) REFERENCES dbo.Users(user_id);
GO
IF OBJECT_ID('dbo.OrderItems', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderItems (
        order_item_id INT IDENTITY(1,1) NOT NULL,
        order_id INT NOT NULL, dish_id INT NULL,
        dish_name NVARCHAR(150) NOT NULL, unit NVARCHAR(50) NOT NULL,
        unit_price DECIMAL(12,0) NOT NULL, quantity INT NOT NULL, line_total DECIMAL(14,0) NOT NULL,
        CONSTRAINT PK_OrderItems PRIMARY KEY (order_item_id),
        CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(order_id),
        CONSTRAINT FK_OrderItems_Dish FOREIGN KEY (dish_id) REFERENCES dbo.Dish(dish_id),
        CONSTRAINT CK_OrderItems_UnitPrice CHECK (unit_price >= 0),
        CONSTRAINT CK_OrderItems_Quantity CHECK (quantity > 0),
        CONSTRAINT CK_OrderItems_LineTotal CHECK (line_total >= 0)
    );
    CREATE INDEX IX_OrderItems_OrderId ON dbo.OrderItems(order_id);
END;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.Orders') AND name = 'CK_Orders_Status')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_Status;
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_Status
    CHECK (order_status IN ('PENDING', 'APPROVED', 'OPEN', 'PAID', 'CANCELLED'));
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'IX_Orders_UserStatusCreatedAt')
    CREATE INDEX IX_Orders_UserStatusCreatedAt ON dbo.Orders(user_id, order_status, created_at DESC);
GO

-- 9: add_qr_order_approval(9).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL THROW 50001, 'RestaurantTables does not exist.', 1;
IF OBJECT_ID('dbo.Orders', 'U') IS NULL THROW 50002, 'Orders does not exist.', 1;
IF OBJECT_ID('dbo.OrderItems', 'U') IS NULL THROW 50003, 'OrderItems does not exist.', 1;
GO
IF COL_LENGTH('dbo.RestaurantTables', 'qr_token') IS NULL ALTER TABLE dbo.RestaurantTables ADD qr_token VARCHAR(36) NULL;
GO
UPDATE dbo.RestaurantTables SET qr_token = LOWER(CONVERT(VARCHAR(36), NEWID())) WHERE qr_token IS NULL;
GO
ALTER TABLE dbo.RestaurantTables ALTER COLUMN qr_token VARCHAR(36) NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_RestaurantTables_QrToken')
    ALTER TABLE dbo.RestaurantTables ADD CONSTRAINT DF_RestaurantTables_QrToken DEFAULT (LOWER(CONVERT(VARCHAR(36), NEWID()))) FOR qr_token;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.RestaurantTables') AND name = 'UX_RestaurantTables_QrToken')
    CREATE UNIQUE INDEX UX_RestaurantTables_QrToken ON dbo.RestaurantTables(qr_token);
GO
IF COL_LENGTH('dbo.OrderItems', 'item_status') IS NULL
    ALTER TABLE dbo.OrderItems ADD item_status VARCHAR(20) NOT NULL CONSTRAINT DF_OrderItems_ItemStatus DEFAULT 'PENDING';
IF COL_LENGTH('dbo.OrderItems', 'item_note') IS NULL ALTER TABLE dbo.OrderItems ADD item_note NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.OrderItems', 'requested_quantity') IS NULL ALTER TABLE dbo.OrderItems ADD requested_quantity INT NULL;
GO
UPDATE dbo.OrderItems SET requested_quantity = quantity WHERE requested_quantity IS NULL;
UPDATE oi SET item_status = CASE o.order_status
    WHEN 'PENDING' THEN 'PENDING' WHEN 'CANCELLED' THEN 'REJECTED'
    WHEN 'PAID' THEN 'SERVED' ELSE 'COOKING' END
FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.order_id = oi.order_id
WHERE oi.item_status = 'PENDING' AND o.order_status <> 'PENDING';
GO
ALTER TABLE dbo.OrderItems ALTER COLUMN requested_quantity INT NOT NULL;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_OrderItems_ItemStatus')
    ALTER TABLE dbo.OrderItems DROP CONSTRAINT CK_OrderItems_ItemStatus;
ALTER TABLE dbo.OrderItems WITH CHECK ADD CONSTRAINT CK_OrderItems_ItemStatus
    CHECK (item_status IN ('PENDING', 'COOKING', 'SERVED', 'REJECTED'));
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.OrderItems') AND name = 'IX_OrderItems_Status')
    CREATE INDEX IX_OrderItems_Status ON dbo.OrderItems(item_status, order_id);
GO
UPDATE dbo.Orders SET order_status = 'OPEN' WHERE order_status = 'APPROVED';
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.Orders') AND name = 'CK_Orders_Status')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_Status;
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_Status
    CHECK (order_status IN ('PENDING', 'OPEN', 'PAID', 'CANCELLED'));
GO

-- 10: add_news(10).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Users', 'U') IS NULL THROW 50001, 'Users does not exist.', 1;
GO
IF OBJECT_ID('dbo.News', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.News (
        news_id INT IDENTITY(1,1) NOT NULL,
        title NVARCHAR(200) NOT NULL, summary NVARCHAR(500) NOT NULL,
        content NVARCHAR(MAX) NOT NULL, image_url NVARCHAR(1000) NULL,
        news_status VARCHAR(10) NOT NULL CONSTRAINT DF_News_Status DEFAULT 'DRAFT',
        created_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_News_CreatedAt DEFAULT SYSDATETIME(),
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_News_UpdatedAt DEFAULT SYSDATETIME(),
        published_at DATETIME2(0) NULL,
        CONSTRAINT PK_News PRIMARY KEY (news_id),
        CONSTRAINT FK_News_CreatedBy FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_News_Status CHECK (news_status IN ('DRAFT', 'PUBLISHED')),
        CONSTRAINT CK_News_PublishedAt CHECK (
            (news_status = 'DRAFT' AND published_at IS NULL)
            OR (news_status = 'PUBLISHED' AND published_at IS NOT NULL))
    );
    CREATE INDEX IX_News_StatusPublishedAt ON dbo.News (news_status, published_at DESC, news_id DESC);
    CREATE INDEX IX_News_Title ON dbo.News (title);
END;
GO

-- 11: add_reservation_booking_flow(11).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Reservations', 'U') IS NULL THROW 50001, 'Reservations does not exist.', 1;
GO
SET QUOTED_IDENTIFIER ON;
GO
IF OBJECT_ID('dbo.MealShifts', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.MealShifts (
        shift_code VARCHAR(10) NOT NULL, shift_name NVARCHAR(50) NOT NULL,
        start_time TIME(0) NOT NULL, end_time TIME(0) NOT NULL,
        CONSTRAINT PK_MealShifts PRIMARY KEY (shift_code),
        CONSTRAINT CK_MealShifts_Time CHECK (start_time < end_time)
    );
    INSERT INTO dbo.MealShifts (shift_code, shift_name, start_time, end_time) VALUES
    ('LUNCH', N'Ca trưa', '10:00', '14:30'),
    ('DINNER', N'Ca tối', '17:00', '22:30');
END;
GO
ALTER TABLE dbo.Reservations ALTER COLUMN customer_email NVARCHAR(100) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'table_type_requested') IS NULL ALTER TABLE dbo.Reservations ADD table_type_requested NVARCHAR(50) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'meal_shift') IS NULL ALTER TABLE dbo.Reservations ADD meal_shift VARCHAR(10) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'staff_note') IS NULL ALTER TABLE dbo.Reservations ADD staff_note NVARCHAR(500) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'cancel_reason') IS NULL ALTER TABLE dbo.Reservations ADD cancel_reason NVARCHAR(255) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'contacted_at') IS NULL ALTER TABLE dbo.Reservations ADD contacted_at DATETIME2(0) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'updated_at') IS NULL ALTER TABLE dbo.Reservations ADD updated_at DATETIME2(0) NULL;
GO
UPDATE dbo.Reservations SET meal_shift = CASE WHEN reservation_time < '15:00' THEN 'LUNCH' ELSE 'DINNER' END
WHERE meal_shift IS NULL AND reservation_status = 'APPROVED';
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Reservations_MealShifts')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT FK_Reservations_MealShifts FOREIGN KEY (meal_shift) REFERENCES dbo.MealShifts(shift_code);
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Reservations_Status')
    ALTER TABLE dbo.Reservations DROP CONSTRAINT CK_Reservations_Status;
GO
ALTER TABLE dbo.Reservations ADD CONSTRAINT CK_Reservations_Status CHECK (
    reservation_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'));
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Reservations_ApprovedHasTable')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT CK_Reservations_ApprovedHasTable CHECK (
        reservation_status NOT IN ('APPROVED', 'COMPLETED') OR (table_id IS NOT NULL AND meal_shift IS NOT NULL));
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Reservations_TableDateShift')
    CREATE UNIQUE INDEX UX_Reservations_TableDateShift ON dbo.Reservations (table_id, reservation_date, meal_shift)
        WHERE reservation_status IN ('APPROVED', 'COMPLETED');
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Reservations_DateShiftStatus')
    CREATE INDEX IX_Reservations_DateShiftStatus ON dbo.Reservations (reservation_date, meal_shift, reservation_status)
        INCLUDE (table_id, table_type_requested, guest_count);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Reservations_User')
    CREATE INDEX IX_Reservations_User ON dbo.Reservations (user_id, created_at DESC) WHERE user_id IS NOT NULL;
GO
-- Batch kết thúc phần truy vấn mẫu (chỉ là chú thích) trong file gốc.
GO

-- 12: seed_restaurant_tables(12).sql
USE AlacarteRestaurantDB;
GO
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO
DECLARE @MergeIndoor BIT = 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF @MergeIndoor = 1
    BEGIN
        UPDATE dbo.RestaurantTables SET table_type = N'Trung tâm' WHERE table_type = N'Trong nhà';
        UPDATE dbo.Reservations SET table_type_requested = N'Trung tâm' WHERE table_type_requested = N'Trong nhà';
    END;
    DECLARE @Plan TABLE (table_type NVARCHAR(50) PRIMARY KEY, target INT NOT NULL, descr NVARCHAR(255) NOT NULL);
    INSERT INTO @Plan VALUES
    (N'Trung tâm', 20, N'Khu vực trung tâm nhà hàng'),
    (N'Cửa sổ', 10, N'Gần cửa sổ, nhìn ra phố'),
    (N'Phòng riêng', 10, N'Phòng riêng, không gian kín'),
    (N'Ngoài trời', 10, N'Khu vực ngoài trời, sân vườn');
    ;WITH n AS (
        SELECT TOP (20) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS i FROM sys.all_objects
    ), cand AS (
        SELECT p.table_type, p.descr, p.target,
               table_name = p.table_type + N' ' + RIGHT('0' + CAST(n.i AS VARCHAR(2)), 2),
               capacity = CASE p.table_type
                   WHEN N'Trung tâm' THEN CASE WHEN n.i <= 6 THEN 2 WHEN n.i <= 14 THEN 4 WHEN n.i <= 18 THEN 6 ELSE 8 END
                   WHEN N'Cửa sổ' THEN CASE WHEN n.i <= 4 THEN 2 ELSE 4 END
                   WHEN N'Phòng riêng' THEN CASE WHEN n.i <= 3 THEN 6 WHEN n.i <= 7 THEN 8 WHEN n.i <= 9 THEN 10 ELSE 12 END
                   ELSE CASE WHEN n.i <= 3 THEN 2 WHEN n.i <= 8 THEN 4 ELSE 6 END
               END,
               rn = ROW_NUMBER() OVER (PARTITION BY p.table_type ORDER BY n.i)
        FROM @Plan p JOIN n ON n.i <= 20
        WHERE NOT EXISTS (SELECT 1 FROM dbo.RestaurantTables t
            WHERE t.table_name = p.table_type + N' ' + RIGHT('0' + CAST(n.i AS VARCHAR(2)), 2))
    )
    INSERT INTO dbo.RestaurantTables (table_name, table_type, capacity, table_status, description)
    SELECT c.table_name, c.table_type, c.capacity, 'AVAILABLE', c.descr FROM cand c
    WHERE c.rn <= c.target - (SELECT COUNT(*) FROM dbo.RestaurantTables e WHERE e.table_type = c.table_type);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
SELECT table_type, COUNT(*) AS so_ban, MIN(capacity) AS min_cho, MAX(capacity) AS max_cho
FROM dbo.RestaurantTables GROUP BY table_type ORDER BY table_type;
GO

-- 13: add_reservation_checkin(13).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Reservations', 'U') IS NULL THROW 50001, 'Reservations does not exist.', 1;
GO
IF COL_LENGTH('dbo.Reservations', 'checked_in_at') IS NULL ALTER TABLE dbo.Reservations ADD checked_in_at DATETIME2(0) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'checked_in_by') IS NULL ALTER TABLE dbo.Reservations ADD checked_in_by INT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Reservations_CheckedInBy')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT FK_Reservations_CheckedInBy FOREIGN KEY (checked_in_by) REFERENCES dbo.Users(user_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Reservations_Phone')
    CREATE INDEX IX_Reservations_Phone ON dbo.Reservations (customer_phone);
GO
UPDATE dbo.Reservations SET checked_in_at = COALESCE(updated_at, created_at)
WHERE reservation_status = 'COMPLETED' AND checked_in_at IS NULL;
GO

-- 14a: add_payment(14a).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Orders', 'U') IS NULL THROW 50001, 'Orders does not exist.', 1;
GO
IF COL_LENGTH('dbo.Orders', 'payment_method') IS NULL ALTER TABLE dbo.Orders ADD payment_method VARCHAR(10) NULL;
IF COL_LENGTH('dbo.Orders', 'paid_by') IS NULL ALTER TABLE dbo.Orders ADD paid_by INT NULL;
IF COL_LENGTH('dbo.Orders', 'paid_at') IS NULL ALTER TABLE dbo.Orders ADD paid_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.Orders', 'payment_reference') IS NULL ALTER TABLE dbo.Orders ADD payment_reference NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders', 'cash_received') IS NULL ALTER TABLE dbo.Orders ADD cash_received DECIMAL(14,0) NULL;
IF COL_LENGTH('dbo.Orders', 'cash_change') IS NULL ALTER TABLE dbo.Orders ADD cash_change DECIMAL(14,0) NULL;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.Orders') AND name = 'CK_Orders_PaymentMethod')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_PaymentMethod;
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_PaymentMethod CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'QR'));
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_PaidBy')
    ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_PaidBy FOREIGN KEY (paid_by) REFERENCES dbo.Users(user_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'IX_Orders_Payment')
    CREATE INDEX IX_Orders_Payment ON dbo.Orders(order_status, payment_method, paid_at);
GO
IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ServiceHistory (
        history_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        table_id INT NOT NULL, customer_name NVARCHAR(100) NULL, customer_phone NVARCHAR(20) NULL,
        reservation_time DATETIME2(0) NULL, checked_in_at DATETIME2(0) NULL,
        paid_at DATETIME2(0) NOT NULL, payment_method VARCHAR(10) NOT NULL,
        total_amount DECIMAL(14,0) NOT NULL, cash_received DECIMAL(14,0) NULL,
        cash_change DECIMAL(14,0) NULL, paid_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_ServiceHistory_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ServiceHistory_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_ServiceHistory_PaidBy FOREIGN KEY (paid_by) REFERENCES dbo.Users(user_id)
    );
    CREATE INDEX IX_ServiceHistory_PaidAt ON dbo.ServiceHistory(paid_at DESC);
END;
GO
IF OBJECT_ID('dbo.ServiceHistoryItems', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ServiceHistoryItems (
        history_item_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        history_id INT NOT NULL, dish_name NVARCHAR(150) NOT NULL,
        unit NVARCHAR(50) NOT NULL, unit_price DECIMAL(12,0) NOT NULL,
        quantity INT NOT NULL, line_total DECIMAL(14,0) NOT NULL,
        CONSTRAINT FK_ServiceHistoryItems_History FOREIGN KEY (history_id) REFERENCES dbo.ServiceHistory(history_id)
    );
END;
GO

-- 14b: add_merged_tables(14b).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Reservations', 'U') IS NULL THROW 50001, 'Reservations does not exist.', 1;
GO
IF OBJECT_ID('dbo.ReservationTables', 'U') IS NULL
    CREATE TABLE dbo.ReservationTables (
        reservation_id INT NOT NULL, table_id INT NOT NULL,
        CONSTRAINT PK_ReservationTables PRIMARY KEY (reservation_id, table_id),
        CONSTRAINT FK_ReservationTables_Reservation FOREIGN KEY (reservation_id) REFERENCES dbo.Reservations(reservation_id),
        CONSTRAINT FK_ReservationTables_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id)
    );
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ReservationTables_Table')
    CREATE INDEX IX_ReservationTables_Table ON dbo.ReservationTables (table_id);
GO
UPDATE dbo.Reservations SET reservation_status = 'COMPLETED', updated_at = SYSDATETIME()
WHERE reservation_status = 'APPROVED' AND checked_in_at IS NOT NULL;
GO

-- 15: add_loyalty_points(15).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Users', 'U') IS NULL THROW 50001, 'Users does not exist.', 1;
IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL THROW 50001, 'ServiceHistory does not exist.', 1;
GO
IF COL_LENGTH('dbo.Users', 'loyalty_points') IS NULL
    ALTER TABLE dbo.Users ADD loyalty_points INT NOT NULL CONSTRAINT DF_Users_LoyaltyPoints DEFAULT 0 WITH VALUES;
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.Users') AND name = 'CK_Users_LoyaltyPoints')
    ALTER TABLE dbo.Users ADD CONSTRAINT CK_Users_LoyaltyPoints CHECK (loyalty_points >= 0);
GO
IF OBJECT_ID('dbo.LoyaltySettings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoyaltySettings (
        settings_id INT NOT NULL CONSTRAINT PK_LoyaltySettings PRIMARY KEY CONSTRAINT CK_LoyaltySettings_Single CHECK (settings_id = 1),
        earn_spend_amount DECIMAL(14,0) NOT NULL CONSTRAINT DF_LoyaltySettings_Spend DEFAULT 1000,
        earn_points INT NOT NULL CONSTRAINT DF_LoyaltySettings_Points DEFAULT 1,
        redeem_value_per_point DECIMAL(14,0) NOT NULL CONSTRAINT DF_LoyaltySettings_Redeem DEFAULT 1000,
        updated_by INT NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_LoyaltySettings_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_LoyaltySettings_Positive CHECK (earn_spend_amount > 0 AND earn_points > 0 AND redeem_value_per_point > 0),
        CONSTRAINT FK_LoyaltySettings_UpdatedBy FOREIGN KEY (updated_by) REFERENCES dbo.Users(user_id)
    );
END;
GO
IF NOT EXISTS (SELECT 1 FROM dbo.LoyaltySettings WHERE settings_id = 1)
    INSERT INTO dbo.LoyaltySettings (settings_id, earn_spend_amount, earn_points, redeem_value_per_point) VALUES (1, 1000, 1, 1000);
GO
IF OBJECT_ID('dbo.TableBillLoyalty', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.TableBillLoyalty (
        table_id INT NOT NULL CONSTRAINT PK_TableBillLoyalty PRIMARY KEY,
        customer_id INT NOT NULL,
        points_to_use INT NOT NULL CONSTRAINT DF_TableBillLoyalty_Use DEFAULT 0,
        set_by INT NOT NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_TableBillLoyalty_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_TableBillLoyalty_Use CHECK (points_to_use >= 0),
        CONSTRAINT FK_TableBillLoyalty_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_TableBillLoyalty_Customer FOREIGN KEY (customer_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_TableBillLoyalty_SetBy FOREIGN KEY (set_by) REFERENCES dbo.Users(user_id)
    );
END;
GO
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_customer_id') IS NULL ALTER TABLE dbo.ServiceHistory ADD loyalty_customer_id INT NULL;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_points_used') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_points_used INT NOT NULL CONSTRAINT DF_ServiceHistory_PointsUsed DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_discount') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_discount DECIMAL(14,0) NOT NULL CONSTRAINT DF_ServiceHistory_LoyaltyDiscount DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_points_earned') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD loyalty_points_earned INT NOT NULL CONSTRAINT DF_ServiceHistory_PointsEarned DEFAULT 0 WITH VALUES;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ServiceHistory_LoyaltyCustomer')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT FK_ServiceHistory_LoyaltyCustomer FOREIGN KEY (loyalty_customer_id) REFERENCES dbo.Users(user_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.ServiceHistory') AND name = 'CK_ServiceHistory_Loyalty')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT CK_ServiceHistory_Loyalty CHECK (
        loyalty_points_used >= 0 AND loyalty_discount >= 0 AND loyalty_points_earned >= 0
        AND loyalty_discount <= total_amount
        AND (loyalty_customer_id IS NOT NULL OR (loyalty_points_used = 0 AND loyalty_discount = 0 AND loyalty_points_earned = 0)));
GO
IF OBJECT_ID('dbo.LoyaltyTransactions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoyaltyTransactions (
        txn_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoyaltyTransactions PRIMARY KEY,
        user_id INT NOT NULL, history_id INT NULL, txn_type VARCHAR(10) NOT NULL,
        points INT NOT NULL, balance_after INT NOT NULL,
        bill_amount DECIMAL(14,0) NULL, money_value DECIMAL(14,0) NULL,
        earn_spend_amount DECIMAL(14,0) NULL, earn_points INT NULL,
        redeem_value_per_point DECIMAL(14,0) NULL, note NVARCHAR(255) NULL,
        created_by INT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_LoyaltyTransactions_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_LoyaltyTransactions_Type CHECK (txn_type IN ('EARN', 'REDEEM', 'ADJUST')),
        CONSTRAINT CK_LoyaltyTransactions_Sign CHECK (
            (txn_type = 'EARN' AND points > 0) OR (txn_type = 'REDEEM' AND points < 0) OR txn_type = 'ADJUST'),
        CONSTRAINT CK_LoyaltyTransactions_Balance CHECK (balance_after >= 0),
        CONSTRAINT FK_LoyaltyTransactions_User FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_LoyaltyTransactions_History FOREIGN KEY (history_id) REFERENCES dbo.ServiceHistory(history_id),
        CONSTRAINT FK_LoyaltyTransactions_CreatedBy FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id)
    );
    CREATE INDEX IX_LoyaltyTransactions_User ON dbo.LoyaltyTransactions(user_id, created_at DESC);
    CREATE UNIQUE INDEX UX_LoyaltyTransactions_History_Type ON dbo.LoyaltyTransactions(history_id, txn_type)
        WHERE history_id IS NOT NULL AND txn_type IN ('EARN', 'REDEEM');
END;
GO

-- 17: release_checked_in_tables(17).sql
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Reservations_TableDateShift')
    DROP INDEX UX_Reservations_TableDateShift ON dbo.Reservations;
GO
CREATE UNIQUE INDEX UX_Reservations_TableDateShift ON dbo.Reservations (table_id, reservation_date, meal_shift)
    WHERE reservation_status = 'APPROVED';
GO

-- 18: add_history_customer_email(18).sql
IF COL_LENGTH('dbo.ServiceHistory', 'customer_email') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD customer_email NVARCHAR(100) NULL;
GO

-- 19: add_order_settled_at(19).sql
IF COL_LENGTH('dbo.Orders', 'settled_at') IS NULL ALTER TABLE dbo.Orders ADD settled_at DATETIME2(0) NULL;
GO
UPDATE o SET o.settled_at = (
    SELECT MIN(sh.paid_at) FROM dbo.ServiceHistory sh
    WHERE sh.table_id = o.table_id AND o.created_at <= sh.paid_at
      AND sh.paid_at >= COALESCE(o.cancelled_at, o.closed_at))
FROM dbo.Orders o
WHERE o.order_status = 'CANCELLED' AND o.settled_at IS NULL
  AND EXISTS (SELECT 1 FROM dbo.ServiceHistory sh WHERE sh.table_id = o.table_id
      AND o.created_at <= sh.paid_at AND sh.paid_at >= COALESCE(o.cancelled_at, o.closed_at));
GO

-- 21: add_voucher_system(21).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.VoucherCampaigns', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.VoucherCampaigns (
        campaign_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_VoucherCampaigns PRIMARY KEY,
        discount_percent DECIMAL(5,2) NOT NULL,
        required_total_spent DECIMAL(14,0) NOT NULL,
        created_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_VoucherCampaigns_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT CK_VoucherCampaigns_Percent CHECK (discount_percent > 0 AND discount_percent <= 100),
        CONSTRAINT CK_VoucherCampaigns_RequiredSpent CHECK (required_total_spent >= 0),
        CONSTRAINT FK_VoucherCampaigns_CreatedBy FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id)
    );
END;
GO
IF OBJECT_ID('dbo.Vouchers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Vouchers (
        voucher_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Vouchers PRIMARY KEY,
        campaign_id INT NOT NULL, code CHAR(8) NOT NULL,
        customer_id INT NOT NULL, sent_to_email NVARCHAR(100) NOT NULL,
        discount_percent DECIMAL(5,2) NOT NULL,
        required_total_spent DECIMAL(14,0) NOT NULL, customer_total_spent DECIMAL(14,0) NOT NULL,
        status VARCHAR(10) NOT NULL CONSTRAINT DF_Vouchers_Status DEFAULT 'ACTIVE',
        sent_at DATETIME2(0) NOT NULL CONSTRAINT DF_Vouchers_SentAt DEFAULT SYSDATETIME(),
        used_at DATETIME2(0) NULL,
        CONSTRAINT UX_Vouchers_Code UNIQUE (code),
        CONSTRAINT CK_Vouchers_Code CHECK (code COLLATE Latin1_General_BIN2 NOT LIKE '%[^A-Z0-9]%'),
        CONSTRAINT CK_Vouchers_Email CHECK (sent_to_email LIKE '%_@_%._%'),
        CONSTRAINT CK_Vouchers_Percent CHECK (discount_percent > 0 AND discount_percent <= 100),
        CONSTRAINT CK_Vouchers_Eligibility CHECK (required_total_spent >= 0 AND customer_total_spent >= required_total_spent),
        CONSTRAINT CK_Vouchers_Status CHECK (status IN ('ACTIVE', 'USED')),
        CONSTRAINT CK_Vouchers_UsedAt CHECK (
            (status = 'USED' AND used_at IS NOT NULL) OR (status = 'ACTIVE' AND used_at IS NULL)),
        CONSTRAINT FK_Vouchers_Campaign FOREIGN KEY (campaign_id) REFERENCES dbo.VoucherCampaigns(campaign_id),
        CONSTRAINT FK_Vouchers_Customer FOREIGN KEY (customer_id) REFERENCES dbo.Users(user_id)
    );
END;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Vouchers') AND name = 'IX_Vouchers_Customer')
    CREATE INDEX IX_Vouchers_Customer ON dbo.Vouchers(customer_id, status);
GO
IF OBJECT_ID('dbo.TableBillVoucher', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.TableBillVoucher (
        table_id INT NOT NULL CONSTRAINT PK_TableBillVoucher PRIMARY KEY,
        voucher_id INT NOT NULL, set_by INT NOT NULL,
        updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_TableBillVoucher_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT UX_TableBillVoucher_Voucher UNIQUE (voucher_id),
        CONSTRAINT FK_TableBillVoucher_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_TableBillVoucher_Voucher FOREIGN KEY (voucher_id) REFERENCES dbo.Vouchers(voucher_id),
        CONSTRAINT FK_TableBillVoucher_SetBy FOREIGN KEY (set_by) REFERENCES dbo.Users(user_id)
    );
END;
GO
IF COL_LENGTH('dbo.ServiceHistory', 'voucher_id') IS NULL ALTER TABLE dbo.ServiceHistory ADD voucher_id INT NULL;
IF COL_LENGTH('dbo.ServiceHistory', 'voucher_discount_percent') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD voucher_discount_percent DECIMAL(5,2) NOT NULL CONSTRAINT DF_ServiceHistory_VoucherPercent DEFAULT 0 WITH VALUES;
IF COL_LENGTH('dbo.ServiceHistory', 'voucher_discount') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD voucher_discount DECIMAL(14,0) NOT NULL CONSTRAINT DF_ServiceHistory_VoucherDiscount DEFAULT 0 WITH VALUES;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_ServiceHistory_Voucher')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT FK_ServiceHistory_Voucher FOREIGN KEY (voucher_id) REFERENCES dbo.Vouchers(voucher_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.ServiceHistory') AND name = 'UX_ServiceHistory_Voucher')
    CREATE UNIQUE INDEX UX_ServiceHistory_Voucher ON dbo.ServiceHistory(voucher_id) WHERE voucher_id IS NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.ServiceHistory') AND name = 'CK_ServiceHistory_Voucher')
    ALTER TABLE dbo.ServiceHistory ADD CONSTRAINT CK_ServiceHistory_Voucher CHECK (
        voucher_discount_percent BETWEEN 0 AND 100 AND voucher_discount >= 0
        AND (voucher_id IS NOT NULL OR (voucher_discount = 0 AND voucher_discount_percent = 0))
        AND voucher_discount = ROUND(total_amount * voucher_discount_percent / 100, 0)
        AND loyalty_discount + voucher_discount <= total_amount);
GO

-- 22: update_voucher_eligibility_gte(22).sql
USE AlacarteRestaurantDB;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.Vouchers') AND name = 'CK_Vouchers_Eligibility')
    ALTER TABLE dbo.Vouchers DROP CONSTRAINT CK_Vouchers_Eligibility;
GO
ALTER TABLE dbo.Vouchers WITH CHECK ADD CONSTRAINT CK_Vouchers_Eligibility CHECK (
    required_total_spent >= 0 AND customer_total_spent >= required_total_spent);
GO

-- 23: add_notifications(23).sql
USE AlacarteRestaurantDB;
GO
IF OBJECT_ID('dbo.Notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications (
        notification_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notifications PRIMARY KEY,
        user_id INT NOT NULL, title NVARCHAR(150) NOT NULL, body NVARCHAR(500) NULL,
        is_read BIT NOT NULL CONSTRAINT DF_Notifications_IsRead DEFAULT 0,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT SYSDATETIME(),
        read_at DATETIME2(0) NULL,
        CONSTRAINT FK_Notifications_User FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Notifications_ReadAt CHECK (
            (is_read = 1 AND read_at IS NOT NULL) OR (is_read = 0 AND read_at IS NULL))
    );
END;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Notifications') AND name = 'IX_Notifications_User')
    CREATE INDEX IX_Notifications_User ON dbo.Notifications(user_id, is_read, created_at DESC);
GO
