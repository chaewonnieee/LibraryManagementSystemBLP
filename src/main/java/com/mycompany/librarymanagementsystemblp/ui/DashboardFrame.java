package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.*;
import javax.swing.*;

public class DashboardFrame extends JFrame {

    private final User currentUser;

    public DashboardFrame(User user) {
        this.currentUser = user;
        initComponents();
    }

    private void initComponents() {
        setTitle("Library Management System - Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Welcome message at the top
        JLabel lblWelcome = new JLabel(
            "Welcome, " + currentUser.getFullName() + " (" + currentUser.getRole() + ")",
            SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblWelcome.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(lblWelcome, BorderLayout.NORTH);

        // Module buttons in the middle
        JButton btnBooks = new JButton("Books");
        btnBooks.setPreferredSize(new Dimension(150, 60));
        btnBooks.addActionListener(e -> new BookFrame().setVisible(true));

        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        menuPanel.add(btnBooks);
        add(menuPanel, BorderLayout.CENTER);

        // Logout at the bottom
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel bottom = new JPanel();
        bottom.add(btnLogout);
        add(bottom, BorderLayout.SOUTH);
    }
}