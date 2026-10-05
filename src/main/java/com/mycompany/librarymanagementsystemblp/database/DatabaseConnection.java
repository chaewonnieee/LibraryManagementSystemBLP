package com.mycompany.librarymanagementsystemblp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:derby:LibraryDB;create=true";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}