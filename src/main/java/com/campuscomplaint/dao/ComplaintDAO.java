package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * All SQL that touches the complaints table.
 *
 * Methods that take a Connection are part of a larger transaction started by
 * a service (see TransactionManager); they must not open or close their own.
 * Every value - including search keywords - goes through a ? placeholder.
 */
public class ComplaintDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The column list shared by all read queries. Reporter, category and
     * assigned staff names are joined in; feedback_given tells the pages
     * whether the reporter has already rated the work.
     */
    private static final String SELECT_COMPLAINTS =
            "SELECT c.id, c.complaint_code, c.user_id, c.category_id, c.title, c.description, c.block, c.floor, "
          + "c.location, c.priority, c.status, c.attachment_path, c.assigned_staff_id, c.created_at, c.updated_at, "
          + "c.resolved_at, c.closed_at, "
          + "u.name AS user_name, u.email AS user_email, cat.name AS category_name, s.name AS staff_name, "
          + "EXISTS (SELECT 1 FROM feedback f WHERE f.complaint_id = c.id) AS feedback_given "
          + "FROM complaints c "
          + "JOIN users u ON u.id = c.user_id "
          + "JOIN categories cat ON cat.id = c.category_id "
          + "LEFT JOIN users s ON s.id = c.assigned_staff_id ";

    /** Most urgent first, then oldest first - the order a technician works in. */
    private static final String ORDER_BY_PRIORITY =
            " ORDER BY FIELD(c.priority, 'URGENT', 'HIGH', 'MEDIUM', 'LOW'), c.created_at";

    private static final String ORDER_BY_NEWEST = " ORDER BY c.created_at DESC, c.id DESC";

    /**
     * Searches complaints. Only the filter values that are set become
     * conditions; each one is bound as a parameter.
     *
     * @param limit         maximum rows, or 0 for no limit
     * @param priorityFirst order by urgency instead of newest first
     */
    public List<Complaint> search(ComplaintFilter filter, int limit, boolean priorityFirst) {
        StringBuilder sql = new StringBuilder(SELECT_COMPLAINTS).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (filter.getReporterId() != null) {
            sql.append("AND c.user_id = ? ");
            params.add(filter.getReporterId());
        }
        if (filter.getStaffScopeId() != null) {
            sql.append("AND c.assigned_staff_id = ? ");
            params.add(filter.getStaffScopeId());
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            sql.append("AND (c.complaint_code LIKE ? OR c.title LIKE ? OR u.name LIKE ? OR u.email LIKE ?) ");
            String like = like(filter.getKeyword());
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (ComplaintFilter.STATUS_OPEN.equals(filter.getStatus())) {
            sql.append("AND c.status NOT IN (?, ?) ");
            params.add(Complaint.STATUS_RESOLVED);
            params.add(Complaint.STATUS_CLOSED);
        } else if (ComplaintFilter.STATUS_DONE.equals(filter.getStatus())) {
            sql.append("AND c.status IN (?, ?) ");
            params.add(Complaint.STATUS_RESOLVED);
            params.add(Complaint.STATUS_CLOSED);
        } else if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND c.status = ? ");
            params.add(filter.getStatus());
        }
        if (filter.getCategoryId() != null) {
            sql.append("AND c.category_id = ? ");
            params.add(filter.getCategoryId());
        }
        if (filter.getPriority() != null && !filter.getPriority().isEmpty()) {
            sql.append("AND c.priority = ? ");
            params.add(filter.getPriority());
        }
        if (filter.getAssignedStaffId() != null) {
            sql.append("AND c.assigned_staff_id = ? ");
            params.add(filter.getAssignedStaffId());
        }
        if (filter.isUnassignedOnly()) {
            sql.append("AND c.assigned_staff_id IS NULL AND c.status IN (?, ?) ");
            params.add(Complaint.STATUS_SUBMITTED);
            params.add(Complaint.STATUS_UNDER_REVIEW);
        }
        if (filter.getDateFrom() != null) {
            sql.append("AND c.created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getDateFrom().atStartOfDay()));
        }
        if (filter.getDateTo() != null) {
            sql.append("AND c.created_at < ? ");
            params.add(Timestamp.valueOf(filter.getDateTo().plusDays(1).atStartOfDay()));
        }
        sql.append(priorityFirst ? ORDER_BY_PRIORITY : ORDER_BY_NEWEST);
        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        List<Complaint> complaints = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    complaints.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the complaints.", e);
        }
        return complaints;
    }

    /** @return the complaint, or null when the id does not exist */
    public Complaint findById(int complaintId) {
        try (Connection connection = DBConnection.getConnection()) {
            return findById(connection, complaintId, false);
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the complaint.", e);
        }
    }

    /**
     * Loads a complaint inside a transaction. With forUpdate the complaint row
     * is locked until the transaction ends, so two people changing the same
     * complaint at the same moment are handled one after the other.
     */
    public Complaint findById(Connection connection, int complaintId, boolean forUpdate) throws SQLException {
        String sql = SELECT_COMPLAINTS + "WHERE c.id = ?" + (forUpdate ? " FOR UPDATE OF c" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaintId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    /**
     * Inserts a new complaint with a temporary unique code.
     *
     * @return the generated id, used to build the final complaint code
     */
    public int insert(Connection connection, Complaint complaint, String temporaryCode) throws SQLException {
        String sql = "INSERT INTO complaints (complaint_code, user_id, category_id, title, description, block, floor, "
                   + "location, priority, status, attachment_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, temporaryCode);
            statement.setInt(2, complaint.getUserId());
            statement.setInt(3, complaint.getCategoryId());
            statement.setString(4, complaint.getTitle());
            statement.setString(5, complaint.getDescription());
            statement.setString(6, complaint.getBlock());
            statement.setString(7, complaint.getFloor());
            statement.setString(8, complaint.getLocation());
            statement.setString(9, complaint.getPriority());
            statement.setString(10, Complaint.STATUS_SUBMITTED);
            DaoSupport.setNullableString(statement, 11, complaint.getAttachmentPath());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No id was generated for the complaint.");
                }
                return keys.getInt(1);
            }
        }
    }

    /** Replaces the temporary code with the final human readable one. */
    public void updateCode(Connection connection, int complaintId, String code) throws SQLException {
        String sql = "UPDATE complaints SET complaint_code = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            statement.setInt(2, complaintId);
            statement.executeUpdate();
        }
    }

    /**
     * Moves a complaint to a new status and stamps resolved_at / closed_at.
     *
     * The expected current status is part of the WHERE clause: if somebody
     * else changed the complaint in the meantime, nothing is updated and the
     * method returns false.
     */
    public boolean updateStatus(Connection connection, int complaintId, String expectedStatus, String newStatus)
            throws SQLException {
        String sql = "UPDATE complaints SET status = ?, "
                   + "resolved_at = CASE WHEN ? = 'RESOLVED' THEN NOW() WHEN ? IN ('ASSIGNED', 'IN_PROGRESS') THEN NULL ELSE resolved_at END, "
                   + "closed_at = CASE WHEN ? = 'CLOSED' THEN NOW() ELSE closed_at END "
                   + "WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus);
            statement.setString(2, newStatus);
            statement.setString(3, newStatus);
            statement.setString(4, newStatus);
            statement.setInt(5, complaintId);
            statement.setString(6, expectedStatus);
            return statement.executeUpdate() == 1;
        }
    }

    /** Sets the assigned staff member and the ASSIGNED status in one statement. */
    public boolean assign(Connection connection, int complaintId, int staffId, String expectedStatus) throws SQLException {
        String sql = "UPDATE complaints SET assigned_staff_id = ?, status = ?, resolved_at = NULL "
                   + "WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, staffId);
            statement.setString(2, Complaint.STATUS_ASSIGNED);
            statement.setInt(3, complaintId);
            statement.setString(4, expectedStatus);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updatePriority(Connection connection, int complaintId, String priority) throws SQLException {
        String sql = "UPDATE complaints SET priority = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, priority);
            statement.setInt(2, complaintId);
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Number of complaints per status.
     *
     * @param reporterId only complaints of this reporter (null = everybody)
     * @param staffId    only complaints assigned to this staff member (null = everybody)
     */
    public StatusSummary countByStatus(Integer reporterId, Integer staffId) {
        StringBuilder sql = new StringBuilder("SELECT status, COUNT(*) AS total FROM complaints WHERE 1 = 1 ");
        if (reporterId != null) {
            sql.append("AND user_id = ? ");
        }
        if (staffId != null) {
            sql.append("AND assigned_staff_id = ? ");
        }
        sql.append("GROUP BY status");

        StatusSummary summary = new StatusSummary();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int index = 1;
            if (reporterId != null) {
                statement.setInt(index++, reporterId);
            }
            if (staffId != null) {
                statement.setInt(index, staffId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    summary.add(resultSet.getString("status"), resultSet.getInt("total"));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the complaints.", e);
        }
        return summary;
    }

    /** Wraps a keyword for LIKE and escapes the LIKE wildcards typed by the user. */
    private static String like(String keyword) {
        String escaped = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Copies the current row of the ResultSet into a Complaint object. */
    private Complaint mapRow(ResultSet resultSet) throws SQLException {
        Complaint complaint = new Complaint();
        complaint.setId(resultSet.getInt("id"));
        complaint.setComplaintCode(resultSet.getString("complaint_code"));
        complaint.setUserId(resultSet.getInt("user_id"));
        complaint.setUserName(resultSet.getString("user_name"));
        complaint.setUserEmail(resultSet.getString("user_email"));
        complaint.setCategoryId(resultSet.getInt("category_id"));
        complaint.setCategoryName(resultSet.getString("category_name"));
        complaint.setTitle(resultSet.getString("title"));
        complaint.setDescription(resultSet.getString("description"));
        complaint.setBlock(resultSet.getString("block"));
        complaint.setFloor(resultSet.getString("floor"));
        complaint.setLocation(resultSet.getString("location"));
        complaint.setPriority(resultSet.getString("priority"));
        complaint.setStatus(resultSet.getString("status"));
        complaint.setAttachmentPath(resultSet.getString("attachment_path"));
        complaint.setAssignedStaffId(DaoSupport.nullableInt(resultSet, "assigned_staff_id"));
        complaint.setAssignedStaffName(resultSet.getString("staff_name"));
        complaint.setCreatedAt(DaoSupport.dateTime(resultSet, "created_at"));
        complaint.setUpdatedAt(DaoSupport.dateTime(resultSet, "updated_at"));
        complaint.setResolvedAt(DaoSupport.dateTime(resultSet, "resolved_at"));
        complaint.setClosedAt(DaoSupport.dateTime(resultSet, "closed_at"));
        complaint.setFeedbackGiven(resultSet.getBoolean("feedback_given"));
        return complaint;
    }
}
