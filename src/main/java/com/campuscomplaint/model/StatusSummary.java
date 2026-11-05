package com.campuscomplaint.model;

import java.io.Serializable;

/**
 * Complaint counts per status, used by the three dashboards.
 */
public class StatusSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private int submitted;
    private int underReview;
    private int assigned;
    private int inProgress;
    private int resolved;
    private int closed;

    /** Adds the count of one status as returned by a GROUP BY query. */
    public void add(String status, int count) {
        switch (status) {
            case Complaint.STATUS_SUBMITTED -> submitted += count;
            case Complaint.STATUS_UNDER_REVIEW -> underReview += count;
            case Complaint.STATUS_ASSIGNED -> assigned += count;
            case Complaint.STATUS_IN_PROGRESS -> inProgress += count;
            case Complaint.STATUS_RESOLVED -> resolved += count;
            case Complaint.STATUS_CLOSED -> closed += count;
            default -> { }
        }
    }

    public int getTotal() {
        return submitted + underReview + assigned + inProgress + resolved + closed;
    }

    /** Waiting for someone to start work. */
    public int getPending() {
        return submitted + underReview + assigned;
    }

    /** Work finished: resolved or closed. */
    public int getDone() {
        return resolved + closed;
    }

    public int getOpen() {
        return getTotal() - getDone();
    }

    // ------------------------------------------------- getters and setters

    public int getSubmitted() {
        return submitted;
    }

    public void setSubmitted(int submitted) {
        this.submitted = submitted;
    }

    public int getUnderReview() {
        return underReview;
    }

    public void setUnderReview(int underReview) {
        this.underReview = underReview;
    }

    public int getAssigned() {
        return assigned;
    }

    public void setAssigned(int assigned) {
        this.assigned = assigned;
    }

    public int getInProgress() {
        return inProgress;
    }

    public void setInProgress(int inProgress) {
        this.inProgress = inProgress;
    }

    public int getResolved() {
        return resolved;
    }

    public void setResolved(int resolved) {
        this.resolved = resolved;
    }

    public int getClosed() {
        return closed;
    }

    public void setClosed(int closed) {
        this.closed = closed;
    }
}
