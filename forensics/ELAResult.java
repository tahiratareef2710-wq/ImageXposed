package displayPackage.forensics;

import java.awt.image.BufferedImage;

/**
 * Data Transfer Object carrying the full result of an
 * Error Level Analysis (ELA) run on a single image.
 *
 * Produced by ForensicsEngine.performELA() and consumed by
 * AnalyzeImageController to populate the analysis step in WorkflowView.
 */
public class ELAResult {

    // ── Raw ELA metrics ────────────────────────────────────────────────────

    /** Average per-channel pixel difference (0–255 scale, pre-amplification). */
    public double avgDifference;

    /** Maximum per-channel pixel difference seen in any single pixel. */
    public double maxDifference;

    /**
     * Percentage of pixels whose amplified difference exceeds the anomaly
     * threshold (0–100). Higher = more suspicious regions.
     */
    public double anomalyPercentage;

    /** JPEG quality level used for the re-compression comparison (e.g., 75). */
    public int recompressionQuality;

    // ── Visualisation ──────────────────────────────────────────────────────

    /**
     * Amplified difference image for visualisation.
     * Bright areas indicate regions where the image was likely edited.
     * May be null if the source image could not be read as a raster.
     */
    public BufferedImage elaImage;

    // ── Verdict ────────────────────────────────────────────────────────────

    /**
     * Human-readable verdict string:
     *   "AUTHENTIC", "POSSIBLY TAMPERED", or "TAMPERED"
     */
    public String verdict;

    /**
     * Confidence percentage string e.g. "91.4%".
     * Derived from the inverse of anomalyPercentage.
     */
    public String confidence;

    /** Human-readable summary of ELA findings for the UI. */
    public String summary;

    // ── Constructor ────────────────────────────────────────────────────────

    public ELAResult() {
        this.verdict    = "UNKNOWN";
        this.confidence = "0%";
        this.summary    = "Analysis not yet run.";
    }

    @Override
    public String toString() {
        return String.format(
            "ELAResult{verdict='%s', confidence='%s', avgDiff=%.2f, anomaly=%.1f%%}",
            verdict, confidence, avgDifference, anomalyPercentage);
    }
}
