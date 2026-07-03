package displayPackage.models;

import displayPackage.core.BaseEntity;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a forensic report generated from a Scan.
 * Handles report compilation, format conversion, and file export.
 *
 * Relationships (from class diagram):
 * - Report is generated from a Scan (1 Scan generates 0..1 Report)
 */
public class Report extends BaseEntity {

    private String scanId;
    private Map<String, Object> reportData;
    private String exportFormat;
    private Date generatedAt;

    public Report() {
        super();
        this.reportData = new HashMap<>();
        this.generatedAt = new Date();
        this.exportFormat = "PDF";
    }

    public Report(String scanId) {
        super();
        this.scanId = scanId;
        this.reportData = new HashMap<>();
        this.generatedAt = new Date();
        this.exportFormat = "PDF";
    }

    // ── Business Logic ─────────────────────────────────────────────────────

    @Override
    public boolean validate() {
        return scanId != null && !scanId.trim().isEmpty();
    }

    /**
     * Compiles a report from the provided scan/analysis data.
     *
     * @param data the raw analysis data from a Scan
     * @return the compiled report data map
     */
    public Map<String, Object> compileReport(Map<String, Object> data) {
        this.reportData = new HashMap<>(data);
        this.reportData.put("reportId", getId());
        this.reportData.put("generatedAt", generatedAt);
        touch();
        return this.reportData;
    }

    /**
     * Converts the report to the specified format.
     * TODO: Implement actual format conversion (PDF, HTML, CSV, TXT).
     *
     * @param format the target format (e.g., "PDF", "TXT", "HTML", "CSV")
     * @return a File reference to the converted report (placeholder for now)
     */
    public File convertToFormat(String format) {
        this.exportFormat = format;
        touch();
        // Placeholder — return a temp file reference
        return new File("report_" + getId() + "." + format.toLowerCase());
    }

    public void exportToFile(String path) {
        String reportText = (reportData != null && reportData.containsKey("reportText"))
                ? (String) reportData.get("reportText")
                : "Report generation error. No data found.";

        try {
            if ("PDF".equalsIgnoreCase(exportFormat)) {
                writeMinimalPDF(path, reportText);
            } else {
                // Default fallback to plain text for TXT, CSV, etc.
                try (java.io.PrintWriter out = new java.io.PrintWriter(path)) {
                    out.println(reportText);
                }
            }
            System.out.println("Exported report " + getId() + " to: " + path);
        } catch (Exception e) {
            System.err.println("Failed to export report: " + e.getMessage());
        }
    }

    private static class JpegLogo {
        byte[] bytes;
        int origW;
        int origH;
        int pdfW;
        int pdfH;
    }

    private JpegLogo loadLogo() {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.File("imagexposed_logo.png"));
            if (img == null)
                return null;
            JpegLogo logo = new JpegLogo();
            logo.origW = img.getWidth();
            logo.origH = img.getHeight();
            // Scale to max height of 50 points
            double scale = 50.0 / logo.origH;
            logo.pdfW = (int) (logo.origW * scale);
            logo.pdfH = 50;

            java.awt.image.BufferedImage rgb = new java.awt.image.BufferedImage(
                    logo.origW, logo.origH, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgb.createGraphics();
            g.drawImage(img, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(rgb, "jpeg", baos);
            logo.bytes = baos.toByteArray();
            return logo;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeMinimalPDF(String path, String text) throws java.io.IOException {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path)) {
            java.util.List<Integer> offsets = new java.util.ArrayList<>();
            offsets.add(0); // dummy for index 0
            int offset = 0;

            String header = "%PDF-1.4\n";
            fos.write(header.getBytes());
            offset += header.length();

            JpegLogo logo = loadLogo();
            int objCount = (logo != null) ? 6 : 5;

            // Object 1: Catalog
            offsets.add(offset);
            String obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
            fos.write(obj1.getBytes());
            offset += obj1.length();

            // Object 2: Pages
            offsets.add(offset);
            String obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n";
            fos.write(obj2.getBytes());
            offset += obj2.length();

            // Object 3: Page
            offsets.add(offset);
            String resDict = "<< /Font << /F1 4 0 R >>";
            if (logo != null) {
                resDict += " /XObject << /Im1 6 0 R >>";
            }
            resDict += " >>";
            String obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources " + resDict
                    + " /Contents 5 0 R >>\nendobj\n";
            fos.write(obj3.getBytes());
            offset += obj3.length();

            // Object 4: Font
            offsets.add(offset);
            String obj4 = "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n";
            fos.write(obj4.getBytes());
            offset += obj4.length();

            // Object 5: Content Stream
            offsets.add(offset);
            StringBuilder streamContent = new StringBuilder();

            int textStartY = 730;
            if (logo != null) {
                int xPos = (612 - logo.pdfW) / 2;
                int yPos = 710;
                streamContent.append("q ").append(logo.pdfW).append(" 0 0 ").append(logo.pdfH).append(" ").append(xPos)
                        .append(" ").append(yPos).append(" cm /Im1 Do Q\n");
                textStartY = yPos - 30; // start text below logo
            }

            streamContent.append("BT\n/F1 11 Tf\n40 ").append(textStartY).append(" Td\n15 TL\n");
            String[] lines = text.split("\n");
            for (String line : lines) {
                line = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
                streamContent.append("(").append(line).append(") Tj T*\n");
            }
            streamContent.append("ET\n");
            String obj5 = "5 0 obj\n<< /Length " + streamContent.length() + " >>\nstream\n" + streamContent.toString()
                    + "endstream\nendobj\n";
            fos.write(obj5.getBytes());
            offset += obj5.length();

            // Object 6: Image XObject (if exists)
            if (logo != null) {
                offsets.add(offset);
                String obj6Header = "6 0 obj\n<< /Type /XObject /Subtype /Image /Width " + logo.origW + " /Height "
                        + logo.origH + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                        + logo.bytes.length + " >>\nstream\n";
                fos.write(obj6Header.getBytes());
                fos.write(logo.bytes);
                String obj6Footer = "\nendstream\nendobj\n";
                fos.write(obj6Footer.getBytes());
                offset += obj6Header.length() + logo.bytes.length + obj6Footer.length();
            }

            // Xref table
            int startXref = offset;
            String xrefHeader = "xref\n0 " + (objCount + 1) + "\n0000000000 65535 f \n";
            fos.write(xrefHeader.getBytes());
            for (int i = 1; i <= objCount; i++) {
                String entry = String.format("%010d 00000 n \n", offsets.get(i));
                fos.write(entry.getBytes());
            }

            // Trailer
            String trailer = "trailer\n<< /Size " + (objCount + 1) + " /Root 1 0 R >>\nstartxref\n" + startXref
                    + "\n%%EOF\n";
            fos.write(trailer.getBytes());
        }
    }

    // ── Map Serialisation ──────────────────────────────────────────────────

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("scanId", scanId);
        map.put("reportData", reportData);
        map.put("exportFormat", exportFormat);
        map.put("generatedAt", generatedAt);
        return map;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String s) {
        this.scanId = s;
        touch();
    }

    public Map<String, Object> getReportData() {
        return reportData;
    }

    public void setReportData(Map<String, Object> data) {
        this.reportData = data;
        touch();
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String f) {
        this.exportFormat = f;
        touch();
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date d) {
        this.generatedAt = d;
        touch();
    }
}
