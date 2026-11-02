-- Lưu Gmail của khách trên hóa đơn (ServiceHistory), giống customer_phone.
-- Chạy một lần trên CSDL AlacarteRestaurantDB. Hóa đơn cũ giữ NULL.
IF COL_LENGTH('dbo.ServiceHistory', 'customer_email') IS NULL
    ALTER TABLE dbo.ServiceHistory ADD customer_email NVARCHAR(100) NULL;
GO
