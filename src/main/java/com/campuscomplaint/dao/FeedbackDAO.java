package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * The feedback table: one rating per resolved complaint.
 */
public class FeedbackDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_FEEDBACK =
            "SELECT f.id, f.complaint_id, f.student_id, f.rating, f.comments, f.created_at, "
          + "c.complaint_code, c.title AS complaint_title, u.name AS student_name, s.name AS staff_name, "
          + "cat.name AS category_name "
          + "FROM feedback f "
          + "JOIN complaints c ON c.id = f.complaint_id "
          + "JOIN users u ON u.id = f.student_id "
          + "JOIN categories cat ON cat.id = c.category_id "
          + "LEFT JOIN users s ON s.id = c.assigned_staff_id ";

    /** Inserts the feedback inside the caller's transaction. */
    public void insert(Connection connection, Feedback feedback) throws SQLException {
        String sql = "INSERT INTO feedback (complaint_id, student_id, rating, comments) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, feedback.getComplaintId());
            statement.setInt(2, feedback.getStudentId());
            statement.setInt(3, feedback.getRating());
            DaoSupport.setNullableString(statement, 4, feedback.getComments());
            statement.executeUpdate();
        }
    }

    /** @return the feedback of a complaint, or null when none was given */
    public Feedback findByComplaint(int complaintId) {
        String sql = SELECT_FEEDBACK + "WHERE f.complaint_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, complaintId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the feedback.", e);
        }
    }

    /** All feedback, newest first (administrator). */
    public List<Feedback> findAll() {
        return query(SELECT_FEEDBACK + "ORDER BY f.created_at DESC", null);
    }

    /** The feedback a reporter has given, newest first. */
    public List<Feedback> findByStudent(int studentId) {
        return query(SELECT_FEEDBACK + "WHERE f.student_id = ? ORDER BY f.created_at DESC", studentId);
    }

    private List<Feedback> query(String sql, Integer studentId) {
        List<Feedback> list = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the feedback.", e);
        }
        return list;
    }

    private Feedback mapRow(ResultSet resultSet) throws SQLException {
        Feedback feedback = new Feedback();
        feedback.setId(resultSet.getInt("id"));
        feedback.setComplaintId(resultSet.getInt("complaint_id"));
        feedback.setComplaintCode(resultSet.getString("complaint_code"));
        feedback.setComplaintTitle(resultSet.getString("complaint_title"));
        feedback.setStudentId(resultSet.getInt("student_id"));
        feedback.setStudentName(resultSet.getString("student_name"));
        feedback.setStaffName(resultSet.getString("staff_name"));
        feedback.setCategoryName(resultSet.getString("category_name"));
        feedback.setRating(resultSet.getInt("rating"));
        feedback.setComments(resultSet.getString("comments"));
        feedback.setCreatedAt(DaoSupport.dateTime(resultSet, "created_at"));
        return feedback;
    }
}
