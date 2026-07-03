package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Scan;
import java.util.Map;

/**
 * Step 4.4 — Save Scan Record
 * Packages and persists the final scan record after analysis.
 *
 * Sequence diagram flow:
 *   ScanRecordView → SaveScanController
 *     → Scan.packageScanData()
 *     → Scan.generateScanId()
 *     → Scan.stampTimestamp()
 *     → DatabaseHandler.persistScan()
 *     → DisplayService notifies success
 */
public class SaveScanController extends BaseController {

    private String lastError  = "";
    private String savedScanId = "";

    public SaveScanController() {
        super();
    }

    @Override
    public void handleRequest() { }

    @Override
    public boolean validate() {
        return !savedScanId.isEmpty();
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Packages and persists the scan record.
     *
     * @param userId   the logged-in user's username
     * @param imageId  the image that was scanned
     * @param scanData map of analysis results to include
     * @return the generated scan ID on success, empty string on failure
     */
    public String saveScanRecord(String userId, String imageId,
                                  Map<String, Object> scanData) {
        lastError   = "";
        savedScanId = "";

        if (userId == null || userId.trim().isEmpty()) {
            lastError = "User ID is required.";
            return "";
        }
        if (imageId == null || imageId.trim().isEmpty()) {
            lastError = "Image ID is required.";
            return "";
        }

        // Build the Scan entity using its own business methods
        Scan scan = new Scan(userId, imageId);
        if (scanData != null) scan.setScanData(scanData);

        scan.generateScanId();
        scan.stampTimestamp();

        // Validate before persisting
        if (!scan.validate()) {
            lastError = "Scan validation failed — missing required fields.";
            return "";
        }

        dbHandler.persistScan(scan);
        savedScanId = scan.getId();

        displaySuccess(savedScanId);
        return savedScanId;
    }

    /**
     * Notifies the user that the scan was saved successfully.
     *
     * @param recordId the ID that was assigned to this scan
     */
    public void displaySuccess(String recordId) {
        System.out.println("[SaveScanController] Scan saved: " + recordId);
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError()   { return lastError; }
    public String getSavedScanId() { return savedScanId; }
}
