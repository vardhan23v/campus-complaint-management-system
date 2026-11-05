package com.campuscomplaint.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.campuscomplaint.model.User;
import com.campuscomplaint.service.ComplaintService;
import com.campuscomplaint.util.DataAccessException;
import com.campuscomplaint.util.FileUploadUtil;
import com.campuscomplaint.util.SessionUtil;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams the image attached to a complaint: /attachment?id=12
 *
 * The upload folder lies outside the web application, so this servlet is
 * the only way to reach a file. It serves the image only to the reporter,
 * the assigned staff member and the administrator; everybody else gets a
 * 404, exactly as if the file did not exist.
 */
public class AttachmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(AttachmentServlet.class.getName());

    private final ComplaintService complaintService = new ComplaintService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = SessionUtil.getCurrentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }

        int complaintId;
        try {
            complaintId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path file;
        String storedName;
        try {
            file = complaintService.getAttachment(complaintId, user);
            storedName = file == null ? null : complaintService.getAttachmentName(complaintId, user);
        } catch (DataAccessException e) {
            LOG.log(Level.WARNING, "Attachment lookup failed", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(FileUploadUtil.contentTypeOf(storedName));
        response.setContentLengthLong(Files.size(file));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Disposition", "inline; filename=\"complaint-" + complaintId
                + (storedName.endsWith(".png") ? ".png" : ".jpg") + "\"");
        response.setHeader("Cache-Control", "private, max-age=300");
        response.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'");
        Files.copy(file, response.getOutputStream());
    }
}
