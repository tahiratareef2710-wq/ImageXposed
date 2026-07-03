package displayPackage.models;

import displayPackage.core.BaseEntity;
import java.util.Date;
import java.util.Map;

/**
 * Represents user feedback submitted through the application.
 *
 * Relationships (from class diagram):
 *   - Feedback is submitted by a User (1 User submits 0..* Feedback)
 */
public class Feedback extends BaseEntity {

    private String userId;
    private String category;
    private String subject;
    private Date submittedAt;

    public Feedback() {
        super();
        this.submittedAt = new Date();
    }

    public Feedback(String userId, String category, String subject) {
        super();
        this.userId = userId;
        this.category = category;
        this.subject = subject;
        this.submittedAt = new Date();
    }

    // ── Business Logic ─────────────────────────────────────────────────────

    @Override
    public boolean validate() {
        return userId != null && !userId.trim().isEmpty()
            && category != null && !category.trim().isEmpty()
            && subject != null && !subject.trim().isEmpty();
    }

    /**
     * Factory-style method to create and populate a Feedback entity.
     *
     * @param userId   the ID of the submitting user
     * @param category the feedback category
     * @param subject  the feedback subject line
     */
    public void createFeedback(String userId, String category, String subject) {
        this.userId = userId;
        this.category = category;
        this.subject = subject;
        this.submittedAt = new Date();
        touch();
    }

    // ── Map Serialisation ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("userId", userId);
        map.put("category", category);
        map.put("subject", subject);
        map.put("submittedAt", submittedAt);
        return map;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getUserId()        { return userId; }
    public void setUserId(String u)  { this.userId = u; touch(); }

    public String getCategory()        { return category; }
    public void setCategory(String c)  { this.category = c; touch(); }

    public String getSubject()        { return subject; }
    public void setSubject(String s)  { this.subject = s; touch(); }

    public Date getSubmittedAt()         { return submittedAt; }
    public void setSubmittedAt(Date d)   { this.submittedAt = d; touch(); }
}
