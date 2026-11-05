package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.Category;
import com.campuscomplaint.service.AdminService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of manage-categories.xhtml.
 */
@Named("categoryBean")
@ViewScoped
public class CategoryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

    private List<Category> categories = Collections.emptyList();
    private Category form = new Category();

    @PostConstruct
    public void init() {
        refresh();
    }

    private void refresh() {
        try {
            categories = adminService.getCategories(SessionUtil.getCurrentUser());
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public void save() {
        try {
            boolean isNew = form.getId() == 0;
            adminService.saveCategory(form, SessionUtil.getCurrentUser());
            FacesUtil.addInfo("Category \"" + form.getName().trim() + "\" " + (isNew ? "added." : "updated."));
            form = new Category();
            refresh();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public void edit(Category category) {
        form = new Category();
        form.setId(category.getId());
        form.setName(category.getName());
        form.setDescription(category.getDescription());
    }

    public void cancel() {
        form = new Category();
    }

    public void toggleActive(Category category) {
        try {
            adminService.setCategoryActive(category.getId(), !category.isActive(), SessionUtil.getCurrentUser());
            FacesUtil.addInfo("\"" + category.getName() + "\" is now " + (category.isActive() ? "hidden from" : "available on")
                    + " the complaint form.");
            refresh();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public void delete(Category category) {
        try {
            adminService.deleteCategory(category.getId(), SessionUtil.getCurrentUser());
            FacesUtil.addInfo("Category \"" + category.getName() + "\" deleted.");
            if (form.getId() == category.getId()) {
                form = new Category();
            }
            refresh();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Category getForm() {
        return form;
    }

    public boolean isEditing() {
        return form.getId() != 0;
    }
}
