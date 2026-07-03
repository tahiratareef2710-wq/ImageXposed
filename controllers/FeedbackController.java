package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Feedback;

/**
 * Step 4.5 — User Feedback
 * Handles the full feedback submission flow:
 *   1. Validate category, subject, and description.
 *   2. Create a Feedback entity.
 *   3. Validate the entity.
 *   4. Persist via DatabaseHandler.
 *
 * Sequence diagram flow:
 *   FeedbackView → FeedbackController
 *     → Feedback.createFeedback(userId, category, subject)
 *     → Feedback.validate()
 *     → DatabaseHandler.saveFeedback()
 */
public class FeedbackController extends BaseController {

    private String lastError = "";

    public FeedbackController() {
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
     * Submits feedback from a user.
     *
     * @param userId      the logged-in user's ID / username
     * @param category    the selected feedback category
     * @param subject     the subject line
     * @param description the detailed description
     * @return true if feedback was saved successfully, false otherwise
     */
    public boolean submitFeedback(String userId, String category,
                                   String subject, String description) {
        lastError = "";

        // 1. Input validation
        if (category == null || category.trim().isEmpty()) {
            lastError = "Please select a category.";
            return false;
        }
        if (subject == null || subject.trim().isEmpty()) {
            lastError = "Subject cannot be empty.";
            return false;
        }
        if (description == null || description.trim().isEmpty()) {
            lastError = "Description cannot be empty.";
            return false;
        }

        // 2. Build entity
        Feedback fb = new Feedback();
        fb.createFeedback(userId, category.trim(), subject.trim());

        // 3. Validate entity
        if (!fb.validate()) {
            lastError = "Feedback validation failed.";
            return false;
        }

        // 4. Persist
        saveFeedback(fb);
        return true;
    }

    /**
     * Persists the Feedback entity (sequence diagram step 6).
     *
     * @param feedback the fully built and validated Feedback entity
     */
    public void saveFeedback(Feedback feedback) {
        dbHandler.saveFeedback(feedback);
        System.out.println("[FeedbackController] Feedback saved: " + feedback.getId());
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() { return lastError; }
}
