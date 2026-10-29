USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Orders', 'U') IS NULL
    THROW 50001, 'Orders does not exist.', 1;
GO

IF COL_LENGTH('dbo.Orders', 'payment_method') IS NULL
    ALTER TABLE dbo.Orders ADD payment_method VARCHAR(10) NULL;
IF COL_LENGTH('dbo.Orders', 'paid_by') IS NULL
    ALTER TABLE dbo.Orders ADD paid_by INT NULL;
IF COL_LENGTH('dbo.Orders', 'paid_at') IS NULL
    ALTER TABLE dbo.Orders ADD paid_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.Orders', 'payment_reference') IS NULL
    ALTER TABLE dbo.Orders ADD payment_reference NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.Orders', 'cash_received') IS NULL
    ALTER TABLE dbo.Orders ADD cash_received DECIMAL(14,0) NULL;
IF COL_LENGTH('dbo.Orders', 'cash_change') IS NULL
    ALTER TABLE dbo.Orders ADD cash_change DECIMAL(14,0) NULL;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints
           WHERE parent_object_id = OBJECT_ID('dbo.Orders')
             AND name = 'CK_Orders_PaymentMethod')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_PaymentMethod;
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_PaymentMethod
    CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'QR'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Orders_PaidBy')
    ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_PaidBy
        FOREIGN KEY (paid_by) REFERENCES dbo.Users(user_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.Orders')
                 AND name = 'IX_Orders_Payment')
    CREATE INDEX IX_Orders_Payment ON dbo.Orders(order_status, payment_method, paid_at);
GO

IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ServiceHistory (
        history_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        table_id INT NOT NULL,
        customer_name NVARCHAR(100) NULL,
        customer_phone NVARCHAR(20) NULL,
        reservation_time DATETIME2(0) NULL,
        checked_in_at DATETIME2(0) NULL,
        paid_at DATETIME2(0) NOT NULL,
        payment_method VARCHAR(10) NOT NULL,
        total_amount DECIMAL(14,0) NOT NULL,
        cash_received DECIMAL(14,0) NULL,
        cash_change DECIMAL(14,0) NULL,
        paid_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_ServiceHistory_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ServiceHistory_Table FOREIGN KEY (table_id) REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_ServiceHistory_PaidBy FOREIGN KEY (paid_by) REFERENCES dbo.Users(user_id)
    );
    CREATE INDEX IX_ServiceHistory_PaidAt ON dbo.ServiceHistory(paid_at DESC);
END;
GO

IF OBJECT_ID('dbo.ServiceHistoryItems', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ServiceHistoryItems (
        history_item_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        history_id INT NOT NULL,
        dish_name NVARCHAR(150) NOT NULL,
        unit NVARCHAR(50) NOT NULL,
        unit_price DECIMAL(12,0) NOT NULL,
        quantity INT NOT NULL,
        line_total DECIMAL(14,0) NOT NULL,
        CONSTRAINT FK_ServiceHistoryItems_History FOREIGN KEY (history_id)
            REFERENCES dbo.ServiceHistory(history_id)
    );
END;
GO
