package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Image;
import displayPackage.services.NotificationService;
import java.io.File;

/**
 * Step 4.2 — Image Upload Pipeline
 * Handles the image upload use-case:
 *   1. Create an Image entity from the selected file.
 *   2. Validate file format + assign a tracking ID.
 *   3. Record the image entry in DatabaseHandler.
 *   4. Notify the user of success.
 *
 * Sequence diagram flow:
 *   UploadView → UploadImageController
 *     → Image.create()
 *     → Image.validateFileFormat()
 *     → Image.assignTrackingID()
 *     → DatabaseHandler.recordImageEntry()
 *     → DisplayService.displayUploadSuccess()
 *     → NotificationService.notifyUploadSuccess()
 */
public class UploadImageController extends BaseController {

    private final NotificationService notificationService;
    private String lastError = "";

    /** The Image entity produced by this controller for downstream steps. */
    private Image uploadedImage = null;

    public UploadImageController() {
        super();
        this.notificationService = NotificationService.getInstance();
    }

    @Override
    public void handleRequest() { }

    @Override
    public boolean validate() {
        return uploadedImage != null && lastError.isEmpty();
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Full upload flow: build → validate → persist → notify.
     *
     * @param file the File selected by the user
     * @return the created Image entity on success, null on failure
     */
    public Image handleUpload(File file) {
        lastError = "";

        if (file == null || !file.exists()) {
            lastError = "Selected file does not exist.";
            return null;
        }

        // 1. Derive file metadata
        String fileName   = file.getName();
        String extension  = getExtension(fileName);
        long   fileSize   = file.length();
        String filePath   = file.getAbsolutePath();

        // 2. Create Image entity
        Image img = new Image(fileName, extension, fileSize, filePath);

        // 3. Validate format
        if (!img.validateFileFormat()) {
            lastError = "Unsupported file format: " + extension
                + ". Use PNG, JPG, BMP, or TIFF.";
            return null;
        }

        // 4. Assign tracking ID
        img.assignTrackingID();

        // 5. Persist the ACTUAL image entity (not a blank one)
        uploadedImage = img;
        dbHandler.recordImageEntry(img);

        // 6. Notify
        displayService.displayUploadSuccess(img.getId());
        notificationService.notifyUploadSuccess(img.getId());

        return img;
    }

    /**
     * Displays a success notification (matches sequence diagram step 10–11).
     */
    public void displayUploadSuccess() {
        if (uploadedImage != null) {
            displayService.displayUploadSuccess(uploadedImage.getId());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot + 1) : "";
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError()       { return lastError; }
    public Image  getUploadedImage()   { return uploadedImage; }
}
