USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.RestaurantReviews', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.RestaurantReviews (
        review_id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        history_id INT NOT NULL,
        customer_id INT NOT NULL,
        rating TINYINT NOT NULL,
        review_text NVARCHAR(1000) NOT NULL,
        status VARCHAR(10) NOT NULL CONSTRAINT DF_RestaurantReviews_Status DEFAULT 'PENDING',
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_RestaurantReviews_CreatedAt DEFAULT SYSDATETIME(),
        reviewed_at DATETIME2(0) NULL,
        reviewed_by INT NULL,
        CONSTRAINT FK_RestaurantReviews_History FOREIGN KEY (history_id) REFERENCES dbo.ServiceHistory(history_id),
        CONSTRAINT FK_RestaurantReviews_Customer FOREIGN KEY (customer_id) REFERENCES dbo.Users(user_id),
        CONSTRAINT FK_RestaurantReviews_Reviewer FOREIGN KEY (reviewed_by) REFERENCES dbo.Users(user_id),
        CONSTRAINT UQ_RestaurantReviews_History UNIQUE (history_id),
        CONSTRAINT CK_RestaurantReviews_Rating CHECK (rating BETWEEN 1 AND 5),
        CONSTRAINT CK_RestaurantReviews_Status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
    );
    CREATE INDEX IX_RestaurantReviews_StatusCreated ON dbo.RestaurantReviews(status, created_at DESC);
END;
GO
