package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.ReportDAO;
import com.mycompany.librarymanagementsystemblp.dao.ReportDAO.Report;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ReportFrame extends JFrame {

    private static final String[] REPORT_NAMES = {
        "Overdue Books",
        "Daily Borrowings",
        "Inventory Status",
        "Books per Category",
        "Most Borrowed Books",
        "Unpaid Fines by Member"
    };

    private final ReportDAO reportDAO = new ReportDAO();

    private final JComboBox<String> cmbReport = new JComboBox<>(REPORT_NAMES);
    private final JLabel lblCount = new JLabel(" ");
    private final JTable table = new JTable();

    public ReportFrame() {
        initComponents();
        generateReport();
    }

    private void initComponents() {
        setTitle("Reports");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JButton btnGenerate = new JButton("GENERATE");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("Report:"));
        top.add(cmbReport);
        top.add(btnGenerate);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        bottom.add(lblCount);
        add(bottom, BorderLayout.SOUTH);

        btnGenerate.addActionListener(e -> generateReport());
        cmbReport.addActionListener(e -> generateReport());
        BackBar.install(this);   // Back button bar at the top of the window

        // Re-run the report whenever the user comes back to this window,
        // so it always matches what was just changed in Fines, Borrow / Return, etc.
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                generateReport(false);
            }
        });
    }

    private void generateReport() {
        generateReport(true);
    }

    // showErrors is false for the automatic refresh, so an error cannot pop up again and again
    private void generateReport(boolean showErrors) {
        try {
            Report report;
            switch (cmbReport.getSelectedIndex()) {
                case 0:  report = reportDAO.getOverdueBooks();       break;
                case 1:  report = reportDAO.getDailyBorrowings();    break;
                case 2:  report = reportDAO.getInventoryStatus();    break;
                case 3:  report = reportDAO.getBooksPerCategory();   break;
                case 4:  report = reportDAO.getMostBorrowedBooks();  break;
                default: report = reportDAO.getUnpaidFinesByMember();
            }

            DefaultTableModel model = new DefaultTableModel(report.columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            for (Object[] row : report.rows) {
                model.addRow(row);
            }
            table.setModel(model);
            lblCount.setText(report.rows.size() + " record(s) found.");

        } catch (SQLException ex) {
            if (showErrors) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
