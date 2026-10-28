USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50001, 'Users does not exist.', 1;
GO

IF OBJECT_ID('dbo.Reviews', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reviews (
        review_id INT IDENTITY(1,1) NOT NULL,
        user_id INT NOT NULL,
        rating TINYINT NOT NULL,
        review_text NVARCHAR(1000) NULL,
        status VARCHAR(10) NOT NULL
            CONSTRAINT DF_Reviews_Status DEFAULT 'PENDING',
        created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_Reviews_CreatedAt DEFAULT SYSDATETIME(),
        approved_by INT NULL,
        approved_at DATETIME2(0) NULL,
        CONSTRAINT PK_Reviews PRIMARY KEY (review_id),
        CONSTRAINT FK_Reviews_User FOREIGN KEY (user_id)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_Reviews_ApprovedBy FOREIGN KEY (approved_by)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Reviews_Rating CHECK (rating BETWEEN 1 AND 5),
        CONSTRAINT CK_Reviews_Status CHECK (status IN ('PENDING', 'APPROVED'))
    );
END;
GO

-- Upgrade the earlier per-order review schema and retain its existing review data.
IF COL_LENGTH('dbo.Reviews', 'user_id') IS NULL
    ALTER TABLE dbo.Reviews ADD user_id INT NULL;
GO

IF COL_LENGTH('dbo.Reviews', 'user_id') IS NOT NULL
   AND COL_LENGTH('dbo.Reviews', 'order_id') IS NOT NULL
   AND OBJECT_ID('dbo.Orders', 'U') IS NOT NULL
    UPDATE r SET user_id=o.user_id
    FROM dbo.Reviews r JOIN dbo.Orders o ON o.order_id=r.order_id
    WHERE r.user_id IS NULL;
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints
           WHERE parent_object_id=OBJECT_ID('dbo.Reviews') AND name='UQ_Reviews_Order')
    ALTER TABLE dbo.Reviews DROP CONSTRAINT UQ_Reviews_Order;
IF COL_LENGTH('dbo.Reviews', 'order_id') IS NOT NULL
   AND EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id=OBJECT_ID('dbo.Reviews') AND name='order_id' AND is_nullable=0)
    ALTER TABLE dbo.Reviews ALTER COLUMN order_id INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Reviews_User')
    ALTER TABLE dbo.Reviews ADD CONSTRAINT FK_Reviews_User
        FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id=OBJECT_ID('dbo.Reviews') AND name='IX_Reviews_StatusCreated')
    CREATE INDEX IX_Reviews_StatusCreated ON dbo.Reviews(status, created_at DESC);
GO
