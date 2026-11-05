package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.Notification;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * The notifications table. Every read and update is limited to the rows of
 * one user_id, so nobody can read or mark somebody else's notifications.
 */
public class NotificationDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Creates a notification inside the caller's transaction. */
    public void insert(Connection connection, int userId, Integer complaintId, String title, String message)
            throws SQLException {
        String sql = "INSERT INTO notifications (user_id, complaint_id, title, message) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            DaoSupport.setNullableInt(statement, 2, complaintId);
            statement.setString(3, truncate(title, 150));
            statement.setString(4, truncate(message, 500));
            statement.executeUpdate();
        }
    }

    /**
     * The notifications of one user, newest first.
     *
     * @param limit maximum rows, or 0 for all
     */
    public List<Notification> findByUser(int userId, boolean unreadOnly, int limit) {
        String sql = "SELECT n.id, n.user_id, n.complaint_id, n.title, n.message, n.is_read, n.created_at, "
                   + "c.complaint_code FROM notifications n "
                   + "LEFT JOIN complaints c ON c.id = n.complaint_id "
                   + "WHERE n.user_id = ? " + (unreadOnly ? "AND n.is_read = FALSE " : "")
                   + "ORDER BY n.created_at DESC, n.id DESC" + (limit > 0 ? " LIMIT ?" : "");
        List<Notification> notifications = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            if (limit > 0) {
                statement.setInt(2, limit);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Notification notification = new Notification();
                    notification.setId(resultSet.getInt("id"));
                    notification.setUserId(resultSet.getInt("user_id"));
                    notification.setComplaintId(DaoSupport.nullableInt(resultSet, "complaint_id"));
                    notification.setComplaintCode(resultSet.getString("complaint_code"));
                    notification.setTitle(resultSet.getString("title"));
                    notification.setMessage(resultSet.getString("message"));
                    notification.setRead(resultSet.getBoolean("is_read"));
                    notification.setCreatedAt(DaoSupport.dateTime(resultSet, "created_at"));
                    notifications.add(notification);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the notifications.", e);
        }
        return notifications;
    }

    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the notifications.", e);
        }
    }

    /** Marks one notification as read - only when it belongs to the user. */
    public boolean markRead(int notificationId, int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, notificationId);
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the notification.", e);
        }
    }

    public int markAllRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the notifications.", e);
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
