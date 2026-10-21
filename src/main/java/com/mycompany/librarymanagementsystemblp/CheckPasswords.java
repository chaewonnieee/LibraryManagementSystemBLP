package com.mycompany.librarymanagementsystemblp;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckPasswords {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT username, password FROM USERS")) {
            while (rs.next()) {
                System.out.println(rs.getString("username") + " -> " + rs.getString("password"));
            }
        }
    }
}
