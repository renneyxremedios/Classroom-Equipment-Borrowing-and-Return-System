package edu.cs26.equipment;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

final class LoginFrame extends JFrame {
    private final Database database;
    private final AuthService auth;
    private final JPanel form = new JPanel(new GridBagLayout());
    private boolean setup;
    private JTextField username;
    private JTextField displayName;
    private JPasswordField password;
    private JPasswordField confirm;

    LoginFrame(Database database) throws SQLException {
        super("Classroom Equipment | Sign in");
        this.database = database;
        this.auth = new AuthService(database);
        this.setup = auth.needsAdminSetup();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 525));
        setSize(970, 590);
        setLocationRelativeTo(null);

        JPanel body = new JPanel(new GridLayout(1, 2));
        JPanel branding = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, Ui.NAVY, getWidth(), getHeight(),
                    new Color(39, 99, 157)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        branding.setBorder(new EmptyBorder(45, 42, 45, 42));
        JLabel brand = new JLabel("<html><span style='font-size:34px'>CLASSROOM<br>"
            + "EQUIPMENT</span><br><br><span style='font-size:16px'>Borrowing & Return<br>"
            + "Management System</span><br><br><span style='font-size:11px'>"
            + "Java Swing · Object-Oriented Programming · MySQL</span></html>");
        brand.setForeground(Color.WHITE);
        branding.add(brand);
        body.add(branding);

        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(38, 45, 38, 45));
        body.add(form);
        setContentPane(body);
        renderForm();
    }

    private void renderForm() {
        form.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(7, 0, 7, 0);
        c.gridy = 0;

        JLabel heading = new JLabel(setup ? "Create administrator" : "Welcome back");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 25));
        heading.setForeground(Ui.NAVY);
        form.add(heading, c);
        c.gridy++;
        JLabel description = new JLabel(setup ? "First launch: create the secure admin account."
            : "Sign in to manage borrowing transactions.");
        description.setForeground(Ui.MUTED);
        form.add(description, c);

        username = Ui.field(22);
        displayName = setup ? Ui.field(22) : null;
        password = new JPasswordField(22);
        confirm = setup ? new JPasswordField(22) : null;

        c.gridy++;
        form.add(fieldGroup("Username", username), c);
        if (setup) {
            c.gridy++;
            form.add(fieldGroup("Full name", displayName), c);
        }
        c.gridy++;
        form.add(fieldGroup("Password", password), c);
        if (setup) {
            c.gridy++;
            form.add(fieldGroup("Confirm password", confirm), c);
        }
        c.gridy++;
        JButton submit = Ui.button(setup ? "Create admin account" : "Sign in");
        submit.setPreferredSize(new Dimension(200, 42));
        form.add(submit, c);
        submit.addActionListener(e -> submit());
        getRootPane().setDefaultButton(submit);

        c.gridy++;
        JLabel note = new JLabel("<html><small>"
            + (setup ? "Use at least 8 characters. No default account is created."
                     : "Access is limited to registered staff accounts.")
            + "</small></html>");
        note.setForeground(Ui.MUTED);
        form.add(note, c);
        c.gridy++;
        c.weighty = 1;
        form.add(Box.createVerticalGlue(), c);
        form.revalidate();
        form.repaint();
        SwingUtilities.invokeLater(username::requestFocusInWindow);
    }

    private JPanel fieldGroup(String label, JComponent input) {
        JPanel p = Ui.panel(new BorderLayout(0, 5));
        JLabel name = new JLabel(label);
        name.setForeground(Ui.TEXT);
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(name, BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(230, 36));
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    private void submit() {
        char[] entered = password.getPassword();
        char[] verify = confirm == null ? new char[0] : confirm.getPassword();
        try {
            String name = username.getText().trim();
            if (setup) {
                if (!Arrays.equals(entered, verify)) throw new IllegalArgumentException("Passwords do not match.");
                auth.createFirstAdmin(name, displayName.getText(), entered);
                JOptionPane.showMessageDialog(this, "Administrator created. Sign in to continue.");
                setup = false;
                renderForm();
            } else {
                Account account = auth.login(name, entered);
                if (account == null) throw new IllegalArgumentException("Invalid username or password.");
                new MainFrame(database, account).setVisible(true);
                dispose();
            }
        } catch (SQLException | RuntimeException ex) {
            Ui.message(this, ex);
        } finally {
            Arrays.fill(entered, '\0');
            Arrays.fill(verify, '\0');
            password.setText("");
            if (confirm != null) confirm.setText("");
        }
    }
}
