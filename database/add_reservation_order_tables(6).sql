USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50001, 'Users does not exist. Run the user schema script first.', 1;
IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
    THROW 50002, 'RestaurantTables does not exist. Run the table schema script first.', 1;
GO

IF OBJECT_ID('dbo.Reservations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reservations (
        reservation_id INT IDENTITY(1,1) NOT NULL,
        user_id INT NULL,
        table_id INT NULL,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NOT NULL,
        customer_phone NVARCHAR(20) NOT NULL,
        reservation_date DATE NOT NULL,
        reservation_time TIME(0) NOT NULL,
        guest_count TINYINT NOT NULL,
        reservation_status VARCHAR(20) NOT NULL
            CONSTRAINT DF_Reservations_Status DEFAULT 'PENDING',
        note NVARCHAR(500) NULL,
        created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_Reservations_CreatedAt DEFAULT SYSDATETIME(),
        confirmed_by INT NULL,
        confirmed_at DATETIME2(0) NULL,
        CONSTRAINT PK_Reservations PRIMARY KEY (reservation_id),
        CONSTRAINT FK_Reservations_Users FOREIGN KEY (user_id)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_Reservations_Tables FOREIGN KEY (table_id)
            REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_Reservations_ConfirmedBy FOREIGN KEY (confirmed_by)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Reservations_GuestCount CHECK (guest_count BETWEEN 1 AND 30),
        CONSTRAINT CK_Reservations_Status CHECK (
            reservation_status IN ('PENDING', 'APPROVED', 'CANCELLED')
        )
    );

    CREATE INDEX IX_Reservations_StatusDate
        ON dbo.Reservations (reservation_status, reservation_date, reservation_time);
    CREATE INDEX IX_Reservations_TableDate
        ON dbo.Reservations (table_id, reservation_date, reservation_time);
END;
GO

IF OBJECT_ID('dbo.Orders', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Orders (
        order_id INT IDENTITY(1,1) NOT NULL,
        reservation_id INT NULL,
        table_id INT NULL,
        user_id INT NULL,
        created_by INT NULL,
        order_status VARCHAR(20) NOT NULL
            CONSTRAINT DF_Orders_Status DEFAULT 'OPEN',
        total_amount DECIMAL(12,0) NOT NULL
            CONSTRAINT DF_Orders_TotalAmount DEFAULT 0,
        note NVARCHAR(500) NULL,
        created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_Orders_CreatedAt DEFAULT SYSDATETIME(),
        closed_at DATETIME2(0) NULL,
        CONSTRAINT PK_Orders PRIMARY KEY (order_id),
        CONSTRAINT FK_Orders_Reservations FOREIGN KEY (reservation_id)
            REFERENCES dbo.Reservations(reservation_id),
        CONSTRAINT FK_Orders_Tables FOREIGN KEY (table_id)
            REFERENCES dbo.RestaurantTables(table_id),
        CONSTRAINT FK_Orders_Users FOREIGN KEY (user_id)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_Orders_CreatedBy FOREIGN KEY (created_by)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_Orders_Status CHECK (
            order_status IN ('OPEN', 'PAID', 'CANCELLED')
        ),
        CONSTRAINT CK_Orders_TotalAmount CHECK (total_amount >= 0)
    );

    CREATE INDEX IX_Orders_StatusCreatedAt
        ON dbo.Orders (order_status, created_at);
    CREATE INDEX IX_Orders_TableStatus
        ON dbo.Orders (table_id, order_status);
END;
GO