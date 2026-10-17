package com.mycompany.librarymanagementsystemblp.database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One-time sample data for testing and demos.
 * Run this class ONCE while the main program is closed (Derby embedded allows one
 * program on the database at a time). Running it again does nothing.
 */
public class DatabaseSeeder {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00");

    // { category, title, author, publisher, total copies, status }
    private static final String[][] BOOKS = {
        {"Fiction", "The Alchemist", "Paulo Coelho", "HarperOne", "3", "Available"},
        {"Fiction", "To Kill a Mockingbird", "Harper Lee", "Grand Central", "2", "Available"},
        {"Fiction", "1984", "George Orwell", "Secker & Warburg", "3", "Available"},
        {"Fiction", "Noli Me Tangere", "Jose Rizal", "Penguin Classics", "4", "Available"},
        {"Fiction", "El Filibusterismo", "Jose Rizal", "Penguin Classics", "2", "Available"},
        {"Fiction", "Harry Potter and the Sorcerer's Stone", "J.K. Rowling", "Scholastic", "1", "Available"},
        {"Non-Fiction", "Atomic Habits", "James Clear", "Avery", "2", "Available"},
        {"Non-Fiction", "Educated", "Tara Westover", "Random House", "1", "Available"},
        {"Non-Fiction", "Sapiens", "Yuval Noah Harari", "Harper", "3", "Available"},
        {"Non-Fiction", "The Diary of a Young Girl", "Anne Frank", "Bantam", "2", "Available"},
        {"Science", "A Brief History of Time", "Stephen Hawking", "Bantam", "2", "Available"},
        {"Science", "Cosmos", "Carl Sagan", "Random House", "1", "Available"},
        {"Science", "The Selfish Gene", "Richard Dawkins", "Oxford Press", "2", "Available"},
        {"Science", "Astrophysics for People in a Hurry", "Neil deGrasse Tyson", "Norton", "3", "Available"},
        {"Reference", "Merriam-Webster Dictionary", "Merriam-Webster", "Merriam-Webster", "2", "Available"},
        {"Reference", "Encyclopedia Britannica Vol. 1", "Britannica Editors", "Britannica", "1", "Available"},
        {"Reference", "The Chicago Manual of Style", "University of Chicago Press", "UChicago Press", "1", "Unavailable"},
        {"Textbook", "Database System Concepts", "Silberschatz", "McGraw-Hill", "4", "Available"},
        {"Textbook", "Java: The Complete Reference", "Herbert Schildt", "McGraw-Hill", "3", "Available"},
        {"Textbook", "Computer Networking: A Top-Down Approach", "Kurose and Ross", "Pearson", "2", "Available"},
        {"Textbook", "Introduction to Algorithms", "Cormen", "MIT Press", "2", "Available"}
    };

    // { first name, last name, contact no, status }  (email is built from the name)
    private static final String[][] MEMBERS = {
        {"Maria", "Santos", "09171234501", "Active"},
        {"Juan", "Reyes", "09171234502", "Active"},
        {"Ana", "Cruz", "09171234503", "Active"},
        {"Pedro", "Garcia", "09171234504", "Active"},
        {"Liza", "Mendoza", "09171234505", "Active"},
        {"Carlo", "Torres", "09171234506", "Active"},
        {"Bea", "Ramos", "09171234507", "Active"},
        {"Miguel", "Flores", "09171234508", "Active"},
        {"Sofia", "Villanueva", "09171234509", "Active"},
        {"Luis", "Aquino", "09171234510", "Active"},
        {"Karen", "Bautista", "09171234511", "Active"},
        {"Ryan", "Castillo", "09171234512", "Inactive"}
    };

    public static void main(String[] args) {
        DatabaseSetup.initialize();   // make sure the tables exist
        seed();
    }

    public static void seed() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (exists(conn, "SELECT COUNT(*) FROM BOOKS WHERE isbn = ?", isbn(1))) {
                    System.out.println("Sample data is already in the database. Nothing was added.");
                    return;
                }

                int userId = firstId(conn, "SELECT user_id FROM USERS ORDER BY user_id");

