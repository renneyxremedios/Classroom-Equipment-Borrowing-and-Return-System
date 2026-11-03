package edu.cs26.equipment;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

final class Ui {
    static final Color NAVY = new Color(24, 43, 68);
    static final Color BLUE = new Color(42, 108, 172);
    static final Color PALE = new Color(235, 242, 250);
    static final Color BACKGROUND = new Color(245, 247, 251);
    static final Color TEXT = new Color(36, 52, 70);
    static final Color MUTED = new Color(110, 126, 144);
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");

    private Ui() { }

    static void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    }

    static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Color.WHITE);
        return p;
    }

    static JPanel page(String heading, String subtitle) {
        JPanel p = panel(new BorderLayout(0, 16));
        p.setBorder(new EmptyBorder(20, 25, 22, 25));
        JPanel head = panel(new GridLayout(2, 1));
        JLabel title = new JLabel(heading);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(NAVY);
        JLabel caption = new JLabel(subtitle);
        caption.setForeground(MUTED);
        head.add(title);
        head.add(caption);
        p.add(head, BorderLayout.NORTH);
        return p;
    }

    static JButton button(String text) {
        JButton b = new JButton(text);
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setMargin(new Insets(9, 16, 9, 16));
        return b;
    }

    static JButton secondary(String text) {
        JButton b = button(text);
        b.setBackground(PALE);
        b.setForeground(NAVY);
        return b;
    }

    static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return f;
    }

    static JTable table(javax.swing.table.TableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(30);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setGridColor(new Color(226, 234, 242));
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JTableHeader h = t.getTableHeader();
        h.setFont(new Font("Segoe UI", Font.BOLD, 12));
        h.setBackground(PALE);
        h.setForeground(NAVY);
        h.setPreferredSize(new Dimension(0, 34));
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        return t;
    }

    static JPanel actions(Component... buttons) {
        JPanel p = panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        for (Component b : buttons) p.add(b);
        return p;
    }

    static String time(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    static void message(Component owner, Exception error) {
        JOptionPane.showMessageDialog(owner, error.getMessage(), "Operation failed",
            JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component owner, String message) {
        return JOptionPane.showConfirmDialog(owner, message, "Confirm action",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static String prompt(Component owner, String title, String text, String current) {
        return (String) JOptionPane.showInputDialog(owner, text, title, JOptionPane.PLAIN_MESSAGE,
            null, null, current);
    }
}
