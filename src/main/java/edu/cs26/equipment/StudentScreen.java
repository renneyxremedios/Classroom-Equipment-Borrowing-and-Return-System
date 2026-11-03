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

final class StudentScreen extends Screen {
    private final MainFrame main;
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Student ID","Full Name","Course","Section"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = Ui.table(model);
    private final JTextField search = Ui.field(21);
    private final List<Student> records = new ArrayList<>();

    StudentScreen(MainFrame main) {
        super("Student Records", "Manage borrower information and classroom sections");
        this.main = main;
        JPanel page = (JPanel) getComponent(0);
        JPanel center = Ui.panel(new BorderLayout(0, 12));
        JPanel toolbar = Ui.panel(new BorderLayout());
        JPanel searchBox = Ui.panel(new FlowLayout(FlowLayout.LEFT));
        searchBox.add(new JLabel("Search students:"));
        searchBox.add(search);
        toolbar.add(searchBox, BorderLayout.WEST);
        JButton add = Ui.button("+ Add Student");
        JButton edit = Ui.secondary("Edit");
        JButton delete = Ui.secondary("Delete");
        JButton refresh = Ui.secondary("Refresh");
        toolbar.add(Ui.actions(refresh, edit, delete, add), BorderLayout.EAST);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        add.addActionListener(e -> openDialog(null));
        edit.addActionListener(e -> { Student row = selected(); if (row != null) openDialog(row); });
        delete.addActionListener(e -> remove());
        refresh.addActionListener(e -> main.showScreen("Students"));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
    }

    @Override void reload() throws SQLException {
        records.clear();
        records.addAll(main.repo.students());
        model.setRowCount(0);
        for (Student s : records) model.addRow(new Object[]{s.id(), s.name(), s.course(), s.section()});
        filter();
    }

    private void filter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        String text = search.getText().trim();
        if (!text.isBlank()) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1, 2, 3));
    }

    private Student selected() {
        int visible = table.getSelectedRow();
        if (visible < 0) { JOptionPane.showMessageDialog(this, "Select a student first."); return null; }
        return records.get(table.convertRowIndexToModel(visible));
    }

    private void openDialog(Student existing) {
        JTextField id = Ui.field(18), name = Ui.field(18), course = Ui.field(18), section = Ui.field(18);
        if (existing != null) {
            id.setText(existing.id());
            id.setEditable(false);
            name.setText(existing.name());
            course.setText(existing.course());
            section.setText(existing.section());
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 12));
        form.add(new JLabel("Student ID:")); form.add(id);
        form.add(new JLabel("Full Name:")); form.add(name);
        form.add(new JLabel("Course:")); form.add(course);
        form.add(new JLabel("Section:")); form.add(section);
        if (JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add Student" : "Edit Student",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            main.repo.saveStudent(new Student(id.getText().trim(), name.getText().trim(),
                course.getText().trim(), section.getText().trim()), existing != null);
            main.refreshAll();
        } catch (SQLException | RuntimeException ex) { Ui.message(this, ex); }
    }

    private void remove() {
        Student row = selected();
        if (row == null) return;
        if (!Ui.confirm(this, "Delete " + row.name() + "?\n"
                + "Students with borrowing history cannot be deleted.")) return;
        try {
            main.repo.deleteStudent(row.id());
            main.refreshAll();
        } catch (SQLException ex) { Ui.message(this, ex); }
    }
}
