USE AlacarteRestaurantDB;
GO

-- =========================================================
-- Gộp bàn: một đơn đặt bàn có thể giữ nhiều bàn (vd 16 khách = 2 bàn 8 chỗ).
-- Reservations.table_id vẫn là BÀN CHÍNH; các bàn ghép thêm nằm ở bảng này.
-- Chạy SAU add_reservation_checkin(13).sql
--
-- Từ nay check-in ("Khách đã đến") mở mọi bàn của đơn sang OCCUPIED và đặt luôn đơn thành COMPLETED.
-- =========================================================

IF OBJECT_ID('dbo.Reservations', 'U') IS NULL
    THROW 50001, 'Reservations does not exist.', 1;
GO

IF OBJECT_ID('dbo.ReservationTables', 'U') IS NULL
    CREATE TABLE dbo.ReservationTables (
        reservation_id INT NOT NULL,
        table_id       INT NOT NULL,
        CONSTRAINT PK_ReservationTables PRIMARY KEY (reservation_id, table_id),
        CONSTRAINT FK_ReservationTables_Reservation FOREIGN KEY (reservation_id) REFERENCES dbo.Reservations(reservation_id),
        CONSTRAINT FK_ReservationTables_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id)
    );
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ReservationTables_Table')
    CREATE INDEX IX_ReservationTables_Table ON dbo.ReservationTables (table_id);
GO

-- Đơn đã check-in nhưng còn APPROVED (dữ liệu cũ) coi như đã hoàn thành
UPDATE dbo.Reservations
SET reservation_status = 'COMPLETED', updated_at = SYSDATETIME()
WHERE reservation_status = 'APPROVED' AND checked_in_at IS NOT NULL;
GO
