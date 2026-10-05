package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.UserDAO;
import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnClear;
    private JButton btnExit;

    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Library Management System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 280);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);

        // Title
        JLabel lblTitle = new JLabel("SYSTEM LOGIN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(lblTitle, gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Username:"), gbc);

        txtUsername = new JTextField(15);
        gbc.gridx = 1;
        add(txtUsername, gbc);

        // Password
        gbc.gridy = 2;
        gbc.gridx = 0;
        add(new JLabel("Password:"), gbc);

        txtPassword = new JPasswordField(15);
        gbc.gridx = 1;
        add(txtPassword, gbc);

        // Buttons
        btnLogin = new JButton("LOGIN");
        btnClear = new JButton("CLEAR");
        btnExit = new JButton("EXIT");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnClear);
        buttonPanel.add(btnExit);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(buttonPanel, gbc);

        // Pressing Enter triggers Login
        getRootPane().setDefaultButton(btnLogin);

        // Button events
        btnLogin.addActionListener(e -> doLogin());
        btnClear.addActionListener(e -> clearFields());
        btnExit.addActionListener(e -> System.exit(0));
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDAO.login(username, password);

            if (user == null) {
                JOptionPane.showMessageDialog(this,
                    "Incorrect username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                txtPassword.requestFocus();
            } else {
                dispose();
                new DashboardFrame(user).setVisible(true);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtUsername.requestFocus();
    }
}