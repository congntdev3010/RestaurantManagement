USE AlacarteRestaurantDB;
GO

IF COL_LENGTH('dbo.Users', 'is_deleted') IS NULL
BEGIN
    ALTER TABLE dbo.Users
        ADD is_deleted BIT NOT NULL
            CONSTRAINT DF_Users_IsDeleted DEFAULT (0) WITH VALUES;
END;
GO