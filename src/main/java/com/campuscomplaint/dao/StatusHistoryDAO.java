package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * The append-only complaint_status_history table.
 *
 * There is deliberately no update or delete method: a history row, once
 * written, is never changed.
 */
public class StatusHistoryDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_HISTORY =
            "SELECT h.id, h.complaint_id, h.previous_status, h.new_status, h.updated_by, h.remarks, h.created_at, "
          + "u.name AS updated_by_name, u.role AS updated_by_role, c.complaint_code, c.title AS complaint_title "
          + "FROM complaint_status_history h "
          + "JOIN users u ON u.id = h.updated_by "
          + "JOIN complaints c ON c.id = h.complaint_id ";

    /** Appends one history row inside the caller's transaction. */
    public void insert(Connection connection, int complaintId, String previousStatus, String newStatus,
                       int updatedBy, String remarks) throws SQLException {
        String sql = "INSERT INTO complaint_status_history (complaint_id, previous_status, new_status, updated_by, remarks) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaintId);
            if (previousStatus == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, previousStatus);
            }
            statement.setString(3, newStatus);
            statement.setInt(4, updatedBy);
            DaoSupport.setNullableString(statement, 5, remarks);
            statement.executeUpdate();
        }
    }

    /** The full timeline of one complaint, oldest first. */
    public List<ComplaintStatusHistory> findByComplaint(int complaintId) {
        String sql = SELECT_HISTORY + "WHERE h.complaint_id = ? ORDER BY h.created_at, h.id";
        List<ComplaintStatusHistory> history = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, complaintId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the status history.", e);
        }
        return history;
    }

    /** The latest activity across all complaints - the administrator's feed. */
    public List<ComplaintStatusHistory> findRecent(int limit) {
        String sql = SELECT_HISTORY + "ORDER BY h.created_at DESC, h.id DESC LIMIT ?";
        List<ComplaintStatusHistory> history = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the recent activity.", e);
        }
        return history;
    }

    private ComplaintStatusHistory mapRow(ResultSet resultSet) throws SQLException {
        ComplaintStatusHistory entry = new ComplaintStatusHistory();
        entry.setId(resultSet.getInt("id"));
        entry.setComplaintId(resultSet.getInt("complaint_id"));
        entry.setPreviousStatus(resultSet.getString("previous_status"));
        entry.setNewStatus(resultSet.getString("new_status"));
        entry.setUpdatedBy(resultSet.getInt("updated_by"));
        entry.setUpdatedByName(resultSet.getString("updated_by_name"));
        entry.setUpdatedByRole(resultSet.getString("updated_by_role"));
        entry.setRemarks(resultSet.getString("remarks"));
        entry.setCreatedAt(DaoSupport.dateTime(resultSet, "created_at"));
        entry.setComplaintCode(resultSet.getString("complaint_code"));
        entry.setComplaintTitle(resultSet.getString("complaint_title"));
        return entry;
    }
}
