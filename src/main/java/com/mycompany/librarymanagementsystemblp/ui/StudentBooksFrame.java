package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.BookDAO;
import com.mycompany.librarymanagementsystemblp.dao.ReservationDAO;
import com.mycompany.librarymanagementsystemblp.model.Book;
import com.mycompany.librarymanagementsystemblp.model.Member;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Student side: search the catalog and reserve a book that has no copies on the shelf.
 * Borrowing itself is still done at the librarian's desk.
 */
public class StudentBooksFrame extends JFrame {

    private final BookDAO bookDAO = new BookDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final Member member;

    private List<Book> currentBooks = new ArrayList<>();

    private final JTextField txtSearch = new JTextField(16);
    private final JCheckBox chkAvailableOnly = new JCheckBox("On the shelf only");

    private DefaultTableModel tableModel;
    private JTable table;

    public StudentBooksFrame(Member member) {
        this.member = member;
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Browse Books");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel note = new JLabel("Search the library catalog. A book with no copies on the shelf "
                               + "can be reserved; to borrow a book, please see the librarian.");
        note.setFont(note.getFont().deriveFont(Font.ITALIC));
        note.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        JButton btnSearch = new JButton("SEARCH");
        JButton btnShowAll = new JButton("SHOW ALL");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (title, author, ISBN, category):"));
        searchPanel.add(txtSearch);
        searchPanel.add(chkAvailableOnly);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        JPanel top = new JPanel(new BorderLayout());
        top.add(note, BorderLayout.NORTH);
        top.add(searchPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "ISBN", "Title", "Author", "Category",
                         "On Shelf", "Availability"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {50, 140, 280, 160, 100, 110, 110};     // roomy Title column
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnReserve = new JButton("RESERVE SELECTED BOOK");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        bottom.add(btnReserve);
        add(bottom, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> refreshTable());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            chkAvailableOnly.setSelected(false);
            refreshTable();
        });
        btnReserve.addActionListener(e -> reserveSelected());
        txtSearch.addActionListener(e -> refreshTable());     // Enter key searches
        BackBar.install(this);   // Back button bar at the top of the window
    }

    // ---------- Loading data ----------

    private void refreshTable() {
        String keyword = txtSearch.getText().trim();
        try {
            List<Book> all = keyword.isEmpty() ? bookDAO.getAllBooks() : bookDAO.searchBooks(keyword);

            currentBooks = new ArrayList<>();
            tableModel.setRowCount(0);
            for (Book b : all) {
                if (chkAvailableOnly.isSelected() && b.getAvailableCopies() <= 0) {
                    continue;
                }
                currentBooks.add(b);
                tableModel.addRow(new Object[]{
                    b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getCategoryName(),
                    b.getAvailableCopies() + " of " + b.getTotalCopies(), availability(b)
                });
            }
            if (currentBooks.isEmpty() && !keyword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private String availability(Book b) {
        if ("Unavailable".equals(b.getStatus())) {
            return "Unavailable";
        }
        return b.getAvailableCopies() > 0 ? "On the shelf" : "All copies out";
    }

    // ---------- Reserving ----------

    private void reserveSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentBooks.size()) {
            JOptionPane.showMessageDialog(this,
                "Please select a book from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Book book = currentBooks.get(row);

        int choice = JOptionPane.showConfirmDialog(this,
            "Reserve \"" + book.getTitle() + "\"?",
            "Confirm Reservation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            reservationDAO.addReservation(member.getMemberId(), book.getBookId());
            JOptionPane.showMessageDialog(this,
                "Book reserved successfully. The librarian will hold it for you when it is returned.");
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Reserve", JOptionPane.WARNING_MESSAGE);      // a reservation rule was broken
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}
