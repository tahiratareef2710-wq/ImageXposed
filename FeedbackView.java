package displayPackage;

import displayPackage.controllers.FeedbackController;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * GRASP: Controller — FeedbackController wired via TODOs.
 */
public class FeedbackView {

    private final String currentUser;
    private final DashboardView dashboard;
    private final JPanel rootPanel;

    public FeedbackView(String currentUser, DashboardView dashboard) {
        this.currentUser = currentUser;
        this.dashboard   = dashboard;
        rootPanel = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel p = LoginView.blankCard();

        LoginView.centeredLabel(p, "User Feedback", 22, Font.BOLD,
            new Color(233, 69, 96), 40);

        JLabel lblBack = LoginView.linkLabel("← Dashboard");
        lblBack.setBounds(30, 43, 120, 22);
        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dashboard.showCard("home"); }
        });
        p.add(lblBack);

        LoginView.makeLabel(p, "Category", 100);
        JComboBox<String> cmbCat = new JComboBox<>(
            new String[]{"Bug Report","Feature Request","General","Performance","Other"});
        cmbCat.setBounds(280, 120, 300, 36);
        cmbCat.setBackground(new Color(15, 52, 96));
        cmbCat.setForeground(Color.WHITE);
        cmbCat.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(cmbCat);

        LoginView.makeLabel(p, "Subject", 175);
        JTextField txtSubject = new JTextField();
        LoginView.styleField(txtSubject);
        txtSubject.setBounds(280, 195, 300, 36);
        p.add(txtSubject);

        LoginView.makeLabel(p, "Description", 252);
        JTextArea txtDesc = new JTextArea();
        txtDesc.setBackground(new Color(15, 52, 96));
        txtDesc.setForeground(Color.WHITE);
        txtDesc.setCaretColor(Color.WHITE);
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(txtDesc);
        sp.setBounds(280, 272, 300, 120);
        sp.setBorder(BorderFactory.createLineBorder(new Color(233, 69, 96), 1));
        sp.getViewport().setBackground(new Color(15, 52, 96));
        p.add(sp);

        LoginView.makeLabel(p, "Rating", 412);
        JComboBox<String> cmbRating = new JComboBox<>(
            new String[]{"5 - Excellent","4 - Good","3 - Average","2 - Poor","1 - Very Poor"});
        cmbRating.setBounds(280, 432, 300, 34);
        cmbRating.setBackground(new Color(15, 52, 96));
        cmbRating.setForeground(Color.WHITE);
        cmbRating.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(cmbRating);

        JButton btnSubmit = LoginView.accentBtn("SUBMIT");
        btnSubmit.setBounds(280, 488, 300, 42);
        p.add(btnSubmit);

        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(80, 200, 120));
        lblStatus.setBounds(0, 542, 860, 22);
        p.add(lblStatus);

        btnSubmit.addActionListener(e -> {
            String category    = (String) cmbCat.getSelectedItem();
            String subject     = txtSubject.getText().trim();
            String description = txtDesc.getText().trim();
            FeedbackController ctrl = new FeedbackController();
            if (ctrl.submitFeedback(currentUser, category, subject, description)) {
                lblStatus.setText("\u2713 Thank you for your feedback!");
                txtSubject.setText("");
                txtDesc.setText("");
            } else {
                lblStatus.setText("\u2717 " + ctrl.getLastError());
                lblStatus.setForeground(new java.awt.Color(220, 80, 80));
            }
        });

        return p;
    }

    public JPanel getPanel() { return rootPanel; }
}