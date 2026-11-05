package com.campuscomplaint.service;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.campuscomplaint.dao.AssignmentDAO;
import com.campuscomplaint.dao.CategoryDAO;
import com.campuscomplaint.dao.ComplaintDAO;
import com.campuscomplaint.dao.FeedbackDAO;
import com.campuscomplaint.dao.ReportDAO;
import com.campuscomplaint.dao.StatusHistoryDAO;
import com.campuscomplaint.dao.UserDAO;
import com.campuscomplaint.model.Category;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintAssignment;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.model.Feedback;
import com.campuscomplaint.model.StaffStat;
import com.campuscomplaint.model.StatCount;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.InputValidator;
import com.campuscomplaint.util.PasswordUtil;
import com.campuscomplaint.util.TransactionManager;
import com.campuscomplaint.util.ValidationException;

/**
 * Everything the administrator (the complaint cell) can do: review, assign,
 * prioritise, close, manage accounts and categories, and read reports.
 *
 * Every public method starts with Access.requireAdmin(admin), so even a
 * hand-made request from a student or staff session is refused here.
 */
public class AdminService implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final StatusHistoryDAO historyDAO = new StatusHistoryDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final UserDAO userDAO = new UserDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final ComplaintWorkflow workflow = new ComplaintWorkflow();

    // ------------------------------------------------------ complaint reads

    public List<Complaint> search(ComplaintFilter filter, User admin) {
        Access.requireAdmin(admin);
        filter.setReporterId(null);
        filter.setStaffScopeId(null);
        return complaintDAO.search(filter, 0, false);
    }

    public List<Complaint> getRecent(int limit, User admin) {
        Access.requireAdmin(admin);
        return complaintDAO.search(new ComplaintFilter(), limit, false);
    }

    /** New complaints nobody has been assigned to yet, most urgent first. */
    public List<Complaint> getAwaitingAssignment(int limit, User admin) {
        Access.requireAdmin(admin);
        ComplaintFilter filter = new ComplaintFilter();
        filter.setUnassignedOnly(true);
        return complaintDAO.search(filter, limit, true);
    }

    public Complaint getComplaint(int complaintId, User admin) {
        Access.requireAdmin(admin);
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null) {
            throw new ValidationException("That complaint does not exist.");
        }
        return complaint;
    }

    public List<ComplaintStatusHistory> getHistory(int complaintId, User admin) {
        Access.requireAdmin(admin);
        return historyDAO.findByComplaint(complaintId);
    }

    public List<ComplaintAssignment> getAssignments(int complaintId, User admin) {
        Access.requireAdmin(admin);
        return assignmentDAO.findByComplaint(complaintId);
    }

    public Feedback getFeedback(int complaintId, User admin) {
        Access.requireAdmin(admin);
        return feedbackDAO.findByComplaint(complaintId);
    }

    public List<ComplaintStatusHistory> getRecentActivity(int limit, User admin) {
        Access.requireAdmin(admin);
        return historyDAO.findRecent(limit);
    }

    public List<User> getActiveStaff(User admin) {
        Access.requireAdmin(admin);
        return userDAO.findActiveStaff();
    }

    // ----------------------------------------------------- complaint writes

    /** SUBMITTED -> UNDER_REVIEW. */
    public void markUnderReview(int complaintId, String remarks, User admin) {
        Access.requireAdmin(admin);
        String note = InputValidator.optionalText(remarks, "Remarks", 1000);
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = workflow.lock(connection, complaintId);
            requireAdminMove(complaint, Complaint.STATUS_UNDER_REVIEW);
            workflow.transition(connection, complaint, Complaint.STATUS_UNDER_REVIEW, admin,
                    note == null ? "Complaint taken up for review by the complaint cell." : note);
            return null;
        });
    }

    /**
     * Assigns (or reassigns) a complaint to a maintenance staff member.
     *
     * A SUBMITTED complaint first passes UNDER_REVIEW, so the history always
     * shows the full lifecycle. The assignment row, the status change, the
     * history rows and the notifications are written in one transaction.
     */
    public void assign(int complaintId, Integer staffId, String remarks, User admin) {
        Access.requireAdmin(admin);
        if (staffId == null) {
            throw new ValidationException("Please choose a maintenance staff member.");
        }
        String note = InputValidator.optionalText(remarks, "Assignment note", 500);
        User staff = userDAO.findById(staffId);
        if (staff == null || !User.ROLE_STAFF.equals(staff.getRole()) || !staff.isActive()) {
            throw new ValidationException("Please choose an active maintenance staff member.");
        }

        TransactionManager.inTransaction(connection -> {
            Complaint complaint = workflow.lock(connection, complaintId);
            if (!ComplaintWorkflow.assignable(complaint.getStatus())) {
                throw new ValidationException("A " + complaint.getStatusLabel().toLowerCase()
                        + " complaint cannot be assigned. Reopen it first.");
            }
            Integer previousStaff = complaint.getAssignedStaffId();
            if (staffId.equals(previousStaff)
                    && Complaint.STATUS_ASSIGNED.equals(complaint.getStatus())) {
                throw new ValidationException("The complaint is already assigned to " + staff.getName() + ".");
            }

            if (Complaint.STATUS_SUBMITTED.equals(complaint.getStatus())) {
                workflow.transition(connection, complaint, Complaint.STATUS_UNDER_REVIEW, admin,
                        "Reviewed by the complaint cell.");
            }

            String expected = complaint.getStatus();
            assignmentDAO.insert(connection, complaintId, staffId, admin.getId(), note);
            if (!complaintDAO.assign(connection, complaintId, staffId, expected)) {
                throw new ValidationException("This complaint was changed by someone else a moment ago. Please reload the page.");
            }
            boolean reassignment = previousStaff != null;
            String historyText = (reassignment ? "Reassigned to " : "Assigned to ") + staff.getName()
                    + (staff.getDepartment() == null ? "" : " (" + staff.getDepartment() + ")")
                    + (note == null ? "." : ": " + note);
            historyDAO.insert(connection, complaintId, expected, Complaint.STATUS_ASSIGNED,
                    admin.getId(), historyText);
            complaint.setStatus(Complaint.STATUS_ASSIGNED);

            workflow.notifyReporter(connection, complaint, complaint.getComplaintCode() + " is now Assigned",
                    "Your complaint has been assigned to " + staff.getName() + " of the maintenance team.");
            workflow.notifyUser(connection, staffId, complaint, "New assignment " + complaint.getComplaintCode(),
                    complaint.getTitle() + " · " + complaint.getFullLocation()
                    + " · " + complaint.getPriorityLabel() + " priority" + (note == null ? "" : " · " + note));
            if (reassignment && !staffId.equals(previousStaff)) {
                workflow.notifyUser(connection, previousStaff, complaint,
                        complaint.getComplaintCode() + " reassigned",
                        "This complaint has been moved to another staff member. No further action is needed from you.");
            }
            return null;
        });
    }

    /** Changes the priority and records the change as a remark in the history. */
    public void changePriority(int complaintId, String priority, User admin) {
        Access.requireAdmin(admin);
        if (priority == null || !Complaint.PRIORITIES.contains(priority)) {
            throw new ValidationException("Please choose a valid priority.");
        }
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = workflow.lock(connection, complaintId);
            if (Complaint.STATUS_CLOSED.equals(complaint.getStatus())) {
                throw new ValidationException("The priority of a closed complaint cannot be changed.");
            }
            if (priority.equals(complaint.getPriority())) {
                throw new ValidationException("The complaint already has " + Complaint.labelOf(priority) + " priority.");
            }
            complaintDAO.updatePriority(connection, complaintId, priority);
            workflow.remark(connection, complaint, admin, "Priority changed from " + complaint.getPriorityLabel()
                    + " to " + Complaint.labelOf(priority) + ".");
            if (complaint.getAssignedStaffId() != null && complaint.isOpen()) {
                workflow.notifyUser(connection, complaint.getAssignedStaffId(), complaint,
                        complaint.getComplaintCode() + " is now " + Complaint.labelOf(priority) + " priority",
                        "The complaint cell changed the priority of \"" + complaint.getTitle() + "\".");
            }
            return null;
        });
    }

    /** RESOLVED -> CLOSED, after the administrator verified the work. */
    public void close(int complaintId, String remarks, User admin) {
        Access.requireAdmin(admin);
        String note = InputValidator.optionalText(remarks, "Remarks", 1000);
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = workflow.lock(connection, complaintId);
            requireAdminMove(complaint, Complaint.STATUS_CLOSED);
            workflow.transition(connection, complaint, Complaint.STATUS_CLOSED, admin,
                    note == null ? "Resolution verified. Complaint closed by the complaint cell." : note);
            return null;
        });
    }

    /** RESOLVED -> ASSIGNED: the work was not satisfactory and goes back to the staff member. */
    public void reopen(int complaintId, String remarks, User admin) {
        Access.requireAdmin(admin);
        String note = InputValidator.requireText(remarks, "The reason for reopening", 5, 1000);
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = workflow.lock(connection, complaintId);
            requireAdminMove(complaint, Complaint.STATUS_ASSIGNED);
            if (complaint.getAssignedStaffId() == null) {
                throw new ValidationException("This complaint has no staff member to send it back to. Assign it instead.");
            }
            workflow.transition(connection, complaint, Complaint.STATUS_ASSIGNED, admin, "Reopened: " + note);
            workflow.notifyUser(connection, complaint.getAssignedStaffId(), complaint,
                    complaint.getComplaintCode() + " reopened", note);
            return null;
        });
    }

    /** Adds an administrative remark without changing the status. */
    public void addRemark(int complaintId, String remarks, User admin) {
        Access.requireAdmin(admin);
        String note = InputValidator.requireText(remarks, "The remark", 3, 1000);
        TransactionManager.inTransaction(connection -> {
            Complaint complaint = workflow.lock(connection, complaintId);
            if (Complaint.STATUS_CLOSED.equals(complaint.getStatus())) {
                throw new ValidationException("Closed complaints cannot receive new remarks.");
            }
            workflow.remark(connection, complaint, admin, note);
            workflow.notifyReporter(connection, complaint, "New remark on " + complaint.getComplaintCode(), note);
            return null;
        });
    }

    private void requireAdminMove(Complaint complaint, String target) {
        if (!ComplaintWorkflow.adminMayMove(complaint.getStatus(), target)) {
            throw new ValidationException("A complaint cannot move from " + complaint.getStatusLabel()
                    + " to " + Complaint.labelOf(target) + ".");
        }
    }

    // --------------------------------------------------------- dashboards

    public StatusSummary getSummary(User admin) {
        Access.requireAdmin(admin);
        return complaintDAO.countByStatus(null, null);
    }

    public int getSubmittedToday(User admin) {
        Access.requireAdmin(admin);
        return reportDAO.countSubmittedSince(LocalDate.now());
    }

    /** Complaints per category with the percentage of the largest bar. */
    public List<StatCount> getCategoryStats(User admin) {
        Access.requireAdmin(admin);
        return withPercent(reportDAO.countByCategory());
    }

    public List<StatCount> getPriorityStats(User admin) {
        Access.requireAdmin(admin);
        return withPercent(reportDAO.countByPriority());
    }

    public List<StatCount> getMonthlyStats(int months, User admin) {
        Access.requireAdmin(admin);
        return withPercent(reportDAO.monthlySubmissions(months));
    }

    public List<StaffStat> getStaffStats(User admin) {
        Access.requireAdmin(admin);
        return reportDAO.staffStatistics();
    }

    public Double getAverageRating(User admin) {
        Access.requireAdmin(admin);
        return reportDAO.averageRating();
    }

    public Double getAverageResolutionHours(User admin) {
        Access.requireAdmin(admin);
        return reportDAO.averageResolutionHours();
    }

    public List<Feedback> getAllFeedback(User admin) {
        Access.requireAdmin(admin);
        return feedbackDAO.findAll();
    }

    /**
     * Percent relative to the total, used for the bar lengths.
     */
    private static List<StatCount> withPercent(List<StatCount> counts) {
        int total = counts.stream().mapToInt(StatCount::getCount).sum();
        for (StatCount count : counts) {
            count.setPercent(total == 0 ? 0 : count.getCount() * 100.0 / total);
        }
        return counts;
    }

    /** The filtered complaint list as CSV text (opened by Excel / LibreOffice). */
    public String exportCsv(ComplaintFilter filter, User admin) {
        List<Complaint> complaints = search(filter, admin);
        StringBuilder csv = new StringBuilder(
                "Complaint ID,Title,Category,Reporter,Block,Floor,Location,Priority,Status,Assigned to,Submitted,Resolved,Closed\r\n");
        for (Complaint c : complaints) {
            csv.append(cell(c.getComplaintCode())).append(',')
               .append(cell(c.getTitle())).append(',')
               .append(cell(c.getCategoryName())).append(',')
               .append(cell(c.getUserName())).append(',')
               .append(cell(c.getBlock())).append(',')
               .append(cell(c.getFloor())).append(',')
               .append(cell(c.getLocation())).append(',')
               .append(cell(c.getPriorityLabel())).append(',')
               .append(cell(c.getStatusLabel())).append(',')
               .append(cell(c.getAssignedStaffName())).append(',')
               .append(cell(c.getCreatedAt() == null ? "" : c.getCreatedAt().format(CSV_DATE))).append(',')
               .append(cell(c.getResolvedAt() == null ? "" : c.getResolvedAt().format(CSV_DATE))).append(',')
               .append(cell(c.getClosedAt() == null ? "" : c.getClosedAt().format(CSV_DATE))).append("\r\n");
        }
        return csv.toString();
    }

    /**
     * Quotes a CSV cell. A leading = + - @ is prefixed with an apostrophe so a
     * spreadsheet never runs a complaint title as a formula.
     */
    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        String text = value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    // ------------------------------------------------------------ accounts

    /** Student/faculty or staff accounts, optionally searched. */
    public List<User> getAccounts(String role, String keyword, User admin) {
        Access.requireAdmin(admin);
        requireManagedRole(role);
        return userDAO.findByRole(role, keyword);
    }

    /** Creates a student/faculty or maintenance staff account. */
    public void createAccount(User form, String password, String role, User admin) {
        Access.requireAdmin(admin);
        requireManagedRole(role);
        User user = cleanProfile(form);
        InputValidator.requirePassword(password);
        if (userDAO.emailExists(user.getEmail(), 0)) {
            throw new ValidationException("An account with this e-mail address already exists.");
        }
        user.setRole(role);
        user.setPasswordHash(PasswordUtil.hash(password));
        userDAO.insert(user);
    }

    public void updateAccount(User form, String role, User admin) {
        Access.requireAdmin(admin);
        requireManagedRole(role);
        User user = cleanProfile(form);
        user.setId(form.getId());
        if (userDAO.emailExists(user.getEmail(), user.getId())) {
            throw new ValidationException("Another account already uses this e-mail address.");
        }
        if (!userDAO.updateProfile(user, role)) {
            throw new ValidationException("That account no longer exists.");
        }
    }

    public void setAccountActive(int userId, String role, boolean active, User admin) {
        Access.requireAdmin(admin);
        requireManagedRole(role);
        if (userId == admin.getId()) {
            throw new ValidationException("You cannot deactivate your own account.");
        }
        if (!userDAO.setActive(userId, role, active)) {
            throw new ValidationException("That account no longer exists.");
        }
    }

    public void resetPassword(int userId, String role, String newPassword, User admin) {
        Access.requireAdmin(admin);
        requireManagedRole(role);
        InputValidator.requirePassword(newPassword);
        if (!userDAO.updatePassword(userId, role, PasswordUtil.hash(newPassword))) {
            throw new ValidationException("That account no longer exists.");
        }
    }

    public void deleteAccount(int userId, String role, User admin) {
        Access.requireAdmin(admin);
        requireManagedRole(role);
        if (!userDAO.delete(userId, role)) {
            throw new ValidationException("That account no longer exists.");
        }
    }

    /** Administrators are never managed from the web pages. */
    private void requireManagedRole(String role) {
        if (!User.ROLE_STUDENT.equals(role) && !User.ROLE_STAFF.equals(role)) {
            throw new ValidationException("Only student/faculty and staff accounts can be managed here.");
        }
    }

    private User cleanProfile(User form) {
        User user = new User();
        user.setName(InputValidator.requireText(form.getName(), "Full name", 3, 100));
        user.setEmail(InputValidator.requireEmail(form.getEmail()));
        user.setPhone(InputValidator.optionalPhone(form.getPhone()));
        user.setDepartment(InputValidator.optionalText(form.getDepartment(), "Department", 100));
        return user;
    }

    // ---------------------------------------------------------- categories

    public List<Category> getCategories(User admin) {
        Access.requireAdmin(admin);
        return categoryDAO.findAll();
    }

    public void saveCategory(Category form, User admin) {
        Access.requireAdmin(admin);
        Category category = new Category();
        category.setId(form.getId());
        category.setName(InputValidator.requireText(form.getName(), "Category name", 3, 80));
        category.setDescription(InputValidator.optionalText(form.getDescription(), "Description", 255));
        if (categoryDAO.nameExists(category.getName(), category.getId())) {
            throw new ValidationException("A category with this name already exists.");
        }
        if (category.getId() == 0) {
            categoryDAO.insert(category);
        } else if (!categoryDAO.update(category)) {
            throw new ValidationException("That category no longer exists.");
        }
    }

    public void setCategoryActive(int categoryId, boolean active, User admin) {
        Access.requireAdmin(admin);
        if (!categoryDAO.setActive(categoryId, active)) {
            throw new ValidationException("That category no longer exists.");
        }
    }

    public void deleteCategory(int categoryId, User admin) {
        Access.requireAdmin(admin);
        if (!categoryDAO.delete(categoryId)) {
            throw new ValidationException("That category no longer exists.");
        }
    }
}
