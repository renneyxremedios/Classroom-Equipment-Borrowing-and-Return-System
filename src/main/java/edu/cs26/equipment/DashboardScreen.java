package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

final class DashboardScreen extends Screen {
    private final MainFrame main;
    private final JPanel counters = Ui.panel(new GridLayout(1, 5, 12, 12));
    private final DefaultTableModel recent = new DefaultTableModel(
        new String[]{"Requisition","Student","Item","Borrowed","Status"},0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    DashboardScreen(MainFrame main) {
        super("Dashboard", "Overview of equipment inventory and borrowing activity");
        this.main = main;
        JPanel page = (JPanel) getComponent(0);
        JPanel center = Ui.panel(new BorderLayout(0, 22));
        center.setBorder(new EmptyBorder(10, 0, 0, 0));
        center.add(counters, BorderLayout.NORTH);
        JPanel history = Ui.panel(new BorderLayout(0, 8));
        JLabel label = new JLabel("Recent borrowing activity");
        label.setFont(new Font("Segoe UI", Font.BOLD, 17));
        label.setForeground(Ui.NAVY);
        history.add(label, BorderLayout.NORTH);
        history.add(new JScrollPane(Ui.table(recent)), BorderLayout.CENTER);
        center.add(history, BorderLayout.CENTER);
        JButton refresh = Ui.secondary("Refresh dashboard");
        refresh.addActionListener(e -> main.showScreen("Dashboard"));
        center.add(Ui.actions(refresh), BorderLayout.SOUTH);
        page.add(center, BorderLayout.CENTER);
    }

    @Override void reload() throws SQLException {
        Stats stats = main.repo.stats();
        counters.removeAll();
        card("Total items", stats.equipment(), Ui.BLUE);
        card("Available", stats.available(), new Color(44, 134, 99));
        card("Borrowed", stats.borrowed(), new Color(184, 116, 39));
        card("Overdue", stats.overdue(), new Color(174, 68, 76));
        card("Students", stats.students(), Ui.NAVY);
        recent.setRowCount(0);
        List<LoanRow> loans = main.repo.loans(false);
        for (int i = 0; i < Math.min(loans.size(), 12); i++) {
            LoanRow l = loans.get(i);
            recent.addRow(new Object[]{l.requisition(), l.studentName(),
                l.equipmentName(), Ui.time(l.borrowedAt()), l.status()});
        }
        counters.revalidate();
        counters.repaint();
    }

    private void card(String text, int number, Color accent) {
        JPanel c = Ui.panel(new BorderLayout(0, 8));
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 231, 240)),
            new EmptyBorder(15, 15, 15, 15)));
        JLabel top = new JLabel(text);
        top.setForeground(Ui.MUTED);
        JLabel count = new JLabel(Integer.toString(number));
        count.setFont(new Font("Segoe UI", Font.BOLD, 30));
        count.setForeground(accent);
        c.add(top, BorderLayout.NORTH);
        c.add(count, BorderLayout.CENTER);
        counters.add(c);
    }
}
