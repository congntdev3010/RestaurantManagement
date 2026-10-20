USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Orders', 'U') IS NULL
    THROW 50004, 'Orders does not exist. Run the reservation/order schema script first.', 1;
GO

IF COL_LENGTH('dbo.Orders', 'chef_status') IS NULL
    ALTER TABLE dbo.Orders ADD chef_status VARCHAR(20) NOT NULL
        CONSTRAINT DF_Orders_ChefStatus DEFAULT 'PENDING' WITH VALUES;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.Orders') AND name = 'CK_Orders_ChefStatus'
)
    ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_ChefStatus
        CHECK (chef_status IN ('PENDING', 'PREPARING', 'COMPLETED'));
GO