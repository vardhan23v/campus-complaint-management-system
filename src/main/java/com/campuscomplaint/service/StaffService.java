package com.campuscomplaint.service;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.campuscomplaint.dao.ComplaintDAO;
import com.campuscomplaint.dao.FeedbackDAO;
import com.campuscomplaint.dao.StatusHistoryDAO;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.InputValidator;
import com.campuscomplaint.util.TransactionManager;
import com.campuscomplaint.util.ValidationException;

/**
 * The maintenance staff workflow: see assigned complaints, start work, post
 * progress updates and mark the complaint resolved.
 *
 * Assignment rule: a staff member may only read or change a complaint whose
 * assigned_staff_id is their own id - checked again inside the transaction,
 * so a complaint reassigned a second ago can no longer be touched.
 */
public class StaffService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final StatusHistoryDAO historyDAO = new StatusHistoryDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final ComplaintWorkflow workflow = new ComplaintWorkflow();

    // ---------------------------------------------------------------- reads

    public StatusSummary getSummary(User staff) {
        Access.requireStaff(staff);
        return complaintDAO.countByStatus(null, staff.getId());
    }

    /** Assigned complaints matching the filter, most urgent first. */
    public List<Complaint> search(ComplaintFilter filter, User staff) {
        Access.requireStaff(staff);
        filter.setStaffScopeId(staff.getId());   // scope forced from the session
        filter.setReporterId(null);
        filter.setAssignedStaffId(null);
        return complaintDAO.search(filter, 0, true);
    }

    /** Open tasks (assigned or in progress) for the dashboard cards. */
    public List<Complaint> getOpenTasks(User staff, int limit) {
        Access.requireStaff(staff);
        ComplaintFilter filter = new ComplaintFilter();
        filter.setStaffScopeId(staff.getId());
        filter.setStatus(ComplaintFilter.STATUS_OPEN);
        return complaintDAO.search(filter, limit, true);
    }

    public Complaint getAssigned(int complaintId, User staff) {
        Access.requireStaff(staff);
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null || !Access.canView(staff, complaint)) {
            throw new ValidationException("That complaint is not assigned to you.");
        }
        return complaint;
    }

    public List<ComplaintStatusHistory> getHistory(int complaintId, User staff) {
        getAssigned(complaintId, staff);
        return historyDAO.findByComplaint(complaintId);
    }

    public Feedback getFeedback(int complaintId, User staff) {
        getAssigned(complaintId, staff);
        return feedbackDAO.findByComplaint(complaintId);
    }

    // -------------------------------------------------------------- writes

    /** ASSIGNED -> IN_PROGRESS. */
    public void startWork(int complaintId, String remarks, User staff) {
        Access.requireStaff(staff);
        String note = InputValidator.optionalText(remarks, "Work update", 1000);
        move(complaintId, Complaint.STATUS_IN_PROGRESS, staff,
                note == null ? "Work started by " + staff.getName() + "." : note);
    }

    /** IN_PROGRESS -> RESOLVED. A short description of the fix is required. */
    public void resolve(int complaintId, String remarks, User staff) {
        Access.requireStaff(staff);
        String note = InputValidator.requireText(remarks, "A short description of the work done", 5, 1000);
        move(complaintId, Complaint.STATUS_RESOLVED, staff, note);
    }

    /** A progress note without a status change. The reporter is notified. */
    public void addUpdate(int complaintId, String remarks, User staff) {
        Access.requireStaff(staff);
        String note = InputValidator.requireText(remarks, "The work update", 3, 1000);
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = lockAssigned(connection, complaintId, staff);
            if (!complaint.isOpen()) {
                throw new ValidationException("Updates can only be added while the work is open.");
            }
            workflow.remark(connection, complaint, staff, note);
            workflow.notifyReporter(connection, complaint, "Work update on " + complaint.getComplaintCode(), note);
            return null;
        });
    }

    private void move(int complaintId, String target, User staff, String remarks) {
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = lockAssigned(connection, complaintId, staff);
            if (!ComplaintWorkflow.staffMayMove(complaint.getStatus(), target)) {
                throw new ValidationException("A " + complaint.getStatusLabel().toLowerCase()
                        + " complaint cannot be marked " + Complaint.labelOf(target).toLowerCase() + ".");
            }
            workflow.transition(connection, complaint, target, staff, remarks);
            return null;
        });
    }

    private Complaint lockAssigned(Connection connection, int complaintId, User staff)
            throws SQLException {
        Complaint complaint = workflow.lock(connection, complaintId);
        if (!Access.canView(staff, complaint)) {
            throw new ValidationException("That complaint is not assigned to you.");
        }
        return complaint;
    }
}
