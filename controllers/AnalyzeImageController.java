package displayPackage.controllers;

import displayPackage.core.BaseController;
import displayPackage.forensics.ELAResult;
import displayPackage.models.Image;
import displayPackage.models.Scan;
import java.util.Map;

/**
 * Step 4.3 — Image Analysis
 * Runs the full authenticity analysis pipeline on an image:
 *   1. Extract metadata.
 *   2. Generate MD5 hash.
 *   3. Generate SHA-256 hash.
 *   4. Perform ELA (Error Level Analysis).
 *   5. Determine verdict.
 *
 * NOTE: analyzeImage() and computeHashesOnly() do NOT persist scan records.
 * Persistence is handled exclusively by SaveScanController at Step 5.
 *
 * GoF: Strategy pattern is applied here — each analysis step (MD5, SHA256, ELA)
 * is currently a direct method call, but each can be extracted into an
 * AnalysisStrategy interface to allow easy swapping in future.
 *
 * Sequence diagram flow:
 *   AnalysisView → AnalyzeImageController
 *     → Image.extractMetadata()
 *     → Image.generateMD5Hash()
 *     → Image.generateSHA256Hash()
 *     → Image.performELA()
 */
public class AnalyzeImageController extends BaseController {

    private String lastError = "";

    public AnalyzeImageController() {
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
     * Runs the full analysis pipeline for a given image (single-image mode).
     * Computes metadata, MD5, SHA-256 and ELA, and returns all results.
     * Does NOT persist a Scan record — that is handled by SaveScanController.
     *
     * @param imageId the ID of the image to analyse
     * @return an AnalysisResult DTO for the view to display, or null on error
     */
    public AnalysisResult analyzeImage(String imageId) {
        lastError = "";
        Image img = dbHandler.getImage(imageId);
        if (img == null) {
            lastError = "Image not found: " + imageId;
            return null;
        }

        AnalysisResult result = new AnalysisResult();
        result.imageId = imageId;

        // Step 2-3: Real metadata extraction via ForensicsEngine
        result.metadata  = img.extractMetadata();

        // Step 4-5: Real MD5 hash via MessageDigest
        result.md5Hash   = img.generateMD5Hash();

        // Step 6-7: Real SHA-256 hash via MessageDigest
        result.sha256    = img.generateSHA256Hash();

        // Step 8-9: Real ELA via ForensicsEngine
        ELAResult ela    = img.performELA();
        result.elaResult = ela.summary;
        result.verdict   = ela.verdict;
        result.confidence= ela.confidence;

        // No auto-save — SaveScanController handles persistence at Step 5
        return result;
    }

    /**
     * Computes ONLY MD5 and SHA-256 hashes for an image — used in dual-image
     * comparison mode where the only goal is to check whether two files are
     * identical. Does NOT run ELA, does NOT persist a Scan record.
     *
     * @param imageId the DB ID of the image to hash
     * @return an AnalysisResult with only md5Hash and sha256 populated, or null on error
     */
    public AnalysisResult computeHashesOnly(String imageId) {
        lastError = "";
        Image img = dbHandler.getImage(imageId);
        if (img == null) {
            lastError = "Image not found: " + imageId;
            return null;
        }

        AnalysisResult result = new AnalysisResult();
        result.imageId = imageId;
        result.md5Hash = img.generateMD5Hash();
        result.sha256  = img.generateSHA256Hash();
        // verdict, confidence, elaResult, metadata intentionally left null — hash only
        return result;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String getLastError() { return lastError; }

    // ── Inner DTO ──────────────────────────────────────────────────────────

    /** Carries all analysis results back to the View layer. */
    public static class AnalysisResult {
        public String imageId;
        public String scanId;
        public String metadata;
        public String md5Hash;
        public String sha256;
        public String elaResult;
        public String verdict;
        public String confidence;
    }
}
