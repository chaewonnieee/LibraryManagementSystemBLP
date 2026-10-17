package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.Category;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {


    public void addCategory(Category c) throws SQLException {
        String sql = "INSERT INTO CATEGORIES (category_name, description) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getCategoryName());
            ps.setString(2, c.getDescription());
            ps.executeUpdate();
        }
    }

   
    public List<Category> getAllCategories() throws SQLException {
        String sql = "SELECT * FROM CATEGORIES ORDER BY category_id";
        List<Category> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Category(
                    rs.getInt("category_id"),
                    rs.getString("category_name"),
                    rs.getString("description")
                ));
            }
        }
        return list;
    }

  
    public List<Category> searchCategories(String keyword) throws SQLException {
        String sql = "SELECT * FROM CATEGORIES "
                   + "WHERE UPPER(category_name) LIKE ? OR UPPER(description) LIKE ? "
                   + "ORDER BY category_id";
        List<Category> list = new ArrayList<>();
        String pattern = "%" + keyword.toUpperCase() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getString("description")
                    ));
                }
            }
        }
        return list;
    }


    public void updateCategory(Category c) throws SQLException {
        String sql = "UPDATE CATEGORIES SET category_name = ?, description = ? "
                   + "WHERE category_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getCategoryName());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getCategoryId());
            ps.executeUpdate();
        }
    }


    public void deleteCategory(int categoryId) throws SQLException {
        String sql = "DELETE FROM CATEGORIES WHERE category_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            ps.executeUpdate();
        }
    }
}