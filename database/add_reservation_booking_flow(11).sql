USE AlacarteRestaurantDB;
GO

-- =========================================================
-- Luồng đặt bàn: Guest/Customer gửi form -> Lễ tân gọi xác nhận
-- -> Duyệt: chọn bàn cụ thể + ca (trưa/tối) -> APPROVED
-- Chạy SAU add_reservation_order_tables(6).sql
-- =========================================================

IF OBJECT_ID('dbo.Reservations', 'U') IS NULL
    THROW 50001, 'Reservations does not exist. Run add_reservation_order_tables(6).sql first.', 1;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- 1. Bảng ca ăn (lễ tân chọn ca khi duyệt)
IF OBJECT_ID('dbo.MealShifts', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.MealShifts (
        shift_code VARCHAR(10) NOT NULL,
        shift_name NVARCHAR(50) NOT NULL,
        start_time TIME(0) NOT NULL,
        end_time TIME(0) NOT NULL,
        CONSTRAINT PK_MealShifts PRIMARY KEY (shift_code),
        CONSTRAINT CK_MealShifts_Time CHECK (start_time < end_time)
    );

    INSERT INTO dbo.MealShifts (shift_code, shift_name, start_time, end_time) VALUES
    ('LUNCH',  N'Ca trưa', '10:00', '14:30'),
    ('DINNER', N'Ca tối',  '17:00', '22:30');
END;
GO

-- 2. Bổ sung cột cho Reservations
-- Guest không có email -> cho phép NULL
ALTER TABLE dbo.Reservations ALTER COLUMN customer_email NVARCHAR(100) NULL;
GO

-- Loại bàn khách muốn (khớp RestaurantTables.table_type: Cửa sổ, Trong nhà, Ngoài trời, Phòng riêng)
IF COL_LENGTH('dbo.Reservations', 'table_type_requested') IS NULL
    ALTER TABLE dbo.Reservations ADD table_type_requested NVARCHAR(50) NULL;
GO
-- Ca ăn lễ tân chốt khi duyệt (bắt buộc khi APPROVED)
IF COL_LENGTH('dbo.Reservations', 'meal_shift') IS NULL
    ALTER TABLE dbo.Reservations ADD meal_shift VARCHAR(10) NULL;
GO
-- Ghi chú của lễ tân sau khi gọi điện cho khách
IF COL_LENGTH('dbo.Reservations', 'staff_note') IS NULL
    ALTER TABLE dbo.Reservations ADD staff_note NVARCHAR(500) NULL;
GO
-- Lý do từ chối / huỷ
IF COL_LENGTH('dbo.Reservations', 'cancel_reason') IS NULL
    ALTER TABLE dbo.Reservations ADD cancel_reason NVARCHAR(255) NULL;
GO
-- Thời điểm lễ tân gọi khách (NULL = chưa gọi)
IF COL_LENGTH('dbo.Reservations', 'contacted_at') IS NULL
    ALTER TABLE dbo.Reservations ADD contacted_at DATETIME2(0) NULL;
GO
IF COL_LENGTH('dbo.Reservations', 'updated_at') IS NULL
    ALTER TABLE dbo.Reservations ADD updated_at DATETIME2(0) NULL;
GO

-- 3. Dữ liệu cũ: gán ca cho đơn APPROVED đã có (tránh vi phạm ràng buộc mới)
UPDATE dbo.Reservations
SET meal_shift = CASE WHEN reservation_time < '15:00' THEN 'LUNCH' ELSE 'DINNER' END
WHERE meal_shift IS NULL AND reservation_status = 'APPROVED';
GO

-- 4. Ràng buộc
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Reservations_MealShifts')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT FK_Reservations_MealShifts
        FOREIGN KEY (meal_shift) REFERENCES dbo.MealShifts(shift_code);
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Reservations_Status')
    ALTER TABLE dbo.Reservations DROP CONSTRAINT CK_Reservations_Status;
GO
-- PENDING: chờ lễ tân | APPROVED: đã chốt bàn+ca | REJECTED: lễ tân từ chối
-- CANCELLED: khách huỷ | COMPLETED: khách đã đến ăn | NO_SHOW: khách không đến
ALTER TABLE dbo.Reservations ADD CONSTRAINT CK_Reservations_Status CHECK (
    reservation_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Reservations_ApprovedHasTable')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT CK_Reservations_ApprovedHasTable CHECK (
        reservation_status NOT IN ('APPROVED', 'COMPLETED')
        OR (table_id IS NOT NULL AND meal_shift IS NOT NULL)
    );
GO

-- 5. Chặn trùng bàn: 1 bàn / 1 ngày / 1 ca chỉ có tối đa 1 đơn đang giữ chỗ
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Reservations_TableDateShift')
    CREATE UNIQUE INDEX UX_Reservations_TableDateShift
        ON dbo.Reservations (table_id, reservation_date, meal_shift)
        WHERE reservation_status IN ('APPROVED', 'COMPLETED');
GO

-- 6. Index cho màn hình lễ tân: danh sách chờ + xem bàn đã đặt theo ngày/ca/loại bàn
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Reservations_DateShiftStatus')
    CREATE INDEX IX_Reservations_DateShiftStatus
        ON dbo.Reservations (reservation_date, meal_shift, reservation_status)
        INCLUDE (table_id, table_type_requested, guest_count);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Reservations_User')
    CREATE INDEX IX_Reservations_User
        ON dbo.Reservations (user_id, created_at DESC) WHERE user_id IS NOT NULL;
GO

-- =========================================================
-- QUERY MẪU CHO CODE
-- =========================================================
-- (a) Lễ tân: danh sách đơn chờ gọi
--   SELECT * FROM Reservations WHERE reservation_status='PENDING'
--   ORDER BY reservation_date, reservation_time;
--
-- (b) Lễ tân: các bàn cùng loại đã bị giữ trong ngày/ca khách muốn (ca suy ra từ giờ khách chọn)
--   SELECT t.table_id, t.table_name, t.capacity,
--          CASE WHEN r.reservation_id IS NULL THEN 1 ELSE 0 END AS is_free
--   FROM RestaurantTables t
--   LEFT JOIN Reservations r ON r.table_id = t.table_id
--        AND r.reservation_date = @date AND r.meal_shift = @shift
--        AND r.reservation_status IN ('APPROVED','COMPLETED')
--   WHERE t.table_type = @type AND t.capacity >= @guests;
--
-- (c) Duyệt (trong transaction, bắt lỗi 2601/2627 = bàn vừa bị đặt):
--   UPDATE Reservations SET reservation_status='APPROVED', table_id=@tableId,
--          meal_shift=@shift, confirmed_by=@staffId, confirmed_at=SYSDATETIME(),
--          updated_at=SYSDATETIME()
--   WHERE reservation_id=@id AND reservation_status='PENDING';
GO
