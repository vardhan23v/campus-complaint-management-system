package com.campuscomplaint.bean;

import java.util.List;

import com.campuscomplaint.model.Complaint;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Fixed option lists for drop-downs and labels used by many pages:
 * statuses, priorities, campus blocks and floors.
 */
@Named("lookup")
@ApplicationScoped
public class LookupBean {

    private static final List<String> BLOCKS = List.of(
            "Main Block", "Block A - Engineering", "Block B - Sciences", "Block C - Management",
            "Central Library", "Laboratory Complex", "Boys Hostel", "Girls Hostel",
            "Cafeteria", "Sports Complex", "Auditorium", "Transport Yard", "Open Campus");

    private static final List<String> FLOORS = List.of(
            "Basement", "Ground floor", "1st floor", "2nd floor", "3rd floor", "4th floor",
            "5th floor", "Terrace", "Not applicable");

    public List<String> getStatuses() {
        return Complaint.STATUSES;
    }

    public List<String> getPriorities() {
        return Complaint.PRIORITIES;
    }

    public List<String> getReporterPriorities() {
        return Complaint.REPORTER_PRIORITIES;
    }

    public List<String> getBlocks() {
        return BLOCKS;
    }

    public List<String> getFloors() {
        return FLOORS;
    }

    /** "IN_PROGRESS" -> "In progress", for f:selectItems labels. */
    public String label(String value) {
        return Complaint.labelOf(value);
    }

    /** "IN_PROGRESS" -> "in-progress", for CSS classes. */
    public String css(String value) {
        return Complaint.cssOf(value);
    }
}
