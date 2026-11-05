package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.User;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * All SQL that touches the users table.
 *
 * Every statement uses ? placeholders filled in with PreparedStatement, so
 * user input is never concatenated into SQL.
 */
public class UserDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_USER =
            "SELECT id, name, email, password_hash, role, phone, department, is_active, created_at, updated_at FROM users ";

    /**
     * Loads a user by e-mail, including the password hash - used only by the login.
     *
     * @return the User, or null when the e-mail is unknown
     */
    public User findByEmail(String email) {
        String sql = SELECT_USER + "WHERE email = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the login details. Is the database running?", e);
        }
    }

    /** @return the User, or null when the id does not exist */
    public User findById(int userId) {
        String sql = SELECT_USER + "WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the account.", e);
        }
    }

    /** true when another account already uses this e-mail address. */
    public boolean emailExists(String email, int excludeUserId) {
        String sql = "SELECT 1 FROM users WHERE email = ? AND id <> ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setInt(2, excludeUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the e-mail address.", e);
        }
    }

    /**
     * Inserts a new account.
     *
     * @return the generated id
     */
    public int insert(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role, phone, department, is_active) "
                   + "VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole());
            DaoSupport.setNullableString(statement, 5, user.getPhone());
            DaoSupport.setNullableString(statement, 6, user.getDepartment());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            if (DaoSupport.isDuplicateKey(e)) {
                throw new DataAccessException("An account with this e-mail address already exists.", e);
            }
            throw new DataAccessException("Could not create the account.", e);
        }
    }

    /**
     * Accounts of one role, with a complaint count: complaints reported for
     * STUDENT, complaints currently assigned for STAFF.
     *
     * @param keyword optional search in name, e-mail and department
     */
    public List<User> findByRole(String role, String keyword) {
        String countColumn = User.ROLE_STAFF.equals(role) ? "c.assigned_staff_id" : "c.user_id";
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.name, u.email, u.password_hash, u.role, u.phone, u.department, u.is_active, "
              + "u.created_at, u.updated_at, "
              + "(SELECT COUNT(*) FROM complaints c WHERE " + countColumn + " = u.id) AS complaint_count "
              + "FROM users u WHERE u.role = ? ");
        boolean search = keyword != null && !keyword.isBlank();
        if (search) {
            sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR u.department LIKE ?) ");
        }
        sql.append("ORDER BY u.is_active DESC, u.name");

        List<User> users = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            statement.setString(1, role);
            if (search) {
                String like = "%" + keyword.trim() + "%";
                statement.setString(2, like);
                statement.setString(3, like);
                statement.setString(4, like);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    User user = mapRow(resultSet);
                    user.setComplaintCount(resultSet.getInt("complaint_count"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the accounts.", e);
        }
        return users;
    }

    /** Active maintenance staff, for the assignment drop-down. */
    public List<User> findActiveStaff() {
        String sql = SELECT_USER + "WHERE role = ? AND is_active = TRUE ORDER BY name";
        List<User> users = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, User.ROLE_STAFF);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the maintenance staff.", e);
        }
        return users;
    }

    /** Ids of every active administrator - they are notified of new complaints. */
    public List<Integer> findActiveAdminIds(Connection connection) throws SQLException {
        String sql = "SELECT id FROM users WHERE role = ? AND is_active = TRUE";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, User.ROLE_ADMIN);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(1));
                }
            }
        }
        return ids;
    }

    /** Updates name, e-mail, phone and department of an account of the given role. */
    public boolean updateProfile(User user, String role) {
        String sql = "UPDATE users SET name = ?, email = ?, phone = ?, department = ? WHERE id = ? AND role = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            DaoSupport.setNullableString(statement, 3, user.getPhone());
            DaoSupport.setNullableString(statement, 4, user.getDepartment());
            statement.setInt(5, user.getId());
            statement.setString(6, role);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (DaoSupport.isDuplicateKey(e)) {
                throw new DataAccessException("Another account already uses this e-mail address.", e);
            }
            throw new DataAccessException("Could not update the account.", e);
        }
    }

    /** Activates or deactivates an account of the given role. */
    public boolean setActive(int userId, String role, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ? AND role = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, active);
            statement.setInt(2, userId);
            statement.setString(3, role);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not change the account status.", e);
        }
    }

    /** Replaces the password hash - used by the administrator's password reset. */
    public boolean updatePassword(int userId, String role, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ? AND role = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            statement.setString(3, role);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not reset the password.", e);
        }
    }

    /**
     * Deletes an account that has no complaints, history or feedback.
     * Accounts with history are protected by the foreign keys and should be
     * deactivated instead.
     */
    public boolean delete(int userId, String role) {
        String sql = "DELETE FROM users WHERE id = ? AND role = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, role);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (DaoSupport.isForeignKeyViolation(e)) {
                throw new DataAccessException("This account has complaint records and cannot be deleted. Deactivate it instead.", e);
            }
            throw new DataAccessException("Could not delete the account.", e);
        }
    }

    /** Copies the current row of the ResultSet into a User object. */
    private User mapRow(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setName(resultSet.getString("name"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(resultSet.getString("role"));
        user.setPhone(resultSet.getString("phone"));
        user.setDepartment(resultSet.getString("department"));
        user.setActive(resultSet.getBoolean("is_active"));
        user.setCreatedAt(DaoSupport.dateTime(resultSet, "created_at"));
        user.setUpdatedAt(DaoSupport.dateTime(resultSet, "updated_at"));
        return user;
    }
}
