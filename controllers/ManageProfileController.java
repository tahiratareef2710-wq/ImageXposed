package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.User;
import java.util.Map;

/**
 * Step 4.1 — User Management
 * Handles viewing and updating a user's profile:
 *   1. Retrieve current profile data from DatabaseHandler.
 *   2. Validate the changed fields.
 *   3. Persist the updates.
 *
 * Sequence diagram flow:
 *   ProfileView → ManageProfileController
 *     → DatabaseHandler.lookupUser()
 *     → User.validate()
 *     → DatabaseHandler.updateUser()
 *     → DisplayService.displayCurrentProfile()
 */
public class ManageProfileController extends BaseController {

    private String lastError = "";

    public ManageProfileController() {
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
     * Retrieves current profile data for the given user.
     *
     * @param userId the username whose profile to load
     * @return the User entity, or null if not found
     */
    public User retrieveProfileData(String userId) {
        User user = dbHandler.lookupUser(userId);
        if (user == null) {
            lastError = "User not found: " + userId;
        } else {
            displayService.displayCurrentProfile(user.toMap());
        }
        return user;
    }

    /**
     * Validates and applies profile changes for a user.
     *
     * @param userId        the username to update
     * @param email         new email (empty string = no change)
     * @param newPassword   new plain-text password (empty = no change)
     * @return true on success, false on validation failure
     */
    public boolean saveUpdatedProfile(String userId, String email,
                                      String newPassword) {
        lastError = "";

        // Validate email if provided
        if (!email.isEmpty() && !email.contains("@")) {
            lastError = "Invalid email address.";
            return false;
        }

        String passHash = newPassword.isEmpty()
            ? null
            : User.hashPassword(newPassword);

        dbHandler.updateUser(userId,
            email.isEmpty() ? null : email,
            passHash);
        return true;
    }

    /**
     * Overload accepting an updatedFields Map (for future extensibility).
     *
     * @param userId        the username to update
     * @param updatedFields map with optional keys "email", "password"
     * @return true on success
     */
    public boolean saveUpdatedProfile(String userId,
                                      Map<String, String> updatedFields) {
        String email    = updatedFields.getOrDefault("email", "");
        String password = updatedFields.getOrDefault("password", "");
        return saveUpdatedProfile(userId, email, password);
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() { return lastError; }
}
