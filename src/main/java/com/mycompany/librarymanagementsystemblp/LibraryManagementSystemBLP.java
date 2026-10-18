package com.mycompany.librarymanagementsystemblp;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.database.DatabaseSetup;
import com.mycompany.librarymanagementsystemblp.ui.LoginFrame;
import javax.swing.SwingUtilities;

public class LibraryManagementSystemBLP {

    public static void main(String[] args) {
        DatabaseSetup.initialize();

        // When the program closes (X, Exit, or Logout then close), shut Derby down cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown));

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}