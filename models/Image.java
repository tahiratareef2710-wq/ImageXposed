package displayPackage.models;

import displayPackage.core.BaseEntity;
import displayPackage.forensics.ELAResult;
import displayPackage.forensics.ForensicsEngine;
import displayPackage.factory.AnalysisFactory;
import displayPackage.factory.Analysis;
import java.util.Map;

/**
 * Represents an uploaded image in the ImageXposed system.
 * Handles file metadata, tracking IDs, hash generation, and ELA analysis.
 *
 * Relationships (from class diagram):
 *   - Image is owned by a User (1 User owns 0..* Images)
 *   - Image produces 1..* Scans
 */
public class Image extends BaseEntity {

    private String fileName;
    private String fileFormat;
    private long fileSize;
    private String filePath;
    private String trackingId;

    public Image() {
        super();
    }

    public Image(String fileName, String fileFormat, long fileSize, String filePath) {
        super();
        this.fileName = fileName;
        this.fileFormat = fileFormat;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.trackingId = "";
    }

    // ── Business Logic ─────────────────────────────────────────────────────

    @Override
    public boolean validate() {
        return fileName != null && !fileName.trim().isEmpty()
                && fileFormat != null && !fileFormat.trim().isEmpty()
                && fileSize > 0
                && filePath != null && !filePath.trim().isEmpty();
    }

    /**
     * Validates that the file format is one of the accepted image types.
     *
     * @return true if format is PNG, JPG, JPEG, BMP, or TIFF
     */
    public boolean validateFileFormat() {
        if (fileFormat == null) return false;
        String fmt = fileFormat.toLowerCase();
        return fmt.equals("png") || fmt.equals("jpg") || fmt.equals("jpeg")
                || fmt.equals("bmp") || fmt.equals("tiff");
    }

    /**
     * Assigns a unique tracking ID to this image.
     *
     * @return the generated tracking ID
     */
    public String assignTrackingID() {
        this.trackingId = "TRK-" + System.currentTimeMillis();
        touch();
        return this.trackingId;
    }

    /**
     * Generates a real MD5 hash of the image file on disk.
     * Uses java.security.MessageDigest via ForensicsEngine.
     *
     * @return lowercase hex-encoded MD5 string
     */
    public String generateMD5Hash() {
        Analysis analysis = AnalysisFactory.createAnalysis("md5", filePath);
        return (String) analysis.execute();
    }

    public String generateSHA256Hash() {
        Analysis analysis = AnalysisFactory.createAnalysis("sha256", filePath);
        return (String) analysis.execute();
    }

    public ELAResult performELA() {
        Analysis analysis = AnalysisFactory.createAnalysis("ela", filePath);
        return (ELAResult) analysis.execute();
    }

    /**
     * Extracts real image metadata (dimensions, format, EXIF fields)
     * using javax.imageio.metadata — pure Java, no external libs.
     *
     * @return formatted metadata string
     */
    public String extractMetadata() {
        if (filePath == null || filePath.isEmpty()) return "[no file path]";
        return ForensicsEngine.extractMetadata(filePath);
    }

    // ── Map Serialisation ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("fileName", fileName);
        map.put("fileFormat", fileFormat);
        map.put("fileSize", fileSize);
        map.put("filePath", filePath);
        map.put("trackingId", trackingId);
        return map;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getFileName()        { return fileName; }
    public void setFileName(String fn) { this.fileName = fn; touch(); }

    public String getFileFormat()       { return fileFormat; }
    public void setFileFormat(String f) { this.fileFormat = f; touch(); }

    public long getFileSize()        { return fileSize; }
    public void setFileSize(long fs) { this.fileSize = fs; touch(); }

    public String getFilePath()       { return filePath; }
    public void setFilePath(String p) { this.filePath = p; touch(); }

    public String getTrackingId()       { return trackingId; }
    public void setTrackingId(String t) { this.trackingId = t; touch(); }
}