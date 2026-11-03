package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

final class BorrowScreen extends Screen {
    private final MainFrame main;
    private final JTextField requisition = Ui.field(16);
    private final JComboBox<Student> student = new JComboBox<>();
    private final JTextField course = Ui.field(10);
    private final JTextField section = Ui.field(10);
    private final JTextField borrowerType = Ui.field(10);
    private final JSpinner from = spinner(0);
    private final JSpinner due = spinner(2);
    private final JComboBox<EquipmentRow> available = new JComboBox<>();
    private final List<EquipmentRow> selected = new ArrayList<>();
    private final DefaultTableModel items = new DefaultTableModel(
        new String[]{"Date Borrow", "Time From", "Time To", "Item Name", "Quantity"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = Ui.table(items);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");

    BorrowScreen(MainFrame main) {
        super("Borrow Item", "Record equipment requisitions and schedule their return");
        this.main = main;
        JPanel page = (JPanel) getComponent(0);
        JPanel work = Ui.panel(new BorderLayout(0, 10));

        JPanel form = Ui.panel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(227, 233, 239)),
            new EmptyBorder(15, 18, 15, 18)));

        course.setEditable(false);
        section.setEditable(false);
        borrowerType.setText("Student");
        borrowerType.setEditable(false);

        addField(form, 0, 0, "Requisition No:", requisition);
        addField(form, 0, 2, "Course:", course);
        addField(form, 1, 0, "Name:", student);
        addField(form, 1, 2, "Section:", section);
        addField(form, 2, 0, "Borrower Type:", borrowerType);
        addField(form, 2, 2, "Date & Time From:", from);
        addField(form, 3, 0, "Return Deadline:", due);

        student.addActionListener(e -> {
            Student s = (Student) student.getSelectedItem();
            course.setText(s == null ? "" : s.course());
            section.setText(s == null ? "" : s.section());
        });
        from.addChangeListener(e -> redraw());
        due.addChangeListener(e -> redraw());

        JPanel choose = Ui.panel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        choose.add(new JLabel("Item Name:"));
        available.setPreferredSize(new Dimension(310, 33));
        available.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EquipmentRow row) setText(row.equipment().toString());
                return this;
            }
        });
        choose.add(available);
        JButton add = Ui.button("Add Item");
        JButton remove = Ui.secondary("Remove Selected");
        choose.add(add);
        choose.add(remove);

        JPanel records = Ui.panel(new BorderLayout(0, 10));
        records.add(choose, BorderLayout.NORTH);
        records.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton save = Ui.button("Save Borrowing");
        JButton clear = Ui.secondary("Clear");
        JButton reload = Ui.secondary("Refresh Available Items");
        JPanel bottom = Ui.actions(reload, clear, save);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
        records.add(bottom, BorderLayout.SOUTH);

        work.add(form, BorderLayout.NORTH);
        work.add(records, BorderLayout.CENTER);
        page.add(work, BorderLayout.CENTER);

        add.addActionListener(e -> addItem());
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                selected.remove(table.convertRowIndexToModel(row));
                redraw();
            }
        });
        clear.addActionListener(e -> clearForm());
        save.addActionListener(e -> saveBorrow());
        reload.addActionListener(e -> main.showScreen("Borrow Item"));
        clearForm();
    }

    private static JSpinner spinner(int hoursFromNow) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, hoursFromNow);
        JSpinner spinner = new JSpinner(new SpinnerDateModel(calendar.getTime(), null, null,
            Calendar.MINUTE));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd HH:mm"));
        return spinner;
    }

    private static void addField(JPanel panel, int row, int col, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = col;
        c.insets = new Insets(6, 6, 6, 8);
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(label), c);
        c.gridx++;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.4;
        field.setPreferredSize(new Dimension(220, 31));
        panel.add(field, c);
    }

    @Override void reload() throws SQLException {
        String oldId = student.getSelectedItem() instanceof Student s ? s.id() : null;
        student.removeAllItems();
        for (Student s : main.repo.students()) {
            student.addItem(s);
            if (s.id().equals(oldId)) student.setSelectedItem(s);
        }
        available.removeAllItems();
        for (EquipmentRow row : main.repo.equipment(true)) {
            if (selected.stream().noneMatch(item -> item.equipment().getId().equals(row.equipment().getId())))
                available.addItem(row);
        }
        redraw();
    }

    private void addItem() {
        EquipmentRow row = (EquipmentRow) available.getSelectedItem();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "No available equipment found. Add equipment first.");
            return;
        }
        if (selected.stream().anyMatch(item -> item.equipment().getId().equals(row.equipment().getId()))) {
            JOptionPane.showMessageDialog(this, "This item has already been added.");
            return;
        }
        selected.add(row);
        available.removeItem(row);
        redraw();
    }

    private void redraw() {
        items.setRowCount(0);
        LocalDateTime start = getTime(from), end = getTime(due);
        for (EquipmentRow row : selected)
            items.addRow(new Object[]{DATE.format(start), TIME.format(start), TIME.format(end),
                row.equipment().toString(), 1});
    }

    private static LocalDateTime getTime(JSpinner spinner) {
        Date value = (Date) spinner.getValue();
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()).withSecond(0).withNano(0);
    }

    private void saveBorrow() {
        try {
            Student s = (Student) student.getSelectedItem();
            if (s == null) throw new IllegalArgumentException("Add a student before borrowing equipment.");
            List<String> ids = selected.stream().map(e -> e.equipment().getId()).toList();
            BorrowRequest request = new BorrowRequest(requisition.getText().trim(), s.id(),
                getTime(from), getTime(due), ids);
            main.repo.borrow(request, main.account.id());
            JOptionPane.showMessageDialog(this, "Borrowing saved for " + ids.size() + " item(s).");
            clearForm();
            main.refreshAll();
        } catch (SQLException | RuntimeException ex) { Ui.message(this, ex); }
    }

    private void clearForm() {
        selected.clear();
        requisition.setText("REQ-" + java.time.LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        from.setValue(new Date());
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, 2);
        due.setValue(c.getTime());
        redraw();
        try { reload(); } catch (SQLException ignored) { }
    }
}
