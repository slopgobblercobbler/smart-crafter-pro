package com.smartcrafter.engine.transaction;

/**
 * Represents a single atomic slot interaction packet/action in a safe transaction sequence.
 */
public class TransactionStep {
    private final SlotActionType actionType;
    private final int slotId;
    private final int button;
    private final String description;

    public TransactionStep(SlotActionType actionType, int slotId, int button, String description) {
        this.actionType = actionType;
        this.slotId = slotId;
        this.button = button;
        this.description = description;
    }

    public static TransactionStep pickup(int slotId, String desc) {
        return new TransactionStep(SlotActionType.PICKUP, slotId, 0, desc);
    }

    public static TransactionStep placeOne(int slotId, String desc) {
        return new TransactionStep(SlotActionType.PLACE_ONE, slotId, 1, desc);
    }

    public static TransactionStep quickMove(int slotId, String desc) {
        return new TransactionStep(SlotActionType.QUICK_MOVE, slotId, 0, desc);
    }

    public static TransactionStep takeResult(String desc) {
        return new TransactionStep(SlotActionType.TAKE_RESULT, 0, 0, desc);
    }

    public static TransactionStep restoreCursor(int slotId, String desc) {
        return new TransactionStep(SlotActionType.RESTORE_CURSOR, slotId, 0, desc);
    }

    public static TransactionStep delay(String desc) {
        return new TransactionStep(SlotActionType.DELAY, -1, 0, desc);
    }

    public SlotActionType getActionType() {
        return actionType;
    }

    public int getSlotId() {
        return slotId;
    }

    public int getButton() {
        return button;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "[" + actionType + "] slot=" + slotId + " (" + description + ")";
    }
}
