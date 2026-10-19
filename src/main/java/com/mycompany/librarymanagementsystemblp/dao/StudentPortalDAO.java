package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.BorrowRecord;
import com.mycompany.librarymanagementsystemblp.model.Fine;
import com.mycompany.librarymanagementsystemblp.model.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only views for the student portal. Every query is limited to ONE member,
 * so a student can only ever see his or her own borrowings, reservations and fines.
 * (Reserving a book reuses ReservationDAO.addReservation, so the same rules apply.)
 */
public class StudentPortalDAO {

    // ---------- My borrowed books (JOIN: details + borrowings + members + books) ----------

    public List<BorrowRecord> getBorrowings(int memberId, boolean activeOnly) throws SQLException {
        String sql = "SELECT d.detail_id, d.borrow_id, br.member_id, "
                   + "m.last_name || ', ' || m.first_name AS member_name, "
                   + "d.book_id, bk.title, br.borrow_date, br.due_date, d.return_date, d.status "
                   + "FROM BORROWING_DETAILS d "
                   + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                   + "INNER JOIN MEMBERS m ON br.member_id = m.member_id "
                   + "INNER JOIN BOOKS bk ON d.book_id = bk.book_id "
                   + "WHERE br.member_id = ? "
                   + (activeOnly ? "AND d.status = 'Borrowed' " : "")
                   + "ORDER BY d.detail_id DESC";
        List<BorrowRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BorrowRecord(
                        rs.getInt("detail_id"),
                        rs.getInt("borrow_id"),
                        rs.getInt("member_id"),
                        rs.getString("member_name"),
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("borrow_date"),
                        rs.getString("due_date"),
                        rs.getString("return_date"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    // ---------- My reservations (JOIN: reservations + members + books) ----------

    public List<Reservation> getReservations(int memberId, boolean pendingOnly) throws SQLException {
        String sql = "SELECT r.reservation_id, r.member_id, "
                   + "m.last_name || ', ' || m.first_name AS member_name, "
                   + "r.book_id, bk.title, r.reservation_date, r.status "
                   + "FROM RESERVATIONS r "
                   + "INNER JOIN MEMBERS m ON r.member_id = m.member_id "
                   + "INNER JOIN BOOKS bk ON r.book_id = bk.book_id "
                   + "WHERE r.member_id = ? "
                   + (pendingOnly ? "AND r.status = 'Pending' " : "")
                   + "ORDER BY r.reservation_id DESC";
        List<Reservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Reservation(
                        rs.getInt("reservation_id"),
                        rs.getInt("member_id"),
                        rs.getString("member_name"),
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("reservation_date"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    // UPDATE: a student can cancel only HIS OR HER OWN pending reservation
    public void cancelReservation(int reservationId, int memberId) throws SQLException {
        String sql = "UPDATE RESERVATIONS SET status = 'Cancelled' "
                   + "WHERE reservation_id = ? AND member_id = ? AND status = 'Pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.setInt(2, memberId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Only your own pending reservations can be cancelled.");
            }
        }
    }

    // ---------- My fines (JOIN: fines + details + borrowings + members + books) ----------

    public List<Fine> getFines(int memberId, boolean unpaidOnly) throws SQLException {
        String sql = "SELECT f.fine_id, f.detail_id, "
                   + "m.last_name || ', ' || m.first_name AS member_name, "
                   + "bk.title, f.amount, f.fine_date, f.status "
                   + "FROM FINES f "
                   + "INNER JOIN BORROWING_DETAILS d ON f.detail_id = d.detail_id "
                   + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                   + "INNER JOIN MEMBERS m ON br.member_id = m.member_id "
                   + "INNER JOIN BOOKS bk ON d.book_id = bk.book_id "
                   + "WHERE br.member_id = ? "
                   + (unpaidOnly ? "AND f.status = 'Unpaid' " : "")
                   + "ORDER BY f.fine_id DESC";
        List<Fine> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Fine(
                        rs.getInt("fine_id"),
                        rs.getInt("detail_id"),
                        rs.getString("member_name"),
                        rs.getString("title"),
                        rs.getBigDecimal("amount"),
                        rs.getString("fine_date"),
                        rs.getString("status")
                    ));
                }
            }
        }
        return list;
    }

    // ---------- Dashboard numbers for one member (label -> value, in display order) ----------

    public Map<String, String> getSummary(int memberId) throws SQLException {
        Map<String, String> summary = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            summary.put("Books Borrowed", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM BORROWING_DETAILS d "
              + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
              + "WHERE br.member_id = ? AND d.status = 'Borrowed'", memberId)));
            summary.put("Overdue", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM BORROWING_DETAILS d "
              + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
              + "WHERE br.member_id = ? AND d.status = 'Borrowed' "
              + "AND br.due_date < CURRENT_DATE", memberId)));
            summary.put("Reservations", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM RESERVATIONS "
              + "WHERE member_id = ? AND status = 'Pending'", memberId)));

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(f.amount) FROM FINES f "
                  + "INNER JOIN BORROWING_DETAILS d ON f.detail_id = d.detail_id "
                  + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                  + "WHERE br.member_id = ? AND f.status = 'Unpaid'")) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    BigDecimal total = rs.getBigDecimal(1);
                    if (total == null) {
                        total = BigDecimal.ZERO;
                    }
                    summary.put("Unpaid Fines", "PHP " + total.setScale(2).toPlainString());
                }
            }
        }
        return summary;
    }

    private int count(Connection conn, String sql, int memberId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
