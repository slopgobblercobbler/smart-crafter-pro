package com.smartcrafter.engine.model;

import java.util.Objects;

/**
 * Lightweight simulation item stack for recipe graph resolution,
 * inventory deficit calculations, and transaction planning.
 */
public class ItemStackSim {
    public static final ItemStackSim EMPTY = new ItemStackSim(ItemIdentifier.of("minecraft:air", "Air"), 0);

    private final ItemIdentifier item;
    private int count;

    public ItemStackSim(ItemIdentifier item, int count) {
        this.item = item != null ? item : ItemIdentifier.of("minecraft:air", "Air");
        this.count = Math.max(0, count);
    }

    public static ItemStackSim of(ItemIdentifier item, int count) {
        if (item == null || count <= 0) return EMPTY.copy();
        return new ItemStackSim(item, count);
    }

    public static ItemStackSim of(String id, int count) {
        return of(ItemIdentifier.of(id), count);
    }

    public ItemIdentifier getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    public boolean isEmpty() {
        return this == EMPTY || count <= 0 || "minecraft:air".equals(item.getId());
    }

    public void grow(int amount) {
        setCount(this.count + amount);
    }

    public void shrink(int amount) {
        setCount(this.count - amount);
    }

    public ItemStackSim split(int amount) {
        int take = Math.min(amount, this.count);
        ItemStackSim taken = new ItemStackSim(this.item, take);
        shrink(take);
        return taken;
    }

    public boolean isItemEqual(ItemStackSim other) {
        if (other == null) return false;
        return Objects.equals(this.item, other.item);
    }

    public ItemStackSim copy() {
        return new ItemStackSim(this.item, this.count);
    }

    @Override
    public String toString() {
        return isEmpty() ? "Empty" : count + "x " + item.getDisplayName();
    }
}
