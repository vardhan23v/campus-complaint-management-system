package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Notification;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.NotificationService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of notifications.xhtml (all roles).
 */
@Named("notificationBean")
@ViewScoped
public class NotificationBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NotificationService notificationService = new NotificationService();

    private boolean unreadOnly;
    private List<Notification> notifications = Collections.emptyList();

    @PostConstruct
    public void init() {
        refresh();
    }

    private void refresh() {
        try {
            notifications = notificationService.getNotifications(SessionUtil.getCurrentUser(), unreadOnly);
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public void showAll() {
        unreadOnly = false;
        refresh();
    }

    public void showUnread() {
        unreadOnly = true;
        refresh();
    }

    /** Marks the notification read and opens the complaint it is about. */
    public String open(Notification notification) {
        User user = SessionUtil.getCurrentUser();
        try {
            notificationService.markRead(notification.getId(), user);
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;
        }
        if (notification.getComplaintId() == null) {
            return "/notifications.xhtml?faces-redirect=true";
        }
        String page = switch (user.getRole()) {
            case User.ROLE_ADMIN -> "/complaint-review.xhtml";
            case User.ROLE_STAFF -> "/staff-complaint-details.xhtml";
            default -> "/complaint-details.xhtml";
        };
        return page + "?faces-redirect=true&id=" + notification.getComplaintId();
    }

    public String markAllRead() {
        try {
            int count = notificationService.markAllRead(SessionUtil.getCurrentUser());
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo(count == 0 ? "You have no unread notifications." : count + " notification(s) marked as read.");
            return "/notifications.xhtml?faces-redirect=true";
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;
        }
    }

    public boolean isUnreadOnly() {
        return unreadOnly;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }
}
