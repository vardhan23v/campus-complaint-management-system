package com.campuscomplaint.util;

import java.util.Map;

import com.campuscomplaint.model.User;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Reads and writes the logged-in user in the HTTP session.
 *
 * The JSF beans, the AuthFilter and the AttachmentServlet all use this class,
 * so the session attribute name ("currentUser") exists in exactly one place.
 */
public final class SessionUtil {

    /** Session attribute holding the logged-in User object. */
    public static final String USER_KEY = "currentUser";

    /** Session attribute holding a one-time message set by the AuthFilter. */
    public static final String AUTH_MESSAGE_KEY = "authMessage";

    private SessionUtil() {
        // utility class: never instantiated
    }

    private static Map<String, Object> sessionMap() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context == null ? null : context.getExternalContext().getSessionMap();
    }

    /** @return the logged-in user, or null when nobody is logged in */
    public static User getCurrentUser() {
        Map<String, Object> session = sessionMap();
        return session == null ? null : (User) session.get(USER_KEY);
    }

    /** @return the logged-in user of a plain servlet request (AuthFilter, AttachmentServlet) */
    public static User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute(USER_KEY);
    }

    /**
     * Stores the user in a brand new session - called by LoginBean after a
     * successful login. Changing the session id at login prevents session
     * fixation: an id planted before login is useless afterwards.
     */
    public static void setCurrentUser(User user) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Object request = context.getExternalContext().getRequest();
        if (request instanceof HttpServletRequest httpRequest) {
            httpRequest.getSession(true);
            httpRequest.changeSessionId();
        }
        context.getExternalContext().getSessionMap().put(USER_KEY, user);
    }

    /** @return true when somebody is logged in with the given role */
    public static boolean hasRole(String role) {
        User user = getCurrentUser();
        return user != null && role != null && role.equals(user.getRole());
    }

    /** Invalidates the whole session - called by LoginBean.logout(). */
    public static void clearSession() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        Object session = context.getExternalContext().getSession(false);
        if (session instanceof HttpSession httpSession) {
            httpSession.invalidate();
        }
    }

    /** Returns the pending message and removes it, so it is shown only once. */
    public static String consumeAuthMessage() {
        Map<String, Object> session = sessionMap();
        if (session == null) {
            return null;
        }
        Object message = session.remove(AUTH_MESSAGE_KEY);
        return message == null ? null : message.toString();
    }
}
