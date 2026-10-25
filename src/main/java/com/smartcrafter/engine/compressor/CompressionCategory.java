package com.smartcrafter.engine.compressor;

/**
 * Filter categories for batch inventory compression.
 */
public enum CompressionCategory {
    ALL("All Compressibles"),
    METALS("Metals (Iron, Gold, Copper, Netherite)"),
    GEMS("Gems (Diamond, Emerald, Amethyst, Quartz)"),
    MINERALS("Minerals (Redstone, Lapis, Coal)"),
    CROPS("Crops & Plants (Wheat, Bone Meal)"),
    MOB_DROPS("Mob Drops (Slimeballs, Bones)");

    private final String displayName;

    CompressionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
