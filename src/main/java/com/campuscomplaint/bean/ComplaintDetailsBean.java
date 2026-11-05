package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.ComplaintService;
import com.campuscomplaint.service.FeedbackService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of complaint-details.xhtml (the reporter's view).
 *
 * The page is opened as complaint-details.xhtml?id=7. <f:viewParam> stores
 * the id, <f:viewAction> calls load(). The service refuses a complaint that
 * belongs to somebody else, so changing the id in the URL shows nothing.
 */
@Named("complaintDetailsBean")
@ViewScoped
public class ComplaintDetailsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComplaintService complaintService = new ComplaintService();
    private final FeedbackService feedbackService = new FeedbackService();

    private Integer id;
    private Complaint complaint;
    private List<ComplaintStatusHistory> history = Collections.emptyList();
    private Feedback feedback;

    public void load() {
        if (id == null) {
            FacesUtil.addError("No complaint was selected.");
            return;
        }
        User user = SessionUtil.getCurrentUser();
        try {
            complaint = complaintService.getForReporter(id, user);
            history = complaintService.getHistory(id, user);
            feedback = feedbackService.getForComplaint(id, user);
        } catch (ValidationException | DataAccessException e) {
            complaint = null;
            FacesUtil.addError(e);
        }
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
}
