package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.Book;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    private static final String SELECT_WITH_CATEGORY =
        "SELECT b.book_id, b.category_id, c.category_name, b.isbn, b.title, "
      + "b.author, b.publisher, b.total_copies, b.available_copies, b.status "
      + "FROM BOOKS b "
      + "INNER JOIN CATEGORIES c ON b.category_id = c.category_id ";

    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("book_id"),
            rs.getInt("category_id"),
            rs.getString("category_name"),
            rs.getString("isbn"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("publisher"),
            rs.getInt("total_copies"),
            rs.getInt("available_copies"),
            rs.getString("status")
        );
    }

    public void addBook(Book b) throws SQLException {
        String sql = "INSERT INTO BOOKS (category_id, isbn, title, author, publisher, "
                   + "total_copies, available_copies, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, b.getCategoryId());
            ps.setString(2, b.getIsbn());
            ps.setString(3, b.getTitle());
            ps.setString(4, b.getAuthor());
            ps.setString(5, b.getPublisher());
            ps.setInt(6, b.getTotalCopies());
            ps.setInt(7, b.getAvailableCopies());
            ps.setString(8, b.getStatus());
            ps.executeUpdate();
        }
    }

    public List<Book> getAllBooks() throws SQLException {
        String sql = SELECT_WITH_CATEGORY + "ORDER BY b.book_id";
        List<Book> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        String sql = SELECT_WITH_CATEGORY
                   + "WHERE UPPER(b.title) LIKE ? OR UPPER(b.author) LIKE ? "
                   + "OR UPPER(b.isbn) LIKE ? OR UPPER(c.category_name) LIKE ? "
                   + "ORDER BY b.book_id";
        List<Book> list = new ArrayList<>();
        String pattern = "%" + keyword.toUpperCase() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public void updateBook(Book b) throws SQLException {
        String sql = "UPDATE BOOKS SET category_id = ?, isbn = ?, title = ?, author = ?, "
                   + "publisher = ?, total_copies = ?, available_copies = ?, status = ? "
                   + "WHERE book_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, b.getCategoryId());
            ps.setString(2, b.getIsbn());
            ps.setString(3, b.getTitle());
            ps.setString(4, b.getAuthor());
            ps.setString(5, b.getPublisher());
            ps.setInt(6, b.getTotalCopies());
            ps.setInt(7, b.getAvailableCopies());
            ps.setString(8, b.getStatus());
            ps.setInt(9, b.getBookId());
            ps.executeUpdate();
        }
    }

    public void deleteBook(int bookId) throws SQLException {
        String sql = "DELETE FROM BOOKS WHERE book_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, bookId);
            ps.executeUpdate();
        }
    }
}
