package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

final class EquipmentScreen extends Screen {
    private final MainFrame main;
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Equipment ID","Item Name","Category","Availability","Notes"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = Ui.table(model);
    private final JTextField search = Ui.field(22);
    private final List<EquipmentRow> records = new ArrayList<>();

    EquipmentScreen(MainFrame main) {
        super("Equipment Management", "Register, edit, and track classroom equipment");
        this.main = main;
        JPanel page = (JPanel) getComponent(0);
        JPanel center = Ui.panel(new BorderLayout(0, 12));
        JPanel toolbar = Ui.panel(new BorderLayout());
        JPanel searchBox = Ui.panel(new FlowLayout(FlowLayout.LEFT));
        searchBox.add(new JLabel("Search by ID or name:"));
        searchBox.add(search);
        toolbar.add(searchBox, BorderLayout.WEST);
        JButton add = Ui.button("+ Add Equipment");
        JButton edit = Ui.secondary("Edit");
        JButton details = Ui.secondary("Handling Notes");
        JButton delete = Ui.secondary("Delete");
        JButton refresh = Ui.secondary("Refresh");
        toolbar.add(Ui.actions(refresh, details, edit, delete, add), BorderLayout.EAST);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);

        add.addActionListener(e -> openDialog(null));
        edit.addActionListener(e -> { EquipmentRow row = selected(); if (row != null) openDialog(row); });
        delete.addActionListener(e -> remove());
        details.addActionListener(e -> {
            EquipmentRow row = selected();
            if (row != null) JOptionPane.showMessageDialog(this,
                row.equipment().getName() + "\n" + row.equipment().handlingInstructions(),
                "Equipment handling", JOptionPane.INFORMATION_MESSAGE);
        });
        refresh.addActionListener(e -> main.showScreen("Equipment"));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
    }

    @Override void reload() throws SQLException {
        records.clear();
        records.addAll(main.repo.equipment(false));
        model.setRowCount(0);
        for (EquipmentRow row : records) {
            Equipment e = row.equipment();
            model.addRow(new Object[]{e.getId(), e.getName(), e.getCategory(), row.status(), e.getNotes()});
        }
        filter();
    }

    private void filter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        String text = search.getText().trim();
        if (!text.isBlank()) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1));
    }

    private EquipmentRow selected() {
        int visible = table.getSelectedRow();
        if (visible < 0) { JOptionPane.showMessageDialog(this, "Select an equipment record first."); return null; }
        return records.get(table.convertRowIndexToModel(visible));
    }

    private void openDialog(EquipmentRow existing) {
        JTextField id = Ui.field(20);
        JTextField name = Ui.field(20);
        JTextField notes = Ui.field(20);
        JComboBox<String> category = new JComboBox<>(new String[]{"Electronic", "Non-Electronic"});
        if (existing != null) {
            Equipment e = existing.equipment();
            id.setText(e.getId());
            id.setEditable(false);
            name.setText(e.getName());
            category.setSelectedItem(e.getCategory());
            notes.setText(e.getNotes());
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 12));
        form.add(new JLabel("Equipment ID:")); form.add(id);
        form.add(new JLabel("Item name:")); form.add(name);
        form.add(new JLabel("Category:")); form.add(category);
        form.add(new JLabel("Notes:")); form.add(notes);
        if (JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add Equipment" : "Edit Equipment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            Equipment e = Equipment.of(id.getText(), name.getText(),
                (String) category.getSelectedItem(), notes.getText());
            main.repo.saveEquipment(e, existing != null);
            main.refreshAll();
        } catch (SQLException | RuntimeException ex) { Ui.message(this, ex); }
    }

    private void remove() {
        EquipmentRow row = selected();
        if (row == null) return;
        if (!Ui.confirm(this, "Delete equipment " + row.equipment().getId() + "?\n"
                + "Items with borrowing history cannot be deleted.")) return;
        try {
            main.repo.deleteEquipment(row.equipment().getId());
            main.refreshAll();
        } catch (SQLException ex) { Ui.message(this, ex); }
    }
}
