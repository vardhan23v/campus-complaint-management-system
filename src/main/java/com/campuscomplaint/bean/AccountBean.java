package com.campuscomplaint.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.campuscomplaint.model.User;
import com.campuscomplaint.service.AdminService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

/**
 * Backing bean of manage-users.xhtml (students / faculty) and
 * manage-staff.xhtml (maintenance staff). The page tells the bean which role
 * it manages through <f:viewAction action="#{accountBean.init('STAFF')}"/>;
 * AdminService checks the role again, so only these two roles can ever be
 * touched and never an administrator.
 */
@Named("accountBean")
@ViewScoped
public class AccountBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

    private String role;
    private String keyword;
    private List<User> accounts = Collections.emptyList();

    /** The add / edit form; null while the form is closed. */
    private User form;
    private String password;

    /** The account whose password is being reset; null when that panel is closed. */
    private User resetTarget;
    private String newPassword;

    public void init(String managedRole) {
        this.role = managedRole;
        search();
    }

    public void search() {
        try {
            accounts = adminService.getAccounts(role, keyword, SessionUtil.getCurrentUser());
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public void newAccount() {
        form = new User();
        password = null;
        resetTarget = null;
    }

    public void edit(User account) {
        form = new User();
        form.setId(account.getId());
        form.setName(account.getName());
        form.setEmail(account.getEmail());
        form.setPhone(account.getPhone());
        form.setDepartment(account.getDepartment());
        resetTarget = null;
    }

    public void cancel() {
        form = null;
        password = null;
        resetTarget = null;
        newPassword = null;
    }

    public void save() {
        User admin = SessionUtil.getCurrentUser();
        try {
            if (form.getId() == 0) {
                adminService.createAccount(form, password, role, admin);
                FacesUtil.addInfo("Account for " + form.getName().trim() + " created.");
            } else {
                adminService.updateAccount(form, role, admin);
                FacesUtil.addInfo("Account for " + form.getName().trim() + " updated.");
            }
            cancel();
            search();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        } finally {
            password = null;
        }
    }

    public void toggleActive(User account) {
        try {
            adminService.setAccountActive(account.getId(), role, !account.isActive(), SessionUtil.getCurrentUser());
            FacesUtil.addInfo(account.getName() + (account.isActive() ? " has been deactivated." : " has been activated."));
            search();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public void startReset(User account) {
        form = null;
        resetTarget = account;
        newPassword = null;
    }

    public void resetPassword() {
        try {
            adminService.resetPassword(resetTarget.getId(), role, newPassword, SessionUtil.getCurrentUser());
            FacesUtil.addInfo("The password of " + resetTarget.getName() + " has been reset.");
            cancel();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        } finally {
            newPassword = null;
        }
    }

    public void delete(User account) {
        try {
            adminService.deleteAccount(account.getId(), role, SessionUtil.getCurrentUser());
            FacesUtil.addInfo("The account of " + account.getName() + " has been deleted.");
            search();
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
        }
    }

    public boolean isStaffPage() {
        return User.ROLE_STAFF.equals(role);
    }

    public long getActiveCount() {
        return accounts.stream().filter(User::isActive).count();
    }

    public String getRole() {
        return role;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<User> getAccounts() {
        return accounts;
    }

    public User getForm() {
        return form;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User getResetTarget() {
        return resetTarget;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
