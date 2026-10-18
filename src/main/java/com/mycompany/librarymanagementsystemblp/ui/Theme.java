package com.mycompany.librarymanagementsystemblp.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

public final class Theme {

    public static final Color NAVY = new Color(16, 10, 57);        // #100A39 dark indigo
    public static final Color NAVY_HOVER = new Color(48, 38, 110);
    public static final Color CREAM = new Color(239, 233, 223);    // #EFE9DF
    public static final Color GOLD = new Color(255, 186, 113);     // #FFBA71 peach
    public static final Color GOLD_HOVER = new Color(255, 206, 150);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font TEXT_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private Theme() {
    }

    // Makes a button flat and colored, with a hover effect
    public static void styleButton(JButton btn, Color normal, Color hover, Color textColor) {
        btn.setUI(new BasicButtonUI());
        btn.setOpaque(true);
        btn.setBackground(normal);
        btn.setForeground(textColor);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normal);
            }
        });
    }
}