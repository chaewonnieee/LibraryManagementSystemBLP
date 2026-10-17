package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.MemberDAO;
import com.mycompany.librarymanagementsystemblp.model.Member;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MemberFrame extends JFrame {

    private final MemberDAO memberDAO = new MemberDAO();

    private List<Member> currentMembers = new ArrayList<>();
    private Member selectedMember = null;

    private final JTextField txtMemberId = new JTextField(10);
    private final JTextField txtFirstName = new JTextField(15);
    private final JTextField txtLastName = new JTextField(15);
    private final JTextField txtContact = new JTextField(15);
    private final JTextField txtEmail = new JTextField(20);
    private final JTextField txtAddress = new JTextField(20);
    private final JComboBox<String> cmbStatus =
        new JComboBox<>(new String[]{"Active", "Inactive"});

    private final JTextField txtSearch = new JTextField(20);

    private DefaultTableModel tableModel;
    private JTable table;

    public MemberFrame() {
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Member Management");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        txtMemberId.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Member ID:", txtMemberId);
        addField(form, 0, 1, "Status:", cmbStatus);
        addField(form, 1, 0, "First Name:", txtFirstName);
        addField(form, 1, 1, "Last Name:", txtLastName);
        addField(form, 2, 0, "Contact No:", txtContact);
        addField(form, 2, 1, "Email:", txtEmail);
        addField(form, 3, 0, "Address:", txtAddress);

        JButton btnSave = new JButton("SAVE");
        JButton btnUpdate = new JButton("UPDATE");
        JButton btnDelete = new JButton("DELETE");
        JButton btnClear = new JButton("CLEAR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnSave);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JButton btnSearch = new JButton("SEARCH");
        JButton btnShowAll = new JButton("SHOW ALL");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (name, email, contact):"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "First Name", "Last Name", "Contact No",
                         "Email", "Address", "Registered", "Status"}, 0) {
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

        btnSave.addActionListener(e -> saveMember());
        btnUpdate.addActionListener(e -> updateMember());
        btnDelete.addActionListener(e -> deleteMember());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchMembers());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable();
        });
        BackBar.install(this);   // Back button bar at the top of the window
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
            fillTable(memberDAO.getAllMembers());
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void fillTable(List<Member> members) {
        currentMembers = members;
        selectedMember = null;
        tableModel.setRowCount(0);
        for (Member m : members) {
            tableModel.addRow(new Object[]{
                m.getMemberId(), m.getFirstName(), m.getLastName(),
                m.getContactNo(), m.getEmail(), m.getAddress(),
                m.getDateRegistered(), m.getStatus()
            });
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentMembers.size()) {
            return;
        }
        selectedMember = currentMembers.get(row);

        txtMemberId.setText(String.valueOf(selectedMember.getMemberId()));
        txtFirstName.setText(selectedMember.getFirstName());
        txtLastName.setText(selectedMember.getLastName());
        txtContact.setText(selectedMember.getContactNo() == null ? "" : selectedMember.getContactNo());
        txtEmail.setText(selectedMember.getEmail() == null ? "" : selectedMember.getEmail());
        txtAddress.setText(selectedMember.getAddress() == null ? "" : selectedMember.getAddress());
        cmbStatus.setSelectedItem(selectedMember.getStatus());
    }

    private void saveMember() {
        Member m = buildMemberFromForm();
        if (m == null) {
            return;
        }
        try {
            memberDAO.addMember(m);
            JOptionPane.showMessageDialog(this, "Member saved successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void updateMember() {
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a member from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Member m = buildMemberFromForm();
        if (m == null) {
            return;
        }
        m.setMemberId(selectedMember.getMemberId());
        try {
            memberDAO.updateMember(m);
            JOptionPane.showMessageDialog(this, "Member updated successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void deleteMember() {
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a member from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this record?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            memberDAO.deleteMember(selectedMember.getMemberId());
            JOptionPane.showMessageDialog(this, "Member deleted successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void searchMembers() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            List<Member> results = memberDAO.searchMembers(keyword);
            fillTable(results);
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void clearForm() {
        txtMemberId.setText("");
        txtFirstName.setText("");
        txtLastName.setText("");
        txtContact.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        cmbStatus.setSelectedIndex(0);
        table.clearSelection();
        selectedMember = null;
    }

    // Returns a Member built from the form, or null if validation fails
    private Member buildMemberFromForm() {
        String first = txtFirstName.getText().trim();
        String last = txtLastName.getText().trim();
        String contact = txtContact.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (first.isEmpty() || last.isEmpty()) {
            showWarning("First Name and Last Name are required.");
            return null;
        }
        if (!contact.isEmpty() && !contact.matches("[0-9+\\- ]{7,30}")) {
            showWarning("Contact No may contain only numbers, spaces, + and -, "
                      + "and must be at least 7 characters.");
            return null;
        }
        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            showWarning("Please enter a valid email address (example: name@mail.com).");
            return null;
        }

        Member m = new Member();
        m.setFirstName(first);
        m.setLastName(last);
        m.setContactNo(contact.isEmpty() ? null : contact);
        m.setEmail(email.isEmpty() ? null : email);
        m.setAddress(address.isEmpty() ? null : address);
        m.setStatus((String) cmbStatus.getSelectedItem());
        return m;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message,
            "Missing or Invalid Information", JOptionPane.WARNING_MESSAGE);
    }

    private void handleSqlError(SQLException ex) {
        String message;
        if ("23505".equals(ex.getSQLState())) {
            message = "A member with this email already exists.";
        } else if ("23503".equals(ex.getSQLState())) {
            message = "This member cannot be deleted because they have "
                    + "borrowing or reservation records.";
        } else {
            message = "Database error: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}