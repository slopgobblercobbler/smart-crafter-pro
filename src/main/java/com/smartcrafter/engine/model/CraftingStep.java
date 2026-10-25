package com.smartcrafter.engine.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single batch crafting execution step in a resolved crafting sequence.
 */
public class CraftingStep {
    private final Recipe recipe;
    private final int batches;
    private final int tier;
    private final Map<ItemIdentifier, Integer> inputCounts;
    private final int totalOutputCount;

    public CraftingStep(Recipe recipe, int batches, int tier) {
        this.recipe = recipe;
        this.batches = Math.max(1, batches);
        this.tier = tier;
        this.totalOutputCount = this.batches * recipe.getOutputCount();

        Map<ItemIdentifier, Integer> baseInputs = recipe.getRequiredIngredientsMap();
        Map<ItemIdentifier, Integer> scaled = new LinkedHashMap<>();
        for (Map.Entry<ItemIdentifier, Integer> entry : baseInputs.entrySet()) {
            scaled.put(entry.getKey(), entry.getValue() * this.batches);
        }
        this.inputCounts = Collections.unmodifiableMap(scaled);
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getBatches() {
        return batches;
    }

    public int getTier() {
        return tier;
    }

    public Map<ItemIdentifier, Integer> getInputCounts() {
        return inputCounts;
    }

    public int getTotalOutputCount() {
        return totalOutputCount;
    }

    public boolean requiresCraftingTable() {
        return recipe.requiresCraftingTable();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Tier ").append(tier).append("] Craft ")
          .append(totalOutputCount).append("x ").append(recipe.getOutput().getDisplayName())
          .append(" (").append(batches).append(" batches) using ");

        boolean first = true;
        for (Map.Entry<ItemIdentifier, Integer> entry : inputCounts.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getValue()).append("x ").append(entry.getKey().getDisplayName());
            first = false;
        }
        return sb.toString();
    }
}
