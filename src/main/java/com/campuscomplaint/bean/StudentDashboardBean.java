package com.campuscomplaint.bean;

import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.Notification;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.ComplaintService;
import com.campuscomplaint.service.FeedbackService;
import com.campuscomplaint.service.NotificationService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of student-dashboard.xhtml: counts, recent complaints,
 * recent notifications and how many resolved complaints wait for a rating.
 */
@Named("studentDashboardBean")
@RequestScoped
public class StudentDashboardBean {

    private final ComplaintService complaintService = new ComplaintService();
    private final NotificationService notificationService = new NotificationService();
    private final FeedbackService feedbackService = new FeedbackService();

    private StatusSummary summary = new StatusSummary();
    private List<Complaint> recentComplaints = Collections.emptyList();
    private List<Notification> notifications = Collections.emptyList();
    private int awaitingFeedback;

    @PostConstruct
    public void init() {
        User user = SessionUtil.getCurrentUser();
        try {
            summary = complaintService.getSummaryForReporter(user);
            recentComplaints = complaintService.getRecentForReporter(user, 6);
            notifications = notificationService.getRecent(user, 5);
            awaitingFeedback = feedbackService.getAwaitingFeedback(user).size();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public StatusSummary getSummary() {
        return summary;
    }

    public List<Complaint> getRecentComplaints() {
        return recentComplaints;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public int getAwaitingFeedback() {
        return awaitingFeedback;
    }
}
