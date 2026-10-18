package com.mycompany.librarymanagementsystemblp;                              // must match the package you created it in

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;   // reuse your own connection class
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckPasswords {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();             // open the LibraryDB database
             Statement st = conn.createStatement();                            // create a statement to run SQL
             ResultSet rs = st.executeQuery("SELECT username, password FROM USERS")) {   // get all usernames and passwords
            while (rs.next()) {                                                // go through each user row
                System.out.println(rs.getString("username") + " -> " + rs.getString("password"));   // print username and stored password
            }
        }
    }
}