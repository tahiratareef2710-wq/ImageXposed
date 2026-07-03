package displayPackage;

import displayPackage.controllers.ManageProfileController;
import displayPackage.models.User;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * GRASP: Controller — ManageProfileController wired via TODOs.
 */
public class ProfileView {

    private final String currentUser;
    private final DashboardView dashboard;
    private final JPanel rootPanel;

    public ProfileView(String currentUser, DashboardView dashboard) {
        this.currentUser = currentUser;
        this.dashboard   = dashboard;
        rootPanel = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel p = LoginView.blankCard();

        LoginView.centeredLabel(p, "My Profile", 22, Font.BOLD,
            new Color(233, 69, 96), 50);

        JLabel lblBack = LoginView.linkLabel("← Dashboard");
        lblBack.setBounds(30, 52, 120, 22);
        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dashboard.showCard("home"); }
        });
        p.add(lblBack);

        // Pre-fill email from DB
        ManageProfileController ctrl = new ManageProfileController();
        User user = ctrl.retrieveProfileData(currentUser);
        LoginView.makeLabel(p, "Username", 130);
        JTextField txtUsername = new JTextField(currentUser);
        txtUsername.setEditable(false);            // username is immutable
        LoginView.styleField(txtUsername);
        txtUsername.setBounds(280, 150, 300, 38);
        p.add(txtUsername);

        LoginView.makeLabel(p, "Email", 210);
        JTextField txtEmail = new JTextField(user != null ? user.getEmail() : "");
        LoginView.styleField(txtEmail);
        txtEmail.setBounds(280, 230, 300, 38);
        p.add(txtEmail);

        LoginView.makeLabel(p, "New Password", 290);
        JPasswordField txtPass = new JPasswordField();
        LoginView.styleField(txtPass);
        txtPass.setBounds(280, 310, 300, 38);
        p.add(txtPass);

        JButton btnSave = LoginView.accentBtn("SAVE CHANGES");
        btnSave.setBounds(280, 375, 300, 42);
        p.add(btnSave);

        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(80, 200, 120));
        lblStatus.setBounds(0, 430, 860, 22);
        p.add(lblStatus);

        btnSave.addActionListener(e -> {
            ManageProfileController saveCtrl = new ManageProfileController();
            String email   = txtEmail.getText().trim();
            String newPass = new String(txtPass.getPassword());
            if (saveCtrl.saveUpdatedProfile(currentUser, email, newPass)) {
                lblStatus.setText("\u2713 Profile updated successfully.");
                lblStatus.setForeground(new java.awt.Color(80, 200, 120));
            } else {
                lblStatus.setText("\u2717 " + saveCtrl.getLastError());
                lblStatus.setForeground(new java.awt.Color(220, 80, 80));
            }
        });

        return p;
    }

    public JPanel getPanel() { return rootPanel; }
}