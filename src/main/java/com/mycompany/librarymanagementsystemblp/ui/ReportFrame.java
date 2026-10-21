package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.ReportDAO;
import com.mycompany.librarymanagementsystemblp.dao.ReportDAO.Report;
import java.awt.*;
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
    private final JLabel lblCount = new JLabel("Choose a report, then click GENERATE.");
    private final JButton btnExport = new JButton("EXPORT CSV");
    private final JTable table = new JTable();

    public ReportFrame() {
        initComponents();
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
        top.add(btnExport);
        btnExport.setEnabled(false);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        bottom.add(lblCount);
        add(bottom, BorderLayout.SOUTH);

        btnGenerate.addActionListener(e -> generateReport());
        btnExport.addActionListener(e -> exportCsv());
        cmbReport.addActionListener(e -> clearResult());
        BackBar.install(this);
    }

    private void clearResult() {
        table.setModel(new DefaultTableModel());
        lblCount.setText("Choose a report, then click GENERATE.");
        btnExport.setEnabled(false);
    }

    private void generateReport() {
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
            lblCount.setText(report.rows.size() + " record(s) found.  Generated "
                + java.time.LocalTime.now().withNano(0) + ".");
            btnExport.setEnabled(!report.rows.isEmpty());

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        String name = cmbReport.getSelectedItem().toString().replace(' ', '_')
                    + "_" + java.time.LocalDate.now() + ".csv";
        chooser.setSelectedFile(new java.io.File(name));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (java.io.PrintWriter out = new java.io.PrintWriter(chooser.getSelectedFile(), "UTF-8")) {
            DefaultTableModel m = (DefaultTableModel) table.getModel();
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < m.getColumnCount(); c++) {
                sb.append(c > 0 ? "," : "").append(csv(m.getColumnName(c)));
            }
            out.println(sb);
            for (int r = 0; r < m.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < m.getColumnCount(); c++) {
                    sb.append(c > 0 ? "," : "").append(csv(String.valueOf(m.getValueAt(r, c))));
                }
                out.println(sb);
            }
            JOptionPane.showMessageDialog(this, "Report saved to:\n" + chooser.getSelectedFile());
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save file: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csv(String v) {
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
