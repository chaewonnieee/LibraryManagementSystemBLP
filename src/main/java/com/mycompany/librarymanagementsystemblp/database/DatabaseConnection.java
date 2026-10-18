package com.mycompany.librarymanagementsystemblp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {
        try {
            // Load Derby driver
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Connect to embedded Derby database
        return DriverManager.getConnection("jdbc:derby:LibraryDB;create=true");
    }

    // Shuts Derby down cleanly. Without this, Derby loses the ID numbers it had reserved
    // in advance, and the next ID jumps ahead (for example straight to 101).
    public static void shutdown() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            // Derby reports a successful full shutdown as an exception (state XJ015). That is normal.
        }
    }
}
