package com.campuscomplaint.bean;

import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.StaffService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of staff-dashboard.xhtml: task counters and the open work
 * orders of the logged-in maintenance staff member, most urgent first.
 */
@Named("staffDashboardBean")
@RequestScoped
public class StaffDashboardBean {

    private final StaffService staffService = new StaffService();

    private StatusSummary summary = new StatusSummary();
    private List<Complaint> openTasks = Collections.emptyList();

    @PostConstruct
    public void init() {
        User staff = SessionUtil.getCurrentUser();
        try {
            summary = staffService.getSummary(staff);
            openTasks = staffService.getOpenTasks(staff, 0);
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public StatusSummary getSummary() {
        return summary;
    }

    /** Up to six cards for the dashboard. */
    public List<Complaint> getTaskCards() {
        return openTasks.size() > 6 ? openTasks.subList(0, 6) : openTasks;
    }

    public List<Complaint> getOpenTasks() {
        return openTasks;
    }
}
