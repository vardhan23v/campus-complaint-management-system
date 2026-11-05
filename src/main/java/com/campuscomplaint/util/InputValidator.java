package com.campuscomplaint.util;

import java.util.regex.Pattern;

/**
 * Server-side input checks shared by the services.
 *
 * The pages validate too (required="true", f:validateLength), but a page can
 * be bypassed with a hand-made request; these checks cannot. Every method
 * returns the cleaned (trimmed) value or throws a ValidationException with a
 * message for the user.
 */
public final class InputValidator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9][0-9 -]{6,18}[0-9]$");

    private InputValidator() {
        // utility class: never instantiated
    }

    /** A required text between min and max characters (after trimming). */
    public static String requireText(String value, String label, int min, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ValidationException(label + " is required.");
        }
        if (text.length() < min) {
            throw new ValidationException(label + " must be at least " + min + " characters long.");
        }
        if (text.length() > max) {
            throw new ValidationException(label + " must not be longer than " + max + " characters.");
        }
        return text;
    }

    /** An optional text: null when empty, otherwise trimmed and length-checked. */
    public static String optionalText(String value, String label, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > max) {
            throw new ValidationException(label + " must not be longer than " + max + " characters.");
        }
        return text;
    }

    /** A required, well-formed e-mail address, returned in lower case. */
    public static String requireEmail(String value) {
        String email = requireText(value, "E-mail address", 5, 150).toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Please enter a valid e-mail address.");
        }
        return email;
    }

    /** An optional phone number (digits, spaces, dashes, leading +). */
    public static String optionalPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String phone = value.trim();
        if (!PHONE.matcher(phone).matches()) {
            throw new ValidationException("Please enter a valid phone number (8-20 digits).");
        }
        return phone;
    }

    /** At least 8 characters with a letter and a digit. */
    public static String requirePassword(String value) {
        if (value == null || value.length() < 8) {
            throw new ValidationException("The password must be at least 8 characters long.");
        }
        if (value.length() > 100) {
            throw new ValidationException("The password must not be longer than 100 characters.");
        }
        if (!value.matches(".*[A-Za-z].*") || !value.matches(".*[0-9].*")) {
            throw new ValidationException("The password must contain at least one letter and one digit.");
        }
        return value;
    }
}
