package com.campuscomplaint.service;

import com.campuscomplaint.dao.UserDAO;
import com.campuscomplaint.model.User;
import com.campuscomplaint.util.InputValidator;
import com.campuscomplaint.util.PasswordUtil;
import com.campuscomplaint.util.ValidationException;

/**
 * Login and self-registration.
 *
 * Self-registration always creates a STUDENT account (students and faculty
 * who report complaints). Maintenance staff are created by the administrator,
 * and there is no way at all to create an administrator from the web pages.
 */
public class AuthService {

    /**
     * A valid hash of a random password. Checking a typed password against it
     * when the e-mail is unknown makes a failed login take as long as a real
     * one, so response times do not reveal which e-mail addresses exist.
     */
    private static final String DUMMY_HASH = PasswordUtil.hash("timing-equaliser-" + System.nanoTime());

    private final UserDAO userDAO = new UserDAO();

    /**
     * Checks the e-mail and password.
     *
     * @return the user (without the password hash), or null when the
     *         e-mail/password combination is wrong
     * @throws ValidationException when the account has been deactivated
     */
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            return null;
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            PasswordUtil.matches(password, DUMMY_HASH);
            return null;
        }
        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            return null;
        }
        if (!user.isActive()) {
            throw new ValidationException("This account has been deactivated. Please contact the complaint cell.");
        }
        user.setPasswordHash(null);   // never keep the hash in the session
        return user;
    }

    /** Creates a new student / faculty account. */
    public void register(String name, String email, String phone, String department,
                         String password, String confirmPassword) {
        User user = new User();
        user.setName(InputValidator.requireText(name, "Full name", 3, 100));
        user.setEmail(InputValidator.requireEmail(email));
        user.setPhone(InputValidator.optionalPhone(phone));
        user.setDepartment(InputValidator.optionalText(department, "Department", 100));
        InputValidator.requirePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("The two passwords do not match.");
        }
        if (userDAO.emailExists(user.getEmail(), 0)) {
            throw new ValidationException("An account with this e-mail address already exists.");
        }
        user.setRole(User.ROLE_STUDENT);   // fixed: the form can never create staff or admin accounts
        user.setPasswordHash(PasswordUtil.hash(password));
        userDAO.insert(user);
    }
}
