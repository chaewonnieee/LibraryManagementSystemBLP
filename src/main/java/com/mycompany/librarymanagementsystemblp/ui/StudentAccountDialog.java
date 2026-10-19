package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.StudentAccountDAO;
import com.mycompany.librarymanagementsystemblp.model.Member;
import com.mycompany.librarymanagementsystemblp.model.StudentAccount;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

/**
 * One small window for the three student-login tasks:
 *   ACTIVATE         - a student creates his or her own login (from the login screen)
 *   STAFF_SET        - the librarian creates or resets a student's login (from Members)
 *   CHANGE_PASSWORD  - a student changes his or her own password (from the student portal)
 */
public class StudentAccountDialog extends JDialog {

    public enum Mode { ACTIVATE, STAFF_SET, CHANGE_PASSWORD }

    private final Mode mode;
    private final Member member;                    // null when a student is activating
    private final StudentAccountDAO accountDAO = new StudentAccountDAO();

    private final JTextField txtMemberId = new JTextField(20);
    private final JTextField txtMemberName = new JTextField(20);
    private final JTextField txtEmail = new JTextField(20);
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtCurrent = new JPasswordField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JPasswordField txtConfirm = new JPasswordField(20);

    private boolean saved = false;

    // ---------- Three ways to open the window ----------

    public static StudentAccountDialog forActivation(Window owner) {
        return new StudentAccountDialog(owner, Mode.ACTIVATE, null);
    }

    public static StudentAccountDialog forStaff(Window owner, Member member) {
        return new StudentAccountDialog(owner, Mode.STAFF_SET, member);
    }

    public static StudentAccountDialog forPasswordChange(Window owner, Member member) {
        return new StudentAccountDialog(owner, Mode.CHANGE_PASSWORD, member);
    }

    private StudentAccountDialog(Window owner, Mode mode, Member member) {
        super(owner, "", ModalityType.APPLICATION_MODAL);
        this.mode = mode;
        this.member = member;
        initComponents();
    }

    public boolean isSaved() {
        return saved;
    }

    // ---------- Layout ----------

