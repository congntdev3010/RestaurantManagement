USE AlacarteRestaurantDB;
GO

SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

-- =========================================================
-- Bổ sung bàn cho sơ đồ, KHÔNG xoá/sửa bàn đã có.
-- Mục tiêu: Trung tâm 20 bàn; Cửa sổ, Phòng riêng, Ngoài trời mỗi nơi 10 bàn.
-- Mỗi khu vực chỉ thêm phần còn thiếu (đã có bao nhiêu thì thêm ít đi bấy nhiêu),
-- nên chạy lại nhiều lần cũng không bị thừa bàn.
--
-- @MergeIndoor = 1: bàn cũ loại "Trong nhà" được đổi tên loại thành "Trung tâm"
--   (bàn vẫn giữ nguyên id, đơn gọi món và đặt bàn cũ không bị ảnh hưởng).
--   Đặt 0 nếu muốn giữ "Trong nhà" như một khu vực riêng.
-- =========================================================
DECLARE @MergeIndoor BIT = 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF @MergeIndoor = 1
    BEGIN
        UPDATE dbo.RestaurantTables SET table_type = N'Trung tâm' WHERE table_type = N'Trong nhà';
        -- Đơn đặt bàn đang chờ có yêu cầu loại "Trong nhà" cũng đổi theo để lễ tân thấy đúng
        UPDATE dbo.Reservations SET table_type_requested = N'Trung tâm' WHERE table_type_requested = N'Trong nhà';
    END;

    DECLARE @Plan TABLE (table_type NVARCHAR(50) PRIMARY KEY, target INT NOT NULL, descr NVARCHAR(255) NOT NULL);
    INSERT INTO @Plan VALUES
    (N'Trung tâm',  20, N'Khu vực trung tâm nhà hàng'),
    (N'Cửa sổ',     10, N'Gần cửa sổ, nhìn ra phố'),
    (N'Phòng riêng',10, N'Phòng riêng, không gian kín'),
    (N'Ngoài trời', 10, N'Khu vực ngoài trời, sân vườn');

    ;WITH n AS (
        SELECT TOP (20) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS i FROM sys.all_objects
    ),
    cand AS (
        -- Các số thứ tự chưa bị trùng tên với bàn đang có; cap = sức chứa theo cơ cấu từng khu vực
        SELECT p.table_type, p.descr, p.target,
               table_name = p.table_type + N' ' + RIGHT('0' + CAST(n.i AS VARCHAR(2)), 2),
               capacity = CASE p.table_type
                   WHEN N'Trung tâm'  THEN CASE WHEN n.i <= 6 THEN 2 WHEN n.i <= 14 THEN 4 WHEN n.i <= 18 THEN 6 ELSE 8 END
                   WHEN N'Cửa sổ'     THEN CASE WHEN n.i <= 4 THEN 2 ELSE 4 END
                   WHEN N'Phòng riêng'THEN CASE WHEN n.i <= 3 THEN 6 WHEN n.i <= 7 THEN 8 WHEN n.i <= 9 THEN 10 ELSE 12 END
                   ELSE                    CASE WHEN n.i <= 3 THEN 2 WHEN n.i <= 8 THEN 4 ELSE 6 END
               END,
               rn = ROW_NUMBER() OVER (PARTITION BY p.table_type ORDER BY n.i)
        FROM @Plan p
        JOIN n ON n.i <= 20
        WHERE NOT EXISTS (
            SELECT 1 FROM dbo.RestaurantTables t
            WHERE t.table_name = p.table_type + N' ' + RIGHT('0' + CAST(n.i AS VARCHAR(2)), 2))
    )
    INSERT INTO dbo.RestaurantTables (table_name, table_type, capacity, table_status, description)
    SELECT c.table_name, c.table_type, c.capacity, 'AVAILABLE', c.descr
    FROM cand c
    -- Chỉ lấy đúng số bàn còn thiếu: target - số bàn loại đó đang có
    WHERE c.rn <= c.target - (SELECT COUNT(*) FROM dbo.RestaurantTables e WHERE e.table_type = c.table_type);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- Kiểm tra: số bàn mỗi khu vực
SELECT table_type, COUNT(*) AS so_ban, MIN(capacity) AS min_cho, MAX(capacity) AS max_cho
FROM dbo.RestaurantTables
GROUP BY table_type
ORDER BY table_type;
GO
