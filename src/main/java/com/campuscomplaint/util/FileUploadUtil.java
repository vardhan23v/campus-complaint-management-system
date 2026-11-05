package com.campuscomplaint.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jakarta.servlet.http.Part;

/**
 * Validates and stores the optional complaint image.
 *
 * The rules, in the order they are checked:
 *   1. the extension of the original name is .jpg, .jpeg or .png
 *   2. the browser-declared content type is image/jpeg or image/png
 *   3. the file is not larger than upload.maxBytes (5 MB by default)
 *   4. the first bytes are a real JPEG or PNG signature (the declared type
 *      and the extension are both controlled by the client)
 *   5. the bytes decode as an image
 *
 * The original file name is never used on disk. The file is saved as a
 * random UUID plus the extension that matches the detected content, inside
 * the private upload folder configured in app.properties - outside the web
 * application, so Tomcat can never serve or execute it directly.
 */
public final class FileUploadUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/jpg", "image/pjpeg", "image/png");

    /** Exactly the names this class creates: a UUID with .jpg or .png. */
    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)");

    private FileUploadUtil() {
        // utility class: never instantiated
    }

    /** @return true when the user did not choose a file at all */
    public static boolean isEmpty(Part part) {
        return part == null || part.getSize() == 0
                || part.getSubmittedFileName() == null || part.getSubmittedFileName().isBlank();
    }

    /**
     * Checks the upload and writes it to the upload folder.
     *
     * @return the generated file name to store in complaints.attachment_path
     * @throws ValidationException with a readable message for an invalid file
     */
    public static String validateAndStore(Part part) {
        byte[] content = validate(part);
        String extension = isPng(content) ? "png" : "jpg";
        String storedName = UUID.randomUUID() + "." + extension;

        try {
            Path directory = AppConfig.uploadDirectory();
            Files.createDirectories(directory);
            Path target = directory.resolve(storedName).normalize();
            if (!target.startsWith(directory)) {
                throw new ValidationException("The image could not be stored.");
            }
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return storedName;
        } catch (IOException e) {
            throw new DataAccessException("The image could not be saved on the server. Please try again.", e);
        }
    }

    /** Runs every check and returns the file content. */
    static byte[] validate(Part part) {
        String originalName = part.getSubmittedFileName();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("Only JPG, JPEG and PNG images can be attached.");
        }

        String contentType = part.getContentType() == null ? "" : part.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException("The selected file is not a JPG or PNG image.");
        }

        long maxBytes = AppConfig.uploadMaxBytes();
        if (part.getSize() > maxBytes) {
            throw new ValidationException("The image is too large. The maximum size is "
                    + (maxBytes / (1024 * 1024)) + " MB.");
        }

        byte[] content;
        try (InputStream input = part.getInputStream()) {
            content = input.readNBytes((int) maxBytes + 1);
        } catch (IOException e) {
            throw new ValidationException("The image could not be read. Please choose it again.");
        }
        if (content.length > maxBytes) {
            throw new ValidationException("The image is too large. The maximum size is "
                    + (maxBytes / (1024 * 1024)) + " MB.");
        }

        boolean png = isPng(content);
        boolean jpeg = isJpeg(content);
        if (!png && !jpeg) {
            throw new ValidationException("The file content is not a valid JPG or PNG image.");
        }
        if (png != "png".equals(extension)) {
            throw new ValidationException("The file extension does not match the image content.");
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new ValidationException("The image appears to be damaged and cannot be opened.");
            }
        } catch (IOException e) {
            throw new ValidationException("The image appears to be damaged and cannot be opened.");
        }
        return content;
    }

    /**
     * Resolves a stored file name to its path, refusing anything that is not a
     * name this class generated - so a tampered database value such as
     * "../../etc/passwd" can never reach the file system.
     *
     * @return the path, or null when the name is invalid or the file is missing
     */
    public static Path resolveStored(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            return null;
        }
        Path directory = AppConfig.uploadDirectory();
        Path file = directory.resolve(storedName).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            return null;
        }
        return file;
    }

    /** Deletes a stored file, used when the database insert after an upload fails. */
    public static void deleteQuietly(String storedName) {
        Path file = resolveStored(storedName);
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // an orphaned image is harmless; nothing to tell the user
            }
        }
    }

    /** @return the MIME type for a stored file name */
    public static String contentTypeOf(String storedName) {
        return storedName != null && storedName.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        // Some browsers send a full client path; only the last segment matters.
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }
}
