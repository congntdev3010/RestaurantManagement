-- 1. BẢNG DANH MỤC MÓN ĂN (Category)
CREATE TABLE Category (
    category_id INT PRIMARY KEY IDENTITY(1,1),
    category_name NVARCHAR(100) NOT NULL,
    description NVARCHAR(255),
    status BIT DEFAULT 1
);
GO

-- 2. BẢNG MÓN ĂN (Dish)
CREATE TABLE Dish (
    dish_id INT PRIMARY KEY IDENTITY(1,1),
    category_id INT NOT NULL,
    dish_name NVARCHAR(150) NOT NULL,
    description NVARCHAR(MAX),
    price DECIMAL(12, 0) NOT NULL,           -- Đơn vị VNĐ
    unit NVARCHAR(50) DEFAULT N'Phần',        -- Phần, Đĩa, Tô, Nồi...
    image NVARCHAR(255),                     -- Lưu đường dẫn: 'images/dishes/ten-mon.jpg'
    is_featured BIT DEFAULT 0,               -- 1: Món Bán Chạy / Nổi Bật
    status BIT DEFAULT 1,                    -- 1: Hiển thị, 0: Ẩn món
    created_at DATETIME DEFAULT GETDATE(),

    CONSTRAINT FK_Dish_Category FOREIGN KEY (category_id) 
        REFERENCES Category(category_id)
);
GO
-- 1. CHÈN DỮ LIỆU DANH MỤC (Category)
INSERT INTO Category (category_name, description, status) VALUES
(N'Món Khai Vị', N'Các món gỏi, chả giò nhẹ nhàng kích thích vị giác', 1),
(N'Món Chính', N'Các món ăn đậm đà bản sắc ba miền kết hợp cùng cơm, bún', 1),
(N'Lẩu & Súp', N'Nước lẩu đậm đà và các loại canh truyền thống ấm nóng', 1),
(N'Tráng Miệng', N'Chè Việt, bánh dân gian và đồ ngọt thanh mát', 1),
(N'Đồ Uống & Trà', N'Trà thảo mộc, cà phê phin và nước ép tươi nguyên chất', 1);
GO

-- 2. CHÈN DỮ LIỆU MÓN ĂN (Dish)
-- Cập nhật category_id tương ứng: 1-Khai Vị, 2-Món Chính, 3-Lẩu&Súp, 4-Tráng Miệng, 5-Đồ Uống
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
-- Món Khai Vị (Category 1)
(1, N'Gỏi Ngó Sen Tôm Thịt', N'Ngó sen giòn sần sật kết hợp tôm sú tươi, thịt ba chỉ và nước mắm chua ngọt chuẩn vị.', 145000, N'Đĩa', N'images/dishes/goi-ngo-sen.jpg', 1, 1),
(1, N'Chả Giò Cua Bể', N'Vỏ bánh tráng giòn rụm, nhân thịt cua bể tươi ngọt hòa quyện cùng mộc nhĩ và thịt heo.', 165000, N'Phần', N'images/dishes/cha-gio-cua-be.jpg', 1, 1),
(1, N'Bánh Khọt Miền Tây', N'Bánh khọt vỏ giòn đùm nhân tôm tươi, ăn kèm rau rừng và nước mắm tỏi ớt.', 120000, N'Đĩa', N'images/dishes/banh-khot.jpg', 0, 1),

-- Món Chính (Category 2)
(2, N'Phở Bò Đặc Biệt', N'Bánh phở tươi, nước dùng ninh xương 24h đượm hương thảo quả kèm tái, nạm, gầu, bò viên.', 135000, N'Tô', N'images/dishes/pho-bo.jpg', 1, 1),
(2, N'Bún Chả Hà Nội Truyền Thống', N'Chả miếng và chả viên nướng than hoa thơm lừng, nước chấm đu đủ tỏi ớt ấm nóng.', 140000, N'Phần', N'images/dishes/bun-cha.jpg', 1, 1),
(2, N'Cơm Niêu Cá Kho Tộ', N'Cá kho riềng ớt đậm đà mặn ngọt, ăn kèm cơm niêu cháy giòn đượm hương.', 185000, N'Phần', N'images/dishes/ca-kho-to.jpg', 0, 1),
(2, N'Bò Lúc Lắc Sốt Tiêu Đen', N'Thịt bò Mông Cổ cắt khối xào lửa lớn cùng ớt chuông, hành tây và sốt tiêu đen đậm vị.', 240000, N'Đĩa', N'images/dishes/bo-luc-lac.jpg', 0, 1),

