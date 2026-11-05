package com.campuscomplaint.service;

import java.io.Serializable;
import java.util.List;

import com.campuscomplaint.dao.ComplaintDAO;
import com.campuscomplaint.dao.FeedbackDAO;
import com.campuscomplaint.dao.NotificationDAO;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.InputValidator;
import com.campuscomplaint.util.TransactionManager;
import com.campuscomplaint.util.ValidationException;

/**
 * Feedback after resolution: one 1-5 rating and an optional comment per
 * complaint, only by the person who reported it, only once the work is done.
 */
public class FeedbackService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** Resolved or closed complaints of the reporter that have not been rated yet. */
    public List<Complaint> getAwaitingFeedback(User reporter) {
        Access.requireReporter(reporter);
        ComplaintFilter filter = new ComplaintFilter();
        filter.setReporterId(reporter.getId());
        filter.setStatus(ComplaintFilter.STATUS_DONE);
        return complaintDAO.search(filter, 0, false).stream().filter(c -> !c.isFeedbackGiven()).toList();
    }

    public List<Feedback> getMyFeedback(User reporter) {
        Access.requireReporter(reporter);
        return feedbackDAO.findByStudent(reporter.getId());
    }

    /** The feedback of a complaint the user may see, or null. */
    public Feedback getForComplaint(int complaintId, User user) {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (!Access.canView(user, complaint)) {
            throw new ValidationException("You are not allowed to view this complaint.");
        }
        return feedbackDAO.findByComplaint(complaintId);
    }

    public void submit(int complaintId, Integer rating, String comments, User reporter) {
        Access.requireReporter(reporter);
        if (rating == null || rating < 1 || rating > 5) {
            throw new ValidationException("Please choose a rating from 1 to 5 stars.");
        }
        String text = InputValidator.optionalText(comments, "Comments", 1000);

        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null || complaint.getUserId() != reporter.getId()) {
            throw new ValidationException("That complaint was not found in your account.");
        }
        if (!Complaint.STATUS_RESOLVED.equals(complaint.getStatus())
                && !Complaint.STATUS_CLOSED.equals(complaint.getStatus())) {
            throw new ValidationException("Feedback can be given once the complaint is resolved.");
        }
        if (complaint.isFeedbackGiven()) {
            throw new ValidationException("You have already given feedback for this complaint.");
        }

        Feedback feedback = new Feedback();
        feedback.setComplaintId(complaintId);
        feedback.setStudentId(reporter.getId());
        feedback.setRating(rating);
        feedback.setComments(text);

        try {
            TransactionManager.inTransaction(connection -> {
                feedbackDAO.insert(connection, feedback);
                if (complaint.getAssignedStaffId() != null) {
                    notificationDAO.insert(connection, complaint.getAssignedStaffId(), complaintId,
                            "Feedback on " + complaint.getComplaintCode() + ": " + feedback.getStars(),
                            text == null ? complaint.getUserName() + " rated your work." : text);
                }
                return null;
            });
        } catch (com.campuscomplaint.util.DataAccessException e) {
            // UNIQUE(complaint_id) caught a double submit (two tabs, double click)
            if (e.getCause() instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062) {
                throw new ValidationException("You have already given feedback for this complaint.");
            }
            throw e;
        }
    }
}
