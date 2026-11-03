package edu.cs26.equipment;

import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class App {
    private App() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.applyLookAndFeel();
            Database database = new Database();
            try {
                database.initialize();
                new LoginFrame(database).setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                    "Could not connect to MySQL.\n\n"
                    + "1. Install and start MySQL Server.\n"
                    + "2. Run database/schema.sql once.\n"
                    + "3. Set DB_URL, DB_USER, DB_PASSWORD environment variables.\n\n"
                    + "Details: " + ex.getMessage(),
                    "Database connection required", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