                // ----- members -----
                int[] memberIds = new int[MEMBERS.length];
                for (int i = 0; i < MEMBERS.length; i++) {
                    String email = (MEMBERS[i][0] + "." + MEMBERS[i][1]).toLowerCase() + "@mail.com";
                    memberIds[i] = addMember(conn, MEMBERS[i], email);
                }

                // ----- books -----
                int[] bookIds = new int[BOOKS.length];
                for (int i = 0; i < BOOKS.length; i++) {
                    bookIds[i] = addBook(conn, BOOKS[i], isbn(i + 1));
                }

                // ----- borrowings -----
                // borrow(member#, book#, days ago borrowed, loan days, days ago returned (-1 = still out), fine paid?)
                // Still out, on time
                borrow(conn, userId, memberIds[0], bookIds[5], 3, 7, -1, false);    // Santos - Harry Potter
                borrow(conn, userId, memberIds[3], bookIds[17], 2, 14, -1, false);  // Garcia - Database Concepts
                borrow(conn, userId, memberIds[3], bookIds[18], 2, 14, -1, false);  // Garcia - Java
                borrow(conn, userId, memberIds[4], bookIds[0], 5, 7, -1, false);    // Mendoza - Alchemist
                borrow(conn, userId, memberIds[6], bookIds[15], 1, 7, -1, false);   // Ramos - Britannica
                // Still out, OVERDUE
                borrow(conn, userId, memberIds[1], bookIds[7], 12, 7, -1, false);   // Reyes - Educated (5 days late)
                borrow(conn, userId, memberIds[2], bookIds[11], 15, 7, -1, false);  // Cruz - Cosmos (8 days late)
                borrow(conn, userId, memberIds[3], bookIds[20], 9, 7, -1, false);   // Garcia - Algorithms (2 days late)
                borrow(conn, userId, memberIds[5], bookIds[2], 20, 14, -1, false);  // Torres - 1984 (6 days late)
                // Returned on time
                borrow(conn, userId, memberIds[7], bookIds[3], 30, 14, 20, false);  // Flores - Noli
                borrow(conn, userId, memberIds[9], bookIds[13], 18, 14, 10, false); // Aquino - Astrophysics
                borrow(conn, userId, memberIds[9], bookIds[0], 28, 14, 15, false);  // Aquino - Alchemist
                borrow(conn, userId, memberIds[1], bookIds[3], 45, 14, 40, false);  // Reyes - Noli
                // Returned LATE (a fine is created)
                borrow(conn, userId, memberIds[7], bookIds[8], 25, 7, 16, true);    // Flores - Sapiens (2 days, paid)
                borrow(conn, userId, memberIds[8], bookIds[10], 40, 14, 22, false); // Villanueva - Brief History (4 days)
                borrow(conn, userId, memberIds[8], bookIds[3], 35, 7, 21, false);   // Villanueva - Noli (7 days)
                borrow(conn, userId, memberIds[10], bookIds[0], 33, 7, 24, false);  // Bautista - Alchemist (2 days)
                borrow(conn, userId, memberIds[2], bookIds[6], 22, 7, 13, true);    // Cruz - Atomic Habits (2 days, paid)

                // ----- reservations (only for books with no copies on the shelf) -----
                reserve(conn, memberIds[4], bookIds[5], 2, "Pending");     // Mendoza -> Harry Potter
                reserve(conn, memberIds[7], bookIds[5], 1, "Pending");     // Flores -> Harry Potter
                reserve(conn, memberIds[8], bookIds[7], 3, "Pending");     // Villanueva -> Educated
                reserve(conn, memberIds[9], bookIds[11], 4, "Fulfilled");  // Aquino -> Cosmos
                reserve(conn, memberIds[10], bookIds[15], 5, "Cancelled"); // Bautista -> Britannica

