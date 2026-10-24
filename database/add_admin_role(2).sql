USE AlacarteRestaurantDB;
GO

IF NOT EXISTS (SELECT 1 FROM Roles WHERE role_name = 'Admin')
BEGIN
    INSERT INTO Roles (role_name) VALUES ('Admin');
END;
GO

IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'admin1@restaurant.com')
BEGIN
    INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active)
    VALUES ('admin1', 'admin1@restaurant.com', '123456', N'Admin One', NULL,
            (SELECT role_id FROM Roles WHERE role_name = 'Admin'), 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM Users WHERE email = 'admin2@restaurant.com')
BEGIN
    INSERT INTO Users (username, email, password, full_name, phone, role_id, is_active)
    VALUES ('admin2', 'admin2@restaurant.com', '123456', N'Admin Two', NULL,
            (SELECT role_id FROM Roles WHERE role_name = 'Admin'), 1);
END;
GO
