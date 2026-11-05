package com.campuscomplaint.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections for the whole application.
 *
 * The settings are read once from db.properties on the classpath
 * (src/main/resources/db.properties), so no credentials are hard coded
 * anywhere else in the code.
 *
 * Usage in a DAO:
 *
 *   try (Connection connection = DBConnection.getConnection();
 *        PreparedStatement statement = connection.prepareStatement(SQL)) {
 *       ...
 *   }
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static final Properties PROPERTIES = new Properties();

    /** Non-null when the configuration could not be loaded. */
    private static String configError;

    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                configError = "db.properties was not found on the classpath. It belongs in src/main/resources.";
            } else {
                PROPERTIES.load(input);
                // Optional with JDBC 4, but writing it down makes the driver
                // requirement visible and gives a clear error when it is missing.
                Class.forName(PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
            }
        } catch (IOException e) {
            configError = "db.properties could not be read: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            configError = "The MySQL JDBC driver is not on the classpath. Check the pom.xml and redeploy the WAR.";
        }
    }

    private DBConnection() {
        // utility class: never instantiated
    }

    /**
     * @return a new connection to the campus_complaint_db database
     * @throws SQLException when MySQL is unreachable or the credentials are wrong
     */
    public static Connection getConnection() throws SQLException {
        if (configError != null) {
            throw new SQLException(configError);
        }
        return DriverManager.getConnection(setting("DB_URL", "db.url"),
                setting("DB_USER", "db.user"), setting("DB_PASSWORD", "db.password"));
    }

    /** The JDBC URL currently configured - used by the connection test. */
    public static String getUrl() {
        String url = setting("DB_URL", "db.url");
        return url.isEmpty() ? "(not configured)" : url;
    }

    /**
     * An environment variable wins over db.properties. Locally nothing is set
     * and db.properties is used; on a host such as Railway the credentials
     * come from DB_URL / DB_USER / DB_PASSWORD and never enter the code.
     */
    private static String setting(String environmentVariable, String property) {
        String value = System.getenv(environmentVariable);
        return value == null || value.isBlank() ? PROPERTIES.getProperty(property, "") : value.trim();
    }
}
