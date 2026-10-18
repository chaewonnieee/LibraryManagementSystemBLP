package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.UserDAO;
import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

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
        setSize(400, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Theme.CREAM);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);

        // Title
        JLabel lblTitle = new JLabel("LUMORA", SwingConstants.CENTER);
        lblTitle.setFont(Theme.TITLE_FONT);
        lblTitle.setForeground(Theme.NAVY);
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
        Theme.styleButton(btnLogin, Theme.NAVY, Theme.NAVY_HOVER, Color.WHITE);
        Theme.styleButton(btnClear, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY);
        Theme.styleButton(btnExit, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
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
        
        // Header band: logo, name, tagline
        JLabel lblLogo = new JLabel();
        java.net.URL logoUrl = getClass().getResource("/logo.png");
        if (logoUrl != null) {
            Image img = new ImageIcon(logoUrl).getImage().getScaledInstance(70, -1, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(img));
        }

        JLabel lblSystem = new JLabel("LUMORA");
        lblSystem.setFont(new Font("Segoe UI", Font.PLAIN, 26));
        lblSystem.setForeground(Color.WHITE);

        JLabel lblTagline = new JLabel("EXPLORE \u2022 LEARN \u2022 DISCOVER");
        lblTagline.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTagline.setForeground(Theme.NAVY);
        lblTagline.setOpaque(true);
        lblTagline.setBackground(Theme.GOLD);
        lblTagline.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSystem.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        header.add(lblLogo);
        header.add(Box.createVerticalStrut(4));
        header.add(lblSystem);
        header.add(Box.createVerticalStrut(8));
        header.add(lblTagline);

        JPanel root = new JPanel(new BorderLayout());
        root.add(header, BorderLayout.NORTH);
        root.add(getContentPane(), BorderLayout.CENTER);
        setContentPane(root);
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