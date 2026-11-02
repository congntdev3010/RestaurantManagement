-- Đơn đã check-in (COMPLETED) không còn giữ bàn nữa: bàn chỉ bị giữ bởi đơn APPROVED (khách chưa đến).
-- Sau khi khách đến và rời đi, lễ tân trả bàn về AVAILABLE ở sơ đồ bàn thì bàn trống trở lại.
-- Chạy một lần trên CSDL đã có UX_Reservations_TableDateShift (script add_reservation_booking_flow).

IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Reservations_TableDateShift')
    DROP INDEX UX_Reservations_TableDateShift ON dbo.Reservations;
GO

CREATE UNIQUE INDEX UX_Reservations_TableDateShift
    ON dbo.Reservations (table_id, reservation_date, meal_shift)
    WHERE reservation_status = 'APPROVED';
GO
