package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Report;
import displayPackage.models.Scan;
import java.util.Map;

/**
 * Step 4.5 — Report Generation
 * Fetches analysis results for a scan, compiles them into a
 * Report entity, and surfaces a preview for the view.
 *
 * Sequence diagram flow:
 *   ReportGenerationView → GenerateReportController
 *     → DatabaseHandler.fetchAnalysisResults(scanId)
 *     → Report.compileReport(scanData)
 *     → Report.applyLayout()
 *     → DisplayService.displayReportPreview(reportData)
 */
public class GenerateReportController extends BaseController {

    private String lastError   = "";
    private Report lastReport  = null;

    public GenerateReportController() {
        super();
    }

    @Override
    public void handleRequest() { }

    @Override
    public boolean validate() {
        return lastReport != null && lastError.isEmpty();
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Generates a forensic report for the given scan.
     *
     * @param scanId the scan to generate a report for
     * @return a ReportResult DTO containing the report entity + preview text
     */
    public ReportResult generateReport(String scanId) {
        lastError  = "";
        lastReport = null;

        if (scanId == null || scanId.trim().isEmpty()) {
            lastError = "Scan ID cannot be empty.";
            return null;
        }

        // Step 2-4: Fetch analysis data from DB
        Map<String, Object> scanData = fetchAnalysisResults(scanId);
        if (scanData.isEmpty()) {
            lastError = "No analysis data found for scan: " + scanId;
            return null;
        }

        // Step 6-8: Build Report entity
        Report report = new Report(scanId);
        report.compileReport(scanData);

        // Build preview text (handles both single-image and dual-image scans)
        String preview = buildPreviewText(report, scanId, scanData);
        report.getReportData().put("reportText", preview);

        // Persist the report
        dbHandler.persistReport(report);
        lastReport = report;

        displayService.displayReportPreview(report.toMap());

        ReportResult result = new ReportResult();
        result.report      = report;
        result.reportId    = report.getId();
        result.previewText = preview;
        return result;
    }

    /**
     * Fetches analysis results from the database (sequence diagram step 2–5).
     *
     * @param scanId the scan ID to look up
     * @return the scan's data map (never null; may be empty if not found)
     */
    public Map<String, Object> fetchAnalysisResults(String scanId) {
        return dbHandler.fetchAnalysisResults(scanId);
    }

    /**
     * Safe string getter — returns the map value as a String, or the
     * fallback if the key is absent OR the value is null.
     * Prevents NullPointerException when fields are absent in dual-mode scans.
     */
    private String safeGet(Map<String, Object> data, String key, String fallback) {
        Object val = data.get(key);
        return (val != null) ? val.toString() : fallback;
    }

    /**
     * Builds a structured, professional plain-text report from forensic data.
     * Automatically adapts its layout based on the verdict:
     *   - MATCH / MISMATCH  → Hash Comparison report (dual-image mode)
     *   - anything else     → Full forensic report (single-image mode)
     */
    private String buildPreviewText(Report report, String scanId,
                                    Map<String, Object> data) {

        String verdict    = safeGet(data, "verdict",    "UNKNOWN");
        String conf       = safeGet(data, "confidence", "N/A");
        String md5        = safeGet(data, "md5Hash",    "N/A");
        String sha        = safeGet(data, "sha256",     "N/A");
        String ela        = safeGet(data, "elaResult",  null);   // null = not available
        String meta       = safeGet(data, "metadata",   null);   // null = not available

        boolean isDualMode = "MATCH".equals(verdict) || "MISMATCH".equals(verdict);

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================\n");
        sb.append("                   IMAGEXPOSED FORENSIC REPORT\n");
        sb.append("====================================================================\n\n");

        sb.append("[ REPORT DETAILS ]\n");
        sb.append("  Report ID       : ").append(report.getId()).append("\n");
        sb.append("  Scan ID         : ").append(scanId).append("\n");
        sb.append("  Generated On    : ").append(report.getGeneratedAt()).append("\n\n");

        sb.append("--------------------------------------------------------------------\n");

        if (isDualMode) {
            // ── Dual-image hash comparison report ─────────────────────────────
            sb.append("[ EXECUTIVE SUMMARY — IMAGE COMPARISON ]\n");
            sb.append("  Two images were compared using cryptographic hash functions.\n");
            sb.append("  No pixel-level or ELA analysis was performed.\n\n");
            sb.append("  COMPARISON RESULT : ").append(verdict).append("\n");
            sb.append("  CONFIDENCE        : ").append(conf).append("\n");
            sb.append("--------------------------------------------------------------------\n\n");

            sb.append("[ CRYPTOGRAPHIC HASH COMPARISON ]\n");
            sb.append("  Image A — MD5     : ").append(md5).append("\n");
            sb.append("  Image A — SHA-256 : ").append(sha).append("\n\n");
            sb.append("  Note: Hashes for Image B were compared in memory during analysis.\n");
            sb.append("  A ").append(verdict).append(" result means the two files are ");
            if ("MATCH".equals(verdict)) {
                sb.append("byte-for-byte identical.\n");
            } else {
                sb.append("different.\n");
            }
            sb.append("\n");

        } else {
            // ── Single-image full forensic report ─────────────────────────────
            sb.append("[ EXECUTIVE SUMMARY ]\n");
            sb.append("  The target image was subjected to a full forensic analysis pipeline\n");
            sb.append("  including integrity checks, structural analysis, and ELA processing.\n\n");
            sb.append("  FINAL VERDICT   : ").append(verdict).append("\n");
            sb.append("  CONFIDENCE      : ").append(conf).append("\n");
            sb.append("--------------------------------------------------------------------\n\n");

            sb.append("[ FILE INTEGRITY & HASHES ]\n");
            sb.append("  MD5 Checksum    : ").append(md5).append("\n");
            sb.append("  SHA-256 Hash    : ").append(sha).append("\n\n");

            sb.append("[ ERROR LEVEL ANALYSIS (ELA) ]\n");
            if (ela != null && !ela.isEmpty()) {
                // Split on sentence boundaries for clean formatting
                String[] elaLines = ela.split("(?<=[.!?])\\s*");
                for (String line : elaLines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) sb.append("  ").append(trimmed).append("\n");
                }
            } else {
                sb.append("  ELA data not available.\n");
            }
            sb.append("\n");

            sb.append("[ METADATA & EXIF EXTRACTION ]\n");
            if (meta != null && !meta.isEmpty()) {
                String[] metaLines = meta.split("\n");
                for (String mLine : metaLines) {
                    String trimmed = mLine.trim();
                    if (!trimmed.isEmpty()) sb.append("  ").append(trimmed).append("\n");
                }
            } else {
                sb.append("  Metadata not available.\n");
            }
            sb.append("\n");
        }

        sb.append("====================================================================\n");
        sb.append("  End of Report.\n");
        sb.append("  Generated automatically by the ImageXposed Forensics Engine.\n");
        sb.append("====================================================================\n");

        return sb.toString();
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError()  { return lastError; }
    public Report getLastReport() { return lastReport; }

    // ── Inner DTO ──────────────────────────────────────────────────────────
    public static class ReportResult {
        public Report report;
        public String reportId;
        public String previewText;
    }
}
