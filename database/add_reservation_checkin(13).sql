USE AlacarteRestaurantDB;
GO

-- =========================================================
-- Check-in khách đã đặt bàn: lễ tân bấm "Khách đã đến" -> ghi nhận giờ đến + mở bàn (OCCUPIED).
-- Đơn vẫn ở trạng thái APPROVED (OrderDAO chỉ cho gọi món theo đơn APPROVED) và vẫn giữ bàn;
-- checked_in_at khác NULL nghĩa là khách đã ngồi vào bàn.
-- Chạy SAU add_reservation_booking_flow(11).sql
-- =========================================================

IF OBJECT_ID('dbo.Reservations', 'U') IS NULL
    THROW 50001, 'Reservations does not exist.', 1;
GO

IF COL_LENGTH('dbo.Reservations', 'checked_in_at') IS NULL
    ALTER TABLE dbo.Reservations ADD checked_in_at DATETIME2(0) NULL;
GO

IF COL_LENGTH('dbo.Reservations', 'checked_in_by') IS NULL
    ALTER TABLE dbo.Reservations ADD checked_in_by INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Reservations_CheckedInBy')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT FK_Reservations_CheckedInBy
        FOREIGN KEY (checked_in_by) REFERENCES dbo.Users(user_id);
GO

-- Tìm nhanh theo số điện thoại ở màn hình check-in
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Reservations_Phone')
    CREATE INDEX IX_Reservations_Phone ON dbo.Reservations (customer_phone);
GO

-- Đơn đã COMPLETED từ trước coi như đã check-in để dữ liệu nhất quán
UPDATE dbo.Reservations
SET checked_in_at = COALESCE(updated_at, created_at)
WHERE reservation_status = 'COMPLETED' AND checked_in_at IS NULL;
GO
