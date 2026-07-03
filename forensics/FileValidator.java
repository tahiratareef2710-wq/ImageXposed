package displayPackage.forensics;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

/**
 * Pure Java file validation engine.
 * Performs four independent checks on an image file:
 *
 *   1. MAGIC NUMBER CHECK — reads the first bytes of the file and
 *      verifies they match a known image format signature, regardless
 *      of the file extension. Catches renamed .txt → .png, etc.
 *
 *   2. CORRUPTION CHECK — attempts to fully decode the image into a
 *      BufferedImage via javax.imageio. A corrupt file (truncated,
 *      bad headers, incomplete data) will fail to decode or produce
 *      a null/zero-dimension result.
 *
 *   3. FORMAT CONSISTENCY CHECK — compares the extension the user gave
 *      the file (e.g. ".png") against what the magic bytes actually say
 *      the file is (e.g. JPEG). Detects misnamed files.
 *
 *   4. SIZE & DIMENSION CHECK — validates that the file size is within
 *      acceptable limits and that the pixel dimensions are reasonable
 *      (not zero, not absurdly large).
 *
 * All methods are static. No external libraries.
 */
public class FileValidator {

    // ── Thresholds ─────────────────────────────────────────────────────────

    /** Maximum accepted file size: 50 MB */
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    /** Maximum accepted dimension on either axis: 20,000 px */
    private static final int MAX_DIMENSION = 20_000;

    /** Minimum accepted dimension on either axis: 1 px */
    private static final int MIN_DIMENSION = 1;

    // ── Private constructor — static utility class ─────────────────────────
    private FileValidator() { }

    // ══════════════════════════════════════════════════════════════════════
    // 1. MAGIC NUMBER / SIGNATURE CHECK
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Reads the first bytes of the file and checks whether they match
     * a known image format's magic number (file signature).
     *
     * Supported signatures:
     *   PNG   — 89 50 4E 47 0D 0A 1A 0A
     *   JPEG  — FF D8 FF
     *   BMP   — 42 4D
     *   TIFF  — 49 49 2A 00  (little-endian)  or  4D 4D 00 2A  (big-endian)
     *   GIF   — 47 49 46 38
     *   WEBP  — 52 49 46 46 ... 57 45 42 50
     *
     * @param filePath absolute path to the file
     * @return a ValidationCheck result
     */
    public static ValidationCheck checkMagicNumber(String filePath) {
        ValidationCheck ck = new ValidationCheck("Magic Number / Signature");

        File file = new File(filePath);
        if (!file.exists()) { ck.fail("File does not exist."); return ck; }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 4) {
                ck.fail("File is too small to contain a valid image header (" + raf.length() + " bytes).");
                return ck;
            }

            byte[] header = new byte[12]; // longest signature we check is WEBP (12 bytes)
            int bytesRead = raf.read(header);
            if (bytesRead < 4) {
                ck.fail("Could not read file header.");
                return ck;
            }

            String detected = identifyFormat(header);
            if (detected == null) {
                ck.fail("File signature does not match any known image format. "
                    + "Header bytes: " + hexDump(header, Math.min(bytesRead, 8)));
                return ck;
            }

