package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.Reservation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    // JOIN across 3 tables: reservations + members + books
    private static final String SELECT_RESERVATIONS =
        "SELECT r.reservation_id, r.member_id, "
      + "m.last_name || ', ' || m.first_name AS member_name, "
      + "r.book_id, bk.title, r.reservation_date, r.status "
      + "FROM RESERVATIONS r "
      + "INNER JOIN MEMBERS m ON r.member_id = m.member_id "
      + "INNER JOIN BOOKS bk ON r.book_id = bk.book_id ";

    private Reservation mapRow(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("reservation_id"),
            rs.getInt("member_id"),
            rs.getString("member_name"),
            rs.getInt("book_id"),
            rs.getString("title"),
            rs.getString("reservation_date"),
            rs.getString("status")
        );
    }

    // READ: all reservations, or only pending ones, filtered by member name or book title
    public List<Reservation> getReservations(String keyword, boolean pendingOnly)
            throws SQLException {
        String sql = SELECT_RESERVATIONS
                   + "WHERE (UPPER(m.first_name) LIKE ? OR UPPER(m.last_name) LIKE ? "
                   + "OR UPPER(bk.title) LIKE ?) "
                   + (pendingOnly ? "AND r.status = 'Pending' " : "")
                   + "ORDER BY r.reservation_id";
        List<Reservation> list = new ArrayList<>();
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

    // CREATE: reserve a book. Rules are checked first; a broken rule throws IllegalStateException.
    public void addReservation(int memberId, int bookId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Rule 1: the member must exist and be active
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM MEMBERS WHERE member_id = ?")) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Member not found.");
                    }
                    if (!"Active".equals(rs.getString("status"))) {
                        throw new IllegalStateException(
                            "This member is inactive and cannot reserve books.");
                    }
                }
            }

            // Rule 2: only books with no copies on the shelf can be reserved
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT available_copies FROM BOOKS WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Book not found.");
                    }
                    if (rs.getInt("available_copies") > 0) {
                        throw new IllegalStateException(
                            "This book is available right now. Borrow it instead of reserving it.");
                    }
                }
            }

            // Rule 3: no duplicate pending reservation for the same member and book
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM RESERVATIONS "
                  + "WHERE member_id = ? AND book_id = ? AND status = 'Pending'")) {
                ps.setInt(1, memberId);
                ps.setInt(2, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new IllegalStateException(
                            "This member already has a pending reservation for this book.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO RESERVATIONS (member_id, book_id) VALUES (?, ?)")) {
                ps.setInt(1, memberId);
                ps.setInt(2, bookId);
                ps.executeUpdate();
            }
        }
    }

    // UPDATE: set a pending reservation to 'Fulfilled' or 'Cancelled'
    public void changeStatus(int reservationId, String newStatus) throws SQLException {
        String sql = "UPDATE RESERVATIONS SET status = ? "
                   + "WHERE reservation_id = ? AND status = 'Pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, reservationId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Only pending reservations can be changed.");
            }
        }
    }
}
