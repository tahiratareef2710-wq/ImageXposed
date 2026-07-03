package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.User;

/**
 * Step 4.1 — Authentication
 * Handles the user registration use-case:
 *   1. Check username/email uniqueness via DatabaseHandler.
 *   2. Validate the form fields.
 *   3. Hash the password and create the User entity.
 *   4. Persist the new user.
 *
 * Sequence diagram flow:
 *   RegistrationView → RegistrationController
 *     → DatabaseHandler.checkUniqueness()
 *     → User.validateFormData()
 *     → DatabaseHandler.saveUser()
 *     → DisplayService.displayDashboard()
 */
public class RegistrationController extends BaseController {

    private String lastError = "";

    public RegistrationController() {
        super();
    }

    @Override
    public void handleRequest() { }

    @Override
    public boolean validate() {
        return lastError.isEmpty();
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Checks whether the username and email are both available.
     *
     * @param username the desired username
     * @param email    the supplied email address
     * @return true if both are unique (not already registered)
     */
    public boolean checkUniqueness(String username, String email) {
        boolean unique = dbHandler.checkUniqueness(username, email);
        if (!unique) {
            lastError = "Username or email is already registered.";
        }
        return unique;
    }

    /**
     * Full registration flow: validate → uniqueness check → create → save.
     *
     * @param username        the desired username
     * @param email           the user's email address
     * @param password        the plain-text password
     * @param confirmPassword the repeated password (must match)
     * @return true if registration succeeds, false otherwise
     */
    public boolean submitRegistration(String username, String email,
                                      String password, String confirmPassword) {
        lastError = "";

        // 1. Basic form validation
        if (!User.validateFormData(username, email)) {
            lastError = "Please enter a valid username and email.";
            return false;
        }
        if (password == null || password.length() < 6) {
            lastError = "Password must be at least 6 characters.";
            return false;
        }
        if (!password.equals(confirmPassword)) {
            lastError = "Passwords do not match.";
            return false;
        }

        // 2. Uniqueness check
        if (!checkUniqueness(username, email)) return false;

        // 3. Create + persist user
        String hash = User.hashPassword(password);
        User newUser = new User(username, email, hash);
        dbHandler.saveUser(newUser);

        displayService.displayDashboard();
        return true;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() { return lastError; }
}
