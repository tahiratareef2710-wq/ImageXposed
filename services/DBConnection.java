package displayPackage.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the JDBC Connection to the SQL Server database.
 * Follows the Singleton pattern to reuse a single connection.
 */
public class DBConnection {

    // Microsoft SQL Server JDBC Connection String
    // If you use Windows Authentication, append ";integratedSecurity=true;"
    // For standard SQL auth, we pass user/pass in the getConnection method.
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=ImageXposedDB;encrypt=true;trustServerCertificate=true;";

    private static final String USER = "sa";
    private static final String PASS = "admin123"; // the password you just made
    private static final boolean USE_WINDOWS_AUTH = false; // Change this to false!

    private static Connection connection = null;

    private DBConnection() {
        // Private constructor for singleton
    }

    /**
     * Gets the active database connection. Creates it if it doesn't exist.
     */
    public static Connection getConnection() {
        if (connection == null) {
            try {
                // The driver is loaded automatically in newer JDBC, but good practice to
                // include
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

                if (USE_WINDOWS_AUTH) {
                    String authUrl = URL + "integratedSecurity=true;";
                    connection = DriverManager.getConnection(authUrl);
                } else {
                    connection = DriverManager.getConnection(URL, USER, PASS);
                }
                System.out.println("[DBConnection] Connected to SQL Server successfully.");

            } catch (ClassNotFoundException e) {
                System.err
                        .println("[DBConnection] MS SQL JDBC Driver not found. Ensure mssql-jdbc.jar is in classpath.");
            } catch (SQLException e) {
                System.err.println("[DBConnection] Database connection failed: " + e.getMessage());
            }
        }
        return connection;
    }

    /**
     * Closes the active connection.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[DBConnection] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            }
        }
    }
}
