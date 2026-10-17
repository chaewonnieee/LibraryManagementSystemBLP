package com.mycompany.librarymanagementsystemblp.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Adds a TUP-maroon "Back" button bar to the top of a window.
 * Usage (last line of a frame's initComponents):   BackBar.install(this);
 * Clicking Back closes that window, which returns the user to the Dashboard.
 */
public final class BackBar {

    public static final Color TUP_MAROON = new Color(128, 0, 0);
    private static final Color MAROON_HOVER = new Color(158, 22, 22);
    private static final Color MAROON_PRESSED = new Color(96, 0, 0);

    private BackBar() {
    }

    // Wraps the window's existing content in a panel that has the Back bar on top
    public static void install(JFrame frame) {
        Container existingContent = frame.getContentPane();

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 205, 205)));
        bar.add(createBackButton(frame::dispose));

        JPanel root = new JPanel(new BorderLayout());
        root.add(bar, BorderLayout.NORTH);
        root.add(existingContent, BorderLayout.CENTER);
        frame.setContentPane(root);
    }

    public static JButton createBackButton(Runnable action) {
        JButton btn = new JButton("\u2190  Back");
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
