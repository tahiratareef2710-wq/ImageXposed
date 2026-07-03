package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.models.Report;
import displayPackage.services.NotificationService;
import java.io.File;
import java.util.Map;

/**
 * Step 4.5 — Report Export
 * Converts a generated report to the requested format
 * and writes it to the user-supplied destination path.
 *
 * GoF: Strategy — each export format (PDF, TXT, HTML, CSV) is
 * currently handled by a switch; the stub is designed so each branch
 * can be extracted into a concrete ExportStrategy later with zero
 * changes to this controller's public API.
 *
 * Sequence diagram flow:
 *   ExportView → ExportReportController
 *     → Report.convertToFormat(format)
 *     → Report.exportToFile(filePath)
 *     → DatabaseHandler.saveExportRecord()
 *     → NotificationService.notifyExportComplete()
 */
public class ExportReportController extends BaseController {

    private final NotificationService notificationService;
    private String lastError = "";

    public ExportReportController() {
        super();
        this.notificationService = NotificationService.getInstance();
    }

    @Override
    public void handleRequest() { }

    @Override
    public boolean validate() {
        return lastError.isEmpty();
    }

    // ── Use-case methods (match sequence diagram) ──────────────────────────

    /**
     * Exports the given report to a file in the requested format.
     *
     * @param reportId the ID of the report to export
     * @param format   the target format: "PDF", "TXT", "HTML", or "CSV"
     * @param destPath the destination folder or file path
     * @return the exported File on success, null on failure
     */
    public File exportReport(String reportId, String format, String destPath) {
        lastError = "";

        if (reportId == null || reportId.trim().isEmpty()) {
            lastError = "Report ID cannot be empty.";
            return null;
        }
        if (destPath == null || destPath.trim().isEmpty()) {
            lastError = "Please choose an export destination.";
            return null;
        }

        Report report = dbHandler.getReport(reportId);
        if (report == null) {
            lastError = "Report not found: " + reportId;
            return null;
        }

        // Inject structured scan data for advanced PDF layouts
        Map<String, Object> scanData = dbHandler.fetchAnalysisResults(report.getScanId());
        if (scanData != null) {
            report.getReportData().putAll(scanData);
        }

        // Convert and export via Report entity methods
        File exported = convertToFormat(report, format);
        report.exportToFile(destPath + File.separator + exported.getName());

        // Persist export record
        report.setExportFormat(format);
        dbHandler.persistReport(report);

        // Notify user
        String fullPath = destPath + File.separator + exported.getName();
        notificationService.notifyExportComplete(fullPath);
        return exported;
    }

    /**
     * Delegates format conversion to the Report entity.
     * (This is where concrete ExportStrategy implementations
     *  would be plugged in when the GoF Strategy is applied.)
     *
     * @param report the Report entity to convert
     * @param format the target format string
     * @return a File object representing the converted report
     */
    public File convertToFormat(Report report, String format) {
        // TODO: Replace switch with ExportStrategyFactory.get(format).execute(report)
        switch (format.toUpperCase()) {
            case "PDF":
            case "HTML":
            case "CSV":
            case "TXT":
            default:
                return report.convertToFormat(format);
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() { return lastError; }
}
