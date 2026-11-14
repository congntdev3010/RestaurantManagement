USE AlacarteRestaurantDB;
GO

/* Run once after deploying the corrected review form.
   The previous form displayed radio choices in reverse order, so invert the
   stored ratings that were submitted through that form. Do not run again. */
IF OBJECT_ID('dbo.RestaurantReviews', 'U') IS NULL
    THROW 50025, 'RestaurantReviews does not exist. Run add_reviews(24).sql first.', 1;
GO

UPDATE dbo.RestaurantReviews
SET rating = 6 - rating;
GO
