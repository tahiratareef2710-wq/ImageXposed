package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Scan;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Step 4.4 — Scan History Management
 * Loads the scan history for a user and supports
 * deleting individual scan records.
 *
 * Sequence diagram flow:
 * ScanHistoryView → ManageScanHistoryController
 * → DatabaseHandler.loadScanHistory(userId)
 * → DatabaseHandler.deleteScan(scanId) [on delete]
 */
public class ManageScanHistoryController extends BaseController {

    private String lastError = "";

    /** Formatter used to display timestamps in a human-readable form. */
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");

    public ManageScanHistoryController() {
        super();
    }

    @Override
    public void handleRequest() {
    }

    @Override
    public boolean validate() {
        return lastError.isEmpty();
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Loads all scan records for a given user.
     *
     * @param userId the user's username
     * @return list of Scan entities (may be empty if none found)
     */
    public List<Scan> loadScanHistory(String userId) {
        lastError = "";
        if (userId == null || userId.trim().isEmpty()) {
            lastError = "User ID cannot be empty.";
            return List.of();
        }
        return dbHandler.loadScanHistory(userId);
    }

    /**
     * Deletes a scan record by its ID.
     *
     * @param scanId the scan ID to delete
     * @return true if the scan existed and was deleted, false otherwise
     */
    public boolean deleteScanRecord(String scanId) {
        lastError = "";
        if (scanId == null || scanId.trim().isEmpty()) {
            lastError = "Scan ID cannot be empty.";
            return false;
        }
        Scan existing = dbHandler.getScan(scanId);
        if (existing == null) {
            lastError = "Scan not found: " + scanId;
            return false;
        }
        dbHandler.deleteScan(scanId);
        return true;
    }

    /**
     * Converts a list of Scans into a 2D Object array
     * suitable for direct use as a JTable model's data source.
     * The "Date" column is formatted via SimpleDateFormat so the
     * exact DB timestamp is displayed, NOT the current time.
     *
     * @param scans the list of Scan entities
     * @return 2D array: each row is {scanId, imageId, date, verdict, confidence}
     */
    public Object[][] toTableData(List<Scan> scans) {
        Object[][] data = new Object[scans.size()][5];
        for (int i = 0; i < scans.size(); i++) {
            Scan s = scans.get(i);
            data[i][0] = s.getId();
            data[i][1] = s.getImageId();
            // Format the timestamp that was read from the DB (not new Date())
            data[i][2] = s.getTimestamp() != null
                    ? DATE_FMT.format(s.getTimestamp())
                    : "\u2014";
            Object verdict = s.getScanData().get("verdict");
            data[i][3] = verdict != null ? verdict.toString() : "\u2014";
            Object conf = s.getScanData().get("confidence");
            data[i][4] = conf != null ? conf.toString() : "\u2014";
        }
        return data;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() {
        return lastError;
    }
}