    private void initComponents() {
        String title;
        String note;
        String saveText;
        switch (mode) {
            case ACTIVATE:
                title = "ACTIVATE STUDENT ACCOUNT";
                note = "Enter your Member ID and the email address registered with the library, "
                     + "then choose a username and password.";
                saveText = "ACTIVATE";
                break;
            case STAFF_SET:
                title = "STUDENT LOGIN";
                note = describeStaffNote();
                saveText = "SAVE LOGIN";
                break;
            default:
                title = "CHANGE PASSWORD";
                note = "Enter your current password, then choose a new one.";
                saveText = "CHANGE";
                break;
        }
        setTitle(title);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        ImageIcon icon = Theme.loadLogo(64);
        if (icon != null) {
            setIconImage(icon.getImage());
        }

        // navy title band (left alone by Theme.styleTree)
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        JPanel band = new JPanel(new BorderLayout());
        band.putClientProperty(Theme.SKIP, Boolean.TRUE);
        band.setBackground(Theme.NAVY);
        band.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        band.add(lblTitle, BorderLayout.CENTER);

        // note + form
        JLabel lblNote = new JLabel("<html><body style='width:330px'>" + note + "</body></html>");
        lblNote.setFont(lblNote.getFont().deriveFont(Font.ITALIC));

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        if (mode == Mode.ACTIVATE) {
            addRow(form, row++, "Member ID:", txtMemberId);
            addRow(form, row++, "Email:", txtEmail);
            addRow(form, row++, "Username:", txtUsername);
            addRow(form, row++, "Password:", txtPassword);
            addRow(form, row++, "Confirm Password:", txtConfirm);
        } else if (mode == Mode.STAFF_SET) {
            txtMemberName.setText(member.getFirstName() + " " + member.getLastName()
                                  + "  (ID " + member.getMemberId() + ")");
            txtMemberName.setEditable(false);
            addRow(form, row++, "Member:", txtMemberName);
            addRow(form, row++, "Username:", txtUsername);
            addRow(form, row++, "New Password:", txtPassword);
            addRow(form, row++, "Confirm Password:", txtConfirm);
        } else {
            addRow(form, row++, "Current Password:", txtCurrent);
            addRow(form, row++, "New Password:", txtPassword);
            addRow(form, row++, "Confirm Password:", txtConfirm);
        }

        JLabel lblRules = new JLabel("<html><body style='width:330px'>Username: 4-30 letters, numbers, "
            + "dots or underscores. Password: at least 6 characters.</body></html>");
        lblRules.setFont(lblRules.getFont().deriveFont(Font.ITALIC));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBorder(BorderFactory.createEmptyBorder(16, 22, 6, 22));
        center.add(lblNote, BorderLayout.NORTH);
        center.add(form, BorderLayout.CENTER);
        if (mode != Mode.CHANGE_PASSWORD) {
            center.add(lblRules, BorderLayout.SOUTH);
        }

        // buttons
        JButton btnSave = new JButton(saveText);
        JButton btnCancel = new JButton("CANCEL");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        buttons.add(btnSave);
        buttons.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(band, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        Theme.styleTree(root);          // gives every field, label and button the Lumora look
        setContentPane(root);
        pack();
        setLocationRelativeTo(getOwner());

        getRootPane().setDefaultButton(btnSave);
        btnSave.addActionListener(e -> save());
        btnCancel.addActionListener(e -> dispose());
    }

    // Tells the librarian whether this member already has a login
    private String describeStaffNote() {
        String text = "Choose a username and password for this member. "
                    + "The member can then log in from the student side of the login screen.";
        try {
            StudentAccount existing = accountDAO.getAccountByMember(member.getMemberId());
            if (existing != null) {
                text = "This member already has a login (username: " + existing.getUsername()
                     + "). Saving will replace the username and password.";
            }
        } catch (SQLException ex) {
            // not critical: the generic text above is shown instead
        }
        return text;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.insets = new Insets(6, 0, 6, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        form.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        form.add(field, gbc);
    }

    // ---------- Saving ----------

    private void save() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirm.getPassword());

        try {
            if (mode == Mode.ACTIVATE) {
                String idText = txtMemberId.getText().trim();
                String email = txtEmail.getText().trim();
                if (idText.isEmpty() || email.isEmpty()) {
                    showWarning("Member ID and Email are required.");
                    return;
                }
                int memberId;
                try {
                    memberId = Integer.parseInt(idText);
                } catch (NumberFormatException ex) {
                    showWarning("Member ID must be a number.");
                    return;
                }
                if (!credentialsAreValid(username, password, confirm)) {
                    return;
                }
                accountDAO.activateAccount(memberId, email, username, password);
                showInfo("Your student account is ready.\nYou can now log in with your username and password.");

            } else if (mode == Mode.STAFF_SET) {
                if (!credentialsAreValid(username, password, confirm)) {
                    return;
                }
                boolean created = accountDAO.setLogin(member.getMemberId(), username, password);
                showInfo(created ? "Student login created successfully."
                                 : "Student login updated successfully.");

            } else {
                String current = new String(txtCurrent.getPassword());
                if (current.isEmpty()) {
                    showWarning("Please enter your current password.");
                    return;
                }
                if (!passwordIsValid(password, confirm)) {
                    return;
                }
                accountDAO.changePassword(member.getMemberId(), current, password);
                showInfo("Your password was changed successfully.");
            }

            saved = true;
            dispose();

        } catch (IllegalStateException ex) {
            showWarning(ex.getMessage());           // a rule was broken (wrong email, name taken, ...)
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean credentialsAreValid(String username, String password, String confirm) {
        if (username.isEmpty()) {
            showWarning("Username is required.");
            return false;
        }
        if (!username.matches("[A-Za-z0-9_.]{4,30}")) {
            showWarning("Username must be 4-30 characters: letters, numbers, dots or underscores only.");
            return false;
        }
        return passwordIsValid(password, confirm);
    }

    private boolean passwordIsValid(String password, String confirm) {
        if (password.isEmpty()) {
            showWarning("Password is required.");
            return false;
        }
        if (password.length() < 6) {
            showWarning("Password must be at least 6 characters.");
            return false;
        }
        if (!password.equals(confirm)) {
            showWarning("Password and Confirm Password do not match.");
            txtConfirm.setText("");
            txtConfirm.requestFocus();
            return false;
        }
        return true;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message,
            "Missing or Invalid Information", JOptionPane.WARNING_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
