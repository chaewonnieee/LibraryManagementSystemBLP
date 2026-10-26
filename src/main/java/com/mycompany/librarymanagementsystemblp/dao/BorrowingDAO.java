package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.BorrowRecord;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BorrowingDAO {

    public static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00");
    public static final int MAX_ACTIVE_LOANS = 3;
    public static final int DEFAULT_LOAN_DAYS = 7;

    public static class AutoLoan {
        public final String memberName;
        public final String bookTitle;
        public final LocalDate dueDate;

        public AutoLoan(String memberName, String bookTitle, LocalDate dueDate) {
            this.memberName = memberName;
            this.bookTitle = bookTitle;
            this.dueDate = dueDate;
        }
    }

    public static class ReturnResult {
        public final BigDecimal fine;
        public final List<AutoLoan> autoLoans;

        public ReturnResult(BigDecimal fine, List<AutoLoan> autoLoans) {
            this.fine = fine;
            this.autoLoans = autoLoans;
        }
    }

    private static final String SELECT_RECORDS =
        "SELECT d.detail_id, d.borrow_id, br.member_id, "
      + "m.last_name || ', ' || m.first_name AS member_name, "
      + "d.book_id, bk.title, br.borrow_date, br.due_date, d.return_date, d.status "
      + "FROM BORROWING_DETAILS d "
      + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
      + "INNER JOIN MEMBERS m ON br.member_id = m.member_id "
      + "INNER JOIN BOOKS bk ON d.book_id = bk.book_id ";

    private BorrowRecord mapRow(ResultSet rs) throws SQLException {
        return new BorrowRecord(
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
        );
    }

    public List<BorrowRecord> getRecords(String keyword, boolean activeOnly) throws SQLException {
        String sql = SELECT_RECORDS
                   + "WHERE (UPPER(m.first_name) LIKE ? OR UPPER(m.last_name) LIKE ? "
                   + "OR UPPER(bk.title) LIKE ?) "
                   + (activeOnly ? "AND d.status = 'Borrowed' " : "")
                   + "ORDER BY d.detail_id DESC";
        List<BorrowRecord> list = new ArrayList<>();
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

    public void borrowBook(int memberId, int userId, int bookId, LocalDate dueDate)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                checkMemberCanBorrow(conn, memberId);
                checkBookCanBeBorrowed(conn, bookId, memberId);
                lendCopy(conn, memberId, userId, bookId, dueDate);

                String sqlOwn = "UPDATE RESERVATIONS SET status = 'Fulfilled' "
                              + "WHERE member_id = ? AND book_id = ? AND status = 'Pending'";
                try (PreparedStatement ps = conn.prepareStatement(sqlOwn)) {
                    ps.setInt(1, memberId);
                    ps.setInt(2, bookId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void lendCopy(Connection conn, int memberId, int userId, int bookId,
                                 LocalDate dueDate) throws SQLException {
        int borrowId;
        String sqlBorrow = "INSERT INTO BORROWINGS (member_id, user_id, due_date) "
                         + "VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(
                sqlBorrow, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, memberId);
            ps.setInt(2, userId);
            ps.setDate(3, java.sql.Date.valueOf(dueDate));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                borrowId = keys.getInt(1);
            }
        }

        String sqlDetail = "INSERT INTO BORROWING_DETAILS (borrow_id, book_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
            ps.setInt(1, borrowId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        }

        String sqlBook = "UPDATE BOOKS SET available_copies = available_copies - 1 "
                       + "WHERE book_id = ? AND available_copies > 0";
        try (PreparedStatement ps = conn.prepareStatement(sqlBook)) {
            ps.setInt(1, bookId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("No copies of this book are available.");
            }
        }
    }

    public static List<AutoLoan> fulfillWaitingReservations(Connection conn, int userId)
            throws SQLException {
        List<Integer> bookIds = new ArrayList<>();
        String sqlBooks = "SELECT DISTINCT r.book_id FROM RESERVATIONS r "
                        + "INNER JOIN BOOKS b ON r.book_id = b.book_id "
                        + "WHERE r.status = 'Pending' AND b.available_copies > 0 "
                        + "AND b.status = 'Available' ORDER BY r.book_id";
        try (PreparedStatement ps = conn.prepareStatement(sqlBooks);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bookIds.add(rs.getInt(1));
            }
        }

        List<AutoLoan> loans = new ArrayList<>();
        for (int bookId : bookIds) {
            AutoLoan loan = fulfillNextReservation(conn, bookId, userId);
            while (loan != null) {
                loans.add(loan);
                loan = fulfillNextReservation(conn, bookId, userId);
            }
        }
        return loans;
    }

    private static AutoLoan fulfillNextReservation(Connection conn, int bookId, int userId)
            throws SQLException {
        String title;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT title, available_copies, status FROM BOOKS WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("available_copies") <= 0
                        || !"Available".equals(rs.getString("status"))) {
                    return null;
                }
                title = rs.getString("title");
            }
        }

        List<int[]> queue = new ArrayList<>();
        List<String> names = new ArrayList<>();
        String sqlQueue = "SELECT r.reservation_id, r.member_id, m.first_name, m.last_name "
                        + "FROM RESERVATIONS r "
                        + "INNER JOIN MEMBERS m ON r.member_id = m.member_id "
                        + "WHERE r.book_id = ? AND r.status = 'Pending' "
                        + "ORDER BY r.reservation_date, r.reservation_id";
        try (PreparedStatement ps = conn.prepareStatement(sqlQueue)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    queue.add(new int[]{rs.getInt("reservation_id"), rs.getInt("member_id")});
                    names.add(rs.getString("first_name") + " " + rs.getString("last_name"));
                }
            }
        }

        for (int i = 0; i < queue.size(); i++) {
            int reservationId = queue.get(i)[0];
            int memberId = queue.get(i)[1];
            try {
                checkMemberCanBorrow(conn, memberId);
                checkBookCanBeBorrowed(conn, bookId, memberId);
            } catch (IllegalStateException ex) {
                continue;
            }

            LocalDate dueDate = LocalDate.now().plusDays(DEFAULT_LOAN_DAYS);
            lendCopy(conn, memberId, userId, bookId, dueDate);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE RESERVATIONS SET status = 'Fulfilled' WHERE reservation_id = ?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }
            return new AutoLoan(names.get(i), title, dueDate);
        }
        return null;
    }

    public ReturnResult returnBook(int detailId, int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int borrowId;
                int bookId;
                LocalDate dueDate;

                String sqlFind = "SELECT d.borrow_id, d.book_id, d.status, br.due_date "
                               + "FROM BORROWING_DETAILS d "
                               + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                               + "WHERE d.detail_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlFind)) {
                    ps.setInt(1, detailId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("This borrowing record was not found.");
                        }
                        if (!"Borrowed".equals(rs.getString("status"))) {
                            throw new IllegalStateException("This book has already been returned.");
                        }
                        borrowId = rs.getInt("borrow_id");
                        bookId = rs.getInt("book_id");
                        dueDate = rs.getDate("due_date").toLocalDate();
                    }
                }

                String sqlReturn = "UPDATE BORROWING_DETAILS "
                                 + "SET return_date = CURRENT_DATE, status = 'Returned' "
                                 + "WHERE detail_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlReturn)) {
                    ps.setInt(1, detailId);
                    ps.executeUpdate();
                }

                String sqlBook = "UPDATE BOOKS SET available_copies = available_copies + 1 "
                               + "WHERE book_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlBook)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                String sqlClose = "UPDATE BORROWINGS SET status = 'Returned' "
                                + "WHERE borrow_id = ? AND NOT EXISTS ("
                                + "SELECT 1 FROM BORROWING_DETAILS "
                                + "WHERE borrow_id = ? AND status = 'Borrowed')";
                try (PreparedStatement ps = conn.prepareStatement(sqlClose)) {
                    ps.setInt(1, borrowId);
                    ps.setInt(2, borrowId);
                    ps.executeUpdate();
                }

                BigDecimal fine = BigDecimal.ZERO;
                long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                if (daysLate > 0) {
                    fine = FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate));
                    String sqlFine = "INSERT INTO FINES (detail_id, amount) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlFine)) {
                        ps.setInt(1, detailId);
                        ps.setBigDecimal(2, fine);
                        ps.executeUpdate();
                    }
                }

                List<AutoLoan> autoLoans = fulfillWaitingReservations(conn, userId);

                conn.commit();
                return new ReturnResult(fine, autoLoans);
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void checkMemberCanBorrow(Connection conn, int memberId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM MEMBERS WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Member not found.");
                }
                if (!"Active".equals(rs.getString("status"))) {
                    throw new IllegalStateException("This member is inactive and cannot borrow books.");
                }
            }
        }

        String sqlCount = "SELECT COUNT(*) FROM BORROWING_DETAILS d "
                        + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                        + "WHERE br.member_id = ? AND d.status = 'Borrowed'";
        try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= MAX_ACTIVE_LOANS) {
                    throw new IllegalStateException("This member already has "
                        + MAX_ACTIVE_LOANS + " books borrowed, which is the limit.");
                }
            }
        }
    }

    private static void checkBookCanBeBorrowed(Connection conn, int bookId, int memberId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT available_copies, status FROM BOOKS WHERE book_id = ?")) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Book not found.");
                }
                if ("Unavailable".equals(rs.getString("status"))) {
                    throw new IllegalStateException("This book is marked as unavailable.");
                }
                if (rs.getInt("available_copies") <= 0) {
                    throw new IllegalStateException("No copies of this book are available.");
                }
            }
        }

        String sqlSame = "SELECT COUNT(*) FROM BORROWING_DETAILS d "
                       + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
                       + "WHERE br.member_id = ? AND d.book_id = ? AND d.status = 'Borrowed'";
        try (PreparedStatement ps = conn.prepareStatement(sqlSame)) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new IllegalStateException("This member has already borrowed this book.");
                }
            }
        }
    }
}
