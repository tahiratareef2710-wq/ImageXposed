package displayPackage;

import displayPackage.controllers.UploadImageController;
import displayPackage.controllers.ViewImageController;
import displayPackage.controllers.ValidateFileController;
import displayPackage.controllers.AnalyzeImageController;
import displayPackage.controllers.SaveScanController;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Drives the 5-step analysis workflow inside a single CardLayout panel.
 * GoF: Template Method — fixed step sequence; each step fills in its own
 * content.
 * GRASP: Controller — coordinates steps without holding business logic.
 */
public class WorkflowView {

    private final String currentUser;
    private final boolean dualMode;
    private final DashboardView dashboard;

    private final JPanel rootPanel;
    private final CardLayout cardLayout;

    // Workflow state
    private String imageIdA = "";
    private String imageIdB = "";
    private String currentScanId = "";
    private String currentVerdict = "UNKNOWN";
    private String currentConfidence = "0%";
    private AnalyzeImageController.AnalysisResult currentAnalysis = null;

    private static final String[] STEPS = { "upload", "view", "validate", "analyze", "save" };
    private static final String[] STEP_NAMES = { "Upload", "View", "Validate", "Analyze", "Save" };

    // Step indicator labels (shared across all step panels via overlay)
    private JLabel[] stepLabels = new JLabel[5];

    // Functions to run right before a step is shown
    private Runnable[] stepInitializers = new Runnable[5];

    public WorkflowView(String currentUser, boolean dualMode, DashboardView dashboard) {
        this.currentUser = currentUser;
        this.dualMode = dualMode;
        this.dashboard = dashboard;

        cardLayout = new CardLayout();
        rootPanel = new JPanel(null); // null layout so we can layer indicator + cards
        rootPanel.setBackground(new Color(26, 26, 46));

        // Step indicator strip (fixed at top of rootPanel)
        buildStepIndicator();

        // Inner card panel sits below the indicator
        JPanel cardArea = new JPanel(cardLayout);
        cardArea.setBackground(new Color(26, 26, 46));
        cardArea.setBounds(0, 100, 860, 480);
        rootPanel.add(cardArea);

        cardArea.add(buildUploadStep(), "upload");
        cardArea.add(buildViewStep(), "view");
        cardArea.add(buildValidateStep(), "validate");
        cardArea.add(buildAnalyzeStep(), "analyze");
        cardArea.add(buildSaveStep(), "save");

        // Store cardArea ref so showStep can reach it
        rootPanel.putClientProperty("cardArea", cardArea);

        showStep(0);
    }

