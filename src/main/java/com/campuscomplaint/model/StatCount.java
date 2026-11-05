package com.campuscomplaint.model;

import java.io.Serializable;

/**
 * One labelled number of a report or chart, e.g. ("Electrical Issues", 7, 23.3%).
 */
public class StatCount implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String label;
    private int count;
    private double percent;
    private int secondary;

    public StatCount() {
    }

    public StatCount(String key, String label, int count) {
        this.key = key;
        this.label = label;
        this.count = count;
    }

    /** Percent rounded for display and for CSS bar widths. */
    public int getPercentRounded() {
        return (int) Math.round(percent);
    }

    public String getCss() {
        return Complaint.cssOf(key);
    }

    // ------------------------------------------------- getters and setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public int getSecondary() {
        return secondary;
    }

    public void setSecondary(int secondary) {
        this.secondary = secondary;
    }
}
