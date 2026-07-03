package displayPackage.forensics;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

/**
 * Core forensics engine implemented entirely in pure Java SE.
 * No external libraries required.
 *
 * Provides:
 *   1. Real MD5 hash computation   — java.security.MessageDigest
 *   2. Real SHA-256 hash computation — java.security.MessageDigest
 *   3. EXIF / image metadata extraction — javax.imageio + IIOMetadata DOM
 *   4. Error Level Analysis (ELA)  — javax.imageio + java.awt.image
 */
public class ForensicsEngine {

    // ── ELA tuning constants ───────────────────────────────────────────────

    /** JPEG quality used when re-compressing for ELA comparison (0–100). */
    private static final int ELA_QUALITY = 75;

    /**
     * Amplification factor applied to pixel differences so subtle
     * alterations become visible in the ELA output image.
     */
    private static final int ELA_AMPLIFY = 15;

    /**
     * Fraction of pixels that must exceed the anomaly threshold
     * before the verdict escalates from AUTHENTIC → POSSIBLY TAMPERED.
     */
    private static final double ANOMALY_THRESHOLD_PCT_LOW  = 2.0;

    /**
     * Fraction of pixels that must exceed the anomaly threshold
     * before the verdict escalates further to TAMPERED.
     */
    private static final double ANOMALY_THRESHOLD_PCT_HIGH = 8.0;

    /** Per-pixel average difference (pre-amplify) that counts as anomalous. */
    private static final double PIXEL_ANOMALY_THRESHOLD = 8.0;

    // ── Private constructor — static utility class ─────────────────────────
    private ForensicsEngine() { }

