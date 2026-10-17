package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.ReportDAO;
import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Map;
import javax.swing.*;

public class DashboardFrame extends JFrame {

    private final User currentUser;
    private final ReportDAO reportDAO = new ReportDAO();
    private final JPanel statsPanel = new JPanel(new GridLayout(1, 0, 12, 0));

    public DashboardFrame(User user) {
        this.currentUser = user;
        initComponents();
        loadStats();
    }

    private void initComponents() {
        setTitle("Library Management System - Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Menu bar
        JMenuItem miLogout = new JMenuItem("Logout");
        JMenuItem miExit = new JMenuItem("Exit");
        JMenu menuFile = new JMenu("File");
        menuFile.add(miLogout);
        menuFile.add(miExit);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuFile);
        setJMenuBar(menuBar);

        // Welcome message + live summary numbers
        JLabel lblWelcome = new JLabel(
            "Welcome, " + currentUser.getFullName() + " (" + currentUser.getRole() + ")",
            SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 20));

        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        north.add(lblWelcome, BorderLayout.NORTH);
        north.add(statsPanel, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // Module buttons
        JPanel menuPanel = new JPanel(new GridLayout(0, 4, 15, 15));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        menuPanel.add(menuButton("Books", () -> new BookFrame().setVisible(true)));
        menuPanel.add(menuButton("Categories", () -> new CategoryFrame().setVisible(true)));
        menuPanel.add(menuButton("Members", () -> new MemberFrame().setVisible(true)));
        menuPanel.add(menuButton("Borrow / Return",
            () -> new BorrowingFrame(currentUser).setVisible(true)));
        menuPanel.add(menuButton("Reservations", () -> new ReservationFrame().setVisible(true)));
        menuPanel.add(menuButton("Fines", () -> new FineFrame().setVisible(true)));
        menuPanel.add(menuButton("Reports", () -> new ReportFrame().setVisible(true)));

        // Only administrators can manage system users
        if ("Administrator".equals(currentUser.getRole())) {
            menuPanel.add(menuButton("Users", () -> new UserFrame(currentUser).setVisible(true)));
        }
        add(menuPanel, BorderLayout.CENTER);

        // Bottom buttons
        JButton btnRefresh = new JButton("Refresh Summary");
        JButton btnLogout = new JButton("Logout");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottom.add(btnRefresh);
        bottom.add(btnLogout);
        add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> loadStats());
        btnLogout.addActionListener(e -> logout());
        miLogout.addActionListener(e -> logout());
        miExit.addActionListener(e -> System.exit(0));

        // Refresh the numbers whenever the user comes back to the dashboard
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                loadStats();
            }
        });
    }

    private JButton menuButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(150, 60));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }

    // ---------- Summary cards ----------

    private void loadStats() {
        statsPanel.removeAll();
        try {
            for (Map.Entry<String, String> entry : reportDAO.getDashboardSummary().entrySet()) {
                statsPanel.add(createStatCard(entry.getKey(), entry.getValue()));
            }
        } catch (SQLException ex) {
            statsPanel.add(new JLabel("Could not load summary: " + ex.getMessage()));
        }
        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private JPanel createStatCard(String title, String value) {
        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(new Color(25, 80, 140));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(new Color(240, 245, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 205, 225)),
            BorderFactory.createEmptyBorder(10, 8, 10, 8)));
        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblTitle, BorderLayout.SOUTH);
        return card;
    }
}
