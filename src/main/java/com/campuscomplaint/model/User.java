package com.campuscomplaint.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A person who can log in: a student/faculty member who reports complaints,
 * a maintenance staff member who fixes them, or an administrator.
 * Plain data holder - all behaviour lives in the service and bean classes.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_ADMIN = "ADMIN";

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private String phone;
    private String department;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int complaintCount;

    /** Human readable role, e.g. "Maintenance staff". */
    public String getRoleLabel() {
        if (ROLE_ADMIN.equals(role)) {
            return "Administrator";
        }
        if (ROLE_STAFF.equals(role)) {
            return "Maintenance staff";
        }
        return "Student / Faculty";
    }

    /** One or two initials for the avatar in the sidebar. */
    public String getInitials() {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String initials = parts[0].substring(0, 1);
        if (parts.length > 1) {
            initials += parts[parts.length - 1].substring(0, 1);
        }
        return initials.toUpperCase();
    }

    /** The first name, for the welcome line on the dashboards. */
    public String getFirstName() {
        return name == null ? "" : name.trim().split("\\s+")[0];
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getComplaintCount() {
        return complaintCount;
    }

    public void setComplaintCount(int complaintCount) {
        this.complaintCount = complaintCount;
    }
}
