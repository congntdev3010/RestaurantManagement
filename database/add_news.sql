USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
    THROW 50001, 'Users does not exist. Run the user schema script first.', 1;
GO

IF OBJECT_ID('dbo.News', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.News (
        news_id INT IDENTITY(1,1) NOT NULL,
        title NVARCHAR(200) NOT NULL,
        summary NVARCHAR(500) NOT NULL,
        content NVARCHAR(MAX) NOT NULL,
        image_url NVARCHAR(1000) NULL,
        news_status VARCHAR(10) NOT NULL
            CONSTRAINT DF_News_Status DEFAULT 'DRAFT',
        created_by INT NOT NULL,
        created_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_News_CreatedAt DEFAULT SYSDATETIME(),
        updated_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_News_UpdatedAt DEFAULT SYSDATETIME(),
        published_at DATETIME2(0) NULL,
        CONSTRAINT PK_News PRIMARY KEY (news_id),
        CONSTRAINT FK_News_CreatedBy FOREIGN KEY (created_by)
            REFERENCES dbo.Users(user_id),
        CONSTRAINT CK_News_Status CHECK (news_status IN ('DRAFT', 'PUBLISHED')),
        CONSTRAINT CK_News_PublishedAt CHECK (
            (news_status = 'DRAFT' AND published_at IS NULL)
            OR (news_status = 'PUBLISHED' AND published_at IS NOT NULL)
        )
    );

    CREATE INDEX IX_News_StatusPublishedAt
        ON dbo.News (news_status, published_at DESC, news_id DESC);
    CREATE INDEX IX_News_Title
        ON dbo.News (title);
END;
GO
