USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.News', 'U') IS NULL
    THROW 50001, 'News does not exist. Run database/add_news(10).sql first.', 1;
GO

DECLARE @CreatedBy INT;

SELECT TOP (1) @CreatedBy = u.user_id
FROM dbo.Users u
JOIN dbo.Roles r ON r.role_id = u.role_id
WHERE r.role_name IN ('Admin', 'Manager')
  AND u.is_active = 1
ORDER BY CASE WHEN r.role_name = 'Admin' THEN 0 ELSE 1 END, u.user_id;

IF @CreatedBy IS NULL
    THROW 50002, 'No active Admin or Manager account was found to own the sample news.', 1;

DECLARE @NewsToInsert TABLE (
    title NVARCHAR(200) PRIMARY KEY,
    summary NVARCHAR(500) NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    image_url NVARCHAR(1000) NOT NULL
);

INSERT INTO @NewsToInsert (title, summary, content, image_url) VALUES
(
    N'Gợi ý món ấm bụng cho những ngày mưa',
    N'Khi thời tiết dịu mát, một món ăn nóng hổi cùng hương vị đậm đà sẽ khiến bữa ăn thêm trọn vẹn. Cùng khám phá gợi ý từ căn bếp của nhà hàng.',
    N'Những ngày mưa thường khiến chúng ta muốn tìm một món ăn nóng, thơm và đủ đầy để quây quần bên gia đình. Tại nhà hàng, các món nước và món kho luôn là lựa chọn được nhiều thực khách yêu thích bởi hương vị thân thuộc, dễ dùng trong cả bữa trưa lẫn bữa tối.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Nồi lẩu gà lá é gây ấn tượng với phần nước dùng nóng hổi, vị ngọt từ thịt gà hòa cùng mùi thơm đặc trưng của lá é. Khi dùng kèm rau tươi và bún, món ăn mang đến cảm giác nhẹ nhàng nhưng vẫn đủ ấm áp cho một bữa ăn dài cùng bạn bè.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Nếu muốn chọn một món ăn đậm đà hơn, cá kho tộ là gợi ý đáng cân nhắc. Cá được kho cùng gia vị đến khi thấm đều, dùng với cơm nóng và rau xanh sẽ tạo nên một bữa ăn giản dị, gần gũi với khẩu vị Việt.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Thực khách có thể xem thực đơn để lựa chọn món phù hợp với khẩu vị và số lượng người dùng. Đội ngũ nhà hàng luôn sẵn sàng hỗ trợ khi bạn cần tư vấn món ăn hoặc muốn chuẩn bị bàn cho nhóm đông.',
    N'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=85'
),
(
    N'Mâm cơm Việt với hương vị thân quen',
    N'Từ cơm nóng, món mặn đến rau ăn kèm, một mâm cơm cân bằng luôn gợi nhớ những bữa ăn sum vầy. Nhà hàng gửi đến thực khách một vài gợi ý cho lần ghé thăm tiếp theo.',
    N'Một bữa cơm ngon đôi khi không cần cầu kỳ. Hạt cơm nóng, món ăn được nêm nếm vừa vặn và những món ăn kèm tươi ngon có thể tạo nên cảm giác thân thuộc như bữa cơm nhà.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Cơm gà Hội An là lựa chọn dành cho thực khách yêu thích hương vị miền Trung. Phần cơm vàng thơm được dùng cùng thịt gà và rau ăn kèm, tạo nên tổng thể hài hòa giữa vị đậm đà, độ tươi và cảm giác vừa miệng.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Để bữa ăn phong phú hơn, bạn có thể gọi thêm một món rau hoặc món khai vị dùng chung. Cách gọi món theo phần riêng giúp mỗi thành viên dễ chọn theo khẩu vị, đồng thời cả bàn vẫn có thể cùng chia sẻ nhiều hương vị khác nhau.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Nhà hàng mong muốn mỗi bữa ăn đều thoải mái và vừa đủ. Nếu có yêu cầu về nguyên liệu hoặc cần hỏi thêm thông tin món ăn, vui lòng trao đổi với nhân viên trước khi gọi món để được hỗ trợ tốt nhất.',
    N'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=85'
),
(
    N'Bữa ăn thêm trọn vị với món khai vị tươi ngon',
    N'Món khai vị giúp mở đầu bữa ăn nhẹ nhàng và cân bằng vị giác. Gỏi ngó sen là một lựa chọn tươi mát để cả bàn cùng thưởng thức trước món chính.',
    N'Một món khai vị phù hợp có thể giúp bữa ăn bắt đầu thật dễ chịu. Thay vì chọn món quá no, nhiều thực khách ưu tiên những món có vị thanh, kết cấu giòn và có thể dùng chung cho cả bàn.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Gỏi ngó sen kết hợp độ giòn mát của ngó sen với rau thơm và phần nước trộn chua ngọt. Món ăn có vị tươi sáng, giúp cân bằng những món chính đậm đà và phù hợp để gọi dùng chung trong bữa ăn gia đình hoặc buổi gặp mặt bạn bè.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Khi dùng theo nhóm, bạn có thể bắt đầu bằng một phần khai vị ở giữa bàn, sau đó chọn món chính theo khẩu vị từng người. Cách gọi món này giúp mọi người có thời gian trò chuyện và thưởng thức bữa ăn theo nhịp độ thoải mái.'
        + NCHAR(13) + NCHAR(10) + NCHAR(13) + NCHAR(10)
        + N'Nhà hàng luôn khuyến khích thực khách báo trước nếu có dị ứng hoặc cần lưu ý đặc biệt về thực phẩm. Nhân viên sẽ ghi nhận yêu cầu và trao đổi với bếp để kiểm tra khả năng đáp ứng trước khi xác nhận món.',
    N'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=85'
);

INSERT INTO dbo.News (title, summary, content, image_url, news_status, created_by, published_at)
SELECT seed.title, seed.summary, seed.content, seed.image_url, 'PUBLISHED', @CreatedBy, SYSDATETIME()
FROM @NewsToInsert seed
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.News existing
    WHERE existing.title = seed.title
);

SELECT news_id, title, image_url, news_status, published_at
FROM dbo.News
WHERE title IN (SELECT title FROM @NewsToInsert)
ORDER BY published_at DESC, news_id DESC;
GO