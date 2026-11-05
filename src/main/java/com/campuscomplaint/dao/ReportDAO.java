package com.campuscomplaint.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.StaffStat;
import com.campuscomplaint.model.StatCount;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.DBConnection;
import com.campuscomplaint.util.DataAccessException;

/**
 * Read-only aggregation queries (GROUP BY, COUNT, AVG) for the dashboards
 * and the reports page. The database does the counting; Java only adds
 * percentages and labels.
 */
public class ReportDAO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    /**
     * Complaints per category, every category included (also those with zero).
     * StatCount.secondary holds the resolved/closed count.
     */
    public List<StatCount> countByCategory() {
        String sql = "SELECT cat.id, cat.name, COUNT(c.id) AS total, "
                   + "COALESCE(SUM(c.status IN ('RESOLVED', 'CLOSED')), 0) AS done "
                   + "FROM categories cat LEFT JOIN complaints c ON c.category_id = cat.id "
                   + "GROUP BY cat.id, cat.name ORDER BY total DESC, cat.name";
        List<StatCount> counts = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                StatCount count = new StatCount(String.valueOf(resultSet.getInt("id")),
                        resultSet.getString("name"), resultSet.getInt("total"));
                count.setSecondary(resultSet.getInt("done"));
                counts.add(count);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the category statistics.", e);
        }
        return counts;
    }

    /** Complaints per priority, in the order LOW .. URGENT (zeros included). */
    public List<StatCount> countByPriority() {
        String sql = "SELECT priority, COUNT(*) AS total FROM complaints GROUP BY priority";
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (String priority : Complaint.PRIORITIES) {
            totals.put(priority, 0);
        }
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                totals.put(resultSet.getString("priority"), resultSet.getInt("total"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the priority statistics.", e);
        }
        List<StatCount> counts = new ArrayList<>();
        totals.forEach((priority, total) -> counts.add(new StatCount(priority, Complaint.labelOf(priority), total)));
        return counts;
    }

    /**
     * Complaints submitted per month for the last N months, oldest first.
     * Months without complaints are included with zero, so the chart has no gaps.
     * StatCount.secondary holds how many of them are resolved/closed by now.
     */
    public List<StatCount> monthlySubmissions(int months) {
        YearMonth current = YearMonth.now();
        YearMonth first = current.minusMonths(months - 1L);
        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS total, "
                   + "COALESCE(SUM(status IN ('RESOLVED', 'CLOSED')), 0) AS done "
                   + "FROM complaints WHERE created_at >= ? GROUP BY month";

        Map<String, StatCount> byMonth = new LinkedHashMap<>();
        for (YearMonth month = first; !month.isAfter(current); month = month.plusMonths(1)) {
            byMonth.put(month.toString(), new StatCount(month.toString(), month.format(MONTH_LABEL), 0));
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(first.atDay(1).atStartOfDay()));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StatCount count = byMonth.get(resultSet.getString("month"));
                    if (count != null) {
                        count.setCount(resultSet.getInt("total"));
                        count.setSecondary(resultSet.getInt("done"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the monthly statistics.", e);
        }
        return new ArrayList<>(byMonth.values());
    }

    /** Workload, completion, rating and speed of every maintenance staff member. */
    public List<StaffStat> staffStatistics() {
        String sql = "SELECT u.id, u.name, u.department, COUNT(c.id) AS total, "
                   + "COALESCE(SUM(c.status IN ('ASSIGNED', 'IN_PROGRESS')), 0) AS open_count, "
                   + "COALESCE(SUM(c.status IN ('RESOLVED', 'CLOSED')), 0) AS resolved_count, "
                   + "AVG(f.rating) AS avg_rating, "
                   + "AVG(CASE WHEN c.resolved_at IS NOT NULL THEN TIMESTAMPDIFF(HOUR, c.created_at, c.resolved_at) END) AS avg_hours "
                   + "FROM users u "
                   + "LEFT JOIN complaints c ON c.assigned_staff_id = u.id "
                   + "LEFT JOIN feedback f ON f.complaint_id = c.id "
                   + "WHERE u.role = ? "
                   + "GROUP BY u.id, u.name, u.department ORDER BY total DESC, u.name";
        List<StaffStat> stats = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, User.ROLE_STAFF);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StaffStat stat = new StaffStat();
                    stat.setStaffId(resultSet.getInt("id"));
                    stat.setName(resultSet.getString("name"));
                    stat.setDepartment(resultSet.getString("department"));
                    stat.setTotalAssigned(resultSet.getInt("total"));
                    stat.setOpenCount(resultSet.getInt("open_count"));
                    stat.setResolvedCount(resultSet.getInt("resolved_count"));
                    stat.setAverageRating(DaoSupport.nullableDouble(resultSet, "avg_rating"));
                    stat.setAverageResolutionHours(DaoSupport.nullableDouble(resultSet, "avg_hours"));
                    stats.add(stat);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the staff statistics.", e);
        }
        return stats;
    }

    /** Average feedback rating over all complaints, or null when there is none. */
    public Double averageRating() {
        return singleDouble("SELECT AVG(rating) FROM feedback");
    }

    /** Average hours from submission to resolution, or null when nothing is resolved. */
    public Double averageResolutionHours() {
        return singleDouble("SELECT AVG(TIMESTAMPDIFF(HOUR, created_at, resolved_at)) FROM complaints "
                + "WHERE resolved_at IS NOT NULL");
    }

    /** Complaints submitted since the given day (today, this week, ...). */
    public int countSubmittedSince(LocalDate day) {
        String sql = "SELECT COUNT(*) FROM complaints WHERE created_at >= ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(day.atStartOfDay()));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the recent complaints.", e);
        }
    }

    /** Runs a fixed query (no user input) that returns one number. */
    private Double singleDouble(String sql) {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                double value = resultSet.getDouble(1);
                return resultSet.wasNull() ? null : value;
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the report.", e);
        }
    }
}
