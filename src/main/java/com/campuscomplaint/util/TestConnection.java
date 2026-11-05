package com.campuscomplaint.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Stand-alone check of the database configuration.
 *
 * Run it in Eclipse with:  right click the file -> Run As -> Java Application
 * or from the terminal with:
 *   mvn -q compile exec:java -Dexec.mainClass=com.campuscomplaint.util.TestConnection
 *
 * It prints either the MySQL version and the number of rows in each table,
 * or a readable explanation of what is wrong.
 */
public final class TestConnection {

    private static final String[] TABLES = {
        "users", "categories", "complaints", "complaint_status_history",
        "complaint_assignments", "feedback", "notifications"
    };

    private TestConnection() {
        // only the main method is used
    }

    public static void main(String[] args) {
        System.out.println("Nitte Resolve - database connection test");
        System.out.println("URL: " + DBConnection.getUrl());

        try (Connection connection = DBConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Connected successfully.");
            System.out.println("Database : " + metaData.getDatabaseProductName()
                    + " " + metaData.getDatabaseProductVersion());
            System.out.println("Driver   : " + metaData.getDriverName()
                    + " " + metaData.getDriverVersion());
            for (String table : TABLES) {
                // table names come from the constant above, never from input
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
                     ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    System.out.printf("  %-26s %d rows%n", table, resultSet.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection FAILED: " + e.getMessage());
            System.out.println("Check that MySQL is running, that schema.sql has been run,"
                    + " and that db.properties matches your credentials.");
        }
    }
}
