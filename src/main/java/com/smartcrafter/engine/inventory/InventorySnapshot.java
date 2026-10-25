package com.smartcrafter.engine.inventory;

import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.ItemStackSim;

import java.util.*;

/**
 * Immutable/mutable virtual snapshot of the player's inventory slots.
 * Main storage: 36 slots (0-8 hotbar, 9-35 main inventory), plus offhand.
 */
public class InventorySnapshot {
    public static final int MAIN_SIZE = 36;
    public static final int OFFHAND_SLOT = 36;
    public static final int TOTAL_SLOTS = 37;

    private final ItemStackSim[] slots;

    public InventorySnapshot() {
        this.slots = new ItemStackSim[TOTAL_SLOTS];
        Arrays.fill(this.slots, ItemStackSim.EMPTY);
    }

    public InventorySnapshot(ItemStackSim[] initialSlots) {
        this.slots = new ItemStackSim[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (i < initialSlots.length && initialSlots[i] != null) {
                this.slots[i] = initialSlots[i].copy();
            } else {
                this.slots[i] = ItemStackSim.EMPTY;
            }
        }
    }

    public static InventorySnapshot ofMap(Map<ItemIdentifier, Integer> items) {
        InventorySnapshot snapshot = new InventorySnapshot();
        int slotIndex = 0;
        for (Map.Entry<ItemIdentifier, Integer> entry : items.entrySet()) {
            ItemIdentifier item = entry.getKey();
            int remaining = entry.getValue();
            while (remaining > 0 && slotIndex < MAIN_SIZE) {
                int toPut = Math.min(remaining, item.getMaxStackSize());
                snapshot.setSlot(slotIndex++, new ItemStackSim(item, toPut));
                remaining -= toPut;
            }
        }
        return snapshot;
    }

    public ItemStackSim getSlot(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) return ItemStackSim.EMPTY;
        return slots[index] != null ? slots[index] : ItemStackSim.EMPTY;
    }

    public void setSlot(int index, ItemStackSim stack) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            slots[index] = (stack != null && !stack.isEmpty()) ? stack.copy() : ItemStackSim.EMPTY;
        }
    }

    public int countItem(ItemIdentifier item) {
        if (item == null) return 0;
        int sum = 0;
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStackSim stack = slots[i];
            if (stack != null && !stack.isEmpty() && stack.getItem().equals(item)) {
                sum += stack.getCount();
            }
        }
        return sum;
    }

    public Map<ItemIdentifier, Integer> getAllItemCounts() {
        Map<ItemIdentifier, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStackSim stack = slots[i];
            if (stack != null && !stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    public List<Integer> findSlotsWithItem(ItemIdentifier item) {
        List<Integer> list = new ArrayList<>();
        if (item == null) return list;
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStackSim stack = slots[i];
            if (stack != null && !stack.isEmpty() && stack.getItem().equals(item)) {
                list.add(i);
            }
        }
        return list;
    }

    public int findFirstEmptySlot() {
        for (int i = 0; i < MAIN_SIZE; i++) {
            if (slots[i] == null || slots[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public int getAvailableSpaceFor(ItemIdentifier item) {
        if (item == null) return 0;
        int space = 0;
        for (int i = 0; i < MAIN_SIZE; i++) {
            ItemStackSim stack = slots[i];
            if (stack == null || stack.isEmpty()) {
                space += item.getMaxStackSize();
            } else if (stack.getItem().equals(item)) {
                space += Math.max(0, item.getMaxStackSize() - stack.getCount());
            }
        }
        return space;
    }

    public int deductItem(ItemIdentifier item, int amount) {
        if (item == null || amount <= 0) return 0;
        int remaining = amount;
        for (int i = MAIN_SIZE - 1; i >= 0 && remaining > 0; i--) {
            ItemStackSim stack = slots[i];
            if (stack != null && !stack.isEmpty() && stack.getItem().equals(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (stack.isEmpty()) {
                    slots[i] = ItemStackSim.EMPTY;
                }
            }
        }
        return amount - remaining;
    }

    public int addItem(ItemIdentifier item, int amount) {
        if (item == null || amount <= 0) return 0;
        int remaining = amount;

        // Pass 1: Fill existing non-full stacks
        for (int i = 0; i < MAIN_SIZE && remaining > 0; i++) {
            ItemStackSim stack = slots[i];
            if (stack != null && !stack.isEmpty() && stack.getItem().equals(item)) {
                int canAdd = Math.min(remaining, item.getMaxStackSize() - stack.getCount());
                if (canAdd > 0) {
                    stack.grow(canAdd);
                    remaining -= canAdd;
                }
            }
        }

        // Pass 2: Fill empty slots
        for (int i = 0; i < MAIN_SIZE && remaining > 0; i++) {
            ItemStackSim stack = slots[i];
            if (stack == null || stack.isEmpty()) {
                int toAdd = Math.min(remaining, item.getMaxStackSize());
                slots[i] = new ItemStackSim(item, toAdd);
                remaining -= toAdd;
            }
        }

        return remaining; // Leftover unadded amount (0 if completely added)
    }

    public InventorySnapshot copy() {
        return new InventorySnapshot(this.slots);
    }
}
