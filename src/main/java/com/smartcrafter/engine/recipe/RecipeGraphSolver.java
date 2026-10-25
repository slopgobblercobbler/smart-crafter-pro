package com.smartcrafter.engine.recipe;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.CraftingStep;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.Recipe;

import java.util.*;

/**
 * Recursive Dependency Graph Solver for crafting recipes.
 * Resolves multi-tier recipe trees (e.g., Log -> Planks -> Sticks; Planks + Cobble + Iron + Redstone -> Pistons),
 * reuses existing inventory stocks, tracks surplus yields, and computes missing raw materials.
 */
public class RecipeGraphSolver {
    private final RecipeRegistry registry;

    public RecipeGraphSolver(RecipeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
    }

    public RecipeGraphSolver() {
        this(RecipeRegistry.getInstance());
    }

    /**
     * Resolves a complete CraftingPlan for the target item and count, taking into account
     * intermediate items already present in the player's inventory.
     */
    public CraftingPlan resolvePlan(ItemIdentifier targetItem, int targetCount, InventorySnapshot currentInventory) {
        if (targetItem == null || targetCount <= 0) {
            return new CraftingPlan(targetItem != null ? targetItem : ItemIdentifier.of("minecraft:air"),
                    0, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        InventorySnapshot virtualInv = currentInventory != null ? currentInventory.copy() : new InventorySnapshot();
        Map<ItemIdentifier, Integer> requiredRaw = new LinkedHashMap<>();
        Map<ItemIdentifier, Integer> missingRaw = new LinkedHashMap<>();
        Map<ItemIdentifier, Integer> surplusMap = new LinkedHashMap<>();
        List<CraftingStep> resolvedSteps = new ArrayList<>();
        Set<ItemIdentifier> visiting = new HashSet<>();

        // Resolve graph recursively
        resolveNode(targetItem, targetCount, virtualInv, requiredRaw, missingRaw, surplusMap, resolvedSteps, visiting, 0);

        // Group / merge compatible consecutive steps or keep clean topological sequence
        List<CraftingStep> mergedSteps = optimizeSteps(resolvedSteps);

        return new CraftingPlan(targetItem, targetCount, mergedSteps, requiredRaw, missingRaw, surplusMap);
    }

    private void resolveNode(ItemIdentifier item,
                             int countNeeded,
                             InventorySnapshot virtualInv,
                             Map<ItemIdentifier, Integer> requiredRaw,
                             Map<ItemIdentifier, Integer> missingRaw,
                             Map<ItemIdentifier, Integer> surplusMap,
                             List<CraftingStep> stepCollector,
                             Set<ItemIdentifier> visiting,
                             int depth) {
        if (countNeeded <= 0) return;

        // 1. Check if item can be satisfied by inventory
        int inInventory = virtualInv.countItem(item);
        if (inInventory > 0) {
            int take = Math.min(inInventory, countNeeded);
            virtualInv.deductItem(item, take);
            countNeeded -= take;
        }

        if (countNeeded <= 0) {
            return; // Completely satisfied by existing inventory
        }

        // 2. Prevent infinite cyclic recipe loops
        if (visiting.contains(item)) {
            // Cycle detected: treat as raw material
            recordRawMaterial(item, countNeeded, virtualInv, requiredRaw, missingRaw);
            return;
        }

        Recipe recipe = registry.findRecipeFor(item);
        if (recipe == null) {
            // Base raw material (e.g. Logs, Cobblestone, Iron Ingot, Redstone, Sugar Cane, Gunpowder)
            recordRawMaterial(item, countNeeded, virtualInv, requiredRaw, missingRaw);
            return;
        }

        // 3. Crafting calculation
        visiting.add(item);

        int perCraft = recipe.getOutputCount();
        int batches = (countNeeded + perCraft - 1) / perCraft;
        int totalProduced = batches * perCraft;
        int surplus = totalProduced - countNeeded;

        // If recipe produces surplus, deposit it into virtual inventory for other branches to reuse
        if (surplus > 0) {
            virtualInv.addItem(item, surplus);
            surplusMap.merge(item, surplus, Integer::sum);
        }

        // 4. Recursively resolve required input sub-ingredients (Topological post-order)
        Map<ItemIdentifier, Integer> inputs = recipe.getRequiredIngredientsMap();
        for (Map.Entry<ItemIdentifier, Integer> entry : inputs.entrySet()) {
            ItemIdentifier inputItem = entry.getKey();
            int requiredInputCount = entry.getValue() * batches;

            resolveNode(inputItem, requiredInputCount, virtualInv, requiredRaw, missingRaw, surplusMap, stepCollector, visiting, depth + 1);
        }

        // 5. Append this crafting step (sub-ingredients were already added earlier, so this comes after them)
        stepCollector.add(new CraftingStep(recipe, batches, depth));

        visiting.remove(item);
    }

    private void recordRawMaterial(ItemIdentifier item,
                                   int countNeeded,
                                   InventorySnapshot virtualInv,
                                   Map<ItemIdentifier, Integer> requiredRaw,
                                   Map<ItemIdentifier, Integer> missingRaw) {
        requiredRaw.merge(item, countNeeded, Integer::sum);
        int available = virtualInv.countItem(item);
        if (available < countNeeded) {
            missingRaw.merge(item, countNeeded - available, Integer::sum);
            virtualInv.deductItem(item, available);
        } else {
            virtualInv.deductItem(item, countNeeded);
        }
    }

    private List<CraftingStep> optimizeSteps(List<CraftingStep> steps) {
        // In topological order, merge adjacent steps crafting the same recipe
        List<CraftingStep> result = new ArrayList<>();
        for (CraftingStep step : steps) {
            if (!result.isEmpty()) {
                CraftingStep last = result.get(result.size() - 1);
                if (last.getRecipe().equals(step.getRecipe())) {
                    // Merge batches
                    result.set(result.size() - 1, new CraftingStep(last.getRecipe(), last.getBatches() + step.getBatches(), Math.min(last.getTier(), step.getTier())));
                    continue;
                }
            }
            result.add(step);
        }
        return result;
    }
}
