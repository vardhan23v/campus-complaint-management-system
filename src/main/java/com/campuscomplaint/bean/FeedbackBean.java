package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.FeedbackService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of feedback.xhtml.
 *
 * Without a parameter the page lists resolved complaints waiting for a
 * rating plus the feedback already given; feedback.xhtml?id=7 opens the
 * rating form for that complaint.
 */
@Named("feedbackBean")
@ViewScoped
public class FeedbackBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FeedbackService feedbackService = new FeedbackService();

    private Integer id;
    private Complaint selected;
    private Integer rating;
    private String comments;

    private List<Complaint> awaiting = Collections.emptyList();
    private List<Feedback> given = Collections.emptyList();

    public void load() {
        User user = SessionUtil.getCurrentUser();
        try {
            awaiting = feedbackService.getAwaitingFeedback(user);
            given = feedbackService.getMyFeedback(user);
            if (id != null) {
                selected = awaiting.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
                if (selected == null) {
                    FacesUtil.addError("That complaint is not waiting for feedback. It may not be resolved yet, or you have already rated it.");
                }
            }
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public String submit() {
        try {
            rating = parseRating(FacesUtil.param("stars"));
            feedbackService.submit(selected == null ? 0 : selected.getId(), rating, comments,
                    SessionUtil.getCurrentUser());
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Thank you! Your feedback for " + selected.getComplaintCode() + " has been recorded.");
            return "/complaint-details.xhtml?faces-redirect=true&id=" + selected.getId();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;
        }
    }

    /** The star radios are plain HTML inputs; anything but 1-5 becomes null and is refused by the service. */
    private static Integer parseRating(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Complaint getSelected() {
        return selected;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<Complaint> getAwaiting() {
        return awaiting;
    }

    public List<Feedback> getGiven() {
        return given;
    }
}
