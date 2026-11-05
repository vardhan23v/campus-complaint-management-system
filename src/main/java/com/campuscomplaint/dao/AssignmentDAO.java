package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.ComplaintAssignment;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * The complaint_assignments table: who was given which complaint, by whom.
 */
public class AssignmentDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Records an assignment inside the caller's transaction. */
    public void insert(Connection connection, int complaintId, int staffId, int assignedBy, String remarks)
            throws SQLException {
        String sql = "INSERT INTO complaint_assignments (complaint_id, staff_id, assigned_by, remarks) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaintId);
            statement.setInt(2, staffId);
            statement.setInt(3, assignedBy);
            DaoSupport.setNullableString(statement, 4, remarks);
            statement.executeUpdate();
        }
    }

    /** Every assignment of one complaint, newest first. */
    public List<ComplaintAssignment> findByComplaint(int complaintId) {
        String sql = "SELECT a.id, a.complaint_id, a.staff_id, a.assigned_by, a.assigned_at, a.remarks, "
                   + "s.name AS staff_name, b.name AS assigned_by_name "
                   + "FROM complaint_assignments a "
                   + "JOIN users s ON s.id = a.staff_id "
                   + "JOIN users b ON b.id = a.assigned_by "
                   + "WHERE a.complaint_id = ? ORDER BY a.assigned_at DESC, a.id DESC";
        List<ComplaintAssignment> assignments = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, complaintId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ComplaintAssignment assignment = new ComplaintAssignment();
                    assignment.setId(resultSet.getInt("id"));
                    assignment.setComplaintId(resultSet.getInt("complaint_id"));
                    assignment.setStaffId(resultSet.getInt("staff_id"));
                    assignment.setStaffName(resultSet.getString("staff_name"));
                    assignment.setAssignedBy(resultSet.getInt("assigned_by"));
                    assignment.setAssignedByName(resultSet.getString("assigned_by_name"));
                    assignment.setAssignedAt(DaoSupport.dateTime(resultSet, "assigned_at"));
                    assignment.setRemarks(resultSet.getString("remarks"));
                    assignments.add(assignment);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the assignment history.", e);
        }
        return assignments;
    }
}
