package com.smartcrafter.engine.model;

import java.util.*;

/**
 * Complete resolved crafting execution plan for a target item and quantity.
 * Contains ordered execution steps, required raw materials, inventory deficits,
 * and surplus yields.
 */
public class CraftingPlan {
    private final ItemIdentifier targetItem;
    private final int targetCount;
    private final List<CraftingStep> steps;
    private final Map<ItemIdentifier, Integer> requiredRawIngredients;
    private final Map<ItemIdentifier, Integer> missingRawIngredients;
    private final Map<ItemIdentifier, Integer> intermediateSurplus;
    private final boolean requiresCraftingTable;

    public CraftingPlan(ItemIdentifier targetItem,
                        int targetCount,
                        List<CraftingStep> steps,
                        Map<ItemIdentifier, Integer> requiredRawIngredients,
                        Map<ItemIdentifier, Integer> missingRawIngredients,
                        Map<ItemIdentifier, Integer> intermediateSurplus) {
        this.targetItem = targetItem;
        this.targetCount = targetCount;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.requiredRawIngredients = Collections.unmodifiableMap(new LinkedHashMap<>(requiredRawIngredients));
        this.missingRawIngredients = Collections.unmodifiableMap(new LinkedHashMap<>(missingRawIngredients));
        this.intermediateSurplus = Collections.unmodifiableMap(new LinkedHashMap<>(intermediateSurplus));

        boolean needsTable = false;
        for (CraftingStep step : steps) {
            if (step.requiresCraftingTable()) {
                needsTable = true;
                break;
            }
        }
        this.requiresCraftingTable = needsTable;
    }

    public ItemIdentifier getTargetItem() {
        return targetItem;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public List<CraftingStep> getSteps() {
        return steps;
    }

    public Map<ItemIdentifier, Integer> getRequiredRawIngredients() {
        return requiredRawIngredients;
    }

    public Map<ItemIdentifier, Integer> getMissingRawIngredients() {
        return missingRawIngredients;
    }

    public Map<ItemIdentifier, Integer> getIntermediateSurplus() {
        return intermediateSurplus;
    }

    public boolean isFeasible() {
        return missingRawIngredients.isEmpty();
    }

    public boolean requiresCraftingTable() {
        return requiresCraftingTable;
    }

    public int getTotalSteps() {
        return steps.size();
    }

    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Crafting Plan: ").append(targetCount).append("x ").append(targetItem.getDisplayName()).append(" ===\n");
        sb.append("Status: ").append(isFeasible() ? "READY TO CRAFT" : "MISSING INGREDIENTS").append("\n");
        sb.append("Crafting Table Required: ").append(requiresCraftingTable ? "YES (3x3 grid)" : "NO (2x2 inventory)").append("\n");

        if (!missingRawIngredients.isEmpty()) {
            sb.append("\nMissing Raw Materials:\n");
            for (Map.Entry<ItemIdentifier, Integer> e : missingRawIngredients.entrySet()) {
                sb.append("  - ").append(e.getValue()).append("x ").append(e.getKey().getDisplayName()).append("\n");
            }
        }

        sb.append("\nRequired Base Raw Materials:\n");
        for (Map.Entry<ItemIdentifier, Integer> e : requiredRawIngredients.entrySet()) {
            sb.append("  * ").append(e.getValue()).append("x ").append(e.getKey().getDisplayName()).append("\n");
        }

        sb.append("\nExecution Steps (").append(steps.size()).append(" total):\n");
        for (int i = 0; i < steps.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }

        if (!intermediateSurplus.isEmpty()) {
            sb.append("\nSurplus Intermediate Products Created:\n");
            for (Map.Entry<ItemIdentifier, Integer> e : intermediateSurplus.entrySet()) {
                sb.append("  + ").append(e.getValue()).append("x ").append(e.getKey().getDisplayName()).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "CraftingPlan[" + targetCount + "x " + targetItem.getDisplayName() + ", steps=" + steps.size() + ", feasible=" + isFeasible() + "]";
    }
}