    // ══════════════════════════════════════════════════════════════════════
    // 1. MD5 HASH
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Computes the MD5 hash of the file at the given path.
     * Uses java.security.MessageDigest — no external libs.
     *
     * @param filePath absolute path to the image file
     * @return lowercase hex-encoded MD5 string, or error message on failure
     */
    public static String computeMD5(String filePath) {
        return computeHash(filePath, "MD5");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. SHA-256 HASH
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Computes the SHA-256 hash of the file at the given path.
     * Uses java.security.MessageDigest — no external libs.
     *
     * @param filePath absolute path to the image file
     * @return lowercase hex-encoded SHA-256 string, or error message on failure
     */
    public static String computeSHA256(String filePath) {
        return computeHash(filePath, "SHA-256");
    }

    /**
     * Shared implementation for any MessageDigest algorithm.
     *
     * @param filePath  path to the file to hash
     * @param algorithm "MD5" or "SHA-256"
     * @return hex string
     */
    private static String computeHash(String filePath, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            File file = new File(filePath);
            if (!file.exists()) return "[File not found]";

            try (InputStream fis = new BufferedInputStream(
                    new FileInputStream(file), 65536)) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            return "[Algorithm not available: " + algorithm + "]";
        } catch (IOException e) {
            return "[I/O error: " + e.getMessage() + "]";
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. METADATA EXTRACTION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Extracts image metadata using javax.imageio — pure Java SE.
     * Reads: width, height, colour model, bit depth,
     * and any JPEG EXIF / JFIF fields exposed via IIOMetadata DOM.
     *
     * @param filePath absolute path to the image file
     * @return a formatted metadata string for display
     */
    public static String extractMetadata(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return "File not found: " + filePath;

        StringBuilder sb = new StringBuilder();

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            if (iis == null) return "Cannot open image stream.";

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return "No image reader available for this format.";

            ImageReader reader = readers.next();
            reader.setInput(iis, true, false);

            // Basic geometry
            int width  = reader.getWidth(0);
            int height = reader.getHeight(0);
            String format = reader.getFormatName().toUpperCase();

            sb.append("File: ").append(file.getName()).append("\n");
            sb.append("Format: ").append(format).append("\n");
            sb.append("Dimensions: ").append(width).append(" x ").append(height).append(" px\n");
            sb.append("File size: ").append(formatFileSize(file.length())).append("\n");

            // Try to read the buffered image for additional type info
            try {
                BufferedImage img = reader.read(0);
                if (img != null) {
                    sb.append("Color model: ").append(colorModelName(img.getType())).append("\n");
                    sb.append("Bit depth: ").append(img.getColorModel().getPixelSize()).append(" bpp\n");
                }
            } catch (Exception ignored) { }

            // Parse native IIOMetadata DOM for EXIF / JFIF fields
            try {
                IIOMetadata meta = reader.getImageMetadata(0);
                if (meta != null) {
                    String nativeFmt = meta.getNativeMetadataFormatName();
                    Node root = meta.getAsTree(nativeFmt);
                    parseMetadataNode(root, sb, 0);
                }
            } catch (Exception ignored) {
                sb.append("(Extended metadata not available for this format)\n");
            }

            reader.dispose();

        } catch (IOException e) {
            sb.append("Metadata read error: ").append(e.getMessage());
        }

        return sb.toString().trim();
    }

    /**
     * Recursively traverses the IIOMetadata DOM tree, extracting
     * key-value pairs from node attributes.
     */
    private static void parseMetadataNode(Node node, StringBuilder sb, int depth) {
        if (depth > 4) return; // prevent going too deep
        NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            String nodeName = node.getNodeName();
            // Only print leaf-level nodes with actual data values
            if (attrs.getNamedItem("value") != null) {
                String key = nodeName;
                String val = attrs.getNamedItem("value").getNodeValue();
                // Skip internal/redundant entries
                if (!val.isEmpty() && val.length() < 120) {
                    sb.append(key).append(": ").append(val).append("\n");
                }
            }
        }
        Node child = node.getFirstChild();
        while (child != null) {
            parseMetadataNode(child, sb, depth + 1);
            child = child.getNextSibling();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. ERROR LEVEL ANALYSIS (ELA)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Performs Error Level Analysis on the image file using pure Java.
     *
     * Algorithm:
     *  1. Load the original image as a BufferedImage.
     *  2. Re-save it as JPEG at ELA_QUALITY into an in-memory byte buffer
     *     (using ImageIO + JPEGImageWriteParam — no temp files on disk).
     *  3. Reload the re-compressed image.
     *  4. For each pixel, compute the absolute per-channel (R,G,B) difference.
     *  5. Amplify by ELA_AMPLIFY for the visualisation output.
     *  6. Compute average and anomaly-pixel statistics.
     *  7. Derive verdict and confidence from the anomaly percentage.
     *
     * Authentic images: uniform, low ELA values (JPEG compression is
     *   idempotent — re-compressing changes little).
     * Tampered regions: high ELA values because spliced pixels were added
     *   at a different error level than the surrounding area.
     *
     * @param filePath absolute path to the image file
     * @return a fully populated ELAResult
     */
    public static ELAResult performELA(String filePath) {
        ELAResult result = new ELAResult();
        result.recompressionQuality = ELA_QUALITY;

        File file = new File(filePath);
        if (!file.exists()) {
            result.summary = "File not found: " + filePath;
            result.verdict = "ERROR";
            return result;
        }

        try {
            // Step 1 — Load original
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                result.summary = "Could not read image (unsupported format or corrupt file).";
                result.verdict = "ERROR";
                return result;
            }

            int width  = original.getWidth();
            int height = original.getHeight();

            // Step 2 — Re-compress at ELA_QUALITY into memory
            BufferedImage recompressed = recompressAsJPEG(original, ELA_QUALITY);
            if (recompressed == null) {
                result.summary = "Could not re-compress image for ELA.";
                result.verdict = "ERROR";
                return result;
            }

            // Step 3 — Pixel-by-pixel difference + visualisation
            BufferedImage elaImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);

            long   totalDiff   = 0;
            double maxDiff     = 0;
            int    anomalyPx   = 0;
            int    totalPixels = width * height;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int origRGB  = original.getRGB(x, y);
                    int recompRGB = recompressed.getRGB(x, y);

                    // Unpack channels
                    int oR = (origRGB  >> 16) & 0xFF;
                    int oG = (origRGB  >>  8) & 0xFF;
                    int oB =  origRGB         & 0xFF;

                    int rR = (recompRGB >> 16) & 0xFF;
                    int rG = (recompRGB >>  8) & 0xFF;
                    int rB =  recompRGB         & 0xFF;

                    // Absolute per-channel diff
                    int dR = Math.abs(oR - rR);
                    int dG = Math.abs(oG - rG);
                    int dB = Math.abs(oB - rB);

                    // Amplified for ELA visualisation
                    int ampR = Math.min(255, dR * ELA_AMPLIFY);
                    int ampG = Math.min(255, dG * ELA_AMPLIFY);
                    int ampB = Math.min(255, dB * ELA_AMPLIFY);

                    elaImage.setRGB(x, y, (ampR << 16) | (ampG << 8) | ampB);

                    // Accumulate stats
                    double pixelAvgDiff = (dR + dG + dB) / 3.0;
                    totalDiff += (dR + dG + dB);
                    if (pixelAvgDiff > maxDiff) maxDiff = pixelAvgDiff;
                    if (pixelAvgDiff > PIXEL_ANOMALY_THRESHOLD) anomalyPx++;
                }
            }

