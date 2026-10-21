package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.Fine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FineDAO {

    private static final String SELECT_FINES =
        "SELECT f.fine_id, f.detail_id, "
      + "m.last_name || ', ' || m.first_name AS member_name, "
      + "bk.title, f.amount, f.fine_date, f.status "
      + "FROM FINES f "
      + "INNER JOIN BORROWING_DETAILS d ON f.detail_id = d.detail_id "
      + "INNER JOIN BORROWINGS br ON d.borrow_id = br.borrow_id "
      + "INNER JOIN MEMBERS m ON br.member_id = m.member_id "
      + "INNER JOIN BOOKS bk ON d.book_id = bk.book_id ";

    private Fine mapRow(ResultSet rs) throws SQLException {
        return new Fine(
            rs.getInt("fine_id"),
            rs.getInt("detail_id"),
            rs.getString("member_name"),
            rs.getString("title"),
            rs.getBigDecimal("amount"),
            rs.getString("fine_date"),
            rs.getString("status")
        );
    }

    public List<Fine> getFines(String keyword, boolean unpaidOnly) throws SQLException {
        String sql = SELECT_FINES
                   + "WHERE (UPPER(m.first_name) LIKE ? OR UPPER(m.last_name) LIKE ? "
                   + "OR UPPER(bk.title) LIKE ?) "
                   + (unpaidOnly ? "AND f.status = 'Unpaid' " : "")
                   + "ORDER BY f.fine_id DESC";
        List<Fine> list = new ArrayList<>();
        String pattern = "%" + keyword.toUpperCase() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public void markAsPaid(int fineId) throws SQLException {
        String sql = "UPDATE FINES SET status = 'Paid' WHERE fine_id = ? AND status = 'Unpaid'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fineId);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("This fine was not found or is already paid.");
            }
        }
    }

    public BigDecimal getTotalUnpaid() throws SQLException {
        String sql = "SELECT SUM(amount) FROM FINES WHERE status = 'Unpaid'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            BigDecimal total = rs.getBigDecimal(1);
            return total == null ? BigDecimal.ZERO : total;
        }
    }
}
