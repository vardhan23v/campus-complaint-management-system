package com.campuscomplaint.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs several DAO calls on one connection as a single database transaction.
 *
 * Used whenever related rows must change together - for example a status
 * change writes the complaint, a status history row and a notification. If
 * any step fails, everything is rolled back and nothing half-done is left in
 * the database.
 *
 * Usage in a service:
 *
 *   TransactionManager.inTransaction(connection -> {
 *       complaintDAO.updateStatus(connection, ...);
 *       historyDAO.insert(connection, ...);
 *       return null;
 *   });
 */
public final class TransactionManager {

    private static final Logger LOG = Logger.getLogger(TransactionManager.class.getName());

    /** A unit of work that receives the transactional connection. */
    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection connection) throws SQLException;
    }

    private TransactionManager() {
        // utility class: never instantiated
    }

    /**
     * Executes the work, commits on success and rolls back on any failure.
     *
     * A ValidationException thrown inside the work is rethrown unchanged, so
     * the user still sees the business message after the rollback.
     */
    public static <T> T inTransaction(Work<T> work) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection);
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                LOG.log(Level.WARNING, "Transaction rolled back", e);
                throw new DataAccessException("The change could not be saved. Nothing was modified - please try again.", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Could not open a database connection", e);
            throw new DataAccessException("The database is not reachable. Please try again later.", e);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Rollback failed", e);
        }
    }
}
