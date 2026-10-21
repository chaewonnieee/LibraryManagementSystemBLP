package com.mycompany.librarymanagementsystemblp.ui;

import com.mycompany.librarymanagementsystemblp.dao.CategoryDAO;
import com.mycompany.librarymanagementsystemblp.model.Category;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CategoryFrame extends JFrame {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    private List<Category> currentCategories = new ArrayList<>();
    private Category selectedCategory = null;

    private final JTextField txtId = new JTextField(10);
    private final JTextField txtName = new JTextField(20);
    private final JTextField txtDescription = new JTextField(25);
    private final JTextField txtSearch = new JTextField(20);

    private DefaultTableModel tableModel;
    private JTable table;

    public CategoryFrame() {
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Category Management");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        txtId.setEditable(false);

        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "Category ID:", txtId);
        addRow(form, 1, "Category Name:", txtName);
        addRow(form, 2, "Description:", txtDescription);

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
        searchPanel.add(new JLabel("Search (name or description):"));
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnShowAll);

        tableModel = new DefaultTableModel(
            new String[]{"ID", "Category Name", "Description"}, 0) {
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

        btnSave.addActionListener(e -> saveCategory());
        btnUpdate.addActionListener(e -> updateCategory());
        btnDelete.addActionListener(e -> deleteCategory());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchCategories());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            refreshTable();
        });
        BackBar.install(this);
    }

    private void addRow(JPanel panel, int row, String label, JComponent comp) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = row;

        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    private void refreshTable() {
        try {
            fillTable(categoryDAO.getAllCategories());
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void fillTable(List<Category> categories) {
        currentCategories = categories;
        selectedCategory = null;
        tableModel.setRowCount(0);
        for (Category c : categories) {
            tableModel.addRow(new Object[]{
                c.getCategoryId(), c.getCategoryName(), c.getDescription()
            });
        }
    }

    private void loadSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentCategories.size()) {
            return;
        }
        selectedCategory = currentCategories.get(row);
        txtId.setText(String.valueOf(selectedCategory.getCategoryId()));
        txtName.setText(selectedCategory.getCategoryName());
        txtDescription.setText(selectedCategory.getDescription() == null
            ? "" : selectedCategory.getDescription());
    }

    private void saveCategory() {
        Category c = buildCategoryFromForm();
        if (c == null) {
            return;
        }
        try {
            categoryDAO.addCategory(c);
            JOptionPane.showMessageDialog(this, "Category saved successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void updateCategory() {
        if (selectedCategory == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a category from the table first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Category c = buildCategoryFromForm();
        if (c == null) {
            return;
        }
        c.setCategoryId(selectedCategory.getCategoryId());
        try {
            categoryDAO.updateCategory(c);
            JOptionPane.showMessageDialog(this, "Category updated successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void deleteCategory() {
        if (selectedCategory == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a category from the table first.",
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
            categoryDAO.deleteCategory(selectedCategory.getCategoryId());
            JOptionPane.showMessageDialog(this, "Category deleted successfully.");
            clearForm();
            refreshTable();
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void searchCategories() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            refreshTable();
            return;
        }
        try {
            List<Category> results = categoryDAO.searchCategories(keyword);
            fillTable(results);
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching records found.");
            }
        } catch (SQLException ex) {
            handleSqlError(ex);
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtDescription.setText("");
        table.clearSelection();
        selectedCategory = null;
    }

    private Category buildCategoryFromForm() {
        String name = txtName.getText().trim();
        String description = txtDescription.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Category Name is required.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Category c = new Category();
        c.setCategoryName(name);
        c.setDescription(description.isEmpty() ? null : description);
        return c;
    }

    private void handleSqlError(SQLException ex) {
        String message;
        if ("23505".equals(ex.getSQLState())) {
            message = "A category with this name already exists.";
        } else if ("23503".equals(ex.getSQLState())) {
            message = "This category cannot be deleted because books are using it.";
        } else {
            message = "Database error: " + ex.getMessage();
        }
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
