package com.campuscomplaint.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row of the append-only status history of a complaint.
 * previousStatus equals newStatus when only a remark was added.
 */
public class ComplaintStatusHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int complaintId;
    private String previousStatus;
    private String newStatus;
    private int updatedBy;
    private String updatedByName;
    private String updatedByRole;
    private String remarks;
    private LocalDateTime createdAt;
    private String complaintCode;
    private String complaintTitle;

    /** false for a remark-only entry. */
    public boolean isStatusChange() {
        return previousStatus == null || !previousStatus.equals(newStatus);
    }

    public String getPreviousStatusLabel() {
        return Complaint.labelOf(previousStatus);
    }

    public String getNewStatusLabel() {
        return Complaint.labelOf(newStatus);
    }

    public String getNewStatusCss() {
        return Complaint.cssOf(newStatus);
    }

    public String getUpdatedByRoleLabel() {
        if (User.ROLE_ADMIN.equals(updatedByRole)) {
            return "Administrator";
        }
        if (User.ROLE_STAFF.equals(updatedByRole)) {
            return "Maintenance";
        }
        return "Reporter";
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(int complaintId) {
        this.complaintId = complaintId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public String getUpdatedByRole() {
        return updatedByRole;
    }

    public void setUpdatedByRole(String updatedByRole) {
        this.updatedByRole = updatedByRole;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getComplaintCode() {
        return complaintCode;
    }

    public void setComplaintCode(String complaintCode) {
        this.complaintCode = complaintCode;
    }

    public String getComplaintTitle() {
        return complaintTitle;
    }

    public void setComplaintTitle(String complaintTitle) {
        this.complaintTitle = complaintTitle;
    }
}