            ck.pass("Valid " + detected + " signature detected.");
            ck.detectedFormat = detected;

        } catch (IOException e) {
            ck.fail("I/O error reading file header: " + e.getMessage());
        }
        return ck;
    }

    /**
     * Identifies the image format from raw header bytes.
     *
     * @param h at least the first 12 bytes of the file
     * @return format name ("PNG", "JPEG", "BMP", "TIFF", "GIF", "WEBP") or null
     */
    private static String identifyFormat(byte[] h) {
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (h[0] == (byte) 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47
            && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) {
            return "PNG";
        }
        // JPEG: FF D8 FF
        if (h[0] == (byte) 0xFF && h[1] == (byte) 0xD8 && h[2] == (byte) 0xFF) {
            return "JPEG";
        }
        // BMP: 42 4D ("BM")
        if (h[0] == 0x42 && h[1] == 0x4D) {
            return "BMP";
        }
        // TIFF little-endian: 49 49 2A 00
        if (h[0] == 0x49 && h[1] == 0x49 && h[2] == 0x2A && h[3] == 0x00) {
            return "TIFF";
        }
        // TIFF big-endian: 4D 4D 00 2A
        if (h[0] == 0x4D && h[1] == 0x4D && h[2] == 0x00 && h[3] == 0x2A) {
            return "TIFF";
        }
        // GIF: 47 49 46 38 ("GIF8")
        if (h[0] == 0x47 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x38) {
            return "GIF";
        }
        // WEBP: starts with RIFF (52 49 46 46) ... then WEBP at offset 8
        if (h[0] == 0x52 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x46
            && h[8] == 0x57 && h[9] == 0x45 && h[10] == 0x42 && h[11] == 0x50) {
            return "WEBP";
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. CORRUPTION CHECK  (full decode attempt)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attempts to fully decode the image using javax.imageio.
     * A corrupt file will either throw an exception, return null,
     * or return a BufferedImage with zero dimensions.
     *
     * @param filePath absolute path to the file
     * @return a ValidationCheck result
     */
    public static ValidationCheck checkCorruption(String filePath) {
        ValidationCheck ck = new ValidationCheck("Corruption / Integrity");

        File file = new File(filePath);
        if (!file.exists()) { ck.fail("File does not exist."); return ck; }

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                ck.fail("Image decoder returned null — file is corrupt or in an unsupported sub-format.");
                return ck;
            }
            if (img.getWidth() <= 0 || img.getHeight() <= 0) {
                ck.fail("Decoded image has invalid dimensions (" +
                    img.getWidth() + "x" + img.getHeight() + ").");
                return ck;
            }

            // Verify we can read pixel data (catches partial decode issues)
            try {
                img.getRGB(0, 0);
                img.getRGB(img.getWidth() - 1, img.getHeight() - 1);
            } catch (Exception e) {
                ck.fail("Pixel data is inaccessible — image data may be truncated.");
                return ck;
            }

            ck.pass("Image decoded successfully (" +
                img.getWidth() + "x" + img.getHeight() + ", " +
                img.getColorModel().getPixelSize() + " bpp).");
            ck.decodedWidth  = img.getWidth();
            ck.decodedHeight = img.getHeight();

        } catch (IOException e) {
            ck.fail("Decode failed with I/O error: " + e.getMessage());
        } catch (Exception e) {
            ck.fail("Decode failed with unexpected error: " + e.getMessage());
        }
        return ck;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. FORMAT CONSISTENCY CHECK
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Compares the file's actual format (from magic bytes) against its extension.
     * Detects cases like a JPEG file saved as "photo.png".
     *
     * @param filePath  absolute path to the file
     * @param extension the file extension provided by the user (e.g. "png")
     * @return a ValidationCheck result
     */
    public static ValidationCheck checkFormatConsistency(String filePath, String extension) {
        ValidationCheck ck = new ValidationCheck("Format Consistency");

        // First, determine actual format from magic bytes
        ValidationCheck magic = checkMagicNumber(filePath);
        if (!magic.passed) {
            ck.fail("Cannot verify format consistency — magic number check failed: " + magic.detail);
            return ck;
        }

        String actualFormat  = magic.detectedFormat.toUpperCase();
        String claimedFormat = normalizeExtension(extension);

        if (claimedFormat.isEmpty()) {
            ck.fail("File has no extension — cannot verify format consistency.");
            return ck;
        }

        // Check if they match (accounting for JPEG/JPG equivalence)
        if (formatsMatch(actualFormat, claimedFormat)) {
            ck.pass("Extension ." + extension + " matches actual format (" + actualFormat + ").");
        } else {
            ck.fail("Extension mismatch — file claims to be ." + extension
                + " but magic bytes indicate " + actualFormat + ".");
        }

        ck.detectedFormat = actualFormat;
        return ck;
    }

    /**
     * Normalises a file extension for comparison.
     * Maps "jpg" and "jpeg" to "JPEG", "tif" to "TIFF", etc.
     */
    private static String normalizeExtension(String ext) {
        if (ext == null) return "";
        switch (ext.toLowerCase().trim()) {
            case "jpg": case "jpeg": return "JPEG";
            case "png":              return "PNG";
            case "bmp":              return "BMP";
            case "tif": case "tiff": return "TIFF";
            case "gif":              return "GIF";
            case "webp":             return "WEBP";
            default:                 return ext.toUpperCase();
        }
    }

    /**
     * Checks whether two format strings represent the same format.
     */
    private static boolean formatsMatch(String actual, String claimed) {
        return actual.equals(claimed);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. SIZE & DIMENSION CHECK
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Validates that the file size is within limits and that the image
     * dimensions are reasonable (not zero, not absurdly large).
     *
     * @param filePath absolute path to the file
     * @return a ValidationCheck result
     */
    public static ValidationCheck checkSizeAndDimensions(String filePath) {
        ValidationCheck ck = new ValidationCheck("Size & Dimensions");

        File file = new File(filePath);
        if (!file.exists()) { ck.fail("File does not exist."); return ck; }

        // File size check
        long size = file.length();
        if (size == 0) {
            ck.fail("File is empty (0 bytes).");
            return ck;
        }
        if (size > MAX_FILE_SIZE) {
            ck.fail(String.format("File size %.2f MB exceeds the %d MB limit.",
                size / (1024.0 * 1024.0), MAX_FILE_SIZE / (1024 * 1024)));
            return ck;
        }

        // Dimensions check — read without fully decoding the entire raster
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            if (iis == null) {
                ck.fail("Cannot open image stream for dimension check.");
                return ck;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                ck.fail("No ImageReader available for this file format.");
                return ck;
            }

            ImageReader reader = readers.next();
            reader.setInput(iis, true, true);
            int width  = reader.getWidth(0);
            int height = reader.getHeight(0);
            reader.dispose();

            if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
                ck.fail("Image dimensions are too small: " + width + "x" + height + " px.");
                return ck;
            }
            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                ck.fail("Image dimensions exceed the " + MAX_DIMENSION
                    + " px limit: " + width + "x" + height + " px.");
                return ck;
            }

            String sizeStr = size < 1024 * 1024
                ? String.format("%.1f KB", size / 1024.0)
                : String.format("%.2f MB", size / (1024.0 * 1024.0));

            ck.pass(String.format("File size: %s | Dimensions: %dx%d px — within limits.",
                sizeStr, width, height));
            ck.decodedWidth  = width;
            ck.decodedHeight = height;

        } catch (IOException e) {
            ck.fail("Error reading dimensions: " + e.getMessage());
        }
        return ck;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONVENIENCE: Run all 4 checks at once
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Runs all four validation checks and returns a composite result.
     *
     * @param filePath  absolute path to the file
     * @param extension the file extension (e.g. "png")
     * @return a FullValidationResult containing all individual checks
     */
    public static FullValidationResult validateAll(String filePath, String extension) {
        FullValidationResult r = new FullValidationResult();
        r.magicNumber       = checkMagicNumber(filePath);
        r.corruption        = checkCorruption(filePath);
        r.formatConsistency = checkFormatConsistency(filePath, extension);
        r.sizeDimensions    = checkSizeAndDimensions(filePath);

        r.allPassed = r.magicNumber.passed
                   && r.corruption.passed
                   && r.formatConsistency.passed
                   && r.sizeDimensions.passed;
        return r;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DTOs
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Result of a single validation check.
     */
    public static class ValidationCheck {
        public String  name;
        public boolean passed;
        public String  detail;
        public String  detectedFormat = "";
        public int     decodedWidth   = 0;
        public int     decodedHeight  = 0;

        public ValidationCheck(String name) {
            this.name   = name;
            this.passed = false;
            this.detail = "Not yet checked.";
        }

        public void pass(String detail) { this.passed = true;  this.detail = detail; }
        public void fail(String detail) { this.passed = false; this.detail = detail; }

        public String statusIcon() { return passed ? "\u2713 Passed" : "\u2717 Failed"; }

        @Override
        public String toString() {
            return name + ": " + statusIcon() + " — " + detail;
        }
    }

    /**
     * Composite result of all four validation checks.
     */
    public static class FullValidationResult {
        public ValidationCheck magicNumber;
        public ValidationCheck corruption;
        public ValidationCheck formatConsistency;
        public ValidationCheck sizeDimensions;
        public boolean allPassed;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Produces a hex dump string of the first n bytes for error reporting.
     */
    private static String hexDump(byte[] bytes, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
