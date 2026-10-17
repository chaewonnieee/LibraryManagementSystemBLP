package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.BookDAO;
import com.mycompany.librarymanagementsystemblp.dao.MemberDAO;
import com.mycompany.librarymanagementsystemblp.dao.ReservationDAO;
import com.mycompany.librarymanagementsystemblp.model.Book;
import com.mycompany.librarymanagementsystemblp.model.Member;
import com.mycompany.librarymanagementsystemblp.model.Reservation;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ReservationFrame extends JFrame {

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final BookDAO bookDAO = new BookDAO();

    private List<Reservation> currentReservations = new ArrayList<>();
    private Reservation selectedReservation = null;

    private final JTextField txtReservationId = new JTextField(10);
    private final JComboBox<Member> cmbMember = new JComboBox<>();
    private final JComboBox<Book> cmbBook = new JComboBox<>();
    private final JTextField txtStatus = new JTextField(10);

    private final JTextField txtSearch = new JTextField(20);
    private final JCheckBox chkPendingOnly = new JCheckBox("Show only pending");

    private DefaultTableModel tableModel;
    private JTable table;

    public ReservationFrame() {
        initComponents();
        loadMembersAndBooks();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Reservations");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        txtReservationId.setEditable(false);
        txtStatus.setEditable(false);
        cmbMember.setPreferredSize(new Dimension(260, 26));
        cmbBook.setPreferredSize(new Dimension(320, 26));

        JPanel form = new JPanel(new GridBagLayout());
        addField(form, 0, 0, "Reservation ID:", txtReservationId);
        addField(form, 0, 1, "Status:", txtStatus);
        addField(form, 1, 0, "Member:", cmbMember);
        addField(form, 1, 1, "Book:", cmbBook);

        JButton btnReserve = new JButton("RESERVE");
        JButton btnFulfill = new JButton("MARK FULFILLED");
        JButton btnCancel = new JButton("CANCEL RESERVATION");
        JButton btnClear = new JButton("CLEAR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttons.add(btnReserve);
        buttons.add(btnFulfill);
        buttons.add(btnCancel);
        buttons.add(btnClear);

        JLabel note = new JLabel("Only books with no copies on the shelf can be reserved.");
        note.setFont(note.getFont().deriveFont(Font.ITALIC));
        note.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.add(note, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JButton btnSearch = new JButton("SEARCH");
        JButton btnShowAll = new JButton("SHOW ALL");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Search (member or book title):"));
        searchPanel.add(txtSearch);
        searchPanel.add(chkPendingOnly);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Member", "Book", "Date Reserved", "Status"}, 0) {
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

        btnReserve.addActionListener(e -> reserveBook());
        btnFulfill.addActionListener(e -> changeStatus("Fulfilled", "marked as fulfilled"));
        btnCancel.addActionListener(e -> changeStatus("Cancelled", "cancelled"));
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> refreshTable());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            chkPendingOnly.setSelected(false);
            refreshTable();
        });
        BackBar.install(this);   // Back button bar at the top of the window
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

    // ---------- Loading data ----------

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
                cmbBook.addItem(b);
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void refreshTable() {
        try {
            String keyword = txtSearch.getText().trim();
            currentReservations = reservationDAO.getReservations(keyword, chkPendingOnly.isSelected());
            selectedReservation = null;
            tableModel.setRowCount(0);
            for (Reservation r : currentReservations) {
                tableModel.addRow(new Object[]{
                    r.getReservationId(), r.getMemberName(), r.getBookTitle(),
                    r.getReservationDate(), r.getStatus()
                });
            }
            if (currentReservations.isEmpty() && !keyword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    // Fills the form when a table row is clicked
    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentReservations.size()) {
            return;
        }
        selectedReservation = currentReservations.get(row);
        txtReservationId.setText(String.valueOf(selectedReservation.getReservationId()));
        txtStatus.setText(selectedReservation.getStatus());

        for (int i = 0; i < cmbMember.getItemCount(); i++) {
            if (cmbMember.getItemAt(i).getMemberId() == selectedReservation.getMemberId()) {
                cmbMember.setSelectedIndex(i);
                break;
            }
        }
        for (int i = 0; i < cmbBook.getItemCount(); i++) {
            if (cmbBook.getItemAt(i).getBookId() == selectedReservation.getBookId()) {
                cmbBook.setSelectedIndex(i);
                break;
            }
        }
    }

    // ---------- Button actions ----------

    private void reserveBook() {
        Member member = (Member) cmbMember.getSelectedItem();
        Book book = (Book) cmbBook.getSelectedItem();
        if (member == null || book == null) {
            JOptionPane.showMessageDialog(this,
                "Please select both a member and a book.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            reservationDAO.addReservation(member.getMemberId(), book.getBookId());
            JOptionPane.showMessageDialog(this, "Reservation saved successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Reserve", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void changeStatus(String newStatus, String doneText) {
        if (selectedReservation == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a reservation from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure this reservation should be " + doneText + "?",
            "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            reservationDAO.changeStatus(selectedReservation.getReservationId(), newStatus);
            JOptionPane.showMessageDialog(this, "Reservation " + doneText + ".");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Update", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearForm() {
        txtReservationId.setText("");
        txtStatus.setText("");
        if (cmbMember.getItemCount() > 0) {
            cmbMember.setSelectedIndex(0);
        }
        if (cmbBook.getItemCount() > 0) {
            cmbBook.setSelectedIndex(0);
        }
        table.clearSelection();
        selectedReservation = null;
    }

    private void handleSqlError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}
