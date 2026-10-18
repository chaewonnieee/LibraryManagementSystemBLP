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

    private static final Color CARD_BORDER = new Color(222, 214, 200);
    private static final Color MUTED = new Color(95, 90, 125);
    private static final Color ALERT = new Color(176, 38, 38);

    private final User currentUser;
    private final ReportDAO reportDAO = new ReportDAO();
    private final JPanel statsPanel = new JPanel(new GridLayout(1, 0, 14, 0));

    public DashboardFrame(User user) {
        this.currentUser = user;
        initComponents();
        loadStats();
    }

    private void initComponents() {
        setTitle("Lumora Library - Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Theme.applyWindowIcon(this);

        setJMenuBar(buildMenuBar());

        JPanel root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(960, 600));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);

        // Refresh the numbers whenever the user comes back to the dashboard
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                loadStats();
            }
        });
    }

    // ---------- Menu bar (same navy as the header, so they look like one piece) ----------

    private JMenuBar buildMenuBar() {
        JMenuItem miLogout = new JMenuItem("Logout");
        JMenuItem miExit = new JMenuItem("Exit");
        miLogout.addActionListener(e -> logout());
        miExit.addActionListener(e -> System.exit(0));

        JMenu menuFile = new JMenu("File");
        menuFile.setForeground(Color.WHITE);
        menuFile.add(miLogout);
        menuFile.add(miExit);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setOpaque(true);
        menuBar.setBackground(Theme.NAVY);
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        menuBar.add(menuFile);
        return menuBar;
    }

    // ---------- Header: brand on the left, user and logout on the right ----------

    private JPanel buildHeader() {
        JLabel lblLogo = new JLabel();
        ImageIcon logo = Theme.loadLogo(54);
        if (logo != null) {
            lblLogo.setIcon(logo);
        }

        JLabel lblName = new JLabel("LUMORA");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 26));
        lblName.setForeground(Color.WHITE);

        JLabel pill = Theme.createPill("EXPLORE \u2022 LEARN \u2022 DISCOVER", Theme.GOLD, Theme.NAVY, 11f);

        JPanel brandText = new JPanel();
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.setOpaque(false);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        pill.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandText.add(lblName);
        brandText.add(Box.createVerticalStrut(4));
        brandText.add(pill);

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brand.setOpaque(false);
        brand.add(lblLogo);
        brand.add(brandText);

        JLabel lblWelcome = new JLabel("Welcome, " + currentUser.getFullName());
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblWelcome.setForeground(Color.WHITE);

        JLabel lblRole = new JLabel(currentUser.getRole());
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRole.setForeground(Theme.GOLD);

        JPanel userText = new JPanel();
        userText.setLayout(new BoxLayout(userText, BoxLayout.Y_AXIS));
        userText.setOpaque(false);
        lblWelcome.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblRole.setAlignmentX(Component.RIGHT_ALIGNMENT);
        userText.add(lblWelcome);
        userText.add(lblRole);

        JButton btnLogout = new JButton("LOGOUT");
        Theme.styleRoundedButton(btnLogout, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY, 9);
        btnLogout.setPreferredSize(new Dimension(96, 36));
        btnLogout.addActionListener(e -> logout());

        JPanel user = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));
        user.setOpaque(false);
        user.add(userText);
        user.add(btnLogout);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(8, 24, 12, 18));
        header.add(brand, BorderLayout.WEST);
        header.add(user, BorderLayout.EAST);
        return header;
    }

    // ---------- Body: overview cards + module buttons ----------

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setBackground(Theme.CREAM);
        body.setBorder(BorderFactory.createEmptyBorder(26, 36, 10, 36));

        // Overview
        JButton btnRefresh = new JButton("REFRESH");
        Theme.styleRoundedButton(btnRefresh, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY, 8);
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btnRefresh.setPreferredSize(new Dimension(100, 28));
        btnRefresh.addActionListener(e -> loadStats());

        JPanel overviewTitle = new JPanel(new BorderLayout());
        overviewTitle.setOpaque(false);
        overviewTitle.add(sectionLabel("OVERVIEW"), BorderLayout.WEST);
        overviewTitle.add(btnRefresh, BorderLayout.EAST);

        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(100, 84));

        JPanel overview = new JPanel(new BorderLayout(0, 10));
        overview.setOpaque(false);
        overview.add(overviewTitle, BorderLayout.NORTH);
        overview.add(statsPanel, BorderLayout.CENTER);
        body.add(overview, BorderLayout.NORTH);

        // Modules
        JPanel grid = new JPanel(new GridLayout(0, 4, 16, 16));
        grid.setOpaque(false);
        grid.add(moduleButton("Books", false, () -> new BookFrame().setVisible(true)));
        grid.add(moduleButton("Categories", false, () -> new CategoryFrame().setVisible(true)));
        grid.add(moduleButton("Members", false, () -> new MemberFrame().setVisible(true)));
        grid.add(moduleButton("Borrow / Return", false,
            () -> new BorrowingFrame(currentUser).setVisible(true)));
        grid.add(moduleButton("Reservations", false, () -> new ReservationFrame().setVisible(true)));
        grid.add(moduleButton("Fines", false, () -> new FineFrame().setVisible(true)));
        grid.add(moduleButton("Reports", false, () -> new ReportFrame().setVisible(true)));

        // Only administrators can manage system users
        if ("Administrator".equals(currentUser.getRole())) {
            grid.add(moduleButton("Users", true, () -> new UserFrame(currentUser).setVisible(true)));
        }

        JPanel gridHolder = new JPanel(new BorderLayout());   // keeps the buttons from stretching too tall
        gridHolder.setOpaque(false);
        gridHolder.add(grid, BorderLayout.NORTH);

        JPanel modules = new JPanel(new BorderLayout(0, 10));
        modules.setOpaque(false);
        modules.add(sectionLabel("MODULES"), BorderLayout.NORTH);
        modules.add(gridHolder, BorderLayout.CENTER);
        body.add(modules, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildFooter() {
        JLabel slogan = new JLabel("WHERE KNOWLEDGE FINDS IT\u2019S HOME", SwingConstants.CENTER);
        slogan.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        slogan.setForeground(Theme.NAVY);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.CREAM);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 10));
        footer.add(slogan, BorderLayout.CENTER);
        return footer;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(MUTED);
        return label;
    }

    // Navy tile for normal modules, gold tile for the administrator-only one
    private JButton moduleButton(String text, boolean highlight, Runnable action) {
        JButton btn = new JButton(text);
        if (highlight) {
            Theme.styleRoundedButton(btn, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY, 14);
        } else {
            Theme.styleRoundedButton(btn, Theme.NAVY, Theme.NAVY_HOVER, Color.WHITE, 14);
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setPreferredSize(new Dimension(150, 92));
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
        boolean warning = title.equals("Overdue") && !value.equals("0");

        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        // long values such as "PHP 1,250.00" get a smaller font so they always fit
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, value.length() > 7 ? 16 : 22));
        lblValue.setForeground(warning ? ALERT : Theme.NAVY);

        JLabel lblTitle = new JLabel(title.toUpperCase(), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTitle.setForeground(MUTED);

        Theme.RoundedPanel card = new Theme.RoundedPanel(
            new BorderLayout(0, 4), Color.WHITE, CARD_BORDER, 12);
        card.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblTitle, BorderLayout.SOUTH);
        return card;
    }
}
