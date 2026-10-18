package com.mycompany.librarymanagementsystemblp.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Gives a module window the Lumora look and a TUP-maroon "Back" button.
 * Usage (last line of a frame's initComponents):   BackBar.install(this);
 * - styles everything already in the window (see Theme.styleTree)
 * - adds a navy header with the Back button, the window title and the logo
 * Clicking Back closes that window, which returns the user to the Dashboard.
 */
public final class BackBar {

    public static final Color TUP_MAROON = new Color(128, 0, 0);
    private static final Color MAROON_HOVER = new Color(158, 22, 22);
    private static final Color MAROON_PRESSED = new Color(96, 0, 0);

    private BackBar() {
    }

    public static void install(JFrame frame) {
        Container existingContent = frame.getContentPane();

        // 1) theme everything that is already in the window
        Theme.styleTree(existingContent);
        if (existingContent instanceof JComponent) {
            ((JComponent) existingContent).setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));
        }
        Theme.applyWindowIcon(frame);

        // 2) navy header: Back button, title, logo
        JLabel lblTitle = new JLabel(String.valueOf(frame.getTitle()).toUpperCase(), SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblLogo = new JLabel();
        ImageIcon logo = Theme.loadLogo(40);
        if (logo != null) {
            lblLogo.setIcon(logo);
        }

        JButton btnBack = createBackButton(frame::dispose);
        btnBack.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 90)),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)));

        // the logo gets the same width as the Back button, so the title stays centered
        lblLogo.setHorizontalAlignment(SwingConstants.RIGHT);
        lblLogo.setPreferredSize(new Dimension(btnBack.getPreferredSize().width, 40));

        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(Theme.NAVY);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        bar.add(btnBack, BorderLayout.WEST);
        bar.add(lblTitle, BorderLayout.CENTER);
        bar.add(lblLogo, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(bar, BorderLayout.NORTH);
        root.add(existingContent, BorderLayout.CENTER);
        frame.setContentPane(root);
    }

    public static JButton createBackButton(Runnable action) {
        JButton btn = new JButton("\u2190  Back");
        btn.putClientProperty(Theme.SKIP, Boolean.TRUE);
        btn.setUI(new BasicButtonUI());          // flat, solid color on every look and feel
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBackground(TUP_MAROON);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMnemonic(KeyEvent.VK_B);          // Alt+B
        btn.setToolTipText("Go back (Alt+B)");

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(MAROON_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(TUP_MAROON);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                btn.setBackground(MAROON_PRESSED);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                btn.setBackground(btn.contains(e.getPoint()) ? MAROON_HOVER : TUP_MAROON);
            }
        });
        btn.addActionListener(e -> action.run());
        return btn;
    }
}
