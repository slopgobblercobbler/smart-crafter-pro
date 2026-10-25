package com.smartcrafter.engine.model;

import java.util.Objects;

/**
 * Immutable lightweight representation of an item in SmartCrafter Pro.
 * Provides fast equality comparisons, formatting, and item categorization.
 */
public class ItemIdentifier implements Comparable<ItemIdentifier> {
    private final String id;
    private final String displayName;
    private final int maxStackSize;
    private final String category;

    public ItemIdentifier(String id, String displayName, int maxStackSize, String category) {
        this.id = id != null && id.contains(":") ? id : "minecraft:" + (id == null ? "air" : id);
        this.displayName = displayName != null ? displayName : deriveName(this.id);
        this.maxStackSize = maxStackSize > 0 ? maxStackSize : 64;
        this.category = category != null ? category : "MISC";
    }

    public ItemIdentifier(String id, String displayName) {
        this(id, displayName, 64, "MISC");
    }

    public ItemIdentifier(String id) {
        this(id, null, 64, "MISC");
    }

    public static ItemIdentifier of(String id) {
        return new ItemIdentifier(id);
    }

    public static ItemIdentifier of(String id, String displayName) {
        return new ItemIdentifier(id, displayName);
    }

    public static ItemIdentifier of(String id, String displayName, int maxStackSize, String category) {
        return new ItemIdentifier(id, displayName, maxStackSize, category);
    }

    private static String deriveName(String fullId) {
        String path = fullId.contains(":") ? fullId.substring(fullId.indexOf(':') + 1) : fullId;
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemIdentifier that)) return false;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(ItemIdentifier o) {
        return this.displayName.compareToIgnoreCase(o.displayName);
    }

    @Override
    public String toString() {
        return displayName + " (" + id + ")";
    }
}