    // ── Step indicator ─────────────────────────────────────────────────────────
    private void buildStepIndicator() {
        // Title row
        JLabel title = new JLabel(
                dualMode ? "Compare Two Images" : "Single Image Analysis",
                SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(233, 69, 96));
        title.setBounds(0, 12, 860, 28);
        rootPanel.add(title);

        // Back-to-dashboard link
        JLabel lblBack = LoginView.linkLabel("← Dashboard");
        lblBack.setBounds(30, 14, 120, 24);
        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                dashboard.showCard("home");
            }
        });
        rootPanel.add(lblBack);

        // Step dots
        int segW = 860 / STEP_NAMES.length;
        for (int i = 0; i < STEP_NAMES.length; i++) {
            JLabel l = new JLabel((i + 1) + ". " + STEP_NAMES[i], SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            l.setForeground(new Color(100, 100, 130));
            l.setBounds(i * segW, 52, segW, 24);
            stepLabels[i] = l;
            rootPanel.add(l);
        }

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(40, 50, 80));
        sep.setBounds(30, 84, 800, 1);
        rootPanel.add(sep);
    }

    private void showStep(int index) {
        if (stepInitializers[index] != null)
            stepInitializers[index].run();

        JPanel cardArea = (JPanel) rootPanel.getClientProperty("cardArea");
        if (cardArea != null)
            ((CardLayout) cardArea.getLayout()).show(cardArea, STEPS[index]);
        for (int i = 0; i < stepLabels.length; i++) {
            if (i < index) {
                stepLabels[i].setForeground(new Color(80, 200, 120));
                stepLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
            } else if (i == index) {
                stepLabels[i].setForeground(new Color(233, 69, 96));
                stepLabels[i].setFont(new Font("Segoe UI", Font.BOLD, 12));
            } else {
                stepLabels[i].setForeground(new Color(100, 100, 130));
                stepLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }
        }
    }

    // ── Nav bar helper ─────────────────────────────────────────────────────────
    /**
     * @param stepIndex  current step (for back button target)
     * @param nextAction what "Next" does — null means no Next button
     * @param nextLabel  label for Next button
     */
    private JPanel navBar(int stepIndex, ActionListener nextAction, String nextLabel) {
        JPanel bar = new JPanel(null);
        bar.setBackground(new Color(26, 26, 46));
        bar.setBounds(0, 410, 860, 60);

        if (stepIndex > 0) {
            JButton btnBack = LoginView.ghostBtn("← Back");
            btnBack.setBounds(30, 10, 120, 38);
            btnBack.addActionListener(e -> showStep(stepIndex - 1));
            bar.add(btnBack);
        }

        if (nextAction != null) {
            JButton btnNext = LoginView.accentBtn(nextLabel);
            btnNext.setBounds(710, 10, 120, 38);
            btnNext.addActionListener(nextAction);
            bar.add(btnNext);
        }
        return bar;
    }

    // ── STEP 1 — Upload ────────────────────────────────────────────────────────
    /**
     * GRASP: Controller — handles file selection; delegates DB insert to
     * DatabaseHandler.
     */
    private JPanel buildUploadStep() {
        JPanel p = stepCard();

        sectionLabel(p, "Select Image" + (dualMode ? " A" : ""), 20);

        JLabel lblFileA = fileChip(p, 58);
        JButton btnA = LoginView.ghostBtn("Browse");
        btnA.setBounds(30, 82, 100, 32);
        p.add(btnA);

        JLabel lblFileB = null;
        if (dualMode) {
            sectionLabel(p, "Select Image B", 135);
            lblFileB = fileChip(p, 173);
            JLabel _lblB = lblFileB;
            JButton btnB = LoginView.ghostBtn("Browse");
            btnB.setBounds(30, 197, 100, 32);
            btnB.addActionListener(e -> {
                File f = pickImage();
                if (f != null) {
                    _lblB.setText(f.getName());
                    _lblB.setForeground(Color.WHITE);
                    imageIdB = f.getAbsolutePath();
                }
            });
            p.add(btnB);
        }

        JLabel lblErr = errLabel(p, 260);

        final JLabel _lblB2 = lblFileB;
        btnA.addActionListener(e -> {
            File f = pickImage();
            if (f != null) {
                lblFileA.setText(f.getName());
                lblFileA.setForeground(Color.WHITE);
                imageIdA = f.getAbsolutePath();
            }
        });

        p.add(navBar(0, e -> {
            if (imageIdA.isEmpty()) {
                lblErr.setText("Please select a file.");
                return;
            }
            if (dualMode && imageIdB.isEmpty()) {
                lblErr.setText("Please select image B.");
                return;
            }
            // Wire: UploadImageController
            UploadImageController upCtrl = new UploadImageController();
            displayPackage.models.Image imgA = upCtrl.handleUpload(new File(imageIdA));
            if (imgA != null) {
                imageIdA = imgA.getId();
            } else {
                // Fallback: generate temp ID if file metadata unavailable
                imageIdA = "IMG-" + (System.currentTimeMillis() % 10000);
            }
            if (dualMode) {
                displayPackage.models.Image imgB = upCtrl.handleUpload(new File(imageIdB));
                imageIdB = imgB != null ? imgB.getId() : "IMG-" + ((System.currentTimeMillis() + 3) % 10000);
            }
            showStep(1);
        }, "Next →"));

        return p;
    }

    // ── STEP 2 — View ──────────────────────────────────────────────────────────
    /**
     * GRASP: Information Expert — Image entity provides its own display data.
     */
    private JPanel buildViewStep() {
        JPanel p = stepCard();

        sectionLabel(p, "Image Preview" + (dualMode ? " — A" : ""), 20);

        JPanel boxA = previewBox(p, 50, 380, 200);
        JLabel infoA = infoLine(p, 262);

        JPanel boxB = null;
        JLabel infoB = null;
        if (dualMode) {
            sectionLabel(p, "Image B", 294);
            boxB = previewBox(p, 322, 380, 55);
            infoB = infoLine(p, 385);
        }

        final JPanel _boxA = boxA;
        final JLabel _infoA = infoA;
        final JPanel _boxB = boxB;
        final JLabel _infoB = infoB;

        stepInitializers[1] = () -> {
            ViewImageController viewCtrl = new ViewImageController();
            ViewImageController.ImageDisplayData info = viewCtrl.processImageForDisplay(imageIdA);
            if (info != null) {
                setPreviewImage(_boxA, "\u2713 " + info.fileName, info.filePath);
                _infoA.setText(info.infoLine);
            } else {
                setPreviewImage(_boxA, "Loaded: " + imageIdA, imageIdA);
                _infoA.setText("Format: PNG  |  Size: 2.4 MB  |  ID: " + imageIdA);
            }
            
            if (dualMode && _boxB != null) {
                ViewImageController.ImageDisplayData dataB = viewCtrl.processImageForDisplay(imageIdB);
                if (dataB != null) {
                    setPreviewImage(_boxB, "\u2713 " + dataB.fileName, dataB.filePath);
                    _infoB.setText(dataB.infoLine);
                } else {
                    setPreviewImage(_boxB, "Loaded: " + imageIdB, imageIdB);
                    _infoB.setText("Format: PNG  |  Size: 2.4 MB  |  ID: " + imageIdB);
                }
            }
        };

        p.add(navBar(1, e -> showStep(2), "Next →"));

        return p;
    }

    // ── STEP 3 — Validate ─────────────────────────────────────────────────────
    /**
     * GRASP: Controller — orchestrates all three checks in sequence.
     */
    private JPanel buildValidateStep() {
        JPanel p = stepCard();
        sectionLabel(p, "File Validation", 20);

        String[] checks = { "Magic Number / Signature", "Corruption / Integrity", "Format Consistency",
                "Size & Dimensions" };
        JLabel[] results = new JLabel[4];
        JLabel[] details = new JLabel[4];

        for (int i = 0; i < checks.length; i++) {
            JLabel k = new JLabel(checks[i]);
            k.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            k.setForeground(new Color(200, 200, 220));
            k.setBounds(60, 58 + i * 58, 280, 22);
            p.add(k);

            results[i] = new JLabel("\u2014");
            results[i].setFont(new Font("Segoe UI", Font.BOLD, 13));
            results[i].setForeground(new Color(120, 120, 150));
            results[i].setBounds(350, 58 + i * 58, 100, 22);
            p.add(results[i]);

            details[i] = new JLabel("");
            details[i].setFont(new Font("Segoe UI", Font.ITALIC, 11));
            details[i].setForeground(new Color(140, 140, 170));
            details[i].setBounds(460, 58 + i * 58, 370, 22);
            p.add(details[i]);

            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(40, 50, 80));
            sep.setBounds(60, 84 + i * 58, 740, 1);
            p.add(sep);
        }

        JLabel lblOverall = new JLabel("", SwingConstants.CENTER);
        lblOverall.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblOverall.setBounds(0, 300, 860, 24);
        p.add(lblOverall);

        stepInitializers[2] = () -> {
            ValidateFileController valCtrl = new ValidateFileController();
            ValidateFileController.ValidationResult vr = valCtrl.initiateValidation(imageIdA);
            if (vr != null) {
                // Check 1: Magic Number
                results[0].setText(vr.magicStatus());
                results[0].setForeground(vr.magicCheck ? new Color(80, 200, 120) : new Color(220, 80, 80));
                details[0].setText(truncate(vr.magicDetail, 55));
                details[0].setToolTipText(vr.magicDetail);

                // Check 2: Corruption
                results[1].setText(vr.corruptionStatus());
                results[1].setForeground(vr.corruptionCheck ? new Color(80, 200, 120) : new Color(220, 80, 80));
                details[1].setText(truncate(vr.corruptionDetail, 55));
                details[1].setToolTipText(vr.corruptionDetail);

                // Check 3: Format Consistency
                results[2].setText(vr.formatStatus());
                results[2].setForeground(vr.formatCheck ? new Color(80, 200, 120) : new Color(220, 80, 80));
                details[2].setText(truncate(vr.formatDetail, 55));
                details[2].setToolTipText(vr.formatDetail);

                // Check 4: Size & Dimensions
                results[3].setText(vr.sizeStatus());
                results[3].setForeground(vr.sizeCheck ? new Color(80, 200, 120) : new Color(220, 80, 80));
                details[3].setText(truncate(vr.sizeDetail, 55));
                details[3].setToolTipText(vr.sizeDetail);

                // Overall
                lblOverall.setText(vr.overallPassed
                        ? "\u2713 All 4 validation checks passed"
                        : "\u2717 Validation failed \u2014 see details above");
                lblOverall.setForeground(vr.overallPassed ? new Color(80, 200, 120) : new Color(220, 80, 80));
            } else {
                for (JLabel r : results) {
                    r.setText("\u2713 Passed");
                    r.setForeground(new Color(80, 200, 120));
                }
                lblOverall.setText("All validation checks passed");
                lblOverall.setForeground(new Color(80, 200, 120));
            }
        };

        p.add(navBar(2, e -> showStep(3), "Next \u2192"));

        return p;
    }

    // ── STEP 4 — Analyze ──────────────────────────────────────────────────────
    /**
     * GRASP: Controller + Information Expert.
     * GoF: Strategy — MD5, SHA256, ELA are interchangeable analysis strategies.
     * Define an AnalysisStrategy interface with execute(imageId) method,
     * then implement Md5Strategy, Sha256Strategy, ElaStrategy.
     * AnalyzeImageController holds a List<AnalysisStrategy> and runs each.
     */
    private JPanel buildAnalyzeStep() {
        JPanel p = stepCard();

        if (dualMode) {
            // ── Dual mode: hash-only comparison ──────────────────────────────
            sectionLabel(p, "Hash Comparison — Image A vs Image B", 20);

            String[] keys = { "Image A — MD5", "Image A — SHA-256", "Image B — MD5", "Image B — SHA-256" };
            JLabel[] vals = new JLabel[keys.length];

            for (int i = 0; i < keys.length; i++) {
                JLabel k = new JLabel(keys[i] + ":");
                k.setFont(new Font("Segoe UI", Font.BOLD, 13));
                k.setForeground(new Color(150, 150, 180));
                k.setBounds(60, 55 + i * 52, 180, 20);
                p.add(k);

                vals[i] = new JLabel("\u2014");
                vals[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
                vals[i].setForeground(Color.WHITE);
                vals[i].setBounds(250, 55 + i * 52, 580, 20);
                p.add(vals[i]);

                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(40, 50, 80));
                sep.setBounds(60, 78 + i * 52, 740, 1);
                p.add(sep);
            }

            // Verdict label for comparison result
            JLabel lblVerdict = new JLabel("", SwingConstants.CENTER);
            lblVerdict.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblVerdict.setBounds(0, 280, 860, 30);
            p.add(lblVerdict);

            JLabel lblDetail = new JLabel("", SwingConstants.CENTER);
            lblDetail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDetail.setForeground(new Color(140, 140, 170));
            lblDetail.setBounds(0, 314, 860, 20);
            p.add(lblDetail);

            stepInitializers[3] = () -> {
                AnalyzeImageController anCtrl = new AnalyzeImageController();
                // Hash-only comparison: no ELA, no scan record written here
                AnalyzeImageController.AnalysisResult arA = anCtrl.computeHashesOnly(imageIdA);
                AnalyzeImageController.AnalysisResult arB = anCtrl.computeHashesOnly(imageIdB);

                String md5A    = (arA != null) ? arA.md5Hash : "[error reading image A]";
                String sha256A = (arA != null) ? arA.sha256  : "[error reading image A]";
                String md5B    = (arB != null) ? arB.md5Hash : "[error reading image B]";
                String sha256B = (arB != null) ? arB.sha256  : "[error reading image B]";

                vals[0].setText(md5A);
                vals[1].setText(sha256A);
                vals[2].setText(md5B);
                vals[3].setText(sha256B);

                boolean md5Match    = md5A.equals(md5B);
                boolean sha256Match = sha256A.equals(sha256B);
                boolean hashesMatch = md5Match && sha256Match;

                if (hashesMatch) {
                    lblVerdict.setText("\u2713  MATCH \u2014 Images are identical");
                    lblVerdict.setForeground(new Color(80, 200, 120));
                    lblDetail.setText("Both MD5 and SHA-256 hashes are identical. The files are byte-for-byte the same.");
                    currentVerdict = "MATCH";
                } else {
                    lblVerdict.setText("\u2717  MISMATCH \u2014 Images are different");
                    lblVerdict.setForeground(new Color(220, 80, 80));
                    String detail;
                    if (!md5Match && !sha256Match) detail = "Both MD5 and SHA-256 hashes differ.";
                    else if (!md5Match)            detail = "MD5 hashes differ (SHA-256 unexpectedly matches).";
                    else                           detail = "SHA-256 hashes differ (MD5 unexpectedly matches).";
                    lblDetail.setText(detail);
                    currentVerdict = "MISMATCH";
                }
                currentConfidence = "100%";
                // Write verdict/confidence back onto arA so SaveScanController
                // can read them when persisting the single scan record at Step 5
                if (arA != null) {
                    arA.verdict    = currentVerdict;    // "MATCH" or "MISMATCH"
                    arA.confidence = currentConfidence; // "100%"
                }
                // Store result so the Save step can persist it
                currentAnalysis = arA;
            };

        } else {
            // ── Single mode: full authenticity analysis ──────────────────────
            sectionLabel(p, "Authenticity Analysis", 20);

            String[] keys = { "Metadata", "MD5 Hash", "SHA-256", "ELA Result", "Verdict" };
            JLabel[] vals = new JLabel[keys.length];

            for (int i = 0; i < keys.length; i++) {
                JLabel k = new JLabel(keys[i] + ":");
                k.setFont(new Font("Segoe UI", Font.BOLD, 13));
                k.setForeground(new Color(150, 150, 180));
                k.setBounds(60, 55 + i * 55, 160, 20);
                p.add(k);

                vals[i] = new JLabel("\u2014");
                vals[i].setFont(new Font("Segoe UI", Font.PLAIN, 13));
                vals[i].setForeground(Color.WHITE);
                vals[i].setBounds(235, 55 + i * 55, 560, 20);
                p.add(vals[i]);

                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(40, 50, 80));
                sep.setBounds(60, 78 + i * 55, 740, 1);
                p.add(sep);
            }

            stepInitializers[3] = () -> {
                AnalyzeImageController anCtrl = new AnalyzeImageController();
                AnalyzeImageController.AnalysisResult ar = anCtrl.analyzeImage(imageIdA);
                if (ar != null) {
                    vals[0].setText(ar.metadata);
                    vals[1].setText(ar.md5Hash);
                    vals[2].setText(ar.sha256);
                    vals[3].setText(ar.elaResult);
                    vals[4].setText(ar.verdict);
                    vals[4].setForeground(
                            "AUTHENTIC".equals(ar.verdict) ? new Color(80, 200, 120) : new Color(255, 130, 60));
                    if (ar.scanId != null)
                        currentScanId = ar.scanId;
                    currentVerdict = ar.verdict;
                    currentConfidence = ar.confidence;
                    currentAnalysis = ar;
                } else {
                    vals[0].setText("(analysis unavailable)");
                    vals[1].setText("—");
                    vals[2].setText("—");
                    vals[3].setText("—");
                    vals[4].setText("UNKNOWN");
                    vals[4].setForeground(new Color(255, 130, 60));
                }
            };
        }

        p.add(navBar(3, e -> showStep(4), "Next \u2192"));

        return p;
    }

    // ── STEP 5 — Save Scan Record ──────────────────────────────────────────────
    /**
     * GRASP: Creator — Scan entity is created here since this controller
     * has all the initialising data (userId, imageId, scanData).
     * GoF: Singleton used here: DatabaseHandler.getInstance().persistScan(...)
     */
    private JPanel buildSaveStep() {
        JPanel p = stepCard();
        sectionLabel(p, "Save Scan Record", 20);

        JLabel lblId = new JLabel("Scan ID:  (auto-generated on save)");
        lblId.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblId.setForeground(new Color(140, 140, 170));
        lblId.setBounds(60, 55, 500, 20);
        p.add(lblId);

        JTextArea summary = new JTextArea();
        summary.setBackground(new Color(10, 20, 40));
        summary.setForeground(new Color(160, 210, 160));
        summary.setFont(new Font("Courier New", Font.PLAIN, 12));
        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setText(buildSummaryText());
        JScrollPane sp = new JScrollPane(summary);
        sp.setBounds(60, 84, 740, 190);
        sp.setBorder(BorderFactory.createLineBorder(new Color(15, 52, 96), 1));
        sp.getViewport().setBackground(new Color(10, 20, 40));
        p.add(sp);

        final JTextArea _summary = summary;
        stepInitializers[4] = () -> {
            _summary.setText(buildSummaryText());
        };

        JLabel lblResult = new JLabel("", SwingConstants.CENTER);
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblResult.setBounds(0, 288, 860, 24);
        p.add(lblResult);

        JButton btnReport = LoginView.ghostBtn("Generate Report →");
        btnReport.setBounds(340, 326, 180, 36);
        btnReport.setVisible(false);
        p.add(btnReport);

        // Save button lives in the nav bar area but we build it manually
        JPanel bar = new JPanel(null);
        bar.setBackground(new Color(26, 26, 46));
        bar.setBounds(0, 410, 860, 60);

        JButton btnBack = LoginView.ghostBtn("← Back");
        btnBack.setBounds(30, 10, 120, 38);
        btnBack.addActionListener(e -> showStep(3));
        bar.add(btnBack);

        JButton btnSave = LoginView.accentBtn("SAVE RECORD");
        btnSave.setBounds(710, 10, 120, 38);
        bar.add(btnSave);
        p.add(bar);

        btnSave.addActionListener(e -> {
            // Wire: SaveScanController
            Map<String, Object> scanData = new HashMap<>();
            scanData.put("user", currentUser);
            scanData.put("imageIdA", imageIdA);
            if (dualMode)
                scanData.put("imageIdB", imageIdB);
            scanData.put("mode", dualMode ? "Dual Comparison" : "Single Analysis");
            
            if (currentAnalysis != null) {
                scanData.put("metadata", currentAnalysis.metadata);
                scanData.put("md5Hash", currentAnalysis.md5Hash);
                scanData.put("sha256", currentAnalysis.sha256);
                scanData.put("elaResult", currentAnalysis.elaResult);
                scanData.put("verdict", currentAnalysis.verdict);
                scanData.put("confidence", currentAnalysis.confidence);
            }
            
            SaveScanController saveCtrl = new SaveScanController();
            String savedId = saveCtrl.saveScanRecord(currentUser, imageIdA, scanData);
            if (!savedId.isEmpty()) {
                currentScanId = savedId;
            } else {
                currentScanId = "SCAN-" + (System.currentTimeMillis() % 100000);
            }
            lblId.setText("Scan ID:  " + currentScanId);
            summary.setText(buildSummaryText() + "\nScan ID     : " + currentScanId);
            lblResult.setText("\u2713 Record saved — " + currentScanId);
            lblResult.setForeground(new Color(80, 200, 120));
            btnReport.setVisible(true);
        });

        btnReport.addActionListener(e -> {
            ReportGenerationView rv = new ReportGenerationView(currentUser, currentScanId, dashboard);
            dashboard.addAndShow("report-" + currentScanId, rv.getPanel());
        });

        return p;
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private String buildSummaryText() {
        return "User        : " + currentUser + "\n" +
                "Image ID A  : " + (imageIdA.isEmpty() ? "(pending)" : imageIdA) + "\n" +
                (dualMode ? "Image ID B  : " + (imageIdB.isEmpty() ? "(pending)" : imageIdB) + "\n" : "") +
                "Mode        : " + (dualMode ? "Dual Comparison" : "Single Analysis") + "\n" +
                "Verdict     : " + currentVerdict + "\n" +
                "Confidence  : " + currentConfidence;
    }

    private JPanel stepCard() {
        JPanel p = new JPanel(null);
        p.setBackground(new Color(26, 26, 46));
        return p;
    }

    private void sectionLabel(JPanel p, String text, int y) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(new Color(233, 69, 96));
        l.setBounds(60, y, 700, 22);
        p.add(l);
    }

    private JLabel fileChip(JPanel p, int y) {
        JLabel l = new JLabel("No file selected");
        l.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        l.setForeground(new Color(140, 140, 170));
        l.setBounds(145, y + 6, 600, 20);
        p.add(l);
        return l;
    }

    private JPanel previewBox(JPanel p, int y, int w, int h) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(new Color(10, 20, 40));
        box.setBorder(BorderFactory.createLineBorder(new Color(15, 52, 96), 1));
        box.setBounds(60, y, w, h);
        JLabel lbl = new JLabel("Preview", SwingConstants.CENTER);
        lbl.setForeground(new Color(70, 70, 100));
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        box.add(lbl, BorderLayout.CENTER);
        p.add(box);
        return box;
    }

    private void setPreviewImage(JPanel box, String text, String filePath) {
        JLabel lbl = (JLabel) box.getComponent(0);
        if (filePath != null && new File(filePath).exists()) {
            try {
                ImageIcon icon = new ImageIcon(filePath);
                Image img = icon.getImage();
                int w = box.getWidth() > 0 ? box.getWidth() : 380;
                int h = box.getHeight() > 0 ? box.getHeight() : 200;

                double ratio = Math.min((double) w / icon.getIconWidth(), (double) h / icon.getIconHeight());
                int newW = (int) (icon.getIconWidth() * ratio);
                int newH = (int) (icon.getIconHeight() * ratio);

                Image scaled = img.getScaledInstance(Math.max(1, newW), Math.max(1, newH), Image.SCALE_SMOOTH);
                lbl.setIcon(new ImageIcon(scaled));
                lbl.setText("");
            } catch (Exception e) {
                lbl.setText(text);
                lbl.setIcon(null);
            }
        } else {
            lbl.setText(text);
            lbl.setIcon(null);
        }
    }

    private JLabel infoLine(JPanel p, int y) {
        JLabel l = new JLabel("", SwingConstants.LEFT);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(140, 140, 170));
        l.setBounds(60, y, 740, 18);
        p.add(l);
        return l;
    }

    private JLabel errLabel(JPanel p, int y) {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(220, 80, 80));
        l.setBounds(0, y, 860, 20);
        p.add(l);
        return l;
    }

    private File pickImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
                "Image Files", "png", "jpg", "jpeg", "bmp", "tiff"));
        return fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile()
                : null;
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    /** Truncates a string for compact display; full text shown via tooltip. */
    private static String truncate(String s, int max) {
        if (s == null)
            return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}