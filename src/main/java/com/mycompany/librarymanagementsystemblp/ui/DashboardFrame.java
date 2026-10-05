package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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

        JLabel lblWelcome = new JLabel(
            "Welcome, " + currentUser.getFullName() + " (" + currentUser.getRole() + ")",
            SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        JPanel bottom = new JPanel();
        bottom.add(btnLogout);

        setLayout(new BorderLayout());
        add(lblWelcome, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}