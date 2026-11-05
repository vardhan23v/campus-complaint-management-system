package com.campuscomplaint.bean;

import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Category;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.service.ComplaintService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

/**
 * Backing bean of submit-complaint.xhtml.
 *
 * @RequestScoped: the form lives for one request. After a successful
 * submission the browser is redirected to the new complaint's detail page
 * (Post/Redirect/Get), so a refresh can never file the complaint twice.
 */
@Named("complaintBean")
@RequestScoped
public class ComplaintBean {

    private final ComplaintService complaintService = new ComplaintService();

    private final Complaint form = new Complaint();
    private Part image;
    private List<Category> categories;

    public ComplaintBean() {
        form.setPriority(Complaint.PRIORITY_MEDIUM);
    }

    /** Action method of the Submit button. */
    public String submit() {
        try {
            String code = complaintService.submit(form, image, SessionUtil.getCurrentUser());
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Complaint " + code + " has been submitted. Keep this ID to track it.");
            return "/my-complaints.xhtml?faces-redirect=true&highlight=" + code;
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;   // stay on the form, the typed values are kept
        }
    }

    /** Active categories, loaded once per request (also needed on postback to validate the choice). */
    public List<Category> getCategories() {
        if (categories == null) {
            try {
                categories = complaintService.getActiveCategories();
            } catch (DataAccessException e) {
                FacesUtil.addError(e);
                categories = Collections.emptyList();
            }
        }
        return categories;
    }

    public Complaint getForm() {
        return form;
    }

    public Part getImage() {
        return image;
    }

    public void setImage(Part image) {
        this.image = image;
    }
}
