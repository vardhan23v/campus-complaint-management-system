package com.campuscomplaint.service;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import com.campuscomplaint.dao.ComplaintDAO;
import com.campuscomplaint.dao.NotificationDAO;
import com.campuscomplaint.dao.StatusHistoryDAO;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.ValidationException;

/**
 * The complaint lifecycle in one place.
 *
 *   SUBMITTED -> UNDER_REVIEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
 *
 * Who may make which move:
 *   administrator  SUBMITTED -> UNDER_REVIEW, (re)assign -> ASSIGNED,
 *                  RESOLVED -> CLOSED, RESOLVED -> ASSIGNED (reopen)
 *   assigned staff ASSIGNED -> IN_PROGRESS, IN_PROGRESS -> RESOLVED
 *
 * Every move goes through transition(), which - inside the caller's
 * transaction - updates the complaint, appends a status history row (never
 * overwriting an old one) and notifies the reporter.
 */
class ComplaintWorkflow implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final StatusHistoryDAO historyDAO = new StatusHistoryDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** true when the administrator may move a complaint from one status to the other. */
    static boolean adminMayMove(String from, String to) {
        return (Complaint.STATUS_SUBMITTED.equals(from) && Complaint.STATUS_UNDER_REVIEW.equals(to))
                || (Complaint.STATUS_RESOLVED.equals(from) && Complaint.STATUS_CLOSED.equals(to))
                || (Complaint.STATUS_RESOLVED.equals(from) && Complaint.STATUS_ASSIGNED.equals(to));
    }

    /** true when the assigned staff member may move a complaint from one status to the other. */
    static boolean staffMayMove(String from, String to) {
        return (Complaint.STATUS_ASSIGNED.equals(from) && Complaint.STATUS_IN_PROGRESS.equals(to))
                || (Complaint.STATUS_IN_PROGRESS.equals(from) && Complaint.STATUS_RESOLVED.equals(to));
    }

    /** Statuses from which an administrator may (re)assign a complaint. */
    static boolean assignable(String status) {
        return Complaint.STATUS_SUBMITTED.equals(status) || Complaint.STATUS_UNDER_REVIEW.equals(status)
                || Complaint.STATUS_ASSIGNED.equals(status) || Complaint.STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Loads and locks a complaint for a change inside a transaction.
     *
     * @throws ValidationException when it does not exist
     */
    Complaint lock(Connection connection, int complaintId) throws SQLException {
        Complaint complaint = complaintDAO.findById(connection, complaintId, true);
        if (complaint == null) {
            throw new ValidationException("That complaint does not exist.");
        }
        return complaint;
    }

    /**
     * Moves the complaint to a new status, writes the history row and
     * notifies the reporter. The caller has already checked the permission.
     */
    void transition(Connection connection, Complaint complaint, String newStatus, User actor, String remarks)
            throws SQLException {
        String previous = complaint.getStatus();
        if (!complaintDAO.updateStatus(connection, complaint.getId(), previous, newStatus)) {
            throw new ValidationException("This complaint was changed by someone else a moment ago. Please reload the page.");
        }
        historyDAO.insert(connection, complaint.getId(), previous, newStatus, actor.getId(), remarks);
        complaint.setStatus(newStatus);

        notifyReporter(connection, complaint,
                complaint.getComplaintCode() + " is now " + Complaint.labelOf(newStatus),
                remarks == null ? reporterMessage(newStatus) : remarks);
    }

    /** Records a remark without changing the status (previous = new in the history). */
    void remark(Connection connection, Complaint complaint, User actor, String remarks) throws SQLException {
        historyDAO.insert(connection, complaint.getId(), complaint.getStatus(), complaint.getStatus(),
                actor.getId(), remarks);
    }

    void notifyReporter(Connection connection, Complaint complaint, String title, String message) throws SQLException {
        notificationDAO.insert(connection, complaint.getUserId(), complaint.getId(), title, message);
    }

    void notifyUser(Connection connection, int userId, Complaint complaint, String title, String message)
            throws SQLException {
        notificationDAO.insert(connection, userId, complaint.getId(), title, message);
    }

    /** A friendly default text when the person changing the status wrote no remark. */
    private static String reporterMessage(String status) {
        return switch (status) {
            case Complaint.STATUS_UNDER_REVIEW -> "The complaint cell is reviewing your complaint.";
            case Complaint.STATUS_ASSIGNED -> "Your complaint has been assigned to the maintenance team.";
            case Complaint.STATUS_IN_PROGRESS -> "Work on your complaint has started.";
            case Complaint.STATUS_RESOLVED -> "Your complaint has been resolved. Please rate the work.";
            case Complaint.STATUS_CLOSED -> "Your complaint has been closed.";
            default -> "The status of your complaint has changed.";
        };
    }
}
