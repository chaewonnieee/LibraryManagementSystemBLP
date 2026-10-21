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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills the database with demo data for presentations.
 *
 * Every table is filled OLDEST FIRST, so a bigger ID always means a newer
 * record. That makes the lists look right in every screen:
 *   - Reservations (shown ID 1 -> last): the newest reservation is at the bottom.
 *   - Borrowings and Fines (shown newest first): the highest ID is at the top.
 *   - Members: the newest member has the highest ID and the latest registration date.
 *
 * All dates are relative to today, so the data always looks fresh
 * (overdue books stay overdue, "borrowed today" is really today).
 *
 * Run this class to RESET and reload the demo data:
 *   - It clears Members, Books, Borrowings, Reservations and Fines.
 *   - It does NOT touch Users (admin/librarian accounts) or Categories.
 *   - Close the main program first (the embedded database allows one program at a time).
 */
public class DatabaseSeeder {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00");
    private static final int MAX_ACTIVE_LOANS = 3;

    // category, title, author, publisher, copies, status
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

    // first name, last name, contact no, status, address, registered (days ago)
    // Oldest member first, so the newest member gets the highest ID.
    private static final String[][] MEMBERS = {
        {"Maria", "Santos", "09171234501", "Active", "Brgy. San Roque, Antipolo, Rizal", "120"},
        {"Juan", "Reyes", "09171234502", "Active", "Brgy. Dolores, Taytay, Rizal", "110"},
        {"Ana", "Cruz", "09171234503", "Active", "Brgy. Sto. Domingo, Cainta, Rizal", "100"},
        {"Pedro", "Garcia", "09171234504", "Active", "Brgy. Mayamot, Antipolo, Rizal", "95"},
        {"Liza", "Mendoza", "09171234505", "Active", "Brgy. San Isidro, Angono, Rizal", "85"},
        {"Carlo", "Torres", "09171234506", "Active", "Brgy. Muzon, Taytay, Rizal", "75"},
        {"Bea", "Ramos", "09171234507", "Active", "Brgy. Calawis, Antipolo, Rizal", "65"},
        {"Miguel", "Flores", "09171234508", "Active", "Brgy. San Juan, Cainta, Rizal", "60"},
        {"Sofia", "Villanueva", "09171234509", "Active", "Brgy. Bagong Nayon, Antipolo, Rizal", "55"},
        {"Luis", "Aquino", "09171234510", "Active", "Brgy. Darangan, Binangonan, Rizal", "50"},
        {"Karen", "Bautista", "09171234511", "Active", "Brgy. Sta. Rosa, Cainta, Rizal", "45"},
        {"Ryan", "Castillo", "09171234512", "Inactive", "Brgy. Cupang, Antipolo, Rizal", "40"}
    };

    /** A borrowing. returnedDaysAgo = -1 means the book is still out. */
    private record LoanPlan(String member, String book, int borrowedDaysAgo, int loanDays,
                            int returnedDaysAgo, boolean finePaid) {
        boolean returned() {
            return returnedDaysAgo >= 0;
        }
    }

    private record ReservationPlan(String member, String book, int daysAgo, String status) {
    }

    private record FinePlan(int detailId, BigDecimal amount, LocalDate fineDate, String status) {
    }

    private static LoanPlan returned(String member, String book, int borrowedAgo, int loanDays,
                                     int returnedAgo, boolean finePaid) {
        return new LoanPlan(member, book, borrowedAgo, loanDays, returnedAgo, finePaid);
    }

    private static LoanPlan out(String member, String book, int borrowedAgo, int loanDays) {
        return new LoanPlan(member, book, borrowedAgo, loanDays, -1, false);
    }

