package com.smartcrafter.engine.inventory;

import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.CraftingStep;
import com.smartcrafter.engine.model.ItemIdentifier;

import java.util.Map;

/**
 * Simulates step-by-step inventory changes during crafting execution.
 * Verifies that the inventory has sufficient slots to receive intermediate items
 * without overflowing or dropping items onto the floor.
 */
public class InventoryDeductionSimulator {

    public static class SimulationResult {
        private final boolean success;
        private final InventorySnapshot finalInventory;
        private final int failedStepIndex;
        private final String errorMessage;

        public SimulationResult(boolean success, InventorySnapshot finalInventory, int failedStepIndex, String errorMessage) {
            this.success = success;
            this.finalInventory = finalInventory;
            this.failedStepIndex = failedStepIndex;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public InventorySnapshot getFinalInventory() {
            return finalInventory;
        }

        public int getFailedStepIndex() {
            return failedStepIndex;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Executes a dry-run simulation of a crafting plan against the provided inventory snapshot.
     */
    public SimulationResult simulate(InventorySnapshot initialInventory, CraftingPlan plan) {
        if (plan == null) {
            return new SimulationResult(false, initialInventory, -1, "Crafting plan is null");
        }

        InventorySnapshot current = initialInventory.copy();

        for (int i = 0; i < plan.getSteps().size(); i++) {
            CraftingStep step = plan.getSteps().get(i);

            // 1. Deduct inputs
            for (Map.Entry<ItemIdentifier, Integer> input : step.getInputCounts().entrySet()) {
                int available = current.countItem(input.getKey());
                if (available < input.getValue()) {
                    return new SimulationResult(false, current, i,
                            "Missing input " + input.getKey().getDisplayName() +
                            ": requires " + input.getValue() + ", but only " + available + " available.");
                }
                current.deductItem(input.getKey(), input.getValue());
            }

            // 2. Add outputs
            int overflow = current.addItem(step.getRecipe().getOutput(), step.getTotalOutputCount());
            if (overflow > 0) {
                return new SimulationResult(false, current, i,
                        "Inventory full! Cannot fit " + overflow + "x " +
                        step.getRecipe().getOutput().getDisplayName() + " produced at step " + (i + 1));
            }
        }

        return new SimulationResult(true, current, -1, "Simulation completed successfully");
    }

    /**
     * Calculates maximum crafts possible for a given target recipe and available inventory.
     */
    public int calculateMaxCraftable(InventorySnapshot inventory, CraftingPlan singlePlan) {
        if (!singlePlan.isFeasible()) return 0;
        int max = Integer.MAX_VALUE;
        for (Map.Entry<ItemIdentifier, Integer> req : singlePlan.getRequiredRawIngredients().entrySet()) {
            int available = inventory.countItem(req.getKey());
            int neededPerBatch = req.getValue();
            if (neededPerBatch > 0) {
                max = Math.min(max, available / neededPerBatch);
            }
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }
}
