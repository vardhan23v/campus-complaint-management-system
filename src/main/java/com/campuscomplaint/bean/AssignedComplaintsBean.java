package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.service.StaffService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of assigned-complaints.xhtml: the staff member's complaints
 * filtered by status and priority. ?status=DONE shows completed work.
 */
@Named("assignedComplaintsBean")
@ViewScoped
public class AssignedComplaintsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StaffService staffService = new StaffService();

    private final ComplaintFilter filter = new ComplaintFilter();
    private List<Complaint> complaints = Collections.emptyList();

    @PostConstruct
    public void init() {
        String status = FacesUtil.param("status");
        if (status != null && (Complaint.STATUSES.contains(status)
                || ComplaintFilter.STATUS_OPEN.equals(status) || ComplaintFilter.STATUS_DONE.equals(status))) {
            filter.setStatus(status);
        }
        search();
    }

    public void search() {
        try {
            complaints = staffService.search(filter, SessionUtil.getCurrentUser());
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            complaints = Collections.emptyList();
        }
    }

    public void clear() {
        filter.clear();
        search();
    }

    /** Quick tabs above the table. */
    public void showOpen() {
        filter.clear();
        filter.setStatus(ComplaintFilter.STATUS_OPEN);
        search();
    }

    public void showDone() {
        filter.clear();
        filter.setStatus(ComplaintFilter.STATUS_DONE);
        search();
    }

    public void showAll() {
        clear();
    }

    public ComplaintFilter getFilter() {
        return filter;
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }
}
