package com.campuscomplaint.bean;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Category;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.AdminService;
import com.campuscomplaint.service.ComplaintService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of admin-complaints.xhtml: every complaint with search
 * (ID, title, reporter) and filters for status, category, priority,
 * assigned staff and submission date, plus a CSV export of the result.
 */
@Named("adminComplaintBean")
@ViewScoped
public class AdminComplaintBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();
    private final ComplaintService complaintService = new ComplaintService();

    private final ComplaintFilter filter = new ComplaintFilter();
    private List<Complaint> complaints = Collections.emptyList();
    private List<Category> categories = Collections.emptyList();
    private List<User> staff = Collections.emptyList();

    @PostConstruct
    public void init() {
        // The dashboard links here with ?status=... or ?queue=unassigned.
        String status = FacesUtil.param("status");
        if (status != null && (Complaint.STATUSES.contains(status) || ComplaintFilter.STATUS_OPEN.equals(status))) {
            filter.setStatus(status);
        }
        filter.setUnassignedOnly("unassigned".equals(FacesUtil.param("queue")));
        User admin = SessionUtil.getCurrentUser();
        try {
            categories = complaintService.getActiveCategories();
            staff = adminService.getActiveStaff(admin);
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
        search();
    }

    public void search() {
        if (filter.getDateFrom() != null && filter.getDateTo() != null && filter.getDateTo().isBefore(filter.getDateFrom())) {
            FacesUtil.addError("The 'to' date must not be before the 'from' date.");
            return;
        }
        if (filter.getDateFrom() != null && filter.getDateFrom().isAfter(LocalDate.now())) {
            FacesUtil.addError("The 'from' date cannot be in the future.");
            return;
        }
        try {
            complaints = adminService.search(filter, SessionUtil.getCurrentUser());
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            complaints = Collections.emptyList();
        }
    }

    public void clear() {
        filter.clear();
        search();
    }

    /** Sends the filtered list to the browser as a CSV download. */
    public void exportCsv() {
        try {
            String csv = adminService.exportCsv(filter, SessionUtil.getCurrentUser());
            FacesContext context = FacesContext.getCurrentInstance();
            ExternalContext external = context.getExternalContext();
            external.responseReset();
            external.setResponseContentType("text/csv; charset=UTF-8");
            external.setResponseHeader("Content-Disposition",
                    "attachment; filename=\"complaints-" + LocalDate.now() + ".csv\"");
            try (OutputStream output = external.getResponseOutputStream()) {
                output.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});   // UTF-8 BOM for Excel
                output.write(csv.getBytes(StandardCharsets.UTF_8));
            }
            context.responseComplete();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        } catch (IOException e) {
            FacesUtil.addError("The export could not be created.");
        }
    }

    public ComplaintFilter getFilter() {
        return filter;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<User> getStaff() {
        return staff;
    }
}
