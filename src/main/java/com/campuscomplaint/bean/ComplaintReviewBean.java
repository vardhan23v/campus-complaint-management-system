package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintAssignment;
import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.AdminService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of complaint-review.xhtml: the administrator's full view of
 * one complaint with every action - review, assign, prioritise, remark,
 * close and reopen.
 *
 * Each action redirects back to the page (Post/Redirect/Get), so the page
 * always shows the stored state and a refresh never repeats the action.
 */
@Named("complaintReviewBean")
@ViewScoped
public class ComplaintReviewBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

    private Integer id;
    private Complaint complaint;
    private List<ComplaintStatusHistory> history = Collections.emptyList();
    private List<ComplaintAssignment> assignments = Collections.emptyList();
    private List<User> staff = Collections.emptyList();
    private Feedback feedback;

    // form fields
    private Integer staffId;
    private String assignNote;
    private String priority;
    private String remark;
    private String statusNote;

    public void load() {
        if (id == null) {
            FacesUtil.addError("No complaint was selected.");
            return;
        }
        User admin = SessionUtil.getCurrentUser();
        try {
            complaint = adminService.getComplaint(id, admin);
            history = adminService.getHistory(id, admin);
            assignments = adminService.getAssignments(id, admin);
            feedback = adminService.getFeedback(id, admin);
            staff = adminService.getActiveStaff(admin);
            priority = complaint.getPriority();
            staffId = complaint.getAssignedStaffId();
        } catch (ValidationException | DataAccessException e) {
            complaint = null;
            FacesUtil.addError(e);
        }
    }

    public String markUnderReview() {
        return run(() -> adminService.markUnderReview(id, statusNote, SessionUtil.getCurrentUser()),
                "The complaint is now under review.");
    }

    public String assign() {
        return run(() -> adminService.assign(id, staffId, assignNote, SessionUtil.getCurrentUser()),
                "The complaint has been assigned and the staff member notified.");
    }

    public String changePriority() {
        return run(() -> adminService.changePriority(id, priority, SessionUtil.getCurrentUser()),
                "The priority has been updated.");
    }

    public String addRemark() {
        return run(() -> adminService.addRemark(id, remark, SessionUtil.getCurrentUser()),
                "Your remark has been added and the reporter notified.");
    }

    public String close() {
        return run(() -> adminService.close(id, statusNote, SessionUtil.getCurrentUser()),
                "The complaint has been closed.");
    }

    public String reopen() {
        return run(() -> adminService.reopen(id, statusNote, SessionUtil.getCurrentUser()),
                "The complaint has been reopened and sent back to the staff member.");
    }

    /** Runs an action; on success redirects back with a message, on failure stays with the error. */
    private String run(Runnable action, String successMessage) {
        try {
            action.run();
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo(successMessage);
            return "/complaint-review.xhtml?faces-redirect=true&id=" + id;
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;
        }
    }

    public boolean isCanReview() {
        return complaint != null && Complaint.STATUS_SUBMITTED.equals(complaint.getStatus());
    }

    public boolean isCanAssign() {
        return complaint != null && complaint.isOpen();
    }

    public boolean isCanCloseOrReopen() {
        return complaint != null && Complaint.STATUS_RESOLVED.equals(complaint.getStatus());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public List<ComplaintStatusHistory> getHistory() {
        return history;
    }

    public List<ComplaintAssignment> getAssignments() {
        return assignments;
    }

    public List<User> getStaff() {
        return staff;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public String getAssignNote() {
        return assignNote;
    }

    public void setAssignNote(String assignNote) {
        this.assignNote = assignNote;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatusNote() {
        return statusNote;
    }

    public void setStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }
}