-- Lẩu & Súp (Category 3)
(3, N'Lẩu Cua Đồng Hải Sản', N'Nước lẩu riêu cua đồng ngọt thanh, gạch cua béo ngậy ăn kèm hải sản tươi sống và rau đồng quê.', 480000, N'Nồi', N'images/dishes/lau-cua-dong.jpg', 1, 1),
(3, N'Canh Chua Cá Lóc Nam Bộ', N'Cá lóc đồng tươi ngon nấu cùng dọc mùng, giá đỗ, dứa và mầm ngổ thơm lừng.', 175000, N'Tô', N'images/dishes/canh-chua-ca-loc.jpg', 0, 1),

-- Tráng Miệng (Category 4)
(4, N'Chè Hạt Sen Long Nhãn', N'Hạt sen Huế ninh mềm thanh mát lồng bên trong nhãn lồng giòn ngọt, nước đường thốt nốt.', 65000, N'Bát', N'images/dishes/che-hat-sen.jpg', 0, 1),
(4, N'Bánh Flan Sữa Dừa', N'Bánh flan mềm mịn thơm ngậy béo ngậy từ sữa dừa, ăn cùng cà phê phin đắng nhẹ và đá bào.', 55000, N'Phần', N'images/dishes/banh-flan.jpg', 0, 1),

-- Đồ Uống & Trà (Category 5)
(5, N'Trà Sen Tây Hồ', N'Trà mạn ướp hoa sen bách diệp Tây Hồ, hương thơm thanh khiết, vị chát dịu hậu ngọt.', 85000, N'Ấm', N'images/dishes/tra-sen.jpg', 0, 1),
(5, N'Cà Phê Trứng Hà Nội', N'Lớp kem trứng đánh bông mịn ngậy béo phủ trên nền cà phê phin đậm đà truyền thống.', 60000, N'Ly', N'images/dishes/ca-phe-trung.jpg', 1, 1);
GO
-- BỔ SUNG MÓN ĂN ĐỂ TEST CHỨC NĂNG PHÂN TRANG (PAGINATION)

-- 1. Món Khai Vị (category_id = 1) - Thêm 5 món
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(1, N'Nộm Bò Khô Hà Thành', N'Đu đủ bào giòn, bò khô cay nồng, rau thơm và lạc rang quyện nước gỏi chua ngọt.', 115000, N'Đĩa', N'images/dishes/nom-bo-kho.jpg', 0, 1),
(1, N'Nem Nướng Nha Trang', N'Nem nướng than thơm phức, ăn kèm bánh tráng chiên giòn, rau sống và sốt chấm tương nếp đặc chế.', 155000, N'Phần', N'images/dishes/nem-nuong.jpg', 1, 1),
(1, N'Chạo Tôm Bọc Mía', N'Tôm quết nhẵn bọc thân mía ngọt, nướng xém cạnh thơm nức mũi.', 160000, N'Đĩa', N'images/dishes/chao-tom.jpg', 0, 1),
(1, N'Phở Cuốn Hà Nội', N'Bánh phở tráng mỏng cuốn thịt bò xào tái lăn, rau thơm và nước mắm tỏi ớt.', 125000, N'Đĩa', N'images/dishes/pho-cuon.jpg', 1, 1),
(1, N'Bánh Bột Lọc Huế', N'Vỏ bánh trong suốt dai giòn, nhân tôm thịt đậm đà gói lá chuối hấp nóng.', 95000, N'Đĩa', N'images/dishes/banh-bot-loc.jpg', 0, 1);

-- 2. Món Chính (category_id = 2) - Thêm 8 món
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(2, N'Bún Bò Huế Cố Đô', N'Nước dùng đậm đà thơm mùi sả mắm ruốc, kèm tiết, chả cua, nạm bò và giò heo.', 145000, N'Tô', N'images/dishes/bun-bo-hue.jpg', 1, 1),
(2, N'Cơm Gà Hội An', N'Cơm xé xé luộc nước luộc gà vàng ươm, thịt gà ta xé phay trộn rau răm hành tây.', 150000, N'Đĩa', N'images/dishes/com-ga-hoi-an.jpg', 1, 1),
(2, N'Bún Đậu Mắm Tôm Đầy Đủ', N'Bún lá, đậu hũ chiên lướt ván, thịt chân giò, chả cốm và mắm tôm Thanh Hóa dậy mùi.', 135000, N'Mẹt', N'images/dishes/bun-dau.jpg', 1, 1),
(2, N'Vịt Quay Lạng Sơn', N'Vịt tẩm mắc mật quay da giòn bóng, thịt mềm ngọt đượm hương rừng Tây Bắc.', 290000, N'Đĩa', N'images/dishes/vit-quay.jpg', 0, 1),
(2, N'Cánh Gà Chiên Nước Mắm', N'Cánh gà ta đậm vị nước mắm Phú Quốc, áo lớp sốt tỏi ớt kẹo ngọt thơm lừng.', 165000, N'Đĩa', N'images/dishes/canh-ga-chiên-mam.jpg', 0, 1),
(2, N'Cá Lóc Nướng Trui Miền Tây', N'Cá lóc đồng nướng rơm nguyên con, cuốn bánh tráng, rau rừng và mắm nêm đậm đà.', 230000, N'Con', N'images/dishes/ca-loc-nuong.jpg', 0, 1),
(2, N'Heo Quay Bánh Hỏi', N'Thịt heo giòn rụm bì ăn kèm bánh hỏi thoa mỡ hành và nước mắm chua ngọt.', 180000, N'Phần', N'images/dishes/heo-quay-banh-hoi.jpg', 0, 1),
(2, N'Tôm Hùm Sốt Bơ Tỏi', N'Tôm hùm Nha Trang bỏ lò sốt bơ tỏi béo ngậy, ăn kèm bánh mì giòn nóng.', 650000, N'Con', N'images/dishes/tom-hum-bo-toi.jpg', 1, 1);

