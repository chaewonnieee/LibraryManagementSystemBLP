package com.mycompany.librarymanagementsystemblp.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    public static void initialize() {
        String[] tables = {
            "CREATE TABLE USERS ("
            + "user_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "username VARCHAR(50) NOT NULL UNIQUE, "
            + "password VARCHAR(255) NOT NULL, "
            + "full_name VARCHAR(100) NOT NULL, "
            + "user_role VARCHAR(30) NOT NULL, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Active')",
            "CREATE TABLE MEMBERS ("
            + "member_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "first_name VARCHAR(50) NOT NULL, "
            + "last_name VARCHAR(50) NOT NULL, "
            + "contact_no VARCHAR(30), "
            + "email VARCHAR(100) UNIQUE, "
            + "address VARCHAR(255), "
            + "date_registered DATE NOT NULL DEFAULT CURRENT_DATE, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Active')",
            "CREATE TABLE CATEGORIES ("
            + "category_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "category_name VARCHAR(100) NOT NULL UNIQUE, "
            + "description VARCHAR(255))",
            "CREATE TABLE BOOKS ("
            + "book_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "category_id INTEGER NOT NULL, "
            + "isbn VARCHAR(20) NOT NULL UNIQUE, "
            + "title VARCHAR(200) NOT NULL, "
            + "author VARCHAR(100) NOT NULL, "
            + "publisher VARCHAR(100), "
            + "total_copies INTEGER NOT NULL DEFAULT 1, "
            + "available_copies INTEGER NOT NULL DEFAULT 1, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Available', "
            + "FOREIGN KEY (category_id) REFERENCES CATEGORIES(category_id))",
            "CREATE TABLE BORROWINGS ("
            + "borrow_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "member_id INTEGER NOT NULL, "
            + "user_id INTEGER NOT NULL, "
            + "borrow_date DATE NOT NULL DEFAULT CURRENT_DATE, "
            + "due_date DATE NOT NULL, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Borrowed', "
            + "FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id), "
            + "FOREIGN KEY (user_id) REFERENCES USERS(user_id))",
            "CREATE TABLE BORROWING_DETAILS ("
            + "detail_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "borrow_id INTEGER NOT NULL, "
            + "book_id INTEGER NOT NULL, "
            + "return_date DATE, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Borrowed', "
            + "FOREIGN KEY (borrow_id) REFERENCES BORROWINGS(borrow_id), "
            + "FOREIGN KEY (book_id) REFERENCES BOOKS(book_id))",
            "CREATE TABLE FINES ("
            + "fine_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "detail_id INTEGER NOT NULL, "
            + "amount DECIMAL(10,2) NOT NULL, "
            + "fine_date DATE NOT NULL DEFAULT CURRENT_DATE, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Unpaid', "
            + "FOREIGN KEY (detail_id) REFERENCES BORROWING_DETAILS(detail_id))",
            "CREATE TABLE RESERVATIONS ("
            + "reservation_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "member_id INTEGER NOT NULL, "
            + "book_id INTEGER NOT NULL, "
            + "reservation_date DATE NOT NULL DEFAULT CURRENT_DATE, "
            + "status VARCHAR(20) NOT NULL DEFAULT 'Pending', "
            + "FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id), "
            + "FOREIGN KEY (book_id) REFERENCES BOOKS(book_id))"
        };

        try (Connection conn = DatabaseConnection.getConnection(); Statement st = conn.createStatement()) {

            for (String sql : tables) {
                try {
                    st.executeUpdate(sql);
                } catch (SQLException e) {

                    if (!"X0Y32".equals(e.getSQLState())) {
                        throw e;
                    }
                }
            }

            insertDefaultAdmin(conn);
            insertDefaultCategories(conn);
            resyncIdentities(conn);
            System.out.println("Database ready!");

        } catch (SQLException e) {
            System.out.println("Database setup failed!");
            e.printStackTrace();
        }
    }

    private static void insertDefaultAdmin(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM USERS")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String sql = "INSERT INTO USERS (username, password, full_name, user_role) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "admin");
                    ps.setString(2, PasswordUtil.hash("admin123"));
                    ps.setString(3, "System Administrator");
                    ps.setString(4, "Administrator");
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void insertDefaultCategories(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM CATEGORIES")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String[][] categories = {
                    {"Fiction", "Novels and stories"},
                    {"Non-Fiction", "Factual and informational books"},
                    {"Science", "Science and technology books"},
                    {"Reference", "Dictionaries, encyclopedias, and manuals"},
                    {"Textbook", "School and course books"}
                };
                String sql = "INSERT INTO CATEGORIES (category_name, description) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (String[] c : categories) {
                        ps.setString(1, c[0]);
                        ps.setString(2, c[1]);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private static void resyncIdentities(Connection conn) throws SQLException {
        String[][] idColumns = {
            {"USERS", "user_id"},
            {"MEMBERS", "member_id"},
            {"CATEGORIES", "category_id"},
            {"BOOKS", "book_id"},
            {"BORROWINGS", "borrow_id"},
            {"BORROWING_DETAILS", "detail_id"},
            {"FINES", "fine_id"},
            {"RESERVATIONS", "reservation_id"}
        };

        for (String[] tc : idColumns) {
            int next;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(" + tc[1] + ") FROM " + tc[0])) {
                rs.next();
                next = rs.getInt(1) + 1;
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE " + tc[0] + " ALTER COLUMN " + tc[1]
                               + " RESTART WITH " + next);
            }
        }
    }
}
