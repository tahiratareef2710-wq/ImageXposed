package displayPackage;

import displayPackage.controllers.ManageScanHistoryController;
import displayPackage.models.Scan;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * GRASP: Controller — ManageScanHistoryController logic wired via TODOs.
 */
public class ScanHistoryView {

    private final String currentUser;
    private final DashboardView dashboard;
    private final JPanel rootPanel;
    private DefaultTableModel model;

    public ScanHistoryView(String currentUser, DashboardView dashboard) {
        this.currentUser = currentUser;
        this.dashboard   = dashboard;
        rootPanel = buildPanel();
    }

    private JPanel buildPanel() {
        JPanel p = LoginView.blankCard();

        LoginView.centeredLabel(p, "Scan History", 22, Font.BOLD,
            new Color(233, 69, 96), 30);

        JLabel lblBack = LoginView.linkLabel("\u2190 Dashboard");
        lblBack.setBounds(30, 32, 120, 22);
        lblBack.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { dashboard.showCard("home"); }
        });
        p.add(lblBack);

        String[] cols = {"Scan ID", "Image ID", "Date", "Verdict", "Confidence"};

        // Empty model — data is loaded via refresh() each time the panel becomes visible
        model = new DefaultTableModel(new Object[0][5], cols) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setBackground(new Color(22, 33, 62));
        table.setForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(233, 69, 96));
        table.getTableHeader().setBackground(new Color(15, 52, 96));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(40, 82, 780, 380);
        sp.getViewport().setBackground(new Color(22, 33, 62));
        sp.setBorder(BorderFactory.createLineBorder(new Color(15, 52, 96), 1));
        p.add(sp);

        JButton btnDelete = LoginView.accentBtn("Delete Selected");
        btnDelete.setBackground(new Color(150, 30, 50));
        btnDelete.setBounds(40, 480, 180, 38);
        p.add(btnDelete);

        JButton btnRefresh = LoginView.ghostBtn("Refresh");
        btnRefresh.setBounds(660, 480, 160, 38);
        p.add(btnRefresh);

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(null, "Select a row first."); return; }
            String sid = (String) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(null, "Delete " + sid + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                ManageScanHistoryController delCtrl = new ManageScanHistoryController();
                if (delCtrl.deleteScanRecord(sid)) {
                    model.removeRow(row);
                } else {
                    JOptionPane.showMessageDialog(null, delCtrl.getLastError());
                }
            }
        });

        btnRefresh.addActionListener(e -> refresh());

        return p;
    }

    /**
     * Reloads all scan records from the database for the current user.
     * Called by DashboardView.showCard("history") so data is always current,
     * and also wired to the Refresh button.
     */
    public void refresh() {
        ManageScanHistoryController ctrl = new ManageScanHistoryController();
        Object[][] fresh = ctrl.toTableData(ctrl.loadScanHistory(currentUser));
        model.setRowCount(0);
        for (Object[] row : fresh) model.addRow(row);
    }

    public JPanel getPanel() { return rootPanel; }
}