            // Step 4 — Compute aggregate scores
            double avgDiff         = totalDiff / (double)(totalPixels * 3L);
            double anomalyPct      = (anomalyPx / (double) totalPixels) * 100.0;

            result.avgDifference   = avgDiff;
            result.maxDifference   = maxDiff;
            result.anomalyPercentage = anomalyPct;
            result.elaImage        = elaImage;

            // Step 5 — Derive verdict
            if (anomalyPct < ANOMALY_THRESHOLD_PCT_LOW) {
                result.verdict    = "AUTHENTIC";
                double conf       = Math.max(70.0, 99.0 - (anomalyPct * 5.0));
                result.confidence = String.format("%.1f%%", conf);
                result.summary    = String.format(
                    "ELA shows uniform error distribution (anomaly: %.1f%%). "
                    + "Image appears unmodified.", anomalyPct);
            } else if (anomalyPct < ANOMALY_THRESHOLD_PCT_HIGH) {
                result.verdict    = "POSSIBLY TAMPERED";
                double conf       = 50.0 + (anomalyPct * 3.0);
                conf              = Math.min(conf, 89.9);
                result.confidence = String.format("%.1f%%", conf);
                result.summary    = String.format(
                    "ELA detected irregular error levels in %.1f%% of pixels "
                    + "(avg diff: %.2f). Possible localised editing detected.",
                    anomalyPct, avgDiff);
            } else {
                result.verdict    = "TAMPERED";
                result.confidence = String.format("%.1f%%",
                    Math.min(99.0, 60.0 + anomalyPct));
                result.summary    = String.format(
                    "ELA shows strong evidence of manipulation: %.1f%% of pixels "
                    + "have high error levels (avg diff: %.2f, max: %.2f).",
                    anomalyPct, avgDiff, maxDiff);
            }

        } catch (IOException e) {
            result.verdict = "ERROR";
            result.summary = "ELA failed: " + e.getMessage();
        }

        return result;
    }

    /**
     * Re-compresses a BufferedImage as JPEG at a specified quality level,
     * entirely in memory (no temp files written to disk).
     *
     * @param src     the source image
     * @param quality compression quality 0–100 (lower = more compression artifacts)
     * @return the re-compressed image, or null on failure
     */
    private static BufferedImage recompressAsJPEG(BufferedImage src, int quality)
            throws IOException {

        // Convert to TYPE_INT_RGB — JPEG writer requires no alpha channel
        BufferedImage rgb = toRGB(src);

        // Find a JPEG writer
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) return null;
        ImageWriter writer = writers.next();

        // Set quality
        JPEGImageWriteParam params = new JPEGImageWriteParam(null);
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality / 100.0f);

        // Write into a byte buffer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), params);
        } finally {
            writer.dispose();
        }

        // Read back from the buffer
        byte[] jpegBytes = baos.toByteArray();
        try (InputStream bais = new ByteArrayInputStream(jpegBytes)) {
            return ImageIO.read(bais);
        }
    }

    /**
     * Converts any BufferedImage to TYPE_INT_RGB, which JPEG writers require
     * (JPEG does not support alpha channels).
     */
    private static BufferedImage toRGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage rgb = new BufferedImage(
            src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = rgb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private static String formatFileSize(long bytes) {
        if (bytes < 1024)       return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private static String colorModelName(int type) {
        switch (type) {
            case BufferedImage.TYPE_INT_RGB:    return "RGB";
            case BufferedImage.TYPE_INT_ARGB:   return "ARGB";
            case BufferedImage.TYPE_INT_BGR:    return "BGR";
            case BufferedImage.TYPE_3BYTE_BGR:  return "3-byte BGR";
            case BufferedImage.TYPE_4BYTE_ABGR: return "4-byte ABGR";
            case BufferedImage.TYPE_BYTE_GRAY:  return "Greyscale";
            default:                            return "Type " + type;
        }
    }
}
