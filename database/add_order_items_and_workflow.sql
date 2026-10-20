USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50001, 'Users does not exist.', 1;
IF OBJECT_ID('dbo.Dish', 'U') IS NULL
    THROW 50002, 'Dish does not exist. Run the dish schema script first.', 1;
IF OBJECT_ID('dbo.Orders', 'U') IS NULL
    THROW 50003, 'Orders does not exist. Run the reservation/order schema script first.', 1;
GO

IF COL_LENGTH('dbo.Orders', 'customer_name') IS NULL
    ALTER TABLE dbo.Orders ADD customer_name NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders', 'customer_phone') IS NULL
    ALTER TABLE dbo.Orders ADD customer_phone NVARCHAR(20) NULL;
IF COL_LENGTH('dbo.Orders', 'customer_email') IS NULL
    ALTER TABLE dbo.Orders ADD customer_email NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders', 'approved_by') IS NULL
    ALTER TABLE dbo.Orders ADD approved_by INT NULL;
IF COL_LENGTH('dbo.Orders', 'approved_at') IS NULL
    ALTER TABLE dbo.Orders ADD approved_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.Orders', 'cancelled_by') IS NULL
    ALTER TABLE dbo.Orders ADD cancelled_by INT NULL;
IF COL_LENGTH('dbo.Orders', 'cancelled_at') IS NULL
    ALTER TABLE dbo.Orders ADD cancelled_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.Orders', 'cancellation_reason') IS NULL
    ALTER TABLE dbo.Orders ADD cancellation_reason NVARCHAR(500) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_ApprovedBy')
    ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_ApprovedBy
        FOREIGN KEY (approved_by) REFERENCES dbo.Users(user_id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_CancelledBy')
    ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_CancelledBy
        FOREIGN KEY (cancelled_by) REFERENCES dbo.Users(user_id);
GO

IF OBJECT_ID('dbo.OrderItems', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderItems (
        order_item_id INT IDENTITY(1,1) NOT NULL,
        order_id INT NOT NULL,
        dish_id INT NULL,
        dish_name NVARCHAR(150) NOT NULL,
        unit NVARCHAR(50) NOT NULL,
        unit_price DECIMAL(12,0) NOT NULL,
        quantity INT NOT NULL,
        line_total DECIMAL(14,0) NOT NULL,
        CONSTRAINT PK_OrderItems PRIMARY KEY (order_item_id),
        CONSTRAINT FK_OrderItems_Orders FOREIGN KEY (order_id)
            REFERENCES dbo.Orders(order_id),
        CONSTRAINT FK_OrderItems_Dish FOREIGN KEY (dish_id)
            REFERENCES dbo.Dish(dish_id),
        CONSTRAINT CK_OrderItems_UnitPrice CHECK (unit_price >= 0),
        CONSTRAINT CK_OrderItems_Quantity CHECK (quantity > 0),
        CONSTRAINT CK_OrderItems_LineTotal CHECK (line_total >= 0)
    );
    CREATE INDEX IX_OrderItems_OrderId ON dbo.OrderItems(order_id);
END;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints
           WHERE parent_object_id = OBJECT_ID('dbo.Orders')
             AND name = 'CK_Orders_Status')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_Status;
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_Status
    CHECK (order_status IN ('PENDING', 'APPROVED', 'OPEN', 'PAID', 'CANCELLED'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.Orders')
                 AND name = 'IX_Orders_UserStatusCreatedAt')
    CREATE INDEX IX_Orders_UserStatusCreatedAt
        ON dbo.Orders(user_id, order_status, created_at DESC);
GO
