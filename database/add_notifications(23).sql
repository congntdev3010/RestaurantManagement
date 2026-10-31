-- =========================================================
-- Thông báo trong app cho người dùng (chuông ở header).
-- Hiện tại dùng cho sự kiện "được nhận voucher".
-- Chạy một lần trên CSDL AlacarteRestaurantDB (idempotent: chạy lại không lỗi).
-- =========================================================
USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Notifications', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Notifications (
        notification_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notifications PRIMARY KEY,
        user_id INT NOT NULL,
        title NVARCHAR(150) NOT NULL,
        body NVARCHAR(500) NULL,
        is_read BIT NOT NULL CONSTRAINT DF_Notifications_IsRead DEFAULT 0,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_Notifications_CreatedAt DEFAULT SYSDATETIME(),
        read_at DATETIME2(0) NULL,
        CONSTRAINT FK_Notifications_User FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Notifications_ReadAt CHECK (
            (is_read = 1 AND read_at IS NOT NULL) OR (is_read = 0 AND read_at IS NULL))
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.Notifications') AND name = 'IX_Notifications_User')
    CREATE INDEX IX_Notifications_User ON dbo.Notifications(user_id, is_read, created_at DESC);
GO
