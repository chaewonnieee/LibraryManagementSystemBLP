package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    public static class Report {
        public final String[] columns;
        public final List<Object[]> rows = new ArrayList<>();

        public Report(String... columns) {
            this.columns = columns;
        }
    }

    public Map<String, String> getDashboardSummary() throws SQLException {
        Map<String, String> summary = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            summary.put("Book Titles", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM BOOKS")));
            summary.put("Members", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM MEMBERS")));
            summary.put("Books Out", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM BORROWING_DETAILS WHERE status = 'Borrowed'")));
            summary.put("Overdue", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM BORROWING_DETAILS d "
              + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
              + "WHERE d.status = 'Borrowed' AND br.due_date < CURRENT_DATE")));
            summary.put("Reservations", String.valueOf(count(conn,
                "SELECT COUNT(*) FROM RESERVATIONS WHERE status = 'Pending'")));

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) FROM FINES WHERE status = 'Unpaid'");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal total = rs.getBigDecimal(1);
                if (total == null) {
                    total = BigDecimal.ZERO;
                }
                summary.put("Unpaid Fines", "PHP " + total.setScale(2).toPlainString());
            }
        }
        return summary;
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public Report getOverdueBooks() throws SQLException {
        String sql = "SELECT d.detail_id, m.last_name, m.first_name, m.contact_no, "
                   + "bk.title, br.due_date "
                   + "FROM BORROWING_DETAILS d "
                   + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                   + "INNER JOIN MEMBERS m ON br.member_id = m.member_id "
                   + "INNER JOIN BOOKS bk ON d.book_id = bk.book_id "
                   + "WHERE d.status = 'Borrowed' AND br.due_date < CURRENT_DATE "
                   + "ORDER BY br.due_date";
        Report report = new Report("Record ID", "Member", "Contact No", "Book",
                                   "Due Date", "Days Overdue", "Fine So Far (PHP)");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LocalDate due = rs.getDate("due_date").toLocalDate();
                long daysLate = ChronoUnit.DAYS.between(due, LocalDate.now());
                BigDecimal fine = BorrowingDAO.FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate));
                String contact = rs.getString("contact_no");
                report.rows.add(new Object[]{
                    rs.getInt("detail_id"),
                    rs.getString("last_name") + ", " + rs.getString("first_name"),
                    contact == null ? "" : contact,
                    rs.getString("title"),
                    due.toString(),
                    daysLate,
                    fine.setScale(2).toPlainString()
                });
            }
        }
        return report;
    }

    public Report getDailyBorrowings() throws SQLException {
        String sql = "SELECT br.borrow_date, COUNT(*) AS books_borrowed "
                   + "FROM BORROWINGS br "
                   + "INNER JOIN BORROWING_DETAILS d ON br.borrow_id = d.borrow_id "
                   + "GROUP BY br.borrow_date "
                   + "ORDER BY br.borrow_date DESC";
        Report report = new Report("Date", "Books Borrowed");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                report.rows.add(new Object[]{rs.getString(1), rs.getInt(2)});
            }
        }
        return report;
    }

    public Report getInventoryStatus() throws SQLException {
        String sql = "SELECT b.title, c.category_name, b.total_copies, "
                   + "b.available_copies, b.status "
                   + "FROM BOOKS b "
                   + "INNER JOIN CATEGORIES c ON b.category_id = c.category_id "
                   + "ORDER BY b.title";
        Report report = new Report("Title", "Category", "Total Copies",
                                   "On Shelf", "On Loan", "Status");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int total = rs.getInt("total_copies");
                int available = rs.getInt("available_copies");
                report.rows.add(new Object[]{
                    rs.getString("title"), rs.getString("category_name"),
                    total, available, total - available, rs.getString("status")
                });
            }
        }
        return report;
    }

    public Report getBooksPerCategory() throws SQLException {
        String sql = "SELECT c.category_id, c.category_name, "
                   + "COUNT(b.book_id) AS titles, SUM(b.total_copies) AS copies "
                   + "FROM CATEGORIES c "
                   + "LEFT JOIN BOOKS b ON c.category_id = b.category_id "
                   + "GROUP BY c.category_id, c.category_name "
                   + "ORDER BY c.category_name";
        Report report = new Report("Category", "Book Titles", "Total Copies");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                report.rows.add(new Object[]{
                    rs.getString("category_name"), rs.getInt("titles"), rs.getInt("copies")
                });
            }
        }
        return report;
    }

    public Report getMostBorrowedBooks() throws SQLException {
        String sql = "SELECT bk.book_id, bk.title, bk.author, "
                   + "COUNT(d.detail_id) AS times_borrowed "
                   + "FROM BORROWING_DETAILS d "
                   + "INNER JOIN BOOKS bk ON d.book_id = bk.book_id "
                   + "GROUP BY bk.book_id, bk.title, bk.author "
                   + "ORDER BY times_borrowed DESC";
        Report report = new Report("Title", "Author", "Times Borrowed");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next() && report.rows.size() < 10) {
                report.rows.add(new Object[]{
                    rs.getString("title"), rs.getString("author"), rs.getInt("times_borrowed")
                });
            }
        }
        return report;
    }

    public Report getUnpaidFinesByMember() throws SQLException {
        String sql = "SELECT m.member_id, m.last_name, m.first_name, "
                   + "COUNT(f.fine_id) AS fine_count, SUM(f.amount) AS total_owed "
                   + "FROM FINES f "
                   + "INNER JOIN BORROWING_DETAILS d ON f.detail_id = d.detail_id "
                   + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                   + "INNER JOIN MEMBERS m ON br.member_id = m.member_id "
                   + "WHERE f.status = 'Unpaid' "
                   + "GROUP BY m.member_id, m.last_name, m.first_name "
                   + "ORDER BY total_owed DESC";
        Report report = new Report("Member", "Unpaid Fines", "Total Owed (PHP)");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                report.rows.add(new Object[]{
                    rs.getString("last_name") + ", " + rs.getString("first_name"),
                    rs.getInt("fine_count"),
                    rs.getBigDecimal("total_owed").setScale(2).toPlainString()
                });
            }
        }
        return report;
    }
}
