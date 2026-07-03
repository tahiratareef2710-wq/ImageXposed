package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.User;

/**
 * Step 4.1 — Authentication
 * Handles the login use-case:
 *   1. Lookup user record by username.
 *   2. Validate the password against the stored hash.
 *   3. On success, hand off navigation to DisplayService.
 *
 * Sequence diagram flow:
 *   LoginView → LoginController → DatabaseHandler → User.validateCredentials()
 *             → DisplayService.displayDashboard()
 */
public class LoginController extends BaseController {

    /** Stores the last error message for the view to display. */
    private String lastError = "";

    /** The authenticated user, populated on successful login. */
    private User authenticatedUser = null;

    public LoginController() {
        super(); // wires dbHandler + displayService
    }

    // ── BaseController contract ────────────────────────────────────────────

    @Override
    public void handleRequest() {
        // Called by the view when the login button is pressed.
        // The view should call submitCredentials() directly for clarity.
    }

    @Override
    public boolean validate() {
        return authenticatedUser != null;
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Looks up a user record by username.
     *
     * @param username the entered username
     * @return the User entity if found, null otherwise
     */
    public User lookupUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            lastError = "Username cannot be empty.";
            return null;
        }
        User user = dbHandler.lookupUser(username.trim());
        if (user == null) {
            lastError = "No account found for: " + username;
        }
        return user;
    }

    /**
     * Validates credentials and completes the login flow.
     *
     * @param username the entered username
     * @param password the entered plain-text password
     * @return true if login is successful, false otherwise
     */
    public boolean submitCredentials(String username, String password) {
        lastError = "";
        if (username == null || username.trim().isEmpty()) {
            lastError = "Please enter your username.";
            return false;
        }
        if (password == null || password.isEmpty()) {
            lastError = "Please enter your password.";
            return false;
        }

        User user = lookupUser(username);
        if (user == null) return false;

        if (!user.validateCredentials(password)) {
            lastError = "Incorrect password.";
            return false;
        }

        authenticatedUser = user;
        displayPackage.core.ActiveSession.getInstance().login(user);
        displayService.displayDashboard();
        return true;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public String getLastError()          { return lastError; }
    public User getAuthenticatedUser()    { return authenticatedUser; }
}
