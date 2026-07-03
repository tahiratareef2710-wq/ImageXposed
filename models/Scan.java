package displayPackage.models;

import displayPackage.core.BaseEntity;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a scan record in the ImageXposed system.
 * A Scan ties together a User, an Image, analysis results,
 * and a timestamp.
 *
 * Relationships (from class diagram):
 *   - Scan is produced by an Image (1 Image produces 1..* Scans)
 *   - Scan generates 0..1 Report
 */
public class Scan extends BaseEntity {

    private String userId;
    private String imageId;
    private Map<String, Object> scanData;
    private Date timestamp;
    private String validationResult;

    public Scan() {
        super();
        this.scanData = new HashMap<>();
        this.timestamp = new Date();
        this.validationResult = "";
    }

    public Scan(String userId, String imageId) {
        super();
        this.userId = userId;
        this.imageId = imageId;
        this.scanData = new HashMap<>();
        this.timestamp = new Date();
        this.validationResult = "";
    }

    // ── Business Logic ─────────────────────────────────────────────────────

    @Override
    public boolean validate() {
        return userId != null && !userId.trim().isEmpty()
            && imageId != null && !imageId.trim().isEmpty();
    }

    /**
     * Packages all scan-related data into a single Map for persistence.
     *
     * @return a map containing all scan fields
     */
    public Map<String, Object> packageScanData() {
        Map<String, Object> pkg = new HashMap<>(scanData);
        pkg.put("scanId", getId());
        pkg.put("userId", userId);
        pkg.put("imageId", imageId);
        pkg.put("timestamp", timestamp);
        pkg.put("validationResult", validationResult);
        return pkg;
    }

    /**
     * Generates a unique scan ID using the full current timestamp
     * plus a random suffix to guarantee no collisions.
     *
     * @return the generated scan ID string
     */
    public String generateScanId() {
        String scanId = "SCAN-" + System.currentTimeMillis()
                + "-" + (int)(Math.random() * 9000 + 1000);
        setId(scanId);
        return scanId;
    }

    /**
     * Stamps the current date/time onto this scan record.
     *
     * @return the timestamp that was set
     */
    public Date stampTimestamp() {
        this.timestamp = new Date();
        touch();
        return this.timestamp;
    }

    // ── Map Serialisation ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("userId", userId);
        map.put("imageId", imageId);
        map.put("scanData", scanData);
        map.put("timestamp", timestamp);
        map.put("validationResult", validationResult);
        return map;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getUserId()        { return userId; }
    public void setUserId(String u)  { this.userId = u; touch(); }

    public String getImageId()        { return imageId; }
    public void setImageId(String i)  { this.imageId = i; touch(); }

    public Map<String, Object> getScanData()             { return scanData; }
    public void setScanData(Map<String, Object> data)    { this.scanData = data; touch(); }
    public void addScanData(String key, Object value)    { this.scanData.put(key, value); touch(); }

    public Date getTimestamp()         { return timestamp; }
    public void setTimestamp(Date t)   { this.timestamp = t; touch(); }

    public String getValidationResult()        { return validationResult; }
    public void setValidationResult(String vr) { this.validationResult = vr; touch(); }
}
