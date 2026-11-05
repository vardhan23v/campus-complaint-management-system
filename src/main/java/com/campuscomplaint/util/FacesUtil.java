package com.campuscomplaint.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

/**
 * Small helpers around FacesMessage, so the beans do not repeat the same
 * four lines every time they want to show something to the user.
 */
public final class FacesUtil {

    private static final Logger LOG = Logger.getLogger(FacesUtil.class.getName());

    private FacesUtil() {
        // utility class: never instantiated
    }

    /** Shows a green "everything worked" message. */
    public static void addInfo(String message) {
        add(FacesMessage.SEVERITY_INFO, message);
    }

    /** Shows a red error message. */
    public static void addError(String message) {
        add(FacesMessage.SEVERITY_ERROR, message);
    }

    /**
     * Shows the message of a ValidationException or DataAccessException.
     * A database failure is also logged with its cause, because the user
     * only sees the short message.
     */
    public static void addError(RuntimeException e) {
        if (e instanceof DataAccessException && e.getCause() != null) {
            LOG.log(Level.WARNING, e.getMessage(), e.getCause());
        }
        addError(e.getMessage());
    }

    /**
     * Adds a global message: it is displayed by <h:messages globalOnly="true"/>
     * in the page template instead of next to one input field.
     */
    private static void add(FacesMessage.Severity severity, String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(severity, message, message));
        }
    }

    /**
     * Keeps the current messages for the next request.
     *
     * FacesMessages normally die with the request they were created in, so a
     * message added just before returning "somepage.xhtml?faces-redirect=true"
     * would never be seen. Calling this method first stores the messages in
     * the flash scope, where they survive exactly one redirect.
     */
    public static void keepMessagesForRedirect() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.getExternalContext().getFlash().setKeepMessages(true);
        }
    }

    /** Reads a request parameter such as ?id=5 (null when missing). */
    public static String param(String name) {
        FacesContext context = FacesContext.getCurrentInstance();
        return context == null ? null : context.getExternalContext().getRequestParameterMap().get(name);
    }
}
