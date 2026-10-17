package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.FineDAO;
import com.mycompany.librarymanagementsystemblp.model.Fine;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class FineFrame extends JFrame {

    private final FineDAO fineDAO = new FineDAO();

    private List<Fine> currentFines = new ArrayList<>();
    private Fine selectedFine = null;

    // Read-only details of the selected fine
    private final JTextField txtFineId = new JTextField(10);
    private final JTextField txtMember = new JTextField(20);
    private final JTextField txtBook = new JTextField(25);
    private final JTextField txtAmount = new JTextField(10);
    private final JTextField txtStatus = new JTextField(10);

    private final JTextField txtSearch = new JTextField(20);
    private final JCheckBox chkUnpaidOnly = new JCheckBox("Show only unpaid fines");
    private final JLabel lblTotal = new JLabel("Total unpaid: PHP 0.00");

    private DefaultTableModel tableModel;
    private JTable table;

    public FineFrame() {
        initComponents();
        chkUnpaidOnly.setSelected(true);
        refreshTable();
    }

    private void initComponents() {
        setTitle("Fines");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 580);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        for (JTextField f : new JTextField[]{txtFineId, txtMember, txtBook, txtAmount, txtStatus}) {
            f.setEditable(false);
        }

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Fine ID:", txtFineId);
        addField(form, 0, 1, "Member:", txtMember);
        addField(form, 1, 0, "Amount (PHP):", txtAmount);
        addField(form, 1, 1, "Book:", txtBook);
        addField(form, 2, 0, "Status:", txtStatus);

        JButton btnPay = new JButton("MARK AS PAID");
        JButton btnClear = new JButton("CLEAR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnPay);
        buttons.add(btnClear);

        JLabel note = new JLabel("Fines are created automatically when a book is returned late.");
        note.setFont(note.getFont().deriveFont(Font.ITALIC));
        note.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.add(note, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JButton btnSearch = new JButton("SEARCH");
        JButton btnShowAll = new JButton("SHOW ALL");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (member or book title):"));
        searchPanel.add(txtSearch);
        searchPanel.add(chkUnpaidOnly);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        tableModel = new DefaultTableModel(
            new String[]{"Fine ID", "Record ID", "Member", "Book",
                         "Amount (PHP)", "Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRow();
            }
        });

        JPanel center = new JPanel(new BorderLayout());
        center.add(searchPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 14f));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        bottom.add(lblTotal);
        add(bottom, BorderLayout.SOUTH);

        btnPay.addActionListener(e -> markAsPaid());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> refreshTable());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            chkUnpaidOnly.setSelected(false);
            refreshTable();
        });
    }

    private void addField(JPanel panel, int row, int col, String label, JComponent comp) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = row;

        gbc.gridx = col * 2;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = col * 2 + 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    private void refreshTable() {
        try {
            String keyword = txtSearch.getText().trim();
            currentFines = fineDAO.getFines(keyword, chkUnpaidOnly.isSelected());
            selectedFine = null;
            tableModel.setRowCount(0);
            for (Fine f : currentFines) {
                tableModel.addRow(new Object[]{
                    f.getFineId(), f.getDetailId(), f.getMemberName(), f.getBookTitle(),
                    f.getAmount().setScale(2).toPlainString(), f.getFineDate(), f.getStatus()
                });
            }
            lblTotal.setText("Total unpaid: PHP "
                + fineDAO.getTotalUnpaid().setScale(2).toPlainString());
            if (currentFines.isEmpty() && !keyword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentFines.size()) {
            return;
        }
        selectedFine = currentFines.get(row);
        txtFineId.setText(String.valueOf(selectedFine.getFineId()));
        txtMember.setText(selectedFine.getMemberName());
        txtBook.setText(selectedFine.getBookTitle());
        txtAmount.setText(selectedFine.getAmount().setScale(2).toPlainString());
        txtStatus.setText(selectedFine.getStatus());
    }

    private void markAsPaid() {
        if (selectedFine == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a fine from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("Paid".equals(selectedFine.getStatus())) {
            JOptionPane.showMessageDialog(this, "This fine is already paid.",
                "Already Paid", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Confirm that " + selectedFine.getMemberName() + " paid PHP "
                + selectedFine.getAmount().setScale(2).toPlainString() + "?",
            "Confirm Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            fineDAO.markAsPaid(selectedFine.getFineId());
            JOptionPane.showMessageDialog(this, "Fine marked as paid.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Update", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearForm() {
        txtFineId.setText("");
        txtMember.setText("");
        txtBook.setText("");
        txtAmount.setText("");
        txtStatus.setText("");
        table.clearSelection();
        selectedFine = null;
    }

    private void handleSqlError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}
