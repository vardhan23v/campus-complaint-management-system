package com.campuscomplaint.model;

import java.io.Serializable;

/**
 * Workload and performance of one maintenance staff member (reports page).
 */
public class StaffStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private int staffId;
    private String name;
    private String department;
    private int totalAssigned;
    private int openCount;
    private int resolvedCount;
    private Double averageRating;
    private Double averageResolutionHours;

    /** Share of assigned complaints that are resolved or closed, 0-100. */
    public int getCompletionPercent() {
        return totalAssigned == 0 ? 0 : (int) Math.round(resolvedCount * 100.0 / totalAssigned);
    }

    // ------------------------------------------------- getters and setters

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getTotalAssigned() {
        return totalAssigned;
    }

    public void setTotalAssigned(int totalAssigned) {
        this.totalAssigned = totalAssigned;
    }

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }

    public int getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(int resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Double getAverageResolutionHours() {
        return averageResolutionHours;
    }

    public void setAverageResolutionHours(Double averageResolutionHours) {
        this.averageResolutionHours = averageResolutionHours;
    }
}
