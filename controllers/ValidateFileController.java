package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.forensics.FileValidator;
import displayPackage.forensics.FileValidator.FullValidationResult;
import displayPackage.forensics.FileValidator.ValidationCheck;
import displayPackage.models.Image;
import displayPackage.models.Scan;
import displayPackage.services.NotificationService;

/**
 * File Validation Pipeline — now backed by real FileValidator checks.
 * Orchestrates four sequential checks on an uploaded image:
 *   1. Magic number / signature check.
 *   2. Corruption / integrity check (full decode).
 *   3. Format consistency check (extension vs magic bytes).
 *   4. Size & dimensions check.
 *
 * NOTE: This controller does NOT persist a Scan record.
 * Validation results are returned to the caller (WorkflowView).
 * Only SaveScanController (Step 5) writes to the Scans table.
 *
 * Sequence diagram flow:
 *   ValidateView → ValidateFileController
 *     → FileValidator.checkMagicNumber()
 *     → FileValidator.checkCorruption()
 *     → FileValidator.checkFormatConsistency()
 *     → FileValidator.checkSizeAndDimensions()
 *     → NotificationService.notifyValidationResult()
 */
public class ValidateFileController extends BaseController {

    private final NotificationService notificationService;
    private String lastError = "";

    /** Stores the validation result string after initiating validation. */
    private String validationResult = "";

    public ValidateFileController() {
        super();
        this.notificationService = NotificationService.getInstance();
    }

    @Override
    public void handleRequest() { }

    @Override
    public boolean validate() {
        return "PASSED".equals(validationResult);
    }

    // ── Use-case methods ───────────────────────────────────────────────────

    /**
     * Runs all four real validation checks on the given image and returns
     * the results. Does NOT write anything to the Scans table — that is
     * handled exclusively by SaveScanController at Step 5.
     *
     * @param imageId the ID of the image to validate
     * @return a ValidationResult DTO with per-check statuses and detail strings
     */
    public ValidationResult initiateValidation(String imageId) {
        lastError = "";
        ValidationResult result = new ValidationResult();

        Image img = dbHandler.getImage(imageId);
        if (img == null) {
            lastError = "Image not found: " + imageId;
            result.overallPassed = false;
            return result;
        }

        String filePath  = img.getFilePath();
        String extension = img.getFileFormat();

        // Run all 4 real checks via FileValidator
        FullValidationResult fvr = FileValidator.validateAll(filePath, extension);

        // Map to our DTO
        result.corruptionCheck  = fvr.corruption.passed;
        result.corruptionDetail = fvr.corruption.detail;

        result.formatCheck  = fvr.formatConsistency.passed;
        result.formatDetail = fvr.formatConsistency.detail;

        result.sizeCheck  = fvr.sizeDimensions.passed;
        result.sizeDetail = fvr.sizeDimensions.detail;

        result.magicCheck  = fvr.magicNumber.passed;
        result.magicDetail = fvr.magicNumber.detail;

        result.overallPassed = fvr.allPassed;

        // Keep the result string in memory for downstream use (e.g. Save step)
        validationResult = result.overallPassed ? "PASSED" : "FAILED";

        notificationService.notifyValidationResult(validationResult);
        return result;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError()        { return lastError; }
    public String getValidationResult() { return validationResult; }

    // ── Inner DTO ──────────────────────────────────────────────────────────

    /**
     * Carries per-check results AND detail strings back to the view.
     */
    public static class ValidationResult {
        public boolean corruptionCheck  = false;
        public String  corruptionDetail = "";

        public boolean formatCheck  = false;
        public String  formatDetail = "";

        public boolean sizeCheck  = false;
        public String  sizeDetail = "";

        public boolean magicCheck  = false;
        public String  magicDetail = "";

        public boolean overallPassed = false;

        public String corruptionStatus() { return corruptionCheck ? "\u2713 Passed" : "\u2717 Failed"; }
        public String formatStatus()     { return formatCheck     ? "\u2713 Passed" : "\u2717 Failed"; }
        public String sizeStatus()       { return sizeCheck       ? "\u2713 Passed" : "\u2717 Failed"; }
        public String magicStatus()      { return magicCheck      ? "\u2713 Passed" : "\u2717 Failed"; }
    }
}
