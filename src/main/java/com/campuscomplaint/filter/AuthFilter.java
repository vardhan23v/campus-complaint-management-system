package com.campuscomplaint.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.campuscomplaint.model.User;
import com.campuscomplaint.util.SessionUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Server side gate in front of every *.xhtml page.
 *
 * Two checks happen here, before JSF even starts working:
 *   1. is somebody logged in?
 *   2. does that user have the role the page requires?
 *
 * The same rules are checked again inside the service classes before any data
 * is read or changed, because a page is never the place where security lives.
 */
public class AuthFilter implements Filter {

    /** Pages anybody may open. */
    private static final Set<String> PUBLIC_PAGES = Set.of("/index.xhtml", "/login.xhtml", "/register.xhtml", "/error.xhtml");

    /** Which role may open which page. Pages not listed need only a login. */
    private static final Map<String, String> PAGE_ROLES = Map.ofEntries(
            Map.entry("/student-dashboard.xhtml", User.ROLE_STUDENT),
            Map.entry("/submit-complaint.xhtml", User.ROLE_STUDENT),
            Map.entry("/my-complaints.xhtml", User.ROLE_STUDENT),
            Map.entry("/complaint-details.xhtml", User.ROLE_STUDENT),
            Map.entry("/feedback.xhtml", User.ROLE_STUDENT),

            Map.entry("/admin-dashboard.xhtml", User.ROLE_ADMIN),
            Map.entry("/admin-complaints.xhtml", User.ROLE_ADMIN),
            Map.entry("/complaint-review.xhtml", User.ROLE_ADMIN),
            Map.entry("/manage-users.xhtml", User.ROLE_ADMIN),
            Map.entry("/manage-staff.xhtml", User.ROLE_ADMIN),
            Map.entry("/manage-categories.xhtml", User.ROLE_ADMIN),
            Map.entry("/reports.xhtml", User.ROLE_ADMIN),

            Map.entry("/staff-dashboard.xhtml", User.ROLE_STAFF),
            Map.entry("/assigned-complaints.xhtml", User.ROLE_STAFF),
            Map.entry("/staff-complaint-details.xhtml", User.ROLE_STAFF));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Basic hardening headers for every page.
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Referrer-Policy", "same-origin");

        String contextPath = httpRequest.getContextPath();
        String path = httpRequest.getRequestURI().substring(contextPath.length());

        // Tomcat may append a path parameter such as ;jsessionid=... to the
        // URI. It is not part of the page name, so it is removed first.
        int pathParameter = path.indexOf(';');
        if (pathParameter != -1) {
            path = path.substring(0, pathParameter);
        }
        if (path.isEmpty() || "/".equals(path)) {
            path = "/index.xhtml";   // the welcome file
        }

        // Public pages, and JSF resources such as the stylesheet: no login needed.
        if (PUBLIC_PAGES.contains(path) || path.startsWith("/jakarta.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }

        User user = SessionUtil.getCurrentUser(httpRequest);
        if (user == null) {
            httpResponse.sendRedirect(contextPath + "/login.xhtml");
            return;
        }

        // Signed-in pages must never be shown from the browser cache after logout.
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");

        String requiredRole = PAGE_ROLES.get(path);
        if (requiredRole != null && !requiredRole.equals(user.getRole())) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.setAttribute(SessionUtil.AUTH_MESSAGE_KEY, "You are not allowed to open that page.");
            }
            httpResponse.sendRedirect(contextPath + "/" + homePage(user));
            return;
        }

        chain.doFilter(request, response);
    }

    /** The dashboard of the user's role. */
    public static String homePage(User user) {
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            return "admin-dashboard.xhtml";
        }
        if (User.ROLE_STAFF.equals(user.getRole())) {
            return "staff-dashboard.xhtml";
        }
        return "student-dashboard.xhtml";
    }
}
