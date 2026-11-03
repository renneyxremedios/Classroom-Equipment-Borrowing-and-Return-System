package edu.cs26.equipment;

import java.time.LocalDateTime;
import java.util.List;

/** Immutable data transfer objects for the UI. */
record Student(String id, String name, String course, String section) {
    @Override public String toString() { return id + " - " + name; }
}
record EquipmentRow(Equipment equipment, String status) { }
record Account(int id, String username, String displayName, String role) { }
record BorrowRequest(String requisition, String studentId, LocalDateTime borrowedAt,
                     LocalDateTime dueAt, List<String> equipmentIds) { }
record LoanRow(long id, String requisition, String studentId, String studentName,
               String equipmentId, String equipmentName, LocalDateTime borrowedAt,
               LocalDateTime dueAt, LocalDateTime returnedAt) {
    String status() {
        if (returnedAt != null) return "RETURNED";
        return dueAt.isBefore(LocalDateTime.now()) ? "OVERDUE" : "BORROWED";
    }
}
record Stats(int equipment, int available, int borrowed, int overdue, int students) { }
