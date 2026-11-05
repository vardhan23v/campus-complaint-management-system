package com.campuscomplaint.bean;

import com.campuscomplaint.filter.AuthFilter;
import com.campuscomplaint.model.User;
import com.campuscomplaint.service.AuthService;
import com.campuscomplaint.service.NotificationService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FacesUtil;
import com.campuscomplaint.util.SessionUtil;
import com.campuscomplaint.util.ValidationException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Backing bean of login.xhtml and of the page template (sidebar, user
 * card, notification bell).
 *
 * @RequestScoped: the form fields only live for one request. The logged-in
 * user is kept in the HTTP session (see SessionUtil), not in this bean.
 */
@Named("loginBean")
@RequestScoped
public class LoginBean {

    private final AuthService authService = new AuthService();
    private final NotificationService notificationService = new NotificationService();

    private String email;
    private String password;

    /** One-time message from AuthFilter, shown after an unauthorized page request. */
    private String authMessage;
    private boolean authMessageLoaded;

    /** Cached for the request: the template asks for it more than once. */
    private Integer unreadCount;

    /**
     * Action method of the login form.
     *
     * @return the dashboard of the user's role, or null to stay on the page
     */
    public String login() {
        try {
            User user = authService.login(email, password);
            password = null;   // never keep the password in memory
            if (user == null) {
                FacesUtil.addError("Invalid e-mail address or password.");
                return null;
            }

            SessionUtil.setCurrentUser(user);
            FacesUtil.keepMessagesForRedirect();
            FacesUtil.addInfo("Welcome back, " + user.getFirstName() + ".");
            return "/" + AuthFilter.homePage(user) + "?faces-redirect=true";
        } catch (ValidationException | DataAccessException e) {
            // e.g. a deactivated account, or MySQL is not running
            FacesUtil.addError(e);
            return null;
        }
    }

    /** Action method of the Log out button: drops the whole session. */
    public String logout() {
        SessionUtil.clearSession();
        return "logout";
    }

    // ------------------------------------------------- used by the template

    public boolean isLoggedIn() {
        return SessionUtil.getCurrentUser() != null;
    }

    public User getCurrentUser() {
        return SessionUtil.getCurrentUser();
    }

    public boolean isStudent() {
        return SessionUtil.hasRole(User.ROLE_STUDENT);
    }

    public boolean isStaff() {
        return SessionUtil.hasRole(User.ROLE_STAFF);
    }

    public boolean isAdmin() {
        return SessionUtil.hasRole(User.ROLE_ADMIN);
    }

    /** The view id of the logged-in user's dashboard (the brand link). */
    public String getHomeOutcome() {
        User user = SessionUtil.getCurrentUser();
        return user == null ? "/index.xhtml" : "/" + AuthFilter.homePage(user);
    }

    /** Unread notifications for the bell in the top bar. */
    public int getUnreadCount() {
        if (unreadCount == null) {
            try {
                unreadCount = notificationService.getUnreadCount(SessionUtil.getCurrentUser());
            } catch (DataAccessException e) {
                unreadCount = 0;   // the bell must never break a page
            }
        }
        return unreadCount;
    }

    /**
     * The message AuthFilter left behind when somebody opened a page without
     * the required role. It is read once and then removed from the session.
     */
    public String getAuthMessage() {
        if (!authMessageLoaded) {
            authMessage = SessionUtil.consumeAuthMessage();
            authMessageLoaded = true;
        }
        return authMessage;
    }

    // ------------------------------------------------------ form properties

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
