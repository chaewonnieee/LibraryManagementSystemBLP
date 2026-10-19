package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.BorrowingDAO;
import com.mycompany.librarymanagementsystemblp.dao.StudentPortalDAO;
import com.mycompany.librarymanagementsystemblp.model.BorrowRecord;
import com.mycompany.librarymanagementsystemblp.model.Fine;
import com.mycompany.librarymanagementsystemblp.model.Member;
import com.mycompany.librarymanagementsystemblp.model.Reservation;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Student side: shows ONE kind of record that belongs to the logged-in student.
 * The same window is used for borrowed books, reservations and fines.
 */
public class StudentRecordsFrame extends JFrame {

    public enum Kind { BORROWED, RESERVATIONS, FINES }

    private static final Color LATE_RED = new Color(176, 38, 38);

    private final StudentPortalDAO portalDAO = new StudentPortalDAO();
    private final Member member;
    private final Kind kind;

    private List<BorrowRecord> currentBorrowings = new ArrayList<>();
    private List<Reservation> currentReservations = new ArrayList<>();

    private final JCheckBox chkFilter = new JCheckBox();
    private final JLabel lblSummary = new JLabel(" ");

    private DefaultTableModel tableModel;
    private JTable table;

    public StudentRecordsFrame(Member member, Kind kind) {
        this.member = member;
        this.kind = kind;
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        String[] columns;
        String noteText;
        switch (kind) {
            case BORROWED:
                setTitle("My Borrowed Books");
                chkFilter.setText("Show only books I still have");
                columns = new String[]{"Record ID", "Book", "Borrowed", "Due", "Returned", "Status"};
                noteText = "Books past their due date are shown in red. Late fine: PHP "
                         + BorrowingDAO.FINE_PER_DAY.toPlainString() + " per day.";
                break;
            case RESERVATIONS:
                setTitle("My Reservations");
                chkFilter.setText("Show only pending");
                columns = new String[]{"ID", "Book", "Date Reserved", "Status"};
                noteText = "Select a pending reservation and click Cancel if you no longer need the book.";
                break;
            default:
                setTitle("My Fines");
                chkFilter.setText("Show only unpaid");
                columns = new String[]{"Fine ID", "Book", "Amount (PHP)", "Date", "Status"};
                noteText = "Fines are paid at the librarian's desk.";
                break;
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel note = new JLabel(noteText);
        note.setFont(note.getFont().deriveFont(Font.ITALIC));
        note.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        JButton btnRefresh = new JButton("REFRESH");
        JButton btnShowAll = new JButton("SHOW ALL");
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(chkFilter);
        filterPanel.add(btnRefresh);
        filterPanel.add(btnShowAll);

        JPanel top = new JPanel(new BorderLayout());
        top.add(note, BorderLayout.NORTH);
        top.add(filterPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (kind == Kind.BORROWED) {
            DefaultTableCellRenderer lateRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        boolean late = row < currentBorrowings.size()
                                       && isOverdue(currentBorrowings.get(row));
                        c.setForeground(late ? LATE_RED : t.getForeground());
                    }
                    return c;
                }
            };
            table.setDefaultRenderer(Object.class, lateRenderer);
            table.setDefaultRenderer(Integer.class, lateRenderer);   // so the ID column turns red too
        }
        int[] widths;                                  // roomy Book column
        if (kind == Kind.BORROWED) {
            widths = new int[]{70, 300, 100, 100, 100, 150};
        } else if (kind == Kind.RESERVATIONS) {
            widths = new int[]{60, 400, 130, 110};
        } else {
            widths = new int[]{70, 360, 110, 110, 100};
        }
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        add(new JScrollPane(table), BorderLayout.CENTER);

        // bottom: summary text, plus a Cancel button for reservations
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        bottom.add(lblSummary);
        if (kind == Kind.RESERVATIONS) {
            JButton btnCancel = new JButton("CANCEL RESERVATION");
            btnCancel.addActionListener(e -> cancelSelected());
            bottom.add(btnCancel);
        }
        add(bottom, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshTable());
        btnShowAll.addActionListener(e -> {
            chkFilter.setSelected(false);
            refreshTable();
        });
        chkFilter.addActionListener(e -> refreshTable());
        BackBar.install(this);   // Back button bar at the top of the window
    }

    // ---------- Loading data ----------

    private void refreshTable() {
        try {
            tableModel.setRowCount(0);
            boolean filter = chkFilter.isSelected();

            if (kind == Kind.BORROWED) {
                currentBorrowings = portalDAO.getBorrowings(member.getMemberId(), filter);
                for (BorrowRecord r : currentBorrowings) {
                    tableModel.addRow(new Object[]{
                        r.getDetailId(), r.getBookTitle(), r.getBorrowDate(), r.getDueDate(),
                        r.getReturnDate() == null ? "" : r.getReturnDate(),
                        isOverdue(r) ? "Borrowed (OVERDUE)" : r.getStatus()
                    });
                }
                lblSummary.setText(currentBorrowings.size() + " record(s)");

            } else if (kind == Kind.RESERVATIONS) {
                currentReservations = portalDAO.getReservations(member.getMemberId(), filter);
                for (Reservation r : currentReservations) {
                    tableModel.addRow(new Object[]{
                        r.getReservationId(), r.getBookTitle(), r.getReservationDate(), r.getStatus()
                    });
                }
                lblSummary.setText(currentReservations.size() + " record(s)");

            } else {
                List<Fine> fines = portalDAO.getFines(member.getMemberId(), filter);
                BigDecimal unpaid = BigDecimal.ZERO;
                for (Fine f : fines) {
                    tableModel.addRow(new Object[]{
                        f.getFineId(), f.getBookTitle(), f.getAmount().toPlainString(),
                        f.getFineDate(), f.getStatus()
                    });
                    if ("Unpaid".equals(f.getStatus())) {
                        unpaid = unpaid.add(f.getAmount());
                    }
                }
                lblSummary.setText("Unpaid fines shown: PHP " + unpaid.setScale(2).toPlainString());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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

    // ---------- Cancelling a reservation ----------

    private void cancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentReservations.size()) {
            JOptionPane.showMessageDialog(this,
                "Please select a reservation from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Reservation r = currentReservations.get(row);

        int choice = JOptionPane.showConfirmDialog(this,
            "Cancel your reservation for \"" + r.getBookTitle() + "\"?",
            "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            portalDAO.cancelReservation(r.getReservationId(), member.getMemberId());
            JOptionPane.showMessageDialog(this, "Reservation cancelled successfully.");
            refreshTable();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
