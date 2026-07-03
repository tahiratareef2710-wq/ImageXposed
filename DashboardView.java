package displayPackage;

import java.awt.*;
import javax.swing.*;

/**
 * DashboardView does not extend JFrame — it provides a JPanel
 * that gets swapped into the single LoginView window.
 * GRASP: Controller — routes user intent to the correct sub-panel.
 */
public class DashboardView {

    private final String currentUser;
    private final JPanel rootPanel;
    private final CardLayout cardLayout;
    private final LoginView window;

    // Sub-panels built once and reused
    private WorkflowView singleWorkflow;
    private WorkflowView dualWorkflow;
    private ScanHistoryView historyView;
    private ProfileView profileView;
    private FeedbackView feedbackView;

    public DashboardView(String username, LoginView window) {
        this.currentUser = username;
        this.window      = window;

        cardLayout = new CardLayout();
        rootPanel  = new JPanel(cardLayout);
        rootPanel.setBackground(new Color(26, 26, 46));

        // Build all sub-panels
        singleWorkflow = new WorkflowView(username, false,  this);
        dualWorkflow   = new WorkflowView(username, true,   this);
        historyView    = new ScanHistoryView(username,      this);
        profileView    = new ProfileView(username,          this);
        feedbackView   = new FeedbackView(username,         this);

        rootPanel.add(buildHomeCard(),          "home");
        rootPanel.add(singleWorkflow.getPanel(),"single");
        rootPanel.add(dualWorkflow.getPanel(),  "dual");
        rootPanel.add(historyView.getPanel(),   "history");
        rootPanel.add(profileView.getPanel(),   "profile");
        rootPanel.add(feedbackView.getPanel(),  "feedback");

        showCard("home");
    }

    // ── Home card ──────────────────────────────────────────────────────────────
    private JPanel buildHomeCard() {
        JPanel p = LoginView.blankCard();

        LoginView.centeredLabel(p, "ImageXposed", 28, Font.BOLD,
            new Color(233, 69, 96), 100);
        LoginView.centeredLabel(p, "Welcome, " + currentUser, 13, Font.PLAIN,
            new Color(140, 140, 170), 148);

        // Row 1 — analysis
        JButton btnSingle = LoginView.accentBtn("Single Image Analysis");
        btnSingle.setBounds(190, 210, 210, 52);
        p.add(btnSingle);

        JButton btnDual = LoginView.ghostBtn("Compare Two Images");
        btnDual.setBounds(460, 210, 210, 52);
        p.add(btnDual);

        // Row 2
        JButton btnHistory = LoginView.ghostBtn("Scan History");
        btnHistory.setBounds(190, 292, 210, 52);
        p.add(btnHistory);

        JButton btnProfile = LoginView.ghostBtn("My Profile");
        btnProfile.setBounds(460, 292, 210, 52);
        p.add(btnProfile);

        // Row 3
        JButton btnFeedback = LoginView.ghostBtn("Feedback");
        btnFeedback.setBounds(190, 374, 210, 52);
        p.add(btnFeedback);

        JButton btnLogout = LoginView.ghostBtn("Logout");
        btnLogout.setBounds(460, 374, 210, 52);
        p.add(btnLogout);

        btnSingle.addActionListener(e  -> showCard("single"));
        btnDual.addActionListener(e    -> showCard("dual"));
        btnHistory.addActionListener(e -> showCard("history"));
        btnProfile.addActionListener(e -> showCard("profile"));
        btnFeedback.addActionListener(e-> showCard("feedback"));
        btnLogout.addActionListener(e  -> logout());

        return p;
    }

    // ── Navigation helpers ─────────────────────────────────────────────────────
    public void showCard(String name) {
        // Refresh scan history data every time that panel becomes visible
        // so the table always shows actual DB timestamps, not stale data.
        if ("history".equals(name)) {
            historyView.refresh();
        }
        cardLayout.show(rootPanel, name);
        updateTitle(name);
    }

    private void updateTitle(String card) {
        switch (card) {
            case "home":     window.setTitle("ImageXposed \u2014 Dashboard");         break;
            case "single":   window.setTitle("ImageXposed \u2014 Single Analysis");   break;
            case "dual":     window.setTitle("ImageXposed \u2014 Compare Images");    break;
            case "history":  window.setTitle("ImageXposed \u2014 Scan History");      break;
            case "profile":  window.setTitle("ImageXposed \u2014 My Profile");        break;
            case "feedback": window.setTitle("ImageXposed \u2014 Feedback");          break;
            default:         window.setTitle("ImageXposed");
        }
    }

    private void logout() {
        displayPackage.core.ActiveSession.getInstance().logout();
        window.resetToLogin();
    }

    public JPanel getRootPanel()    { return rootPanel; }
    public String  getCurrentUser() { return currentUser; }
    public LoginView getWindow()    { return window; }

    // Allow WorkflowView to push a ReportView card at runtime
    public void addAndShow(String key, JPanel panel) {
        rootPanel.add(panel, key);
        showCard(key);
    }
}