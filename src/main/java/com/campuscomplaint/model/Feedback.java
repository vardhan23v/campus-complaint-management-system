package com.campuscomplaint.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The reporter's rating (1-5) and comment on a resolved complaint.
 */
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int complaintId;
    private String complaintCode;
    private String complaintTitle;
    private int studentId;
    private String studentName;
    private String staffName;
    private String categoryName;
    private int rating;
    private String comments;
    private LocalDateTime createdAt;

    /** The rating as filled and empty stars, e.g. "\u2605\u2605\u2605\u2605\u2606". */
    public String getStars() {
        int filled = Math.max(0, Math.min(5, rating));
        return "\u2605".repeat(filled) + "\u2606".repeat(5 - filled);
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(int complaintId) {
        this.complaintId = complaintId;
    }

    public String getComplaintCode() {
        return complaintCode;
    }

    public void setComplaintCode(String complaintCode) {
        this.complaintCode = complaintCode;
    }

    public String getComplaintTitle() {
        return complaintTitle;
    }

    public void setComplaintTitle(String complaintTitle) {
        this.complaintTitle = complaintTitle;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
