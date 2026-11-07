USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.ServiceHistory', 'U') IS NULL
    THROW 50001, 'ServiceHistory does not exist. Run the payment and loyalty scripts first.', 1;
IF COL_LENGTH('dbo.ServiceHistory', 'loyalty_customer_id') IS NULL
    THROW 50002, 'ServiceHistory.loyalty_customer_id does not exist. Run add_loyalty_points(15).sql first.', 1;
GO

IF OBJECT_ID('dbo.CustomerReviews', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CustomerReviews (
        review_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CustomerReviews PRIMARY KEY,
        history_id INT NOT NULL,
        customer_id INT NOT NULL,
        rating TINYINT NOT NULL,
        comment NVARCHAR(1000) NOT NULL,
        status VARCHAR(10) NOT NULL CONSTRAINT DF_CustomerReviews_Status DEFAULT 'PENDING',
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_CustomerReviews_CreatedAt DEFAULT SYSDATETIME(),
        moderated_by INT NULL,
        moderated_at DATETIME2(0) NULL,
        CONSTRAINT UQ_CustomerReviews_History UNIQUE (history_id),
        CONSTRAINT FK_CustomerReviews_History FOREIGN KEY (history_id)
            REFERENCES dbo.ServiceHistory(history_id),
        CONSTRAINT FK_CustomerReviews_Customer FOREIGN KEY (customer_id)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_CustomerReviews_ModeratedBy FOREIGN KEY (moderated_by)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_CustomerReviews_Rating CHECK (rating BETWEEN 1 AND 5),
        CONSTRAINT CK_CustomerReviews_Comment CHECK (LEN(LTRIM(RTRIM(comment))) > 0),
        CONSTRAINT CK_CustomerReviews_Status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.CustomerReviews')
                 AND name = 'IX_CustomerReviews_Status_CreatedAt')
    CREATE INDEX IX_CustomerReviews_Status_CreatedAt
        ON dbo.CustomerReviews(status, created_at);
GO
