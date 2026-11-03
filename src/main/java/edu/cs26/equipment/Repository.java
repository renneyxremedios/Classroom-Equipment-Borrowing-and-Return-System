package edu.cs26.equipment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** All persistent operations use prepared statements; borrowing and returns are transactions. */
final class Repository {
    private final Database db;
    Repository(Database db) { this.db = db; }

    List<Student> students() throws SQLException {
        List<Student> result = new ArrayList<>();
        try (Connection c = db.open(); PreparedStatement p = c.prepareStatement(
                "SELECT student_id,full_name,course,section FROM students ORDER BY full_name");
             ResultSet r = p.executeQuery()) {
            while (r.next()) result.add(new Student(r.getString(1), r.getString(2),
                r.getString(3), r.getString(4)));
        }
        return result;
    }

    void saveStudent(Student s, boolean editing) throws SQLException {
        require(s.id(), "Student ID");
        require(s.name(), "Student name");
        String sql = editing
            ? "UPDATE students SET full_name=?,course=?,section=? WHERE student_id=?"
            : "INSERT INTO students (full_name,course,section,student_id) VALUES (?,?,?,?)";
        try (Connection c = db.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, s.name().trim());
            p.setString(2, s.course().trim());
            p.setString(3, s.section().trim());
            p.setString(4, s.id().trim());
            if (p.executeUpdate() != 1) throw new SQLException("Student record was not found.");
        }
    }

    void deleteStudent(String id) throws SQLException {
        try (Connection c = db.open(); PreparedStatement p =
                c.prepareStatement("DELETE FROM students WHERE student_id=?")) {
            p.setString(1, id);
            p.executeUpdate();
        }
    }

    List<EquipmentRow> equipment(boolean availableOnly) throws SQLException {
        List<EquipmentRow> result = new ArrayList<>();
        String sql = "SELECT equipment_id,name,category,notes,status FROM equipment"
            + (availableOnly ? " WHERE status='AVAILABLE'" : "") + " ORDER BY name,equipment_id";
        try (Connection c = db.open(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            while (r.next()) result.add(new EquipmentRow(
                Equipment.of(r.getString("equipment_id"), r.getString("name"),
                    r.getString("category"), r.getString("notes")), r.getString("status")));
        }
        return result;
    }

    void saveEquipment(Equipment e, boolean editing) throws SQLException {
        String sql = editing
            ? "UPDATE equipment SET name=?,category=?,notes=? WHERE equipment_id=?"
            : "INSERT INTO equipment (name,category,notes,equipment_id,status) VALUES (?,?,?,?,'AVAILABLE')";
        try (Connection c = db.open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, e.getName());
            p.setString(2, e.getCategory());
            p.setString(3, e.getNotes());
            p.setString(4, e.getId());
            if (p.executeUpdate() != 1) throw new SQLException("Equipment record was not found.");
        }
    }

    void deleteEquipment(String id) throws SQLException {
        try (Connection c = db.open(); PreparedStatement p =
                c.prepareStatement("DELETE FROM equipment WHERE equipment_id=? AND status='AVAILABLE'")) {
            p.setString(1, id);
            if (p.executeUpdate() != 1)
                throw new SQLException("Only available equipment can be deleted.");
        }
    }

    /**
     * Locks every equipment row before insertion. A transaction cannot issue an already-borrowed
     * item, even when two operators try to issue the same equipment at the same moment.
     */
    void borrow(BorrowRequest request, int operatorId) throws SQLException {
        require(request.requisition(), "Requisition number");
        require(request.studentId(), "Student");
        if (!request.dueAt().isAfter(request.borrowedAt()))
            throw new IllegalArgumentException("Return deadline must be after borrowing time.");
        if (request.equipmentIds().isEmpty())
            throw new IllegalArgumentException("Add at least one piece of equipment.");
        Set<String> unique = new HashSet<>(request.equipmentIds());
        if (unique.size() != request.equipmentIds().size())
            throw new IllegalArgumentException("The same item cannot be borrowed twice.");
        List<String> sorted = new ArrayList<>(unique);
        sorted.sort(Comparator.naturalOrder());
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                for (String id : sorted) {
                    try (PreparedStatement lock = c.prepareStatement(
                            "SELECT status FROM equipment WHERE equipment_id=? FOR UPDATE")) {
                        lock.setString(1, id);
                        try (ResultSet r = lock.executeQuery()) {
                            if (!r.next()) throw new SQLException("Equipment not found: " + id);
                            if (!"AVAILABLE".equals(r.getString(1)))
                                throw new SQLException("Equipment is currently borrowed: " + id);
                        }
                    }
                    try (PreparedStatement add = c.prepareStatement(
                            "INSERT INTO borrowings (requisition_no,student_id,equipment_id,"
                            + "borrowed_at,due_at,issued_by) VALUES (?,?,?,?,?,?)")) {
                        add.setString(1, request.requisition().trim());
                        add.setString(2, request.studentId());
                        add.setString(3, id);
                        add.setTimestamp(4, Timestamp.valueOf(request.borrowedAt()));
                        add.setTimestamp(5, Timestamp.valueOf(request.dueAt()));
                        add.setInt(6, operatorId);
                        add.executeUpdate();
                    }
                    try (PreparedStatement update = c.prepareStatement(
                            "UPDATE equipment SET status='BORROWED' WHERE equipment_id=?")) {
                        update.setString(1, id);
                        update.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException error) {
                c.rollback();
                throw error;
            }
        }
    }

    void returnEquipment(long loanId, int operatorId) throws SQLException {
        try (Connection c = db.open()) {
            c.setAutoCommit(false);
            try {
                String equipmentId;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT equipment_id,returned_at FROM borrowings WHERE id=? FOR UPDATE")) {
                    p.setLong(1, loanId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new SQLException("Borrowing transaction not found.");
                        if (r.getTimestamp("returned_at") != null)
                            throw new SQLException("This item has already been returned.");
                        equipmentId = r.getString("equipment_id");
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE borrowings SET returned_at=NOW(),received_by=? WHERE id=? AND returned_at IS NULL")) {
                    p.setInt(1, operatorId);
                    p.setLong(2, loanId);
                    if (p.executeUpdate() != 1) throw new SQLException("Return was not recorded.");
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE equipment SET status='AVAILABLE' WHERE equipment_id=?")) {
                    p.setString(1, equipmentId);
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException error) {
                c.rollback();
                throw error;
            }
        }
    }

    List<LoanRow> loans(boolean activeOnly) throws SQLException {
        List<LoanRow> rows = new ArrayList<>();
        String sql = """
            SELECT b.id,b.requisition_no,b.student_id,s.full_name,
                   b.equipment_id,e.name,b.borrowed_at,b.due_at,b.returned_at
            FROM borrowings b
            JOIN students s ON b.student_id=s.student_id
            JOIN equipment e ON b.equipment_id=e.equipment_id
            """ + (activeOnly ? " WHERE b.returned_at IS NULL" : "")
              + " ORDER BY b.borrowed_at DESC,b.id DESC";
        try (Connection c = db.open(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Timestamp returned = r.getTimestamp("returned_at");
                rows.add(new LoanRow(r.getLong(1), r.getString(2), r.getString(3), r.getString(4),
                    r.getString(5), r.getString(6),
                    r.getTimestamp("borrowed_at").toLocalDateTime(),
                    r.getTimestamp("due_at").toLocalDateTime(),
                    returned == null ? null : returned.toLocalDateTime()));
            }
        }
        return rows;
    }

    Stats stats() throws SQLException {
        try (Connection c = db.open(); Statement s = c.createStatement()) {
            int total = count(s, "SELECT COUNT(*) FROM equipment");
            int available = count(s, "SELECT COUNT(*) FROM equipment WHERE status='AVAILABLE'");
            int borrowed = count(s, "SELECT COUNT(*) FROM borrowings WHERE returned_at IS NULL");
            int overdue = count(s, "SELECT COUNT(*) FROM borrowings WHERE returned_at IS NULL AND due_at<NOW()");
            int students = count(s, "SELECT COUNT(*) FROM students");
            return new Stats(total, available, borrowed, overdue, students);
        }
    }

    private static int count(Statement s, String sql) throws SQLException {
        try (ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
    }
}
