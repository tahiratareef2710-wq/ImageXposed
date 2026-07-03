package displayPackage;

import displayPackage.controllers.LoginController;
import displayPackage.controllers.RegistrationController;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class LoginView extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try { new LoginView().setVisible(true); }
            catch (Exception e) { e.printStackTrace(); }
        });
    }
    
    public JPanel buildLoginCard_public() { return buildLoginCard(); }

    public LoginView() {
        setTitle("ImageXposed");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(350, 120, 860, 620);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(26, 26, 46));
        setContentPane(mainPanel);

        mainPanel.add(buildLoginCard(),    "login");
        mainPanel.add(buildRegisterCard(), "register");
        cardLayout.show(mainPanel, "login");
    }

    // ── Login card ─────────────────────────────────────────────────────────────
    private JPanel buildLoginCard() {
        JPanel p = blankCard();

        centeredLabel(p, "ImageXposed", 28, Font.BOLD, new Color(233, 69, 96), 100);
        centeredLabel(p, "Image Forensics Platform", 13, Font.PLAIN, new Color(140, 140, 170), 145);

        makeLabel(p, "Username", 210);
        txtUsername = new JTextField();
        styleField(txtUsername);
        txtUsername.setBounds(280, 232, 300, 38);
        p.add(txtUsername);

        makeLabel(p, "Password", 290);
        txtPassword = new JPasswordField();
        styleField(txtPassword);
        txtPassword.setBounds(280, 312, 300, 38);
        p.add(txtPassword);

        JButton btnLogin = accentBtn("LOGIN");
        btnLogin.setBounds(280, 374, 300, 42);
        p.add(btnLogin);

        JLabel lblReg = linkLabel("No account? Register here");
        lblReg.setBounds(280, 430, 300, 22);
        p.add(lblReg);

        btnLogin.addActionListener(e -> {
            String u  = txtUsername.getText().trim();
            String pw = new String(txtPassword.getPassword());
            LoginController ctrl = new LoginController();
            if (ctrl.submitCredentials(u, pw)) {
                switchToDashboard(u);
            } else {
                showMsg(ctrl.getLastError());
            }
        });

        lblReg.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { cardLayout.show(mainPanel, "register"); }
        });

        return p;
    }

    // ── Register card ──────────────────────────────────────────────────────────
    private JPanel buildRegisterCard() {
        JPanel p = blankCard();

        centeredLabel(p, "Create Account", 24, Font.BOLD, new Color(233, 69, 96), 90);

        JTextField txtU = new JTextField();  styleField(txtU);  txtU.setBounds(280, 160, 300, 38);
        JTextField txtE = new JTextField();  styleField(txtE);  txtE.setBounds(280, 240, 300, 38);
        JPasswordField txtP  = new JPasswordField(); styleField(txtP);  txtP.setBounds(280, 320, 300, 38);
        JPasswordField txtC  = new JPasswordField(); styleField(txtC);  txtC.setBounds(280, 400, 300, 38);

        makeLabel(p, "Username",         138); p.add(txtU);
        makeLabel(p, "Email",            218); p.add(txtE);
        makeLabel(p, "Password",         298); p.add(txtP);
        makeLabel(p, "Confirm Password", 378); p.add(txtC);

        JButton btnReg = accentBtn("REGISTER");
        btnReg.setBounds(280, 458, 300, 42);
        p.add(btnReg);

        JLabel lblBack = linkLabel("← Back to Login");
        lblBack.setBounds(280, 512, 300, 22);
        p.add(lblBack);

        btnReg.addActionListener(e -> {
            String u = txtU.getText().trim();
            String em = txtE.getText().trim();
            String pw = new String(txtP.getPassword());
            String cp = new String(txtC.getPassword());
            RegistrationController ctrl = new RegistrationController();
            if (ctrl.submitRegistration(u, em, pw, cp)) {
                showMsg("Account created! Please log in.");
                cardLayout.show(mainPanel, "login");
            } else {
                showMsg(ctrl.getLastError());
            }
        });
        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { cardLayout.show(mainPanel, "login"); }
        });

        return p;
    }

    // ── Navigation ─────────────────────────────────────────────────────────────
    private void switchToDashboard(String username) {
        // Replace this window content with the dashboard
        DashboardView dash = new DashboardView(username, this);
        setContentPane(dash.getRootPanel());
        setTitle("ImageXposed — Dashboard");
        revalidate();
        repaint();
    }

    public void resetToLogin() {
        txtUsername.setText("");
        txtPassword.setText("");
        setContentPane(mainPanel);
        cardLayout.show(mainPanel, "login");
        setTitle("ImageXposed");
        revalidate();
        repaint();
    }

    // ── Shared helpers (static so other views can reuse) ──────────────────────
    static JPanel blankCard() {
        JPanel p = new JPanel(null);
        p.setBackground(new Color(26, 26, 46));
        return p;
    }

    static void centeredLabel(JPanel p, String text, int size, int style,
                               Color color, int y) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        l.setBounds(0, y, 860, size + 6);
        p.add(l);
    }

    static void makeLabel(JPanel p, String text, int y) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(150, 150, 180));
        l.setBounds(280, y, 300, 18);
        p.add(l);
    }

    static void styleField(JComponent c) {
        c.setBackground(new Color(15, 52, 96));
        if (c instanceof JTextField)     ((JTextField)     c).setForeground(Color.WHITE);
        if (c instanceof JPasswordField) ((JPasswordField) c).setForeground(Color.WHITE);
        if (c instanceof JTextField)     ((JTextField)     c).setCaretColor(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(233, 69, 96), 1),
            new EmptyBorder(5, 10, 5, 10)));
    }

    static JButton accentBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(233, 69, 96));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(22, 33, 62));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setBorder(BorderFactory.createLineBorder(new Color(233, 69, 96), 1));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JLabel linkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(233, 69, 96));
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return l;
    }

    void showMsg(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }
}