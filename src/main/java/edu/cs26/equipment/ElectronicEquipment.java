package edu.cs26.equipment;

/** Inheritance and polymorphism: electrical items need special handling. */
public final class ElectronicEquipment extends Equipment {
    public ElectronicEquipment(String id, String name, String notes) { super(id, name, notes); }
    @Override public String getCategory() { return "Electronic"; }
    @Override public String handlingInstructions() { return "Check power cables and switch off before returning."; }
}
