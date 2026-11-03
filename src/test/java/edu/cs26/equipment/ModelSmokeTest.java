package edu.cs26.equipment;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** No external test framework or MySQL required. */
public final class ModelSmokeTest {
    public static void main(String[] args) {
        Equipment electrical = Equipment.of("EQ-100", "Projector", "Electronic", "A/V Room");
        Equipment nonElectrical = Equipment.of("EQ-200", "Whiteboard", "Non-Electronic", "");
        assertTrue(electrical instanceof ElectronicEquipment, "Electronic inheritance");
        assertTrue(nonElectrical instanceof NonElectronicEquipment, "Non-electronic inheritance");
        assertTrue(!electrical.handlingInstructions().equals(nonElectrical.handlingInstructions()),
            "Polymorphism for handling");
        assertTrue("EQ-100".equals(electrical.getId()), "Equipment encapsulation");
        assertTrue(electrical.getName().equals("Projector"), "Equipment name getter");

        char[] password = "test-strong-password".toCharArray();
        String salt = Passwords.newSalt();
        String hashed = Passwords.hash(password, salt);
        assertTrue(Passwords.matches(password, salt, hashed), "Correct password");
        assertTrue(!Passwords.matches("wrong".toCharArray(), salt, hashed), "Wrong password");
        Arrays.fill(password, '\0');

        BorrowRequest borrow = new BorrowRequest("REQ-1", "STU-1",
            LocalDateTime.now(), LocalDateTime.now().plusHours(2), List.of("EQ-100"));
        assertTrue(borrow.equipmentIds().size() == 1, "Borrow request contains selected equipment");

        LoanRow active = new LoanRow(1L, "REQ-1", "STU-1", "Example Student", "EQ-100",
            "Projector", LocalDateTime.now(), LocalDateTime.now().plusHours(2), null);
        LoanRow returned = new LoanRow(2L, "REQ-1", "STU-1", "Example Student", "EQ-200",
            "Whiteboard", LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(1),
            LocalDateTime.now());
        assertTrue("BORROWED".equals(active.status()), "Borrowed state");
        assertTrue("RETURNED".equals(returned.status()), "Returned state");

        System.out.println("PASS: OOP model, password hashing, and transaction record smoke checks.");
    }

    private static void assertTrue(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
    }
}
