package com.campuscomplaint.bean;

import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.StaffStat;
import com.campuscomplaint.model.StatCount;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.AdminService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of reports.xhtml. All numbers come from MySQL aggregation
 * queries in ReportDAO; the page can be printed / saved as PDF, and the
 * complaint register can be downloaded as CSV from admin-complaints.xhtml.
 */
@Named("reportBean")
@RequestScoped
public class ReportBean {

    private static final int MONTHS = 6;

    private final AdminService adminService = new AdminService();

    private StatusSummary summary = new StatusSummary();
    private List<StatCount> categoryStats = Collections.emptyList();
    private List<StatCount> priorityStats = Collections.emptyList();
    private List<StatCount> monthlyStats = Collections.emptyList();
    private List<StaffStat> staffStats = Collections.emptyList();
    private List<Feedback> feedback = Collections.emptyList();
    private Double averageRating;
    private Double averageResolutionHours;

    @PostConstruct
    public void init() {
        User admin = SessionUtil.getCurrentUser();
        try {
            summary = adminService.getSummary(admin);
            categoryStats = adminService.getCategoryStats(admin);
            priorityStats = adminService.getPriorityStats(admin);
            monthlyStats = adminService.getMonthlyStats(MONTHS, admin);
            staffStats = adminService.getStaffStats(admin);
            feedback = adminService.getAllFeedback(admin);
            averageRating = adminService.getAverageRating(admin);
            averageResolutionHours = adminService.getAverageResolutionHours(admin);
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    /** Share of complaints that are resolved or closed, 0-100. */
    public int getResolutionRate() {
        return summary.getTotal() == 0 ? 0 : (int) Math.round(summary.getDone() * 100.0 / summary.getTotal());
    }

    /** Tallest monthly bar, so it fills the chart height. */
    public int getMaxMonthly() {
        return Math.max(1, monthlyStats.stream().mapToInt(StatCount::getCount).max().orElse(1));
    }

    public int getMaxCategory() {
        return Math.max(1, categoryStats.stream().mapToInt(StatCount::getCount).max().orElse(1));
    }

    /** "3.5 days" or "14 hours" for the resolution time tile. */
    public String getAverageResolutionLabel() {
        if (averageResolutionHours == null) {
            return "–";
        }
        double hours = averageResolutionHours;
        if (hours >= 48) {
            return String.format(java.util.Locale.ROOT, "%.1f days", hours / 24);
        }
        return Math.round(hours) + " hours";
    }

    public String getAverageRatingLabel() {
        return averageRating == null ? "–" : String.format(java.util.Locale.ROOT, "%.1f / 5", averageRating);
    }

    public StatusSummary getSummary() {
        return summary;
    }

    public List<StatCount> getCategoryStats() {
        return categoryStats;
    }

    public List<StatCount> getPriorityStats() {
        return priorityStats;
    }

    public List<StatCount> getMonthlyStats() {
        return monthlyStats;
    }

    public List<StaffStat> getStaffStats() {
        return staffStats;
    }

    public List<Feedback> getFeedback() {
        return feedback;
    }
}
