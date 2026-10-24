/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBContext implements AutoCloseable {
    protected Connection connection;
    public DBContext()
    {
        try {
            String url = setting("RESTAURANT_DB_URL",
                    "jdbc:sqlserver://localhost:1433;databaseName=AlacarteRestaurantDB;encrypt=true;trustServerCertificate=true");
            String username = setting("RESTAURANT_DB_USERNAME", "sa");
            String password = setting("RESTAURANT_DB_PASSWORD", "123");
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException | SQLException ex) {
            throw new IllegalStateException("Cannot connect to the restaurant database. Check that SQL Server is running, "
                    + "the AlacarteRestaurantDB schema has been installed, and RESTAURANT_DB_URL/USERNAME/PASSWORD are correct.", ex);
        }
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }
}
