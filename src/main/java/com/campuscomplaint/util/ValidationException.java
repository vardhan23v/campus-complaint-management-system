package com.campuscomplaint.util;

/**
 * A business rule the user broke: invalid input, a status change that is not
 * allowed, or an action outside the user's role.
 *
 * The message is written for the user and shown on the page as is.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
