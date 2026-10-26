package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.BookDAO;
import com.mycompany.librarymanagementsystemblp.dao.BorrowingDAO;
import com.mycompany.librarymanagementsystemblp.dao.MemberDAO;
import com.mycompany.librarymanagementsystemblp.model.Book;
import com.mycompany.librarymanagementsystemblp.model.BorrowRecord;
import com.mycompany.librarymanagementsystemblp.model.Member;
import com.mycompany.librarymanagementsystemblp.model.User;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class BorrowingFrame extends JFrame {

    private final BorrowingDAO borrowingDAO = new BorrowingDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final BookDAO bookDAO = new BookDAO();
    private final User currentUser;

    private List<BorrowRecord> currentRecords = new ArrayList<>();
    private BorrowRecord selectedRecord = null;

    private final JComboBox<Member> cmbMember = new JComboBox<>();
    private final JComboBox<Book> cmbBook = new JComboBox<>();
    private final JTextField txtDueDate = new JTextField(10);

    private final JTextField txtSearch = new JTextField(20);
    private final JCheckBox chkActiveOnly = new JCheckBox("Show only books still out");
    private final JLabel lblSelected = new JLabel("Select a row below to return a book.");

    private DefaultTableModel tableModel;
    private JTable table;

    public BorrowingFrame(User user) {
        this.currentUser = user;
        initComponents();
        loadMembersAndBooks();
        resetDueDate();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Borrowing and Returns");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        cmbMember.setPreferredSize(new Dimension(260, 26));
        cmbBook.setPreferredSize(new Dimension(320, 26));

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Member:", cmbMember);
        addField(form, 0, 1, "Book:", cmbBook);
        addField(form, 1, 0, "Due Date (yyyy-MM-dd):", txtDueDate);

        JButton btnBorrow = new JButton("BORROW BOOK");
        JButton btnClear = new JButton("CLEAR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnBorrow);
        buttons.add(btnClear);

        JLabel lblRules = new JLabel("Rules: up to " + BorrowingDAO.MAX_ACTIVE_LOANS
            + " books per member. Late fine: PHP " + BorrowingDAO.FINE_PER_DAY + " per day.");
        lblRules.setFont(lblRules.getFont().deriveFont(Font.ITALIC));
        lblRules.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.add(lblRules, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JButton btnSearch = new JButton("SEARCH");
        JButton btnShowAll = new JButton("SHOW ALL");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (member or book title):"));
        searchPanel.add(txtSearch);
        searchPanel.add(chkActiveOnly);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        tableModel = new DefaultTableModel(
            new String[]{"Record ID", "Borrow ID", "Member", "Book",
                         "Borrowed", "Due", "Returned", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(
                    t, value, isSelected, false, row, column);
                c.setHorizontalAlignment(SwingConstants.LEFT);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!isSelected) {
                    boolean late = row < currentRecords.size() && isOverdue(currentRecords.get(row));
                    c.setForeground(late ? Color.RED : t.getForeground());
                }
                return c;
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRow();
            }
        });

        JPanel center = new JPanel(new BorderLayout());
        center.add(searchPanel, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JButton btnReturn = new JButton("RETURN SELECTED BOOK");
        JPanel returnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        returnPanel.add(lblSelected);
        returnPanel.add(btnReturn);
        add(returnPanel, BorderLayout.SOUTH);

        btnBorrow.addActionListener(e -> borrowBook());
        btnClear.addActionListener(e -> clearForm());
        btnReturn.addActionListener(e -> returnBook());
        btnSearch.addActionListener(e -> refreshTable());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            chkActiveOnly.setSelected(false);
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

    private void loadMembersAndBooks() {
        try {
            cmbMember.removeAllItems();
            for (Member m : memberDAO.getAllMembers()) {
                if ("Active".equals(m.getStatus())) {
                    cmbMember.addItem(m);
                }
            }
            cmbBook.removeAllItems();
            for (Book b : bookDAO.getAllBooks()) {
                if ("Available".equals(b.getStatus()) && b.getAvailableCopies() > 0) {
                    cmbBook.addItem(b);
                }
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void refreshTable() {
        try {
            String keyword = txtSearch.getText().trim();
            currentRecords = borrowingDAO.getRecords(keyword, chkActiveOnly.isSelected());
            selectedRecord = null;
            lblSelected.setText("Select a row below to return a book.");
            tableModel.setRowCount(0);
            for (BorrowRecord r : currentRecords) {
                tableModel.addRow(new Object[]{
                    r.getDetailId(), r.getBorrowId(), r.getMemberName(), r.getBookTitle(),
                    r.getBorrowDate(), r.getDueDate(),
                    r.getReturnDate() == null ? "" : r.getReturnDate(),
                    isOverdue(r) ? "Borrowed (OVERDUE)" : r.getStatus()
                });
            }
            if (currentRecords.isEmpty() && !keyword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private boolean isOverdue(BorrowRecord r) {
        if (!"Borrowed".equals(r.getStatus())) {
            return false;
        }
        try {
            return LocalDate.parse(r.getDueDate()).isBefore(LocalDate.now());
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentRecords.size()) {
            return;
        }
        selectedRecord = currentRecords.get(row);
        lblSelected.setText("Selected: " + selectedRecord.getBookTitle()
            + " (" + selectedRecord.getMemberName() + ")");
    }

    private void borrowBook() {
        Member member = (Member) cmbMember.getSelectedItem();
        Book book = (Book) cmbBook.getSelectedItem();

        if (member == null) {
            showWarning("Please select a member. Only active members can borrow.");
            return;
        }
        if (book == null) {
            showWarning("Please select a book. Only available books can be borrowed.");
            return;
        }

        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(txtDueDate.getText().trim());
        } catch (DateTimeParseException ex) {
            showWarning("Due Date must use the format yyyy-MM-dd (example: "
                      + LocalDate.now().plusDays(BorrowingDAO.DEFAULT_LOAN_DAYS) + ").");
            return;
        }
        if (dueDate.isBefore(LocalDate.now())) {
            showWarning("Due Date cannot be in the past.");
            return;
        }

        try {
            borrowingDAO.borrowBook(member.getMemberId(), currentUser.getUserId(),
                                    book.getBookId(), dueDate);
            JOptionPane.showMessageDialog(this, "Book borrowed successfully.\nDue date: " + dueDate);
            clearForm();
            loadMembersAndBooks();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        } catch (IllegalStateException ex) {

            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Borrow", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void returnBook() {
        if (selectedRecord == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a borrowing record from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!"Borrowed".equals(selectedRecord.getStatus())) {
            JOptionPane.showMessageDialog(this,
                "This book has already been returned.",
                "Already Returned", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate due = null;
        try {
            due = LocalDate.parse(selectedRecord.getDueDate());
        } catch (DateTimeParseException ex) {
            // Due date could not be read; treat as not overdue.
        }
        long daysLate = due == null ? 0 : ChronoUnit.DAYS.between(due, today);
        boolean late = daysLate > 0;

        String status;
        if (late) {
            BigDecimal expectedFine = BorrowingDAO.FINE_PER_DAY.multiply(BigDecimal.valueOf(daysLate));
            status = "<font color='#B00020'><b>OVERDUE by " + daysLate + (daysLate == 1 ? " day" : " days")
                   + ".</b><br>A late fine of <b>PHP " + expectedFine.setScale(2).toPlainString()
                   + "</b> will be charged.</font>";
        } else if (due != null && due.equals(today)) {
            status = "<font color='#1B5E20'>Due today. No fine.</font>";
        } else {
            status = "<font color='#1B5E20'>Returned on time. No fine.</font>";
        }

        String message = "<html><body style='width: 300px'>"
            + "Return <b>\"" + selectedRecord.getBookTitle() + "\"</b><br>"
            + "from <b>" + selectedRecord.getMemberName() + "</b> today (" + today + ")?<br><br>"
            + "Borrowed: " + selectedRecord.getBorrowDate() + "<br>"
            + "Due: " + selectedRecord.getDueDate() + "<br><br>"
            + status
            + "</body></html>";
        int choice = JOptionPane.showConfirmDialog(this, message, "Confirm Return",
            JOptionPane.YES_NO_OPTION,
            late ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            BorrowingDAO.ReturnResult result =
                borrowingDAO.returnBook(selectedRecord.getDetailId(), currentUser.getUserId());
            BigDecimal fine = result.fine;
            StringBuilder autoText = new StringBuilder();
            if (!result.autoLoans.isEmpty()) {
                autoText.append("\n\nReservations fulfilled automatically:");
                for (BorrowingDAO.AutoLoan a : result.autoLoans) {
                    autoText.append("\n- ").append(a.memberName).append(" borrowed \"")
                            .append(a.bookTitle).append("\" (due ").append(a.dueDate).append(")");
                }
            }
            if (fine.compareTo(BigDecimal.ZERO) > 0) {
                JOptionPane.showMessageDialog(this,
                    "Book returned LATE.\nA fine of PHP " + fine.setScale(2).toPlainString()
                        + " was charged. You can collect it in the Fines screen."
                        + autoText,
                    "Late Return", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "\"" + selectedRecord.getBookTitle() + "\" was returned on time. No fine."
                        + autoText,
                    "Book Returned", JOptionPane.INFORMATION_MESSAGE);
            }
            loadMembersAndBooks();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Return", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearForm() {
        if (cmbMember.getItemCount() > 0) {
            cmbMember.setSelectedIndex(0);
        }
        if (cmbBook.getItemCount() > 0) {
            cmbBook.setSelectedIndex(0);
        }
        resetDueDate();
        table.clearSelection();
        selectedRecord = null;
        lblSelected.setText("Select a row below to return a book.");
    }

    private void resetDueDate() {
        txtDueDate.setText(LocalDate.now().plusDays(BorrowingDAO.DEFAULT_LOAN_DAYS).toString());
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message,
            "Missing or Invalid Information", JOptionPane.WARNING_MESSAGE);
    }

    private void handleSqlError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}
