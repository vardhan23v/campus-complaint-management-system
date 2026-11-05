package com.campuscomplaint.service;

import com.campuscomplaint.model.Complaint;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.ValidationException;

/**
 * Role and ownership checks used at the start of every service method that
 * reads private data or changes something. The AuthFilter protects the
 * pages; these checks protect the operations themselves.
 */
final class Access {

    private Access() {
        // utility class: never instantiated
    }

    static void requireRole(User user, String role, String message) {
        if (user == null || !role.equals(user.getRole())) {
            throw new ValidationException(message);
        }
    }

    static void requireAdmin(User user) {
        requireRole(user, User.ROLE_ADMIN, "Only an administrator can do this.");
    }

    static void requireStaff(User user) {
        requireRole(user, User.ROLE_STAFF, "Only maintenance staff can do this.");
    }

    static void requireReporter(User user) {
        requireRole(user, User.ROLE_STUDENT, "Only student and faculty accounts can do this.");
    }

    /**
     * Who may see a complaint: the administrator, the person who reported it
     * and the staff member it is currently assigned to.
     */
    static boolean canView(User user, Complaint complaint) {
        if (user == null || complaint == null) {
            return false;
        }
        return switch (user.getRole()) {
            case User.ROLE_ADMIN -> true;
            case User.ROLE_STUDENT -> complaint.getUserId() == user.getId();
            case User.ROLE_STAFF -> complaint.getAssignedStaffId() != null
                    && complaint.getAssignedStaffId() == user.getId();
            default -> false;
        };
    }
}
