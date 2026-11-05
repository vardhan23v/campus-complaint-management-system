package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.StaffService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of staff-complaint-details.xhtml: one assigned complaint
 * with the actions Start work, Post update and Mark resolved.
 */
@Named("staffComplaintBean")
@ViewScoped
public class StaffComplaintBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StaffService staffService = new StaffService();

    private Integer id;
    private Complaint complaint;
    private List<ComplaintStatusHistory> history = Collections.emptyList();
    private Feedback feedback;

    private String startNote;
    private String update;
    private String resolution;

    public void load() {
        if (id == null) {
            FacesUtil.addError("No complaint was selected.");
            return;
        }
        User staff = SessionUtil.getCurrentUser();
        try {
            complaint = staffService.getAssigned(id, staff);
            history = staffService.getHistory(id, staff);
            feedback = staffService.getFeedback(id, staff);
        } catch (ValidationException | DataAccessException e) {
            complaint = null;
            FacesUtil.addError(e);
        }
    }

    public String startWork() {
        return run(() -> staffService.startWork(id, startNote, SessionUtil.getCurrentUser()),
                "Work started. The reporter has been notified.");
    }

    public String postUpdate() {
        return run(() -> staffService.addUpdate(id, update, SessionUtil.getCurrentUser()),
                "Your update has been posted.");
    }

    public String resolve() {
        return run(() -> staffService.resolve(id, resolution, SessionUtil.getCurrentUser()),
                "Marked as resolved. Nice work!");
    }

    private String run(Runnable action, String successMessage) {
        try {
            action.run();
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo(successMessage);
            return "/staff-complaint-details.xhtml?faces-redirect=true&id=" + id;
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;
        }
    }

    public boolean isCanStart() {
        return complaint != null && Complaint.STATUS_ASSIGNED.equals(complaint.getStatus());
    }

    public boolean isCanResolve() {
        return complaint != null && Complaint.STATUS_IN_PROGRESS.equals(complaint.getStatus());
    }

    public boolean isCanUpdate() {
        return complaint != null && complaint.isOpen();
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

    public Feedback getFeedback() {
        return feedback;
    }

    public String getStartNote() {
        return startNote;
    }

    public void setStartNote(String startNote) {
        this.startNote = startNote;
    }

    public String getUpdate() {
        return update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}
