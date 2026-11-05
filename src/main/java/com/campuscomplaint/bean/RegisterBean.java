package com.campuscomplaint.bean;

import com.campuscomplaint.service.AuthService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of register.xhtml. Creates student / faculty accounts only;
 * the role is fixed in AuthService, not chosen on the page.
 */
@Named("registerBean")
@RequestScoped
public class RegisterBean {

    private final AuthService authService = new AuthService();

    private String name;
    private String email;
    private String phone;
    private String department;
    private String password;
    private String confirmPassword;

    public String register() {
        try {
            authService.register(name, email, phone, department, password, confirmPassword);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Your account is ready. Please log in.");
            return "/login.xhtml?faces-redirect=true";
        } catch (ValidationException | DataAccessException e) {
            FacesUtil.addError(e);
            return null;
        } finally {
            password = null;
            confirmPassword = null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
