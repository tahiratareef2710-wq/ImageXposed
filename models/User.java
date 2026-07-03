package displayPackage.models;

import displayPackage.core.BaseEntity;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Represents a registered user in the ImageXposed system.
 *
 * Relationships (from class diagram):
 *   - User owns 0..* Images
 *   - User conducts 0..* Scans
 *   - User submits 0..* Feedback
 */
public class User extends BaseEntity {

    private String username;
    private String email;
    private String passwordHash;
    private String profilePicture;

    public User() {
        super();
    }

    public User(String username, String email, String passwordHash) {
        super();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.profilePicture = "";
    }

    public User(String id, String username, String email, String passwordHash) {
        super(id);
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.profilePicture = "";
    }

    // ── Business Logic ─────────────────────────────────────────────────────

    @Override
    public boolean validate() {
        return username != null && !username.trim().isEmpty()
            && email != null && !email.trim().isEmpty()
            && passwordHash != null && !passwordHash.trim().isEmpty();
    }

    /**
     * Checks whether the provided plain-text password matches the stored hash.
     * For now uses simple string comparison; can be upgraded to BCrypt later.
     *
     * @param password the plain-text password to check
     * @return true if credentials are valid
     */
    public boolean validateCredentials(String password) {
        if (password == null) return false;
        // Simple hash for now — replace with BCrypt.checkpw() for production
        return passwordHash.equals(hashPassword(password));
    }

    /**
     * Validates registration form data.
     *
     * @param username the username to validate
     * @param email    the email to validate
     * @return true if both fields are non-empty and email contains '@'
     */
    public static boolean validateFormData(String username, String email) {
        return username != null && !username.trim().isEmpty()
            && email != null && email.contains("@");
    }

    /**
     * Standard SHA-256 password hashing utility.
     *
     * @param password plain-text password
     * @return hashed password string
     */
    public static String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    // ── Map Serialisation ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("username", username);
        map.put("email", email);
        map.put("passwordHash", passwordHash);
        map.put("profilePicture", profilePicture);
        return map;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getUsername()       { return username; }
    public void setUsername(String u) { this.username = u; touch(); }

    public String getEmail()        { return email; }
    public void setEmail(String e)  { this.email = e; touch(); }

    public String getPasswordHash()        { return passwordHash; }
    public void setPasswordHash(String ph) { this.passwordHash = ph; touch(); }

    public String getProfilePicture()       { return profilePicture; }
    public void setProfilePicture(String p) { this.profilePicture = p; touch(); }
}
