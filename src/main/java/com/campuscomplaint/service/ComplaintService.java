package com.campuscomplaint.service;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import com.campuscomplaint.dao.CategoryDAO;
import com.campuscomplaint.dao.ComplaintDAO;
import com.campuscomplaint.dao.NotificationDAO;
import com.campuscomplaint.dao.StatusHistoryDAO;
import com.campuscomplaint.dao.UserDAO;
import com.campuscomplaint.model.Category;
import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.ComplaintFilter;
import com.campuscomplaint.model.ComplaintStatusHistory;
import com.campuscomplaint.model.StatusSummary;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.AppConfig;
import com.campuscomplaint.util.FileUploadUtil;
import com.campuscomplaint.util.InputValidator;
import com.campuscomplaint.util.TransactionManager;
import com.campuscomplaint.util.ValidationException;

import jakarta.servlet.http.Part;

/**
 * Complaint rules for the people who report complaints (students and
 * faculty), plus the shared read checks used by every role.
 *
 * Privacy rule: a reporter only ever sees complaints with their own user_id.
 * The id comes from the session, never from the page.
 */
public class ComplaintService implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final StatusHistoryDAO historyDAO = new StatusHistoryDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();

    // ---------------------------------------------------------------- reads

    /** Categories a complaint can be filed under. */
    public List<Category> getActiveCategories() {
        return categoryDAO.findActive();
    }

    /** The reporter's own complaints matching the filter. */
    public List<Complaint> searchMine(ComplaintFilter filter, User reporter) {
        Access.requireReporter(reporter);
        filter.setReporterId(reporter.getId());   // scope forced from the session
        filter.setStaffScopeId(null);
        filter.setAssignedStaffId(null);
        return complaintDAO.search(filter, 0, false);
    }

    /** The reporter's latest complaints, for the dashboard. */
    public List<Complaint> getRecentForReporter(User reporter, int limit) {
        Access.requireReporter(reporter);
        ComplaintFilter filter = new ComplaintFilter();
        filter.setReporterId(reporter.getId());
        return complaintDAO.search(filter, limit, false);
    }

    public StatusSummary getSummaryForReporter(User reporter) {
        Access.requireReporter(reporter);
        return complaintDAO.countByStatus(reporter.getId(), null);
    }

    /**
     * Loads a complaint the reporter owns.
     *
     * A complaint that belongs to somebody else is reported exactly like a
     * missing one, so guessing ids reveals nothing.
     */
    public Complaint getForReporter(int complaintId, User reporter) {
        Access.requireReporter(reporter);
        Complaint complaint = complaintDAO.findById(complaintId);
        if (complaint == null || complaint.getUserId() != reporter.getId()) {
            throw new ValidationException("That complaint was not found in your account.");
        }
        return complaint;
    }

    /** The status timeline of a complaint the user is allowed to see. */
    public List<ComplaintStatusHistory> getHistory(int complaintId, User user) {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (!Access.canView(user, complaint)) {
            throw new ValidationException("You are not allowed to view this complaint.");
        }
        return historyDAO.findByComplaint(complaintId);
    }

    /**
     * The stored image of a complaint, for AttachmentServlet.
     *
     * @return the file, or null when the user may not see it or there is none
     */
    public Path getAttachment(int complaintId, User user) {
        Complaint complaint = complaintDAO.findById(complaintId);
        if (!Access.canView(user, complaint) || !complaint.isHasAttachment()) {
            return null;
        }
        return FileUploadUtil.resolveStored(complaint.getAttachmentPath());
    }

    /** @return the stored attachment name (for the content type), or null */
    public String getAttachmentName(int complaintId, User user) {
        Complaint complaint = complaintDAO.findById(complaintId);
        return Access.canView(user, complaint) ? complaint.getAttachmentPath() : null;
    }

    // -------------------------------------------------------------- writes

    /**
     * Files a new complaint.
     *
     * Steps: validate -> store the image (if any) -> in ONE transaction insert
     * the complaint, give it its code, write the first history row and the
     * notifications. If the transaction fails the stored image is removed.
     *
     * @return the new complaint code, e.g. NIT-2026-00012
     */
    public String submit(Complaint form, Part image, User reporter) {
        Access.requireReporter(reporter);

        Complaint complaint = new Complaint();
        complaint.setUserId(reporter.getId());
        complaint.setTitle(InputValidator.requireText(form.getTitle(), "Title", 5, 150));
        complaint.setDescription(InputValidator.requireText(form.getDescription(), "Description", 15, 4000));
        complaint.setBlock(InputValidator.requireText(form.getBlock(), "Campus block", 1, 60));
        complaint.setFloor(InputValidator.requireText(form.getFloor(), "Floor", 1, 30));
        complaint.setLocation(InputValidator.requireText(form.getLocation(), "Room / location", 2, 120));

        String priority = form.getPriority() == null ? Complaint.PRIORITY_MEDIUM : form.getPriority();
        if (!Complaint.REPORTER_PRIORITIES.contains(priority)) {
            throw new ValidationException("Please choose Low, Medium or High priority. Urgent is set by the complaint cell.");
        }
        complaint.setPriority(priority);

        Category category = categoryDAO.findById(form.getCategoryId());
        if (category == null || !category.isActive()) {
            throw new ValidationException("Please choose a valid complaint category.");
        }
        complaint.setCategoryId(category.getId());

        String storedImage = FileUploadUtil.isEmpty(image) ? null : FileUploadUtil.validateAndStore(image);
        complaint.setAttachmentPath(storedImage);

        try {
            return TransactionManager.inTransaction(connection -> {
                // The id is only known after the insert, so a unique temporary
                // code is stored first and replaced in the same transaction.
                String temporary = "TMP-" + UUID.randomUUID().toString().substring(0, 15);
                int id = complaintDAO.insert(connection, complaint, temporary);
                String code = String.format("%s-%d-%05d", AppConfig.complaintCodePrefix(), Year.now().getValue(), id);
                complaintDAO.updateCode(connection, id, code);

                historyDAO.insert(connection, id, null, Complaint.STATUS_SUBMITTED, reporter.getId(),
                        "Complaint submitted by " + reporter.getName() + ".");
                notificationDAO.insert(connection, reporter.getId(), id, "Complaint " + code + " received",
                        "We have received \"" + complaint.getTitle() + "\". You will be notified at every status change.");
                for (int adminId : userDAO.findActiveAdminIds(connection)) {
                    notificationDAO.insert(connection, adminId, id, "New complaint " + code,
                            category.getName() + " · " + complaint.getBlock() + ", " + complaint.getLocation()
                            + " · " + Complaint.labelOf(priority) + " priority");
                }
                return code;
            });
        } catch (RuntimeException e) {
            FileUploadUtil.deleteQuietly(storedImage);
            throw e;
        }
    }
}
