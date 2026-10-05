package com.mycompany.librarymanagementsystemblp;

import com.mycompany.librarymanagementsystemblp.database.DatabaseSetup;
import com.mycompany.librarymanagementsystemblp.ui.LoginFrame;
import javax.swing.SwingUtilities;

public class LibraryManagementSystemBLP {

    public static void main(String[] args) {
        DatabaseSetup.initialize();

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}