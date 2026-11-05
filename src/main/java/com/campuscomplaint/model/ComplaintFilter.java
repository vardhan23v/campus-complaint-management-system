package com.campuscomplaint.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Search and filter values for complaint lists.
 *
 * The page fills keyword/status/category/priority/staff/dates. The service
 * then sets reporterId or staffId itself from the logged-in user, so a
 * student or staff member can never widen the search to other people's
 * complaints by tampering with the form.
 */
public class ComplaintFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Pseudo status: everything not yet resolved or closed. */
    public static final String STATUS_OPEN = "OPEN";

    /** Pseudo status: resolved or closed. */
    public static final String STATUS_DONE = "DONE";

    private String keyword;
    private String status;
    private Integer categoryId;
    private String priority;
    private Integer assignedStaffId;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer reporterId;
    private Integer staffScopeId;
    private boolean unassignedOnly;

    /** Resets every user-entered value. The scope ids are left alone. */
    public void clear() {
        keyword = null;
        status = null;
        categoryId = null;
        priority = null;
        assignedStaffId = null;
        dateFrom = null;
        dateTo = null;
        unassignedOnly = false;
    }

    /** true when at least one user-entered filter is set. */
    public boolean isActive() {
        return (keyword != null && !keyword.isBlank()) || (status != null && !status.isEmpty())
                || categoryId != null || (priority != null && !priority.isEmpty())
                || assignedStaffId != null || dateFrom != null || dateTo != null || unassignedOnly;
    }

    // ------------------------------------------------- getters and setters

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(Integer assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public Integer getReporterId() {
        return reporterId;
    }

    public void setReporterId(Integer reporterId) {
        this.reporterId = reporterId;
    }

    public Integer getStaffScopeId() {
        return staffScopeId;
    }

    public void setStaffScopeId(Integer staffScopeId) {
        this.staffScopeId = staffScopeId;
    }

    public boolean isUnassignedOnly() {
        return unassignedOnly;
    }

    public void setUnassignedOnly(boolean unassignedOnly) {
        this.unassignedOnly = unassignedOnly;
    }
}
