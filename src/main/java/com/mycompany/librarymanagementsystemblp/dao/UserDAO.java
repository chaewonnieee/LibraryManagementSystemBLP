package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.User;
import com.mycompany.librarymanagementsystemblp.database.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("full_name"),
            rs.getString("user_role"),
            rs.getString("status")
        );
    }

    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM USERS "
                   + "WHERE username = ? AND password = ? AND status = 'Active'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public void addUser(User u) throws SQLException {
        String sql = "INSERT INTO USERS (username, password, full_name, user_role, status) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, u.getUsername());
            ps.setString(2, PasswordUtil.hash(u.getPassword()));
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getStatus());
            ps.executeUpdate();
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM USERS ORDER BY user_id";
        List<User> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<User> searchUsers(String keyword) throws SQLException {
        String sql = "SELECT * FROM USERS "
                   + "WHERE UPPER(username) LIKE ? OR UPPER(full_name) LIKE ? "
                   + "OR UPPER(user_role) LIKE ? "
                   + "ORDER BY user_id";
        List<User> list = new ArrayList<>();
        String pattern = "%" + keyword.toUpperCase() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public void updateUser(User u, boolean changePassword) throws SQLException {
        String sql = "UPDATE USERS SET username = ?, full_name = ?, user_role = ?, status = ?"
                   + (changePassword ? ", password = ?" : "")
                   + " WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, u.getUsername());
            ps.setString(i++, u.getFullName());
            ps.setString(i++, u.getRole());
            ps.setString(i++, u.getStatus());
            if (changePassword) {
                ps.setString(i++, PasswordUtil.hash(u.getPassword()));
            }
            ps.setInt(i, u.getUserId());
            ps.executeUpdate();
        }
    }

    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM USERS WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
