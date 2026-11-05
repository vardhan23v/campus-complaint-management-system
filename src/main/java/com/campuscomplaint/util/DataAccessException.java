package com.campuscomplaint.util;

/**
 * Thrown by the DAO classes when a database operation fails.
 *
 * It is a RuntimeException, so the DAOs do not have to declare it, and it
 * carries a message that is short enough to show directly to the user
 * (for example when MySQL is not running). The SQLException is kept as the
 * cause for the server log, never shown on a page.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
