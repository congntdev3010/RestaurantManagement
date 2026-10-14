USE AlacarteRestaurantDB;
GO

IF OBJECT_ID('dbo.RestaurantTables', 'U') IS NULL
BEGIN
    THROW 50001, 'RestaurantTables does not exist.', 1;
END;
GO

SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.RestaurantTables
    SET table_status = 'AVAILABLE'
    WHERE table_status = 'UNAVAILABLE';

    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID('dbo.RestaurantTables')
          AND name = 'CK_RestaurantTables_Status'
    )
    BEGIN
        ALTER TABLE dbo.RestaurantTables DROP CONSTRAINT CK_RestaurantTables_Status;
    END;

    ALTER TABLE dbo.RestaurantTables
        ADD CONSTRAINT CK_RestaurantTables_Status
        CHECK (table_status IN ('AVAILABLE', 'RESERVED', 'OCCUPIED'));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO