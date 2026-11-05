package com.campuscomplaint.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One campus complaint, plus the names joined in from users and categories
 * so the pages do not need extra queries.
 *
 * Lifecycle:  SUBMITTED -> UNDER_REVIEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED
 * The rules for who may move a complaint between statuses live in ComplaintWorkflow.
 */
public class Complaint implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";

    /** All statuses in lifecycle order. */
    public static final List<String> STATUSES = List.of(STATUS_SUBMITTED, STATUS_UNDER_REVIEW,
            STATUS_ASSIGNED, STATUS_IN_PROGRESS, STATUS_RESOLVED, STATUS_CLOSED);

    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    /** All priorities, lowest first. URGENT can only be set by an administrator. */
    public static final List<String> PRIORITIES = List.of(PRIORITY_LOW, PRIORITY_MEDIUM, PRIORITY_HIGH, PRIORITY_URGENT);

    /** The priorities a reporter may choose on the submission form. */
    public static final List<String> REPORTER_PRIORITIES = List.of(PRIORITY_LOW, PRIORITY_MEDIUM, PRIORITY_HIGH);

    private int id;
    private String complaintCode;
    private int userId;
    private String userName;
    private String userEmail;
    private int categoryId;
    private String categoryName;
    private String title;
    private String description;
    private String block;
    private String floor;
    private String location;
    private String priority;
    private String status;
    private String attachmentPath;
    private Integer assignedStaffId;
    private String assignedStaffName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private boolean feedbackGiven;

    /** "IN_PROGRESS" -> "In progress". */
    public static String labelOf(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String text = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** "IN_PROGRESS" -> "in-progress", used as a CSS modifier class. */
    public static String cssOf(String value) {
        return value == null ? "" : value.replace('_', '-').toLowerCase();
    }

    public String getStatusLabel() {
        return labelOf(status);
    }

    public String getStatusCss() {
        return cssOf(status);
    }

    public String getPriorityLabel() {
        return labelOf(priority);
    }

    public String getPriorityCss() {
        return cssOf(priority);
    }

    /** Position of the current status in the lifecycle, 0 to 5. */
    public int getStatusStep() {
        return STATUSES.indexOf(status);
    }

    public boolean isHasAttachment() {
        return attachmentPath != null && !attachmentPath.isBlank();
    }

    /** "Block A · 2nd floor · Room 204" */
    public String getFullLocation() {
        return block + " \u00b7 " + floor + " \u00b7 " + location;
    }

    /** Still waiting for work: not yet resolved or closed. */
    public boolean isOpen() {
        return !STATUS_RESOLVED.equals(status) && !STATUS_CLOSED.equals(status);
    }

    /** The reporter may leave feedback once the work is done. */
    public boolean isFeedbackAllowed() {
        return (STATUS_RESOLVED.equals(status) || STATUS_CLOSED.equals(status)) && !feedbackGiven;
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComplaintCode() {
        return complaintCode;
    }

    public void setComplaintCode(String complaintCode) {
        this.complaintCode = complaintCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public Integer getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(Integer assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public String getAssignedStaffName() {
        return assignedStaffName;
    }

    public void setAssignedStaffName(String assignedStaffName) {
        this.assignedStaffName = assignedStaffName;
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

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isFeedbackGiven() {
        return feedbackGiven;
    }

    public void setFeedbackGiven(boolean feedbackGiven) {
        this.feedbackGiven = feedbackGiven;
    }
}