    // Members are looked up by first name and books by the start of the title.
    // The order below does not matter: loans are sorted by date before saving.
    private static final List<LoanPlan> LOANS = List.of(
        // ---- Returned on time (no fine) ----
        returned("Juan",   "Noli",          45, 14, 40, false),
        returned("Ryan",   "Atomic",        38,  7, 32, false),
        returned("Miguel", "Noli",          30, 14, 20, false),
        returned("Luis",   "Astrophysics",  18, 14, 10, false),
        returned("Luis",   "Harry",         19,  7, 12, false),
        returned("Bea",    "To Kill",       14,  7,  9, false),
        returned("Carlo",  "Java",          17, 14,  6, false),
        returned("Maria",  "The Selfish",   11,  7,  5, false),

        // ---- Returned late (fine = PHP 5.00 per day late) ----
        returned("Sofia",  "A Brief",       40, 14, 22, false),  // 4 days late  -> 20.00 unpaid
        returned("Sofia",  "Noli",          35,  7, 21, false),  // 7 days late  -> 35.00 unpaid
        returned("Karen",  "The Alchemist", 33,  7, 24, false),  // 2 days late  -> 10.00 unpaid
        returned("Luis",   "The Alchemist", 28, 14, 15, false),  // 1 day late   ->  5.00 unpaid
        returned("Miguel", "Sapiens",       25,  7, 16, true),   // 2 days late  -> 10.00 paid
        returned("Ana",    "Atomic",        22,  7, 13, true),   // 2 days late  -> 10.00 paid
        returned("Liza",   "The Diary",     10,  7,  2, false),  // 1 day late   ->  5.00 unpaid

        // ---- Still borrowed and OVERDUE (shows up in Overdue report) ----
        out("Carlo",  "1984",        20, 14),  // 6 days overdue
        out("Ana",    "Cosmos",      15,  7),  // 8 days overdue
        out("Juan",   "Educated",    12,  7),  // 5 days overdue
        out("Pedro",  "Introduction", 9,  7),  // 2 days overdue

        // ---- Still borrowed, not yet due ----
        out("Liza",   "The Alchemist", 5,  7),
        out("Maria",  "Harry",         3,  7),
        out("Pedro",  "Database",      2, 14),
        out("Pedro",  "Java",          2, 14),  // Pedro is at the 3-book limit
        out("Bea",    "Encyclopedia",  1,  7),
        out("Karen",  "Sapiens",       0, 14)   // borrowed today
    );

    // Reservations only make sense for books with no copy on the shelf.
    private static final List<ReservationPlan> RESERVATIONS = List.of(
        new ReservationPlan("Luis",   "Harry",        22, "Fulfilled"),
        new ReservationPlan("Sofia",  "Educated",      8, "Pending"),
        new ReservationPlan("Karen",  "Encyclopedia",  6, "Cancelled"),
        new ReservationPlan("Carlo",  "Cosmos",        4, "Pending"),
        new ReservationPlan("Liza",   "Harry",         2, "Pending"),
        new ReservationPlan("Miguel", "Harry",         1, "Pending"),
        new ReservationPlan("Luis",   "Encyclopedia",  0, "Pending")
    );

    public static void main(String[] args) {
        DatabaseSetup.initialize();
        reset();
        seed();
        DatabaseConnection.shutdown();
    }

