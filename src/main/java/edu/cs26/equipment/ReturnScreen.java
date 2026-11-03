package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

final class ReturnScreen extends Screen {
    private final MainFrame main;
    private final List<LoanRow> active = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Loan #","Requisition","Student","Equipment","Borrowed At","Due At","Status"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = Ui.table(model);

    ReturnScreen(MainFrame main) {
        super("Return Item", "Receive borrowed equipment and release it for future use");
        this.main = main;
        JPanel page = (JPanel) getComponent(0);
        JPanel center = Ui.panel(new BorderLayout(0, 13));
        JPanel bar = Ui.panel(new BorderLayout());
        JLabel caption = new JLabel("Active borrowing transactions");
        caption.setFont(new Font("Segoe UI", Font.BOLD, 16));
        caption.setForeground(Ui.NAVY);
        bar.add(caption, BorderLayout.WEST);
        JButton refresh = Ui.secondary("Refresh");
        JButton receive = Ui.button("Mark Selected as Returned");
        bar.add(Ui.actions(refresh, receive), BorderLayout.EAST);
        center.add(bar, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        refresh.addActionListener(e -> main.showScreen("Return Item"));
        receive.addActionListener(e -> receive());
    }

    @Override void reload() throws SQLException {
        active.clear();
        active.addAll(main.repo.loans(true));
        model.setRowCount(0);
        for (LoanRow loan : active) model.addRow(new Object[]{
            loan.id(), loan.requisition(), loan.studentName() + " (" + loan.studentId() + ")",
            loan.equipmentName() + " (" + loan.equipmentId() + ")",
            Ui.time(loan.borrowedAt()), Ui.time(loan.dueAt()), loan.status()
        });
    }

    private void receive() {
        int visible = table.getSelectedRow();
        if (visible < 0) { JOptionPane.showMessageDialog(this, "Select a borrowing transaction."); return; }
        LoanRow loan = active.get(table.convertRowIndexToModel(visible));
        String instructions = "Return " + loan.equipmentName() + " from " + loan.studentName() + "?";
        if (!Ui.confirm(this, instructions)) return;
        try {
            main.repo.returnEquipment(loan.id(), main.account.id());
            JOptionPane.showMessageDialog(this, "Return successfully recorded.");
            main.refreshAll();
        } catch (SQLException ex) { Ui.message(this, ex); }
    }
}