-- 3. Lẩu & Súp (category_id = 3) - Thêm 5 món
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(3, N'Lẩu Gà Lá É Đà Lạt', N'Nước lẩu vịt cay nhẹ vị ớt hiểm, thơm nức mùi lá é tươi và nấm ớt.', 420000, N'Nồi', N'images/dishes/lau-ga-la-e.jpg', 1, 1),
(3, N'Lẩu Mắm Miền Tây', N'Nước lẩu nấu từ mắm cá linh, cá sặc đượm vị, ăn kèm cá lóc, tôm, mực và 10 loại rau dại.', 490000, N'Nồi', N'images/dishes/lau-mam.jpg', 0, 1),
(3, N'Lẩu Vịt Om Sấu', N'Vịt béo om cùng sấu tươi Hà Nội chua thanh, khoai môn dẻo quánh.', 390000, N'Nồi', N'images/dishes/lau-vit-sau.jpg', 0, 1),
(3, N'Súp Hải Sản Tổ Yến', N'Súp sánh mịn kết hợp tôm, điệp, bào ngư và tổ yến bổ dưỡng.', 195000, N'Thố', N'images/dishes/sup-to-yen.jpg', 1, 1),
(3, N'Canh Nghêu Nấu Chua', N'Nghêu béo nấu cùng cà chua, khế chua và dứa, vị thanh mát giải nhiệt.', 120000, N'Tô', N'images/dishes/canh-ngheu.jpg', 0, 1);

-- 4. Tráng Miệng (category_id = 4) - Thêm 4 món
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(4, N'Chè Bưởi An Giang', N'Cùi bưởi giòn sần sật không đắng, đậu xanh ninh mềm quyện cốt dừa béo ngậy.', 50000, N'Cốc', N'images/dishes/che-buoi.jpg', 0, 1),
(4, N'Kem Xôi Tràng Tiền', N'Xôi nếp dẻo thơm hương lá nếp ăn cùng kem dừa béo lạnh và dừa dừa sấy giòn.', 55000, N'Ly', N'images/dishes/kem-xoi.jpg', 1, 1),
(4, N'Bánh Da Lợn Đậu Xanh', N'Bánh dẻo dai nhiều lớp lá đắng lá nếp và đậu xanh cốt dừa truyền thống.', 45000, N'Đĩa', N'images/dishes/banh-da-lon.jpg', 0, 1),
(4, N'Sương Sa Hạt Lựu', N'Thạch sương sa thanh mát kết hợp hạt lựu củ năng giòn rụm và nước cốt dừa.', 48000, N'Cốc', N'images/dishes/suong-sa.jpg', 0, 1);

-- 5. Đồ Uống & Trà (category_id = 5) - Thêm 3 món
INSERT INTO Dish (category_id, dish_name, description, price, unit, image, is_featured, status) VALUES
(5, N'Nước Sấu Ngâm Đường Hà Nội', N'Sấu ngâm đường gừng thơm nức, chua ngọt hài hòa đập đá mát lạnh.', 45000, N'Ly', N'images/dishes/nuoc-sau.jpg', 0, 1),
(5, N'Sinh Tố Bơ Sáp Bến Tre', N'Bơ sáp dẻo quánh xay cùng sữa đặc nguyên chất béo ngậy.', 65000, N'Ly', N'images/dishes/sinh-to-bo.jpg', 1, 1),
(5, N'Trà Cúc Mật Ong Tây Bắc', N'Trà hoa cúc nguyên bông pha cùng mật ong rừng ngọt dịu thanh nhiệt.', 70000, N'Ấm', N'images/dishes/tra-cuc.jpg', 0, 1);
GO