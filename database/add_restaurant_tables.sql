USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.RestaurantTables (
        table_id INT IDENTITY(1,1) PRIMARY KEY,
        table_name NVARCHAR(50) NOT NULL UNIQUE,
        table_type NVARCHAR(50) NOT NULL,
        capacity INT NOT NULL,
        table_status VARCHAR(20) NOT NULL CONSTRAINT DF_RestaurantTables_Status DEFAULT 'AVAILABLE',
        description NVARCHAR(255) NULL,
        created_at DATETIME NOT NULL CONSTRAINT DF_RestaurantTables_CreatedAt DEFAULT GETDATE(),
        CONSTRAINT CK_RestaurantTables_Capacity CHECK (capacity BETWEEN 1 AND 30),
        CONSTRAINT CK_RestaurantTables_Status CHECK (table_status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED', 'UNAVAILABLE'))
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.RestaurantTables)
BEGIN
    INSERT INTO dbo.RestaurantTables (table_name, table_type, capacity, table_status, description) VALUES
    (N'Bàn 01', N'Cửa sổ', 2, 'AVAILABLE', N'Gần cửa sổ'),
    (N'Bàn 02', N'Cửa sổ', 4, 'AVAILABLE', N'Gần cửa sổ'),
    (N'Bàn 03', N'Trong nhà', 4, 'AVAILABLE', NULL),
    (N'Bàn 04', N'Trong nhà', 6, 'AVAILABLE', NULL),
    (N'Phòng riêng 01', N'Phòng riêng', 8, 'AVAILABLE', N'Phòng riêng, tối đa 8 khách'),
    (N'Bàn sân vườn 01', N'Ngoài trời', 4, 'AVAILABLE', N'Khu vực ngoài trời');
END;
GO
