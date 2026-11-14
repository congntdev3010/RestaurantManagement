USE AlacarteRestaurantDB;
GO

/* Hide deleted review content while retaining its unique history_id as a
   permanent record that this meal has already been reviewed. */
IF OBJECT_ID('dbo.RestaurantReviews', 'U') IS NULL
    THROW 50026, 'RestaurantReviews does not exist. Run add_reviews(24).sql first.', 1;
GO

IF COL_LENGTH('dbo.RestaurantReviews', 'is_deleted') IS NULL
BEGIN
    ALTER TABLE dbo.RestaurantReviews
        ADD is_deleted BIT NOT NULL
            CONSTRAINT DF_RestaurantReviews_IsDeleted DEFAULT 0;
END;
GO
