package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.database.PasswordUtil;
import com.mycompany.librarymanagementsystemblp.model.Member;
import com.mycompany.librarymanagementsystemblp.model.StudentAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Handles the login accounts of students (library members).
 * Rule problems (wrong email, username taken, ...) are reported with an
 * IllegalStateException, the same way the other DAOs report broken rules.
 */
public class StudentAccountDAO {

    // Usernames are stored in lower case, so "Maria" and "maria" are the same login
    private String clean(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    // LOGIN: returns the member if the login is correct, or null if it is not
    public Member login(String username, String password) throws SQLException {
        String sql = "SELECT m.member_id, m.first_name, m.last_name, m.contact_no, m.email, "
                   + "m.address, m.date_registered, m.status, a.status AS account_status "
                   + "FROM STUDENT_ACCOUNTS a "
                   + "INNER JOIN MEMBERS m ON a.member_id = m.member_id "
                   + "WHERE a.username = ? AND a.password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, clean(username));
            ps.setString(2, PasswordUtil.hash(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                if (!"Active".equals(rs.getString("account_status"))) {
                    throw new IllegalStateException(
                        "This account is inactive. Please see the librarian.");
                }
                if (!"Active".equals(rs.getString("status"))) {
                    throw new IllegalStateException(
                        "Your library membership is inactive. Please see the librarian.");
                }
                return new Member(
                    rs.getInt("member_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("contact_no"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("date_registered"),
                    rs.getString("status")
                );
            }
        }
    }

    // CREATE (by the student): the Member ID and the email on file must match
    public void activateAccount(int memberId, String email, String username, String password)
            throws SQLException {
        String wrongDetails = "The Member ID and email do not match our records. "
                            + "Please check them or ask the librarian for help.";

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Rule 1: the member must exist and the email must be the one on file
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT email, status FROM MEMBERS WHERE member_id = ?")) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException(wrongDetails);
                    }
                    String onFile = rs.getString("email");
                    if (onFile == null || onFile.trim().isEmpty()) {
                        throw new IllegalStateException(
                            "No email is registered for this member. "
                          + "Please ask the librarian to set up your login.");
                    }
                    if (!onFile.trim().equalsIgnoreCase(email.trim())) {
                        throw new IllegalStateException(wrongDetails);
                    }
                    if (!"Active".equals(rs.getString("status"))) {
                        throw new IllegalStateException(
                            "Your library membership is inactive. Please see the librarian.");
                    }
                }
            }

            // Rule 2: one account per member
            if (hasAccount(conn, memberId)) {
                throw new IllegalStateException(
                    "This member already has a login. Please log in, "
                  + "or ask the librarian to reset it.");
            }

            // Rule 3: the username must be free
            if (usernameTaken(conn, clean(username), -1)) {
                throw new IllegalStateException("That username is already taken.");
            }

            insertAccount(conn, memberId, clean(username), password);
        }
    }

    // CREATE or UPDATE (by the librarian): sets the login of a member, or resets it.
    // Returns true if a new account was created, false if an existing one was reset.
    public boolean setLogin(int memberId, String username, String password) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int existingAccountId = findAccountId(conn, memberId);

            if (usernameTaken(conn, clean(username), existingAccountId)) {
                throw new IllegalStateException("That username is already taken.");
            }

            if (existingAccountId < 0) {
                insertAccount(conn, memberId, clean(username), password);
                return true;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE STUDENT_ACCOUNTS SET username = ?, password = ?, status = 'Active' "
                  + "WHERE account_id = ?")) {
                ps.setString(1, clean(username));
                ps.setString(2, PasswordUtil.hash(password));
                ps.setInt(3, existingAccountId);
                ps.executeUpdate();
            }
            return false;
        }
    }

    // UPDATE (by the student): the current password must be correct
    public void changePassword(int memberId, String currentPassword, String newPassword)
            throws SQLException {
        String sql = "UPDATE STUDENT_ACCOUNTS SET password = ? "
                   + "WHERE member_id = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, memberId);
            ps.setString(3, PasswordUtil.hash(currentPassword));
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Your current password is incorrect.");
            }
        }
    }

    // READ: the account of a member, or null if the member has no login yet
    public StudentAccount getAccountByMember(int memberId) throws SQLException {
        String sql = "SELECT account_id, member_id, username, status "
                   + "FROM STUDENT_ACCOUNTS WHERE member_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new StudentAccount(
                        rs.getInt("account_id"),
                        rs.getInt("member_id"),
                        rs.getString("username"),
                        rs.getString("status")
                    );
                }
            }
        }
        return null;
    }

    // ---------- small helpers ----------

    private boolean hasAccount(Connection conn, int memberId) throws SQLException {
        return findAccountId(conn, memberId) >= 0;
    }

    // Returns the account ID of the member, or -1 if there is none
    private int findAccountId(Connection conn, int memberId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT account_id FROM STUDENT_ACCOUNTS WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    // Is the username used by an account other than ignoreAccountId?
    private boolean usernameTaken(Connection conn, String username, int ignoreAccountId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM STUDENT_ACCOUNTS WHERE username = ? AND account_id <> ?")) {
            ps.setString(1, username);
            ps.setInt(2, ignoreAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private void insertAccount(Connection conn, int memberId, String username, String password)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO STUDENT_ACCOUNTS (member_id, username, password) VALUES (?, ?, ?)")) {
            ps.setInt(1, memberId);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));
            ps.executeUpdate();
        }
    }
}
