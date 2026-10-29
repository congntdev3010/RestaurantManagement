-- =========================================================
-- Đổi điều kiện nhận voucher: tổng đã chi >= ngưỡng (trước đây là >).
-- Lý do: khách mới (chưa chi gì, ngưỡng 0) cũng được nhận voucher.
-- Chạy một lần trên CSDL AlacarteRestaurantDB (chạy lại không lỗi).
-- Áp dụng cho DB đã chạy add_voucher_system(21).sql.
-- =========================================================
USE AlacarteRestaurantDB;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints
           WHERE parent_object_id = OBJECT_ID('dbo.Vouchers')
             AND name = 'CK_Vouchers_Eligibility')
    ALTER TABLE dbo.Vouchers DROP CONSTRAINT CK_Vouchers_Eligibility;
GO

ALTER TABLE dbo.Vouchers WITH CHECK ADD CONSTRAINT CK_Vouchers_Eligibility CHECK (
    required_total_spent >= 0 AND customer_total_spent >= required_total_spent);
GO