                conn.commit();
                System.out.println("Sample data added: " + MEMBERS.length + " members, "
                    + BOOKS.length + " books, 18 borrowings (with fines), 5 reservations.");
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Seeding failed!");
            e.printStackTrace();
        }
    }

    // ---------- inserts ----------

    private static int addMember(Connection conn, String[] m, String email) throws SQLException {
        int existing = findId(conn, "SELECT member_id FROM MEMBERS WHERE email = ?", email);
        if (existing > 0) {
            return existing;
        }
        String sql = "INSERT INTO MEMBERS (first_name, last_name, contact_no, email, address, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m[0]);
            ps.setString(2, m[1]);
            ps.setString(3, m[2]);
            ps.setString(4, email);
            ps.setString(5, "Antipolo, Rizal");
            ps.setString(6, m[3]);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    private static int addBook(Connection conn, String[] b, String isbn) throws SQLException {
        int categoryId = findId(conn, "SELECT category_id FROM CATEGORIES WHERE category_name = ?", b[0]);
        if (categoryId < 1) {
            throw new IllegalStateException("Category not found: " + b[0]);
        }
        int copies = Integer.parseInt(b[4]);
        String sql = "INSERT INTO BOOKS (category_id, isbn, title, author, publisher, "
                   + "total_copies, available_copies, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, categoryId);
            ps.setString(2, isbn);
            ps.setString(3, b[1]);
            ps.setString(4, b[2]);
            ps.setString(5, b[3]);
            ps.setInt(6, copies);
            ps.setInt(7, copies);
            ps.setString(8, b[5]);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    // Creates a borrowing with dates in the past, the same way BorrowingDAO would
    private static void borrow(Connection conn, int userId, int memberId, int bookId,
                               int daysAgoBorrowed, int loanDays, int daysAgoReturned,
                               boolean finePaid) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate borrowDate = today.minusDays(daysAgoBorrowed);
        LocalDate dueDate = borrowDate.plusDays(loanDays);
        boolean returned = daysAgoReturned >= 0;
        LocalDate returnDate = returned ? today.minusDays(daysAgoReturned) : null;

        int borrowId;
        String sqlBorrow = "INSERT INTO BORROWINGS (member_id, user_id, borrow_date, due_date, status) "
                         + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlBorrow, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, memberId);
            ps.setInt(2, userId);
            ps.setDate(3, Date.valueOf(borrowDate));
            ps.setDate(4, Date.valueOf(dueDate));
            ps.setString(5, returned ? "Returned" : "Borrowed");
            ps.executeUpdate();
            borrowId = generatedKey(ps);
        }

        int detailId;
        String sqlDetail = "INSERT INTO BORROWING_DETAILS (borrow_id, book_id, return_date, status) "
                         + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlDetail, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, borrowId);
            ps.setInt(2, bookId);
            if (returned) {
                ps.setDate(3, Date.valueOf(returnDate));
            } else {
                ps.setNull(3, java.sql.Types.DATE);
            }
            ps.setString(4, returned ? "Returned" : "Borrowed");
            ps.executeUpdate();
            detailId = generatedKey(ps);
        }

        if (!returned) {   // the copy is still out, so it is not on the shelf
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE BOOKS SET available_copies = available_copies - 1 WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
        } else {
            long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
            if (daysLate > 0) {
                String sqlFine = "INSERT INTO FINES (detail_id, amount, fine_date, status) "
                               + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlFine)) {
                    ps.setInt(1, detailId);
                    ps.setBigDecimal(2, FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate)));
                    ps.setDate(3, Date.valueOf(returnDate));
                    ps.setString(4, finePaid ? "Paid" : "Unpaid");
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void reserve(Connection conn, int memberId, int bookId,
                                int daysAgo, String status) throws SQLException {
        String sql = "INSERT INTO RESERVATIONS (member_id, book_id, reservation_date, status) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            ps.setDate(3, Date.valueOf(LocalDate.now().minusDays(daysAgo)));
            ps.setString(4, status);
            ps.executeUpdate();
        }
    }

    // ---------- small helpers ----------

    private static String isbn(int number) {
        return String.format("978-1-0000-%03d-0", number);
    }

    private static int generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getInt(1);
        }
    }

    // Returns the first int found by the query, or 0 if there is no row
    private static int findId(Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int firstId(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("No user found. Start the main program once first.");
            }
            return rs.getInt(1);
        }
    }

    private static boolean exists(Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
