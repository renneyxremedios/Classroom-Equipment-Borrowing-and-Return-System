package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

final class MainFrame extends JFrame {
    final Repository repo;
    final Account account;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Screen> screens = new LinkedHashMap<>();
    private final JLabel status = new JLabel("Ready");
    private final JLabel location = new JLabel("Dashboard");

    MainFrame(Database database, Account account) {
        super("Automated Borrowing and Returning of Equipment");
        this.repo = new Repository(database);
        this.account = account;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(990, 650));
        setSize(1280, 790);
        setLocationRelativeTo(null);
        setJMenuBar(createMenu());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.BACKGROUND);
        root.add(sidebar(), BorderLayout.WEST);
        root.add(header(), BorderLayout.NORTH);

        addScreen("Dashboard", new DashboardScreen(this));
        addScreen("Borrow Item", new BorrowScreen(this));
        addScreen("Return Item", new ReturnScreen(this));
        addScreen("Equipment", new EquipmentScreen(this));
        addScreen("Students", new StudentScreen(this));
        addScreen("Borrowing History", new HistoryScreen(this));

        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        content.setBackground(Ui.BACKGROUND);
        root.add(content, BorderLayout.CENTER);
        JPanel footer = Ui.panel(new BorderLayout());
        footer.setBorder(new EmptyBorder(7, 14, 7, 14));
        status.setForeground(Ui.MUTED);
        footer.add(status, BorderLayout.WEST);
        JLabel online = new JLabel("MySQL  |  Local desktop");
        online.setForeground(Ui.MUTED);
        footer.add(online, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        showScreen("Dashboard");
    }

    private JPanel header() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(12, 27, 12, 22));
        location.setForeground(Ui.NAVY);
        location.setFont(new Font("Segoe UI", Font.BOLD, 20));
        top.add(location, BorderLayout.WEST);
        JLabel user = new JLabel("Signed in: " + account.displayName() + " (" + account.role() + ")");
        user.setForeground(Ui.MUTED);
        top.add(user, BorderLayout.EAST);
        return top;
    }

    private JPanel sidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Ui.NAVY);
        sidebar.setPreferredSize(new Dimension(224, 0));
        sidebar.setBorder(new EmptyBorder(28, 12, 20, 12));
        JLabel logo = new JLabel("<html><center>CLASSROOM<br><b>EQUIPMENT</b></center></html>");
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(38));
        for (String name : new String[]{"Dashboard","Borrow Item","Return Item","Equipment","Students","Borrowing History"}) {
            JButton button = new JButton(name);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(195, 44));
            button.setBackground(new Color(35, 61, 91));
            button.setForeground(Color.WHITE);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBorder(new EmptyBorder(8, 16, 8, 8));
            button.setFocusPainted(false);
            button.addActionListener(e -> showScreen(name));
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(8));
        }
        sidebar.add(Box.createVerticalGlue());
        JButton logout = Ui.secondary("Log out");
        logout.setMaximumSize(new Dimension(195, 38));
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.addActionListener(e -> logout());
        sidebar.add(logout);
        return sidebar;
    }

    private JMenuBar createMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        menuItem(file, "Log out", this::logout);
        menuItem(file, "Exit", this::dispose);
        bar.add(file);

        JMenu transactions = new JMenu("Transactions");
        menuItem(transactions, "Borrow Item", () -> showScreen("Borrow Item"));
        menuItem(transactions, "Return Item", () -> showScreen("Return Item"));
        bar.add(transactions);

        JMenu master = new JMenu("Masterfiles");
        menuItem(master, "Equipment", () -> showScreen("Equipment"));
        menuItem(master, "Students", () -> showScreen("Students"));
        bar.add(master);

        JMenu reports = new JMenu("Reports");
        menuItem(reports, "Borrowing History", () -> showScreen("Borrowing History"));
        bar.add(reports);

        JMenu help = new JMenu("Help");
        menuItem(help, "About", () -> JOptionPane.showMessageDialog(this,
            "Classroom Equipment Borrowing and Return Management System\n"
            + "Java Swing · MySQL · OOP (Encapsulation, Abstraction, Inheritance, Polymorphism)\n"
            + "Software Development Fundamentals", "About", JOptionPane.INFORMATION_MESSAGE));
        bar.add(help);
        return bar;
    }

    private void menuItem(JMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    private void addScreen(String name, Screen screen) {
        screens.put(name, screen);
        content.add(screen, name);
    }

    void showScreen(String name) {
        Screen page = screens.get(name);
        if (page == null) return;
        cards.show(content, name);
        location.setText(name);
        try {
            page.reload();
            status.setText(name + " loaded");
        } catch (SQLException ex) {
            Ui.message(this, ex);
            status.setText("Database error");
        }
    }

    void refreshAll() {
        for (Screen page : screens.values()) {
            try { page.reload(); }
            catch (SQLException ex) { Ui.message(this, ex); break; }
        }
        status.setText("Records refreshed");
    }

    private void logout() {
        try {
            new LoginFrame(new Database()).setVisible(true);
            dispose();
        } catch (SQLException ex) { Ui.message(this, ex); }
    }
}

abstract class Screen extends JPanel {
    Screen(String heading, String subtitle) {
        super(new BorderLayout());
        setOpaque(false);
        add(Ui.page(heading, subtitle), BorderLayout.CENTER);
    }
    abstract void reload() throws SQLException;
}
