USE AlacarteRestaurantDB;
GO

IF NOT EXISTS (SELECT 1 FROM Roles WHERE role_name = 'Admin')
BEGIN
    INSERT INTO Roles (role_name) VALUES ('Admin');
END;
GO

-- Grant Admin to a chosen account by setting its email below and running:
-- UPDATE Users
-- SET role_id = (SELECT role_id FROM Roles WHERE role_name = 'Admin')
-- WHERE email = 'replace-with-admin-email';