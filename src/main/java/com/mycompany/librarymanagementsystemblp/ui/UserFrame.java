package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.UserDAO;
import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class UserFrame extends JFrame {

    private final UserDAO userDAO = new UserDAO();
    private final User currentUser;   // the administrator who is logged in

    private List<User> currentUsers = new ArrayList<>();
    private User selectedUser = null;

    private final JTextField txtUserId = new JTextField(10);
    private final JTextField txtUsername = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JTextField txtFullName = new JTextField(20);
    private final JComboBox<String> cmbRole =
        new JComboBox<>(new String[]{"Administrator", "Librarian"});
    private final JComboBox<String> cmbStatus =
        new JComboBox<>(new String[]{"Active", "Inactive"});

    private final JTextField txtSearch = new JTextField(20);

    private DefaultTableModel tableModel;
    private JTable table;

    public UserFrame(User currentUser) {
        this.currentUser = currentUser;
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        setTitle("User Management");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        txtUserId.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "User ID:", txtUserId);
        addField(form, 0, 1, "Role:", cmbRole);
        addField(form, 1, 0, "Username:", txtUsername);
        addField(form, 1, 1, "Full Name:", txtFullName);
        addField(form, 2, 0, "Password:", txtPassword);
        addField(form, 2, 1, "Status:", cmbStatus);

        JButton btnSave = new JButton("SAVE");
        JButton btnUpdate = new JButton("UPDATE");
        JButton btnDelete = new JButton("DELETE");
        JButton btnClear = new JButton("CLEAR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnSave);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        JLabel note = new JLabel("When updating, leave Password empty to keep the current password.");
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
        searchPanel.add(new JLabel("Search (username, name, role):"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        // The password is never shown in the table
        tableModel = new DefaultTableModel(
            new String[]{"ID", "Username", "Full Name", "Role", "Status"}, 0) {
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

        btnSave.addActionListener(e -> saveUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchUsers());
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
            fillTable(userDAO.getAllUsers());
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void fillTable(List<User> users) {
        currentUsers = users;
        selectedUser = null;
        tableModel.setRowCount(0);
        for (User u : users) {
            tableModel.addRow(new Object[]{
                u.getUserId(), u.getUsername(), u.getFullName(), u.getRole(), u.getStatus()
            });
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentUsers.size()) {
            return;
        }
        selectedUser = currentUsers.get(row);
        txtUserId.setText(String.valueOf(selectedUser.getUserId()));
        txtUsername.setText(selectedUser.getUsername());
        txtFullName.setText(selectedUser.getFullName());
        txtPassword.setText("");
        cmbRole.setSelectedItem(selectedUser.getRole());
        cmbStatus.setSelectedItem(selectedUser.getStatus());
    }

    private void saveUser() {
        User u = buildUserFromForm(true);
        if (u == null) {
            return;
        }
        try {
            userDAO.addUser(u);
            JOptionPane.showMessageDialog(this, "User saved successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void updateUser() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a user from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        User u = buildUserFromForm(false);
        if (u == null) {
            return;
        }
        // An administrator must not lock themselves out
        if (selectedUser.getUserId() == currentUser.getUserId()
                && ("Inactive".equals(u.getStatus()) || !"Administrator".equals(u.getRole()))) {
            showWarning("You cannot deactivate or demote the account you are logged in with.");
            return;
        }
        u.setUserId(selectedUser.getUserId());
        boolean changePassword = !u.getPassword().isEmpty();

        try {
            userDAO.updateUser(u, changePassword);
            JOptionPane.showMessageDialog(this, "User updated successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void deleteUser() {
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a user from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedUser.getUserId() == currentUser.getUserId()) {
            showWarning("You cannot delete the account you are logged in with.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this record?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            userDAO.deleteUser(selectedUser.getUserId());
            JOptionPane.showMessageDialog(this, "User deleted successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void searchUsers() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            List<User> results = userDAO.searchUsers(keyword);
            fillTable(results);
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void clearForm() {
        txtUserId.setText("");
        txtUsername.setText("");
        txtFullName.setText("");
        txtPassword.setText("");
        cmbRole.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        table.clearSelection();
        selectedUser = null;
    }

    // Returns a User built from the form, or null if validation fails.
    // A password is required for a new user but optional when updating.
    private User buildUserFromForm(boolean passwordRequired) {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || fullName.isEmpty()) {
            showWarning("Username and Full Name are required.");
            return null;
        }
        if (!username.matches("[A-Za-z0-9_.]{3,50}")) {
            showWarning("Username must be 3 to 50 characters: letters, numbers, _ or . only.");
            return null;
        }
        if (passwordRequired && password.isEmpty()) {
            showWarning("Password is required for a new user.");
            return null;
        }
        if (!password.isEmpty() && password.length() < 6) {
            showWarning("Password must be at least 6 characters.");
            return null;
        }

        User u = new User();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setPassword(password);
        u.setRole((String) cmbRole.getSelectedItem());
        u.setStatus((String) cmbStatus.getSelectedItem());
        return u;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message,
            "Missing or Invalid Information", JOptionPane.WARNING_MESSAGE);
    }

    private void handleSqlError(SQLException ex) {
        String message;
        if ("23505".equals(ex.getSQLState())) {
            message = "A user with this username already exists.";
        } else if ("23503".equals(ex.getSQLState())) {
            message = "This user cannot be deleted because they have processed borrowings. "
                    + "Set the status to Inactive instead.";
        } else {
            message = "Database error: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
