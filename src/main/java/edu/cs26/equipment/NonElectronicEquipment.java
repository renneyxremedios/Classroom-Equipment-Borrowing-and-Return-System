package edu.cs26.equipment;

public final class NonElectronicEquipment extends Equipment {
    public NonElectronicEquipment(String id, String name, String notes) { super(id, name, notes); }
    @Override public String getCategory() { return "Non-Electronic"; }
    @Override public String handlingInstructions() { return "Inspect for damage and clean before returning."; }
}
