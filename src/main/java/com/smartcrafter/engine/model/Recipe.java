package com.smartcrafter.engine.model;

import java.util.*;

/**
 * Represents a crafting recipe in SmartCrafter Pro.
 * Supports both shaped (grid matrix) and shapeless crafting formats.
 */
public class Recipe {
    private final String id;
    private final ItemIdentifier output;
    private final int outputCount;
    private final int width;
    private final int height;
    private final ItemIdentifier[] pattern; // size: width * height (null for empty slot)
    private final boolean shapeless;
    private final boolean requiresCraftingTable;

    public Recipe(String id, ItemIdentifier output, int outputCount, int width, int height,
                  ItemIdentifier[] pattern, boolean shapeless, boolean forceCraftingTable) {
        this.id = id;
        this.output = Objects.requireNonNull(output, "Output cannot be null");
        this.outputCount = Math.max(1, outputCount);
        this.width = width;
        this.height = height;
        this.pattern = pattern != null ? pattern.clone() : new ItemIdentifier[width * height];
        this.shapeless = shapeless;
        this.requiresCraftingTable = forceCraftingTable || width > 2 || height > 2;
    }

    /**
     * Creates a shaped recipe with specified dimensions.
     */
    public static Recipe shaped(String id, ItemIdentifier output, int outputCount, int width, int height, ItemIdentifier[] pattern) {
        return new Recipe(id, output, outputCount, width, height, pattern, false, false);
    }

    /**
     * Creates a 3x3 shaped recipe.
     */
    public static Recipe shaped3x3(String id, ItemIdentifier output, int outputCount, ItemIdentifier[] pattern9) {
        return new Recipe(id, output, outputCount, 3, 3, pattern9, false, true);
    }

    /**
     * Creates a 2x2 shaped recipe.
     */
    public static Recipe shaped2x2(String id, ItemIdentifier output, int outputCount, ItemIdentifier[] pattern4) {
        return new Recipe(id, output, outputCount, 2, 2, pattern4, false, false);
    }

    /**
     * Creates a shapeless recipe.
     */
    public static Recipe shapeless(String id, ItemIdentifier output, int outputCount, List<ItemIdentifier> ingredients) {
        int count = ingredients.size();
        int size = count > 4 ? 3 : (count > 1 ? 2 : 1);
        ItemIdentifier[] pattern = new ItemIdentifier[size * size];
        for (int i = 0; i < ingredients.size() && i < pattern.length; i++) {
            pattern[i] = ingredients.get(i);
        }
        return new Recipe(id, output, outputCount, size, size, pattern, true, count > 4);
    }

    /**
     * Creates a single-ingredient conversion recipe (e.g. Logs -> 4 Planks, Block -> 9 Ingots).
     */
    public static Recipe singleInput(String id, ItemIdentifier output, int outputCount, ItemIdentifier input) {
        return shapeless(id, output, outputCount, Collections.singletonList(input));
    }

    /**
     * Aggregates required ingredients into a map of ItemIdentifier -> count required per single craft.
     */
    public Map<ItemIdentifier, Integer> getRequiredIngredientsMap() {
        Map<ItemIdentifier, Integer> map = new LinkedHashMap<>();
        for (ItemIdentifier item : pattern) {
            if (item != null && !item.getId().equals("minecraft:air")) {
                map.merge(item, 1, Integer::sum);
            }
        }
        return map;
    }

    public String getId() {
        return id;
    }

    public ItemIdentifier getOutput() {
        return output;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ItemIdentifier[] getPattern() {
        return pattern.clone();
    }

    public ItemIdentifier getSlotIngredient(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return pattern[y * width + x];
    }

    public boolean isShapeless() {
        return shapeless;
    }

    public boolean requiresCraftingTable() {
        return requiresCraftingTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipe recipe)) return false;
        return Objects.equals(id, recipe.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " -> " + outputCount + "x " + output.getDisplayName();
    }
}
