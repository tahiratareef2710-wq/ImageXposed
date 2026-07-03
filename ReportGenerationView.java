package displayPackage;

import displayPackage.controllers.GenerateReportController;
import displayPackage.controllers.ExportReportController;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * GRASP: Controller — GenerateReportController + ExportReportController in one panel.
 * GoF:   Strategy   — export formats (PDF/TXT/HTML/CSV) should each be a concrete
 *                     ExportStrategy implementing: File execute(String reportId, String path)
 *                     ExportReportController selects the right strategy via a Map<String,ExportStrategy>.
 */
public class ReportGenerationView {

    private final String currentUser;
    private final DashboardView dashboard;
    private final JPanel rootPanel;

    private JTextArea txtPreview;
    private JTextField txtPath;
    private String generatedReportId = "";

    /** Called from WorkflowView with a pre-filled scanId */
    public ReportGenerationView(String currentUser, String scanId, DashboardView dashboard) {
        this(currentUser, dashboard);
        generateReport(currentUser, scanId);
    }

    public ReportGenerationView(String currentUser, DashboardView dashboard) {
        this.currentUser = currentUser;
        this.dashboard   = dashboard;
        rootPanel = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel p = LoginView.blankCard();

        LoginView.centeredLabel(p, "Forensic Report", 22, Font.BOLD,
            new Color(233, 69, 96), 25);

        JLabel lblBack = LoginView.linkLabel("← Dashboard");
        lblBack.setBounds(30, 28, 120, 22);
        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dashboard.showCard("home"); }
        });
        p.add(lblBack);

        // Scan ID input row
        LoginView.makeLabel(p, "Scan ID:", 72);
        JTextField txtScanId = new JTextField();
        LoginView.styleField(txtScanId);
        txtScanId.setBounds(30, 92, 680, 36);
        p.add(txtScanId);

        JButton btnGen = LoginView.accentBtn("Generate");
        btnGen.setBounds(722, 92, 110, 36);
        p.add(btnGen);

        // Preview
        LoginView.makeLabel(p, "Preview:", 142);
        txtPreview = new JTextArea("Generate a report to see preview...");
        txtPreview.setBackground(new Color(10, 20, 40));
        txtPreview.setForeground(new Color(160, 210, 160));
        txtPreview.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtPreview.setEditable(false);
        txtPreview.setLineWrap(true);
        txtPreview.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(txtPreview);
        sp.setBounds(30, 162, 800, 220);
        sp.setBorder(BorderFactory.createLineBorder(new Color(15, 52, 96), 1));
        sp.getViewport().setBackground(new Color(10, 20, 40));
        p.add(sp);

        // Divider
        JSeparator div = new JSeparator();
        div.setForeground(new Color(40, 50, 80));
        div.setBounds(30, 398, 800, 1);
        p.add(div);

        // Export section
        JLabel lblExport = new JLabel("Export Report");
        lblExport.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblExport.setForeground(new Color(233, 69, 96));
        lblExport.setBounds(30, 412, 200, 22);
        p.add(lblExport);

        JComboBox<String> cmbFormat = new JComboBox<>(new String[]{"PDF","TXT","HTML","CSV"});
        cmbFormat.setBounds(30, 446, 120, 34);
        cmbFormat.setBackground(new Color(15, 52, 96));
        cmbFormat.setForeground(Color.WHITE);
        cmbFormat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(cmbFormat);

        txtPath = new JTextField("Choose destination folder...");
        txtPath.setBounds(165, 446, 490, 34);
        txtPath.setBackground(new Color(15, 52, 96));
        txtPath.setForeground(new Color(120, 120, 150));
        txtPath.setCaretColor(Color.WHITE);
        txtPath.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(233, 69, 96), 1),
            new EmptyBorder(5, 10, 5, 10)));
        p.add(txtPath);

        JButton btnBrowse = LoginView.ghostBtn("...");
        btnBrowse.setBounds(665, 446, 60, 34);
        p.add(btnBrowse);

        JButton btnExport = LoginView.accentBtn("EXPORT");
        btnExport.setBounds(735, 446, 95, 34);
        p.add(btnExport);

        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(80, 200, 120));
        lblStatus.setBounds(0, 496, 860, 20);
        p.add(lblStatus);

        // Events
        btnGen.addActionListener(e -> {
            String sid = txtScanId.getText().trim();
            if (sid.isEmpty()) { JOptionPane.showMessageDialog(null, "Enter Scan ID."); return; }
            generateReport(currentUser, sid);
        });

        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                txtPath.setText(fc.getSelectedFile().getAbsolutePath());
                txtPath.setForeground(Color.WHITE);
            }
        });

        btnExport.addActionListener(e -> {
            if (generatedReportId.isEmpty()) { JOptionPane.showMessageDialog(null, "Generate a report first."); return; }
            String fmt  = (String) cmbFormat.getSelectedItem();
            String path = txtPath.getText().trim();
            ExportReportController exportCtrl = new ExportReportController();
            java.io.File exported = exportCtrl.exportReport(generatedReportId, fmt, path);
            if (exported != null) {
                lblStatus.setText("\u2713 Exported as " + fmt + "  \u2192  " + path);
            } else {
                lblStatus.setText("\u2717 " + exportCtrl.getLastError());
                lblStatus.setForeground(new java.awt.Color(220, 80, 80));
            }
        });

        return p;
    }

    private void generateReport(String user, String scanId) {
        GenerateReportController ctrl = new GenerateReportController();
        GenerateReportController.ReportResult result = ctrl.generateReport(scanId);
        if (result != null) {
            generatedReportId = result.reportId;
            txtPreview.setText(result.previewText);
        } else {
            // Show the actual error from the controller — no fake data
            generatedReportId = "";
            txtPreview.setText(
                "[Report generation failed]\n\n" +
                "Scan ID : " + scanId + "\n" +
                "Reason  : " + ctrl.getLastError() + "\n\n" +
                "Make sure the scan was saved (Step 5 — SAVE RECORD) before generating a report."
            );
        }
    }

    public JPanel getPanel() { return rootPanel; }
}