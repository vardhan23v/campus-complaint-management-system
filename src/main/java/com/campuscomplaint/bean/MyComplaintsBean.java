package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Category;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.service.ComplaintService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of my-complaints.xhtml: the reporter's complaints with
 * search (ID or title) and status / category filters.
 *
 * @ViewScoped keeps the filter values while the user refines the search.
 */
@Named("myComplaintsBean")
@ViewScoped
public class MyComplaintsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComplaintService complaintService = new ComplaintService();

    private final ComplaintFilter filter = new ComplaintFilter();
    private List<Complaint> complaints = Collections.emptyList();
    private List<Category> categories = Collections.emptyList();

    /** Code of a just-submitted complaint, highlighted in the table. */
    private String highlight;

    @PostConstruct
    public void init() {
        String status = FacesUtil.param("status");
        if (status != null && (Complaint.STATUSES.contains(status)
                || ComplaintFilter.STATUS_OPEN.equals(status) || ComplaintFilter.STATUS_DONE.equals(status))) {
            filter.setStatus(status);
        }
        highlight = FacesUtil.param("highlight");
        try {
            categories = complaintService.getActiveCategories();
        } catch (DataAccessException e) {
            FacesUtil.addError(e);
        }
        search();
    }

    public void search() {
        try {
            complaints = complaintService.searchMine(filter, SessionUtil.getCurrentUser());
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            complaints = Collections.emptyList();
        }
    }

    public void clear() {
        filter.clear();
        search();
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

    public String getHighlight() {
        return highlight;
    }
}
