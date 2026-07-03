package displayPackage;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class RegistrationView extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField txtUsername, txtEmail;
    private JPasswordField txtPass, txtConfirm;

    public RegistrationView() {
        setTitle("ImageXposed — Register");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(500, 170, 400, 450);
        setResizable(false);

        JPanel cp = new JPanel();
        cp.setBackground(new Color(26, 26, 46));
        cp.setLayout(null);
        setContentPane(cp);

        JLabel lbl = new JLabel("Create Account", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(new Color(233, 69, 96));
        lbl.setBounds(0, 22, 400, 30);
        cp.add(lbl);

        LoginView.makeLabel(cp, "Username", 70);
        txtUsername = new JTextField();
        LoginView.styleField(txtUsername);
        txtUsername.setBounds(50, 90, 300, 36);
        cp.add(txtUsername);

        LoginView.makeLabel(cp, "Email", 138);
        txtEmail = new JTextField();
        LoginView.styleField(txtEmail);
        txtEmail.setBounds(50, 158, 300, 36);
        cp.add(txtEmail);

        LoginView.makeLabel(cp, "Password", 206);
        txtPass = new JPasswordField();
        LoginView.styleField(txtPass);
        txtPass.setBounds(50, 226, 300, 36);
        cp.add(txtPass);

        LoginView.makeLabel(cp, "Confirm Password", 274);
        txtConfirm = new JPasswordField();
        LoginView.styleField(txtConfirm);
        txtConfirm.setBounds(50, 294, 300, 36);
        cp.add(txtConfirm);

        JButton btnReg = LoginView.accentBtn("REGISTER");
        btnReg.setBounds(50, 350, 300, 40);
        cp.add(btnReg);

        JLabel lblBack = new JLabel("Back to Login", SwingConstants.CENTER);
        lblBack.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBack.setForeground(new Color(233, 69, 96));
        lblBack.setBounds(0, 400, 400, 20);
        lblBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cp.add(lblBack);

        btnReg.addActionListener(e -> {
            // TODO: RegistrationController.checkUniqueness() → create()
            JOptionPane.showMessageDialog(this, "Account created!");
            new LoginView().setVisible(true);
            dispose();
        });

        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                new LoginView().setVisible(true); dispose();
            }
        });
    }
}