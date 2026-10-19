package com.mycompany.librarymanagementsystemblp.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

public final class Theme {

    public static final Color NAVY = new Color(16, 10, 57);        // #100A39 dark indigo
    public static final Color NAVY_HOVER = new Color(48, 38, 110);
    public static final Color CREAM = new Color(239, 233, 223);    // #EFE9DF
    public static final Color GOLD = new Color(255, 186, 113);     // #FFBA71 peach
    public static final Color GOLD_HOVER = new Color(255, 206, 150);
    public static final Color FIELD_BORDER = new Color(190, 183, 170);
    public static final Color FIELD_READONLY = new Color(244, 240, 232);
    public static final Color GRID = new Color(228, 222, 210);
    public static final Color DANGER = new Color(150, 35, 45);
    public static final Color DANGER_HOVER = new Color(181, 56, 66);

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

    // Same as styleButton, but with rounded corners (like the Lumora design)
    public static void styleRoundedButton(JButton btn, Color normal, Color hover,
                                          Color textColor, int radius) {
        styleButton(btn, normal, hover, textColor);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), radius * 2, radius * 2);
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    // A pill-shaped label, like "EXPLORE . LEARN . DISCOVER"
    public static JLabel createPill(String text, Color background, Color textColor, float fontSize) {
        JLabel pill = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setBackground(background);
        pill.setForeground(textColor);
        pill.setFont(new Font("Segoe UI", Font.PLAIN, 11).deriveFont(fontSize));
        pill.setBorder(BorderFactory.createEmptyBorder(5, 16, 5, 16));
        return pill;
    }

    // A panel with rounded corners and a thin border (used for dashboard cards)
    public static class RoundedPanel extends JPanel {
        private final Color borderColor;
        private final int radius;

        public RoundedPanel(LayoutManager layout, Color background, Color borderColor, int radius) {
            super(layout);
            this.borderColor = borderColor;
            this.radius = radius;
            setOpaque(false);
            setBackground(background);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Styles a text field like the design: white, thin border, some padding
    public static void styleField(JTextField field) {
        field.setFont(TEXT_FONT);
        field.setBackground(Color.WHITE);
        field.setForeground(NAVY);
        field.setCaretColor(NAVY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
    }

    // Loads /logo.png, trims its transparent margin, and scales it to the given width.
    // Returns null if the logo file is missing, so the screens still work without it.
    public static ImageIcon loadLogo(int targetWidth) {
        try {
            java.net.URL url = Theme.class.getResource("/logo.png");
            if (url == null) {
                return null;
            }
            BufferedImage src = ImageIO.read(url);
            int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    if ((src.getRGB(x, y) >>> 24) > 10) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
            if (maxX < 0) {
                return new ImageIcon(src.getScaledInstance(targetWidth, -1, Image.SCALE_SMOOTH));
            }
            BufferedImage cropped = src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
            return new ImageIcon(cropped.getScaledInstance(targetWidth, -1, Image.SCALE_SMOOTH));
        } catch (Exception ex) {
            return null;
        }
    }

    // Uses the logo as the window icon (instead of the Java cup)
    public static void applyWindowIcon(JFrame frame) {
        ImageIcon icon = loadLogo(64);
        if (icon != null) {
            frame.setIconImage(icon.getImage());
        }
    }

    // =====================================================================
    //  Whole-window styling: call Theme.styleTree(content) once and every
    //  panel, label, field, button, table and checkbox below it gets the Lumora look.
    // =====================================================================

    public static final String SKIP = "lumora.skip";   // put this client property on a component to leave it alone

    public static void styleTree(Component c) {
        if (c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty(SKIP))) {
            return;
        }
        if (c instanceof JButton) {
            styleActionButton((JButton) c);
        } else if (c instanceof JScrollPane) {
            styleScroll((JScrollPane) c);
        } else if (c instanceof JTextField) {
            JTextField field = (JTextField) c;
            styleField(field);
            if (!field.isEditable()) {
                field.setBackground(FIELD_READONLY);
            }
        } else if (c instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) c;
            combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            combo.setBackground(Color.WHITE);
            combo.setForeground(NAVY);
        } else if (c instanceof JCheckBox) {
            JCheckBox box = (JCheckBox) c;
            box.setOpaque(false);
            box.setForeground(NAVY);
            box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            box.setFocusPainted(false);
        } else if (c instanceof JLabel) {
            styleLabel((JLabel) c);
        } else if (c instanceof JTable) {
            styleTable((JTable) c);
        } else if (c instanceof Container) {
            if (c instanceof JPanel) {
                ((JPanel) c).setOpaque(true);
                c.setBackground(CREAM);
            }
            for (Component child : ((Container) c).getComponents()) {
                styleTree(child);
            }
        }
    }

    // Keeps the size/style a screen chose on purpose (bold totals, italic notes), swaps the font and color
    private static void styleLabel(JLabel label) {
        Font f = label.getFont();
        boolean lookAndFeelDefault = f.getSize() == 12;
        int style = (lookAndFeelDefault && f.getStyle() == Font.BOLD) ? Font.PLAIN : f.getStyle();
        int size = lookAndFeelDefault ? 13 : f.getSize();
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(NAVY);
    }

    // The button's job decides its color: save/update = navy, clear/show all = gold, delete/cancel = red
    private static void styleActionButton(JButton btn) {
        String text = btn.getText() == null ? "" : btn.getText().toUpperCase();
        if (text.contains("DELETE") || text.contains("CANCEL")) {
            styleRoundedButton(btn, DANGER, DANGER_HOVER, Color.WHITE, 9);
        } else if (text.contains("CLEAR") || text.contains("SHOW ALL")) {
            styleRoundedButton(btn, GOLD, GOLD_HOVER, NAVY, 9);
        } else {
            styleRoundedButton(btn, NAVY, NAVY_HOVER, Color.WHITE, 9);
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    private static void styleScroll(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        Component view = scroll.getViewport().getView();
        if (view != null) {
            styleTree(view);
        }
    }

    // Navy header, thin row lines, gold selection
    public static void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(NAVY);
        table.setBackground(Color.WHITE);
        table.setRowHeight(28);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(GRID);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(GOLD);
        table.setSelectionForeground(NAVY);
        table.setFillsViewportHeight(true);

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(NAVY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));

        // Header text and cell text use the same left alignment and padding,
        // so every column header lines up with the values underneath it.
        final Border pad = BorderFactory.createEmptyBorder(0, 8, 0, 8);
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setBackground(NAVY);
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(pad);
                return this;
            }
        });
        // Replace the stock renderers (numbers are right-aligned by default) with left-aligned padded ones
        javax.swing.table.DefaultTableCellRenderer cells = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, false, row, column);
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(pad);
                return this;
            }
        };
        for (Class<?> type : new Class<?>[]{Object.class, Number.class, Integer.class,
                                             Long.class, Double.class, Boolean.class}) {
            if (table.getDefaultRenderer(type) instanceof javax.swing.plaf.UIResource) {
                table.setDefaultRenderer(type, cells);
            }
        }
    }
}
