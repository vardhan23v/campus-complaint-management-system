package com.campuscomplaint.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application settings read once from app.properties on the classpath
 * (upload folder, upload size limit, complaint code prefix). Database
 * settings stay in db.properties, read by DBConnection.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "app.properties could not be read; defaults are used.", e);
        }
    }

    private AppConfig() {
        // utility class: never instantiated
    }

    /** The private folder holding uploaded complaint images. */
    public static Path uploadDirectory() {
        // UPLOAD_DIRECTORY (e.g. a Railway volume) wins over app.properties.
        String fromEnvironment = System.getenv("UPLOAD_DIRECTORY");
        String configured = fromEnvironment != null && !fromEnvironment.isBlank()
                ? fromEnvironment.trim()
                : PROPERTIES.getProperty("upload.directory", "").trim();
        if (configured.isEmpty()) {
            return Paths.get(System.getProperty("user.home"), "campus-complaint-uploads").toAbsolutePath().normalize();
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    /** Largest accepted upload in bytes. */
    public static long uploadMaxBytes() {
        try {
            return Long.parseLong(PROPERTIES.getProperty("upload.maxBytes", "5242880").trim());
        } catch (NumberFormatException e) {
            return 5L * 1024 * 1024;
        }
    }

    /** The prefix of complaint codes such as NIT-2026-00001. */
    public static String complaintCodePrefix() {
        String prefix = PROPERTIES.getProperty("complaint.codePrefix", "NIT").trim();
        return prefix.isEmpty() ? "NIT" : prefix;
    }
}
