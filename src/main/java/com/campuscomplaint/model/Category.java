package com.campuscomplaint.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A complaint category such as "Electrical Issues". Managed by the administrator;
 * inactive categories stay attached to old complaints but are hidden from the form.
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private int complaintCount;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getComplaintCount() {
        return complaintCount;
    }

    public void setComplaintCount(int complaintCount) {
        this.complaintCount = complaintCount;
    }
}