    /** Clears the demo tables and restarts their IDs at 1. Users and Categories are kept. */
    public static void reset() {
        String[][] tables = {
            {"FINES", "fine_id"},
            {"BORROWING_DETAILS", "detail_id"},
            {"BORROWINGS", "borrow_id"},
            {"RESERVATIONS", "reservation_id"},
            {"BOOKS", "book_id"},
            {"MEMBERS", "member_id"}
        };
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            for (String[] t : tables) {
                st.executeUpdate("DELETE FROM " + t[0]);
                st.executeUpdate("ALTER TABLE " + t[0] + " ALTER COLUMN " + t[1] + " RESTART WITH 1");
            }
            System.out.println("Old sample data cleared.");
        } catch (SQLException e) {
            System.out.println("Reset failed!");
            e.printStackTrace();
        }
    }

    public static void seed() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (count(conn, "SELECT COUNT(*) FROM BOOKS") > 0) {
                    System.out.println("The database already has books. Nothing was added. "
                        + "Run DatabaseSeeder as a program to reset and reload the sample data.");
                    return;
                }

                verifyPlan();

                int userId = firstId(conn, "SELECT user_id FROM USERS ORDER BY user_id");

                // Members, oldest registration first.
                Map<String, Integer> memberIds = new HashMap<>();
                List<String[]> members = new ArrayList<>(List.of(MEMBERS));
                members.sort(Comparator.comparingInt((String[] m) -> Integer.parseInt(m[5])).reversed());
                for (String[] m : members) {
                    memberIds.put(m[0], addMember(conn, m));
                }

                Map<String, Integer> bookIds = new HashMap<>();
                for (int i = 0; i < BOOKS.length; i++) {
                    bookIds.put(BOOKS[i][1], addBook(conn, BOOKS[i], isbn(i + 1)));
                }

                // Borrowings, oldest first, so a higher ID is always a newer borrowing.
                List<LoanPlan> loans = new ArrayList<>(LOANS);
                loans.sort(Comparator.comparingInt(LoanPlan::borrowedDaysAgo).reversed());
                List<FinePlan> fines = new ArrayList<>();
                for (LoanPlan loan : loans) {
                    borrow(conn, userId, memberIds.get(loan.member()),
                           bookIds.get(findTitle(loan.book())), loan, fines);
                }

                // Fines are saved after all borrowings, oldest fine first.
                fines.sort(Comparator.comparing(FinePlan::fineDate).thenComparingInt(FinePlan::detailId));
                for (FinePlan f : fines) {
                    addFine(conn, f);
                }

                // Reservations, oldest first, so the newest one has the highest ID.
                List<ReservationPlan> reservations = new ArrayList<>(RESERVATIONS);
                reservations.sort(Comparator.comparingInt(ReservationPlan::daysAgo).reversed());
                for (ReservationPlan r : reservations) {
                    reserve(conn, memberIds.get(r.member()),
                            bookIds.get(findTitle(r.book())), r);
                }

                conn.commit();
                System.out.println("Sample data added: " + MEMBERS.length + " members, "
                    + BOOKS.length + " books, " + LOANS.size() + " borrowings, "
                    + fines.size() + " fines, " + RESERVATIONS.size() + " reservations.");
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println("Seeding failed!");
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // Safety check: the sample data follows the same rules as the program.
    // If a rule is broken it stops BEFORE anything is saved.
    // ------------------------------------------------------------------
    static void verifyPlan() {
        Map<String, Integer> registeredAgo = new HashMap<>();
        Map<String, String> memberStatus = new HashMap<>();
        for (String[] m : MEMBERS) {
            registeredAgo.put(m[0], Integer.parseInt(m[5]));
            memberStatus.put(m[0], m[3]);
        }

        Map<String, Integer> activeLoans = new HashMap<>();
        Map<String, Integer> onShelf = new HashMap<>();
        Map<String, Integer> totalCopies = new HashMap<>();
        Map<String, String> bookStatus = new HashMap<>();
        for (String[] b : BOOKS) {
            totalCopies.put(b[1], Integer.parseInt(b[4]));
            onShelf.put(b[1], Integer.parseInt(b[4]));
            bookStatus.put(b[1], b[5]);
        }

        List<String> activePairs = new ArrayList<>();
        for (LoanPlan l : LOANS) {
            Integer reg = registeredAgo.get(l.member());
            if (reg == null) {
                throw new IllegalStateException("Unknown member: " + l.member());
            }
            String title = findTitle(l.book());
            if (l.borrowedDaysAgo() > reg) {
                throw new IllegalStateException(l.member() + " borrowed before registering.");
            }
            if (l.returned() && l.returnedDaysAgo() > l.borrowedDaysAgo()) {
                throw new IllegalStateException(l.member() + " returned before borrowing.");
            }
            if ("Unavailable".equals(bookStatus.get(title))) {
                throw new IllegalStateException(title + " is unavailable and cannot be borrowed.");
            }
            if (!l.returned()) {
                if (!"Active".equals(memberStatus.get(l.member()))) {
                    throw new IllegalStateException(l.member() + " is inactive but has a book out.");
                }
                int n = activeLoans.merge(l.member(), 1, Integer::sum);
                if (n > MAX_ACTIVE_LOANS) {
                    throw new IllegalStateException(l.member() + " has more than "
                        + MAX_ACTIVE_LOANS + " books out.");
                }
                String pair = l.member() + "|" + title;
                if (activePairs.contains(pair)) {
                    throw new IllegalStateException(l.member() + " borrowed " + title + " twice.");
                }
                activePairs.add(pair);
                onShelf.merge(title, -1, Integer::sum);
            }
        }

        // At no point in time may more copies be out than the library owns.
        for (String title : totalCopies.keySet()) {
            for (int day = 130; day >= 0; day--) {
                int out = 0;
                for (LoanPlan l : LOANS) {
                    if (!findTitle(l.book()).equals(title)) {
                        continue;
                    }
                    boolean started = day <= l.borrowedDaysAgo();
                    boolean notBack = !l.returned() || day > l.returnedDaysAgo();
                    if (started && notBack) {
                        out++;
                    }
                }
                if (out > totalCopies.get(title)) {
                    throw new IllegalStateException("Too many copies of " + title
                        + " out at " + day + " days ago.");
                }
            }
        }

        List<String> pendingPairs = new ArrayList<>();
        for (ReservationPlan r : RESERVATIONS) {
            Integer reg = registeredAgo.get(r.member());
            if (reg == null || r.daysAgo() > reg) {
                throw new IllegalStateException(r.member() + " reserved before registering.");
            }
            String title = findTitle(r.book());
            if ("Pending".equals(r.status())) {
                if (!"Active".equals(memberStatus.get(r.member()))) {
                    throw new IllegalStateException(r.member() + " is inactive and cannot reserve.");
                }
                if (onShelf.get(title) > 0) {
                    throw new IllegalStateException(title + " is on the shelf, so it cannot be reserved.");
                }
                String pair = r.member() + "|" + title;
                if (pendingPairs.contains(pair)) {
                    throw new IllegalStateException(r.member() + " has two pending reservations for " + title);
                }
                pendingPairs.add(pair);
            }
        }
    }

    /** Finds the one book whose title starts with the given text. */
    private static String findTitle(String start) {
        String found = null;
        for (String[] b : BOOKS) {
            if (b[1].startsWith(start)) {
                if (found != null) {
                    throw new IllegalStateException("More than one book starts with: " + start);
                }
                found = b[1];
            }
        }
        if (found == null) {
            throw new IllegalStateException("No book starts with: " + start);
        }
        return found;
    }

    // ------------------------------------------------------------------
    // Database helpers
    // ------------------------------------------------------------------
    private static int addMember(Connection conn, String[] m) throws SQLException {
        String email = (m[0] + "." + m[1]).toLowerCase() + "@mail.com";
        int existing = findId(conn, "SELECT member_id FROM MEMBERS WHERE email = ?", email);
        if (existing > 0) {
            return existing;
        }
        String sql = "INSERT INTO MEMBERS (first_name, last_name, contact_no, email, address, "
                   + "date_registered, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m[0]);
            ps.setString(2, m[1]);
            ps.setString(3, m[2]);
            ps.setString(4, email);
            ps.setString(5, m[4]);
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(Integer.parseInt(m[5]))));
            ps.setString(7, m[3]);
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

    private static void borrow(Connection conn, int userId, int memberId, int bookId,
                               LoanPlan loan, List<FinePlan> fines) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate borrowDate = today.minusDays(loan.borrowedDaysAgo());
        LocalDate dueDate = borrowDate.plusDays(loan.loanDays());
        boolean returned = loan.returned();
        LocalDate returnDate = returned ? today.minusDays(loan.returnedDaysAgo()) : null;

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

        if (!returned) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE BOOKS SET available_copies = available_copies - 1 WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
        } else {
            long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
            if (daysLate > 0) {
                fines.add(new FinePlan(detailId, FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate)),
                    returnDate, loan.finePaid() ? "Paid" : "Unpaid"));
            }
        }
    }

    private static void addFine(Connection conn, FinePlan f) throws SQLException {
        String sql = "INSERT INTO FINES (detail_id, amount, fine_date, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, f.detailId());
            ps.setBigDecimal(2, f.amount());
            ps.setDate(3, Date.valueOf(f.fineDate()));
            ps.setString(4, f.status());
            ps.executeUpdate();
        }
    }

    private static void reserve(Connection conn, int memberId, int bookId,
                                ReservationPlan r) throws SQLException {
        String sql = "INSERT INTO RESERVATIONS (member_id, book_id, reservation_date, status) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            ps.setDate(3, Date.valueOf(LocalDate.now().minusDays(r.daysAgo())));
            ps.setString(4, r.status());
            ps.executeUpdate();
        }
    }

    /** A valid ISBN-13 (with the correct check digit) such as 978-1-00000-001-7. */
    static String isbn(int number) {
        String body = String.format("978100000%03d", number);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (body.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        int check = (10 - sum % 10) % 10;
        return String.format("978-1-00000-%03d-%d", number, check);
    }

    private static int generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getInt(1);
        }
    }

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

    private static int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
