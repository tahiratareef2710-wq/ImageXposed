package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Image;
import displayPackage.models.Scan;

/**
 * Step 4.3 — Image Viewing
 * Handles loading and displaying an image preview,
 * and logging the view event as a Scan record.
 *
 * Sequence diagram flow:
 *   ImageView → ViewImageController
 *     → Image.scaleImage()        (prepare for display)
 *     → Scan.logViewEvent()
 *     → DatabaseHandler.persist()
 *     → View receives imageData
 */
public class ViewImageController extends BaseController {

    private String lastError = "";

    public ViewImageController() {
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
     * Retrieves image metadata and prepares it for display.
     *
     * @param imageId the ID of the image to view
     * @return an ImageDisplayData DTO, or null if not found
     */
    public ImageDisplayData processImageForDisplay(String imageId) {
        lastError = "";
        Image img = dbHandler.getImage(imageId);
        if (img == null) {
            lastError = "Image not found: " + imageId;
            return null;
        }

        // Log the view event
        logViewEvent(imageId);

        // Build display-ready DTO
        ImageDisplayData data = new ImageDisplayData();
        data.imageId   = img.getId();
        data.fileName  = img.getFileName();
        data.format    = img.getFileFormat().toUpperCase();
        data.sizeKB    = img.getFileSize() / 1024;
        data.filePath  = img.getFilePath();
        data.infoLine  = String.format("Format: %s  |  Size: %.1f KB  |  ID: %s",
            data.format, (double) data.sizeKB, data.imageId);
        return data;
    }

    /**
     * Logs a view event for the image as a Scan record.
     *
     * @param imageId the image that was viewed
     */
    public void logViewEvent(String imageId) {
        String activeUser = displayPackage.core.ActiveSession.getInstance().getLoggedInUser() != null ? 
            displayPackage.core.ActiveSession.getInstance().getLoggedInUser().getUsername() : "admin";
        Scan viewScan = new Scan(activeUser, imageId);
        viewScan.generateScanId();
        viewScan.stampTimestamp();
        viewScan.addScanData("eventType", "VIEW");
        viewScan.addScanData("imageId", imageId);
        dbHandler.persistScan(viewScan);
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() { return lastError; }

    // ── Inner DTO ──────────────────────────────────────────────────────────

    /** Carries display-ready data back to the View layer. */
    public static class ImageDisplayData {
        public String imageId;
        public String fileName;
        public String format;
        public long   sizeKB;
        public String filePath;
        public String infoLine;
    }
}
