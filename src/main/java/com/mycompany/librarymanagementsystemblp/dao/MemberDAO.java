package com.mycompany.librarymanagementsystemblp.dao;

import com.mycompany.librarymanagementsystemblp.database.DatabaseConnection;
import com.mycompany.librarymanagementsystemblp.model.Member;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
            rs.getInt("member_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("contact_no"),
            rs.getString("email"),
            rs.getString("address"),
            rs.getString("date_registered"),
            rs.getString("status")
        );
    }

    // CREATE
    public void addMember(Member m) throws SQLException {
        String sql = "INSERT INTO MEMBERS (first_name, last_name, contact_no, email, address, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getFirstName());
            ps.setString(2, m.getLastName());
            ps.setString(3, m.getContactNo());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getAddress());
            ps.setString(6, m.getStatus());
            ps.executeUpdate();
        }
    }

    // READ (all)
    public List<Member> getAllMembers() throws SQLException {
        String sql = "SELECT * FROM MEMBERS ORDER BY member_id";
        List<Member> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    // READ (search by name, email, or contact number)
    public List<Member> searchMembers(String keyword) throws SQLException {
        String sql = "SELECT * FROM MEMBERS "
                   + "WHERE UPPER(first_name) LIKE ? OR UPPER(last_name) LIKE ? "
                   + "OR UPPER(email) LIKE ? OR UPPER(contact_no) LIKE ? "
                   + "ORDER BY member_id";
        List<Member> list = new ArrayList<>();
        String pattern = "%" + keyword.toUpperCase() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // UPDATE
    public void updateMember(Member m) throws SQLException {
        String sql = "UPDATE MEMBERS SET first_name = ?, last_name = ?, contact_no = ?, "
                   + "email = ?, address = ?, status = ? WHERE member_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getFirstName());
            ps.setString(2, m.getLastName());
            ps.setString(3, m.getContactNo());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getAddress());
            ps.setString(6, m.getStatus());
            ps.setInt(7, m.getMemberId());
            ps.executeUpdate();
        }
    }

    // DELETE
    public void deleteMember(int memberId) throws SQLException {
        String sql = "DELETE FROM MEMBERS WHERE member_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }
}
