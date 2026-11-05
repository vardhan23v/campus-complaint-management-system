package com.campuscomplaint.bean;

import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintStatusHistory;
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
 * Backing bean of admin-dashboard.xhtml: status counters, the status donut,
 * category and priority bars, recent submissions, the assignment queue and
 * the activity feed.
 */
@Named("adminDashboardBean")
@RequestScoped
public class AdminDashboardBean {

    private final AdminService adminService = new AdminService();

    private StatusSummary summary = new StatusSummary();
    private int submittedToday;
    private List<StatCount> categoryStats = Collections.emptyList();
    private List<StatCount> priorityStats = Collections.emptyList();
    private List<Complaint> recent = Collections.emptyList();
    private List<Complaint> awaitingAssignment = Collections.emptyList();
    private List<ComplaintStatusHistory> activity = Collections.emptyList();

    @PostConstruct
    public void init() {
        User admin = SessionUtil.getCurrentUser();
        try {
            summary = adminService.getSummary(admin);
            submittedToday = adminService.getSubmittedToday(admin);
            categoryStats = adminService.getCategoryStats(admin).stream().filter(s -> s.getCount() > 0).limit(8).toList();
            priorityStats = adminService.getPriorityStats(admin);
            recent = adminService.getRecent(6, admin);
            awaitingAssignment = adminService.getAwaitingAssignment(6, admin);
            activity = adminService.getRecentActivity(8, admin);
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    /**
     * CSS conic-gradient for the status donut, built from the counts, e.g.
     * "var(--st-submitted) 0% 20%, var(--st-under-review) 20% 35%, ...".
     */
    public String getStatusGradient() {
        int total = summary.getTotal();
        if (total == 0) {
            return "var(--muted) 0% 100%";
        }
        int[] counts = {summary.getSubmitted(), summary.getUnderReview(), summary.getAssigned(),
                        summary.getInProgress(), summary.getResolved(), summary.getClosed()};
        StringBuilder gradient = new StringBuilder();
        double start = 0;
        for (int i = 0; i < counts.length; i++) {
            double end = start + counts[i] * 100.0 / total;
            if (gradient.length() > 0) {
                gradient.append(", ");
            }
            gradient.append("var(--st-").append(Complaint.cssOf(Complaint.STATUSES.get(i))).append(") ")
                    .append(String.format(java.util.Locale.ROOT, "%.2f%% %.2f%%", start, end));
            start = end;
        }
        return gradient.toString();
    }

    /** Per-status rows for the donut legend. */
    public List<StatCount> getStatusRows() {
        int[] counts = {summary.getSubmitted(), summary.getUnderReview(), summary.getAssigned(),
                        summary.getInProgress(), summary.getResolved(), summary.getClosed()};
        int total = summary.getTotal();
        java.util.List<StatCount> rows = new java.util.ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            String status = Complaint.STATUSES.get(i);
            StatCount row = new StatCount(status, Complaint.labelOf(status), counts[i]);
            row.setPercent(total == 0 ? 0 : counts[i] * 100.0 / total);
            rows.add(row);
        }
        return rows;
    }

    /** Largest category count, so the longest bar fills the track. */
    public int getMaxCategory() {
        return categoryStats.stream().mapToInt(StatCount::getCount).max().orElse(1);
    }

    public StatusSummary getSummary() {
        return summary;
    }

    public int getSubmittedToday() {
        return submittedToday;
    }

    public List<StatCount> getCategoryStats() {
        return categoryStats;
    }

    public List<StatCount> getPriorityStats() {
        return priorityStats;
    }

    public List<Complaint> getRecent() {
        return recent;
    }

    public List<Complaint> getAwaitingAssignment() {
        return awaitingAssignment;
    }

    public List<ComplaintStatusHistory> getActivity() {
        return activity;
    }
}
