package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.StudentAccountDAO;
import com.mycompany.librarymanagementsystemblp.dao.UserDAO;
import com.mycompany.librarymanagementsystemblp.model.Member;
import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.swing.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnClear;
    private JButton btnExit;
    private JLabel lblActivate;          // "First time? Activate your student account" (student side only)

    private boolean studentMode = false; // false = staff login, true = student login

    private final UserDAO userDAO = new UserDAO();
    private final StudentAccountDAO studentDAO = new StudentAccountDAO();

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Lumora Library - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        Theme.applyWindowIcon(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(400, 580));   // exact size of the usable area
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);

        // Pressing Enter triggers Login
        getRootPane().setDefaultButton(btnLogin);

        // Button events
        btnLogin.addActionListener(e -> doLogin());
        btnClear.addActionListener(e -> clearFields());
        btnExit.addActionListener(e -> System.exit(0));
    }

    // Dark band at the top: small logo, name, and the tagline pill
    private JPanel buildHeader() {
        JLabel lblLogo = new JLabel();
        ImageIcon logo = Theme.loadLogo(46);
        if (logo != null) {
            lblLogo.setIcon(logo);
        }

        JLabel lblName = new JLabel("LUMORA");
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        lblName.setForeground(Color.WHITE);

        JLabel pill = Theme.createPill("EXPLORE \u2022 LEARN \u2022 DISCOVER", Theme.GOLD, Theme.NAVY, 11f);

        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        pill.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));
        header.add(lblLogo);
        header.add(Box.createVerticalStrut(2));
        header.add(lblName);
        header.add(Box.createVerticalStrut(6));
        header.add(pill);
        return header;
    }

    // Cream area: big logo + titles, then the form, then the buttons
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.CREAM);

        // ----- brand block -----
        JLabel lblLogo = new JLabel();
        ImageIcon logo = Theme.loadLogo(100);
        if (logo != null) {
            lblLogo.setIcon(logo);
        }

        JLabel lblTitle = new JLabel("LUMORA");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Theme.NAVY);

        JLabel lblSystem = new JLabel("LIBRARY MANAGEMENT SYSTEM");
        lblSystem.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSystem.setForeground(Theme.NAVY);

        JLabel lblSlogan = new JLabel("WHERE KNOWLEDGE FINDS ITS HOME");
        lblSlogan.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSlogan.setForeground(Theme.NAVY);

        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSystem.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSlogan.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(18, 10, 0, 10));
        brand.add(lblLogo);
        brand.add(Box.createVerticalStrut(4));
        brand.add(lblTitle);
        brand.add(Box.createVerticalStrut(2));
        brand.add(lblSystem);
        brand.add(Box.createVerticalStrut(8));
        brand.add(lblSlogan);
        body.add(brand, BorderLayout.NORTH);

        // ----- form -----
        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        Theme.styleField(txtUsername);
        Theme.styleField(txtPassword);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(22, 28, 0, 28));

        // Staff / Student switch
        ModeToggle toggle = new ModeToggle("STAFF", "STUDENT", this::switchMode);
        GridBagConstraints toggleGbc = new GridBagConstraints();
        toggleGbc.gridx = 0;
        toggleGbc.gridy = 0;
        toggleGbc.gridwidth = 2;
        toggleGbc.insets = new Insets(0, 0, 14, 0);
        form.add(toggle, toggleGbc);

        addFormRow(form, 1, "USERNAME :", txtUsername);
        addFormRow(form, 2, "PASSWORD :", txtPassword);

        // Students create their own login here (hidden while "STAFF" is selected)
        lblActivate = new JLabel("<html><u>First time? Activate your student account</u></html>",
                                 SwingConstants.CENTER);
        lblActivate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblActivate.setForeground(Theme.NAVY);
        lblActivate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblActivate.setVisible(false);
        lblActivate.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StudentAccountDialog.forActivation(LoginFrame.this).setVisible(true);
            }
        });
        GridBagConstraints linkGbc = new GridBagConstraints();
        linkGbc.gridx = 0;
        linkGbc.gridy = 3;
        linkGbc.gridwidth = 2;
        linkGbc.insets = new Insets(10, 0, 0, 0);
        form.add(lblActivate, linkGbc);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = 4;
        filler.weighty = 1.0;       // pushes the rows to the top
        form.add(Box.createGlue(), filler);
        body.add(form, BorderLayout.CENTER);

        // ----- buttons -----
        btnLogin = new JButton("LOGIN");
        btnClear = new JButton("CLEAR");
        btnExit = new JButton("EXIT");
        Theme.styleRoundedButton(btnLogin, Theme.NAVY, Theme.NAVY_HOVER, Color.WHITE, 9);
        Theme.styleRoundedButton(btnClear, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY, 9);
        Theme.styleRoundedButton(btnExit, Theme.GOLD, Theme.GOLD_HOVER, Theme.NAVY, 9);
        for (JButton b : new JButton[]{btnLogin, btnClear, btnExit}) {
            b.setPreferredSize(new Dimension(92, 36));
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 28, 0));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnClear);
        buttonPanel.add(btnExit);
        body.add(buttonPanel, BorderLayout.SOUTH);

        return body;
    }

    private void addFormRow(JPanel form, int row, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(Theme.NAVY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.insets = new Insets(6, 0, 6, 0);

        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 0, 6, 14);
        form.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        form.add(field, gbc);
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
            if (studentMode) {
                Member member = studentDAO.login(username, password);
                if (member == null) {
                    showLoginFailed();
                } else {
                    dispose();
                    new StudentDashboardFrame(member).setVisible(true);
                }
            } else {
                User user = userDAO.login(username, password);
                if (user == null) {
                    showLoginFailed();
                } else {
                    dispose();
                    new DashboardFrame(user).setVisible(true);
                }
            }

        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),    // inactive student account
                "Login Not Allowed", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLoginFailed() {
        JOptionPane.showMessageDialog(this,
            "Incorrect username or password.",
            "Login Failed", JOptionPane.ERROR_MESSAGE);
        txtPassword.setText("");
        txtPassword.requestFocus();
    }

    // Called when the Staff / Student switch is clicked
    private void switchMode(boolean student) {
        studentMode = student;
        lblActivate.setVisible(student);
        clearFields();
    }

    private void clearFields() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtUsername.requestFocus();
    }

    // Rounded two-option switch (STAFF | STUDENT) drawn in the Lumora colors
    private static class ModeToggle extends JComponent {

        private final String leftText;
        private final String rightText;
        private final Consumer<Boolean> onChange;
        private boolean rightSelected = false;

        ModeToggle(String leftText, String rightText, Consumer<Boolean> onChange) {
            this.leftText = leftText;
            this.rightText = rightText;
            this.onChange = onChange;
            setPreferredSize(new Dimension(260, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    boolean right = e.getX() >= getWidth() / 2;
                    if (right != rightSelected) {
                        rightSelected = right;
                        repaint();
                        ModeToggle.this.onChange.accept(right);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // white track with a thin border
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            g2.setColor(Theme.FIELD_BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

            // navy pill behind the selected option
            int half = w / 2;
            g2.setColor(Theme.NAVY);
            if (rightSelected) {
                g2.fillRoundRect(half, 3, half - 4, h - 7, h - 6, h - 6);
            } else {
                g2.fillRoundRect(4, 3, half - 4, h - 7, h - 6, h - 6);
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(rightSelected ? Theme.NAVY : Color.WHITE);
            g2.drawString(leftText, (half - fm.stringWidth(leftText)) / 2, baseline);
            g2.setColor(rightSelected ? Color.WHITE : Theme.NAVY);
            g2.drawString(rightText, half + (half - fm.stringWidth(rightText)) / 2, baseline);
            g2.dispose();
        }
    }
}
