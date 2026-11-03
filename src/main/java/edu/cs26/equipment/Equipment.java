package edu.cs26.equipment;

/** Abstraction: each equipment type has distinct handling instructions. */
public abstract class Equipment {
    private final String id;
    private final String name;
    private final String notes;

    protected Equipment(String id, String name, String notes) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Equipment ID and name are required.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.notes = notes == null ? "" : notes.trim();
    }

    public final String getId() { return id; }
    public final String getName() { return name; }
    public final String getNotes() { return notes; }
    public abstract String getCategory();
    public abstract String handlingInstructions();

    @Override public String toString() { return id + " - " + name; }

    public static Equipment of(String id, String name, String category, String notes) {
        if ("Electronic".equalsIgnoreCase(category)) {
            return new ElectronicEquipment(id, name, notes);
        }
        if ("Non-Electronic".equalsIgnoreCase(category)) {
            return new NonElectronicEquipment(id, name, notes);
        }
        throw new IllegalArgumentException("Unknown equipment category: " + category);
    }
}
