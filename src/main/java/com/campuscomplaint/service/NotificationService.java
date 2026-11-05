package com.campuscomplaint.service;

import java.io.Serializable;
import java.util.List;

import com.campuscomplaint.dao.NotificationDAO;
import com.campuscomplaint.model.Notification;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.ValidationException;

/**
 * In-app notifications of the logged-in user. Notifications are created by
 * the other services, inside the same transaction as the change they report.
 */
public class NotificationService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public List<Notification> getNotifications(User user, boolean unreadOnly) {
        requireUser(user);
        return notificationDAO.findByUser(user.getId(), unreadOnly, 0);
    }

    public List<Notification> getRecent(User user, int limit) {
        requireUser(user);
        return notificationDAO.findByUser(user.getId(), false, limit);
    }

    public int getUnreadCount(User user) {
        return user == null ? 0 : notificationDAO.countUnread(user.getId());
    }

    public void markRead(int notificationId, User user) {
        requireUser(user);
        notificationDAO.markRead(notificationId, user.getId());
    }

    public int markAllRead(User user) {
        requireUser(user);
        return notificationDAO.markAllRead(user.getId());
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ValidationException("Please log in again.");
        }
    }
}
