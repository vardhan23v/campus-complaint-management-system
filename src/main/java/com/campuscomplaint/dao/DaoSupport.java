package com.campuscomplaint.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * JDBC conversions shared by the DAO classes: LocalDateTime and nullable
 * Integer columns have no one-line ResultSet/PreparedStatement methods.
 */
final class DaoSupport {

    private DaoSupport() {
        // utility class: never instantiated
    }

    static LocalDateTime dateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    static Integer nullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    static void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    /** true when MySQL refused a DELETE because other rows still reference the row. */
    static boolean isForeignKeyViolation(SQLException e) {
        return e.getErrorCode() == 1451 || "23000".equals(e.getSQLState()) && e.getErrorCode() != 1062;
    }

    /** true when MySQL refused an INSERT/UPDATE because of a UNIQUE constraint. */
    static boolean isDuplicateKey(SQLException e) {
        return e.getErrorCode() == 1062;
    }
}
