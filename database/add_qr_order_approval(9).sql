USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL THROW 50001, 'RestaurantTables does not exist.', 1;
IF OBJECT_ID('dbo.Orders', 'U') IS NULL THROW 50002, 'Orders does not exist.', 1;
IF OBJECT_ID('dbo.OrderItems', 'U') IS NULL
    THROW 50003, 'OrderItems does not exist. Run add_order_items_and_workflow.sql first.', 1;
GO

-- 1. Moi ban co token QR ngau nhien rieng (khong doan duoc tu so ban).
IF COL_LENGTH('dbo.RestaurantTables', 'qr_token') IS NULL
    ALTER TABLE dbo.RestaurantTables ADD qr_token VARCHAR(36) NULL;
GO
UPDATE dbo.RestaurantTables SET qr_token = LOWER(CONVERT(VARCHAR(36), NEWID())) WHERE qr_token IS NULL;
GO
ALTER TABLE dbo.RestaurantTables ALTER COLUMN qr_token VARCHAR(36) NOT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_RestaurantTables_QrToken')
    ALTER TABLE dbo.RestaurantTables ADD CONSTRAINT DF_RestaurantTables_QrToken
        DEFAULT (LOWER(CONVERT(VARCHAR(36), NEWID()))) FOR qr_token;
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.RestaurantTables') AND name = 'UX_RestaurantTables_QrToken')
    CREATE UNIQUE INDEX UX_RestaurantTables_QrToken ON dbo.RestaurantTables(qr_token);
GO

-- 2. Trang thai tung mon: PENDING (cho duyet), COOKING (dang nau), SERVED (da phuc vu), REJECTED (het mon).
IF COL_LENGTH('dbo.OrderItems', 'item_status') IS NULL
    ALTER TABLE dbo.OrderItems ADD item_status VARCHAR(20) NOT NULL
        CONSTRAINT DF_OrderItems_ItemStatus DEFAULT 'PENDING';
IF COL_LENGTH('dbo.OrderItems', 'item_note') IS NULL
    ALTER TABLE dbo.OrderItems ADD item_note NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.OrderItems', 'requested_quantity') IS NULL
    ALTER TABLE dbo.OrderItems ADD requested_quantity INT NULL;
GO
UPDATE dbo.OrderItems SET requested_quantity = quantity WHERE requested_quantity IS NULL;
-- Du lieu cu: don da chay thi coi nhu mon da duoc duyet.
UPDATE oi SET item_status = CASE o.order_status
        WHEN 'PENDING' THEN 'PENDING'
        WHEN 'CANCELLED' THEN 'REJECTED'
        WHEN 'PAID' THEN 'SERVED'
        ELSE 'COOKING' END
FROM dbo.OrderItems oi JOIN dbo.Orders o ON o.order_id = oi.order_id
WHERE oi.item_status = 'PENDING' AND o.order_status <> 'PENDING';
GO
ALTER TABLE dbo.OrderItems ALTER COLUMN requested_quantity INT NOT NULL;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_OrderItems_ItemStatus')
    ALTER TABLE dbo.OrderItems DROP CONSTRAINT CK_OrderItems_ItemStatus;
ALTER TABLE dbo.OrderItems WITH CHECK ADD CONSTRAINT CK_OrderItems_ItemStatus
    CHECK (item_status IN ('PENDING', 'COOKING', 'SERVED', 'REJECTED'));
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.OrderItems') AND name = 'IX_OrderItems_Status')
    CREATE INDEX IX_OrderItems_Status ON dbo.OrderItems(item_status, order_id);
GO

-- 3. Don: PENDING (cho server duyet) -> OPEN (dang phuc vu) -> PAID / CANCELLED.
--    Buoc APPROVED cu bi bo vi viec duyet nay duoc lam theo tung mon.
UPDATE dbo.Orders SET order_status = 'OPEN' WHERE order_status = 'APPROVED';
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints
           WHERE parent_object_id = OBJECT_ID('dbo.Orders') AND name = 'CK_Orders_Status')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_Status;
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_Status
    CHECK (order_status IN ('PENDING', 'OPEN', 'PAID', 'CANCELLED'));
GO
