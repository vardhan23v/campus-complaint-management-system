package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.campuscomplaint.model.Category;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * All SQL that touches the categories table.
 */
public class CategoryDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SELECT_CATEGORIES =
            "SELECT cat.id, cat.name, cat.description, cat.is_active, cat.created_at, "
          + "(SELECT COUNT(*) FROM complaints c WHERE c.category_id = cat.id) AS complaint_count "
          + "FROM categories cat ";

    /** Every category with its complaint count - for the management page. */
    public List<Category> findAll() {
        return query(SELECT_CATEGORIES + "ORDER BY cat.name", false);
    }

    /** Active categories only - for the complaint form and the filters. */
    public List<Category> findActive() {
        return query(SELECT_CATEGORIES + "WHERE cat.is_active = ? ORDER BY cat.name", true);
    }

    private List<Category> query(String sql, boolean activeOnly) {
        List<Category> categories = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (activeOnly) {
                statement.setBoolean(1, true);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the complaint categories.", e);
        }
        return categories;
    }

    /** @return the category, or null when the id does not exist */
    public Category findById(int categoryId) {
        String sql = SELECT_CATEGORIES + "WHERE cat.id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the category.", e);
        }
    }

    /** true when another category already has this name (case-insensitive collation). */
    public boolean nameExists(String name, int excludeId) {
        String sql = "SELECT 1 FROM categories WHERE name = ? AND id <> ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setInt(2, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the category name.", e);
        }
    }

    public boolean insert(Category category) {
        String sql = "INSERT INTO categories (name, description, is_active) VALUES (?, ?, TRUE)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, category.getName());
            DaoSupport.setNullableString(statement, 2, category.getDescription());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (DaoSupport.isDuplicateKey(e)) {
                throw new DataAccessException("A category with this name already exists.", e);
            }
            throw new DataAccessException("Could not save the category.", e);
        }
    }

    public boolean update(Category category) {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, category.getName());
            DaoSupport.setNullableString(statement, 2, category.getDescription());
            statement.setInt(3, category.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (DaoSupport.isDuplicateKey(e)) {
                throw new DataAccessException("A category with this name already exists.", e);
            }
            throw new DataAccessException("Could not update the category.", e);
        }
    }

    public boolean setActive(int categoryId, boolean active) {
        String sql = "UPDATE categories SET is_active = ? WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, active);
            statement.setInt(2, categoryId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not change the category.", e);
        }
    }

    /** Deletes a category that no complaint uses (the foreign key protects used ones). */
    public boolean delete(int categoryId) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, categoryId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            if (DaoSupport.isForeignKeyViolation(e)) {
                throw new DataAccessException("This category is used by complaints and cannot be deleted. Deactivate it instead.", e);
            }
            throw new DataAccessException("Could not delete the category.", e);
        }
    }

    private Category mapRow(ResultSet resultSet) throws SQLException {
        Category category = new Category();
        category.setId(resultSet.getInt("id"));
        category.setName(resultSet.getString("name"));
        category.setDescription(resultSet.getString("description"));
        category.setActive(resultSet.getBoolean("is_active"));
        category.setCreatedAt(DaoSupport.dateTime(resultSet, "created_at"));
        category.setComplaintCount(resultSet.getInt("complaint_count"));
        return category;
    }
}
