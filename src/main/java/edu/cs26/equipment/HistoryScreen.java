package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

final class HistoryScreen extends Screen {
    private final MainFrame main;
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Requisition","Student ID","Borrower","Equipment ID","Item",
                     "Borrowed At","Due At","Returned At","Status"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = Ui.table(model);
    private final JTextField search = Ui.field(25);

    HistoryScreen(MainFrame main) {
        super("Borrowing History", "Complete borrowing and return transaction records");
        this.main = main;
        JPanel page = (JPanel) getComponent(0);
        JPanel center = Ui.panel(new BorderLayout(0, 12));
        JPanel toolbar = Ui.panel(new BorderLayout());
        JPanel query = Ui.panel(new FlowLayout(FlowLayout.LEFT));
        query.add(new JLabel("Search requisition, borrower, or item:"));
        query.add(search);
        toolbar.add(query, BorderLayout.WEST);
        JButton refresh = Ui.secondary("Refresh");
        refresh.addActionListener(e -> main.showScreen("Borrowing History"));
        toolbar.add(Ui.actions(refresh), BorderLayout.EAST);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
    }

    @Override void reload() throws SQLException {
        model.setRowCount(0);
        for (LoanRow loan : main.repo.loans(false)) {
            model.addRow(new Object[]{loan.requisition(), loan.studentId(), loan.studentName(),
                loan.equipmentId(), loan.equipmentName(), Ui.time(loan.borrowedAt()),
                Ui.time(loan.dueAt()), Ui.time(loan.returnedAt()), loan.status()});
        }
        filter();
    }

    private void filter() {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        String text = search.getText().trim();
        if (!text.isEmpty()) sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }
}
