package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.*;
import com.mycompany.librarymanagementsystemblp.model.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BookFrame extends JFrame {

    private final BookDAO bookDAO = new BookDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    private List<Book> currentBooks = new ArrayList<>();
    private Book selectedBook = null;

    private final JTextField txtBookId = new JTextField(10);
    private final JComboBox<Category> cmbCategory = new JComboBox<>();
    private final JTextField txtIsbn = new JTextField(15);
    private final JTextField txtTitle = new JTextField(20);
    private final JTextField txtAuthor = new JTextField(15);
    private final JTextField txtPublisher = new JTextField(20);
    private final JTextField txtTotal = new JTextField(10);
    private final JComboBox<String> cmbStatus =
        new JComboBox<>(new String[]{"Available", "Unavailable"});

    private final JTextField txtSearch = new JTextField(20);

    private DefaultTableModel tableModel;
    private JTable table;

    private final User currentUser;

    public BookFrame(User user) {
        this.currentUser = user;
        initComponents();
        loadCategories();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Book Management");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        txtBookId.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Book ID:", txtBookId);
        addField(form, 0, 1, "Category:", cmbCategory);
        addField(form, 1, 0, "ISBN:", txtIsbn);
        addField(form, 1, 1, "Title:", txtTitle);
        addField(form, 2, 0, "Author:", txtAuthor);
        addField(form, 2, 1, "Publisher:", txtPublisher);
        addField(form, 3, 0, "Total Copies:", txtTotal);
        addField(form, 3, 1, "Status:", cmbStatus);

        JButton btnSave = new JButton("SAVE");
        JButton btnUpdate = new JButton("UPDATE");
        JButton btnDelete = new JButton("DELETE");
        JButton btnClear = new JButton("CLEAR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnSave);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JButton btnSearch = new JButton("SEARCH");
        JButton btnShowAll = new JButton("SHOW ALL");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (title, author, ISBN, category):"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "ISBN", "Title", "Author", "Category",
                         "Publisher", "Total", "Available", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRow();
            }
        });

        JPanel center = new JPanel(new BorderLayout());
        center.add(searchPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        btnSave.addActionListener(e -> saveBook());
        btnUpdate.addActionListener(e -> updateBook());
        btnDelete.addActionListener(e -> deleteBook());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchBooks());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable();
        });
        BackBar.install(this);
    }

    private void addField(JPanel panel, int row, int col, String label, JComponent comp) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = row;

        gbc.gridx = col * 2;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = col * 2 + 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    private void loadCategories() {
        try {
            cmbCategory.removeAllItems();
            for (Category c : categoryDAO.getAllCategories()) {
                cmbCategory.addItem(c);
            }
            if (cmbCategory.getItemCount() == 0) {
                JOptionPane.showMessageDialog(this,
                    "There are no categories yet. Please add a category first.",
                    "No Categories", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void refreshTable() {
        try {
            fillTable(bookDAO.getAllBooks());
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void fillTable(List<Book> books) {
        currentBooks = books;
        selectedBook = null;
        tableModel.setRowCount(0);
        for (Book b : books) {
            tableModel.addRow(new Object[]{
                b.getBookId(), b.getIsbn(), b.getTitle(), b.getAuthor(),
                b.getCategoryName(), b.getPublisher(), b.getTotalCopies(),
                b.getAvailableCopies(), b.getStatus()
            });
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentBooks.size()) {
            return;
        }
        selectedBook = currentBooks.get(row);

        txtBookId.setText(String.valueOf(selectedBook.getBookId()));
        txtIsbn.setText(selectedBook.getIsbn());
        txtTitle.setText(selectedBook.getTitle());
        txtAuthor.setText(selectedBook.getAuthor());
        txtPublisher.setText(selectedBook.getPublisher() == null ? "" : selectedBook.getPublisher());
        txtTotal.setText(String.valueOf(selectedBook.getTotalCopies()));
        cmbStatus.setSelectedItem(selectedBook.getStatus());

        for (int i = 0; i < cmbCategory.getItemCount(); i++) {
            if (cmbCategory.getItemAt(i).getCategoryId() == selectedBook.getCategoryId()) {
                cmbCategory.setSelectedIndex(i);
                break;
            }
        }
    }

    private void saveBook() {
        Book b = buildBookFromForm();
        if (b == null) {
            return;
        }
        b.setAvailableCopies(b.getTotalCopies());

        try {
            bookDAO.addBook(b);
            JOptionPane.showMessageDialog(this, "Book saved successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void updateBook() {
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a book from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Book b = buildBookFromForm();
        if (b == null) {
            return;
        }

        int difference = b.getTotalCopies() - selectedBook.getTotalCopies();
        int newAvailable = selectedBook.getAvailableCopies() + difference;
        if (newAvailable < 0) {
            JOptionPane.showMessageDialog(this,
                "Total copies cannot be lower than the number of copies currently borrowed.",
                "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
            return;
        }
        b.setBookId(selectedBook.getBookId());
        b.setAvailableCopies(newAvailable);

        try {
            List<BorrowingDAO.AutoLoan> autoLoans = bookDAO.updateBook(b, currentUser.getUserId());
            StringBuilder message = new StringBuilder("Book updated successfully.");
            if (!autoLoans.isEmpty()) {
                message.append("\n\nReservations fulfilled automatically:");
                for (BorrowingDAO.AutoLoan a : autoLoans) {
                    message.append("\n- ").append(a.memberName).append(" borrowed \"")
                           .append(a.bookTitle).append("\" (due ").append(a.dueDate).append(")");
                }
            }
            JOptionPane.showMessageDialog(this, message.toString());
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void deleteBook() {
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a book from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete this record?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            bookDAO.deleteBook(selectedBook.getBookId());
            JOptionPane.showMessageDialog(this, "Book deleted successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void searchBooks() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            List<Book> results = bookDAO.searchBooks(keyword);
            fillTable(results);
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void clearForm() {
        txtBookId.setText("");
        txtIsbn.setText("");
        txtTitle.setText("");
        txtAuthor.setText("");
        txtPublisher.setText("");
        txtTotal.setText("");
        cmbStatus.setSelectedIndex(0);
        if (cmbCategory.getItemCount() > 0) {
            cmbCategory.setSelectedIndex(0);
        }
        table.clearSelection();
        selectedBook = null;
    }

    private Book buildBookFromForm() {
        Category category = (Category) cmbCategory.getSelectedItem();
        String isbn = txtIsbn.getText().trim();
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String publisher = txtPublisher.getText().trim();
        String totalText = txtTotal.getText().trim();

        if (category == null) {
            showWarning("Please select a category. Add one first if the list is empty.");
            return null;
        }
        if (isbn.isEmpty() || title.isEmpty() || author.isEmpty() || totalText.isEmpty()) {
            showWarning("ISBN, Title, Author, and Total Copies are required.");
            return null;
        }

        int total;
        try {
            total = Integer.parseInt(totalText);
        } catch (NumberFormatException ex) {
            showWarning("Total Copies must be a whole number.");
            return null;
        }
        if (total < 1) {
            showWarning("Total Copies must be at least 1.");
            return null;
        }

        Book b = new Book();
        b.setCategoryId(category.getCategoryId());
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthor(author);
        b.setPublisher(publisher.isEmpty() ? null : publisher);
        b.setTotalCopies(total);
        b.setStatus((String) cmbStatus.getSelectedItem());
        return b;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message,
            "Missing or Invalid Information", JOptionPane.WARNING_MESSAGE);
    }

    private void handleSqlError(SQLException ex) {
        String message;
        if ("23505".equals(ex.getSQLState())) {
            message = "A book with this ISBN already exists.";
        } else if ("23503".equals(ex.getSQLState())) {
            message = "This book cannot be deleted because it has borrowing "
                    + "or reservation records.";
        } else {
            message = "Database error: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
