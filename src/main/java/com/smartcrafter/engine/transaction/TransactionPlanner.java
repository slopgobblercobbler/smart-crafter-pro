package com.smartcrafter.engine.transaction;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans safe, low-level slot transaction sequences for crafting steps.
 * Accurately translates high-level recipe ingredients into atomic click packets
 * mapped to Minecraft container slots (3x3 Crafting Table or 2x2 Inventory).
 */
public class TransactionPlanner {

    public enum ContainerType {
        CRAFTING_TABLE_3X3(3, 3, 0, 1, 9, 10, 37),
        PLAYER_INVENTORY_2X2(2, 2, 0, 1, 4, 9, 36);

        final int gridWidth;
        final int gridHeight;
        final int resultSlot;
        final int gridStartSlot;
        final int gridSlotCount;
        final int mainInventoryStart;
        final int hotbarStart;

        ContainerType(int gridWidth, int gridHeight, int resultSlot, int gridStartSlot, int gridSlotCount,
                      int mainInventoryStart, int hotbarStart) {
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
            this.resultSlot = resultSlot;
            this.gridStartSlot = gridStartSlot;
            this.gridSlotCount = gridSlotCount;
            this.mainInventoryStart = mainInventoryStart;
            this.hotbarStart = hotbarStart;
        }

        public int mapPlayerSlot(int playerSlot) {
            if (playerSlot < 0 || playerSlot >= InventorySnapshot.MAIN_SIZE) return -1;
            if (this == CRAFTING_TABLE_3X3) {
                // Hotbar 0..8 -> container 37..45
                if (playerSlot < 9) return hotbarStart + playerSlot;
                // Main inventory 9..35 -> container 10..36
                return mainInventoryStart + (playerSlot - 9);
            } else {
                // 2x2 Inventory: Hotbar 0..8 -> 36..44
                if (playerSlot < 9) return hotbarStart + playerSlot;
                // Main 9..35 -> 9..35
                return playerSlot;
            }
        }
    }

    /**
     * Converts a complete CraftingPlan into a safe sequence of TransactionSteps.
     */
    public List<TransactionStep> planTransactions(CraftingPlan plan, InventorySnapshot initialInventory, ContainerType containerType) {
        List<TransactionStep> steps = new ArrayList<>();
        if (plan == null || !plan.isFeasible() || plan.getSteps().isEmpty()) {
            return steps;
        }

        InventorySnapshot workingInventory = initialInventory.copy();

        for (CraftingStep step : plan.getSteps()) {
            List<TransactionStep> stepTransactions = planStepTransactions(step, workingInventory, containerType);
            steps.addAll(stepTransactions);
        }

        return steps;
    }

    /**
     * Generates slot interactions for an individual CraftingStep.
     */
    public List<TransactionStep> planStepTransactions(CraftingStep step, InventorySnapshot workingInventory, ContainerType containerType) {
        List<TransactionStep> list = new ArrayList<>();
        Recipe recipe = step.getRecipe();
        int batchesRemaining = step.getBatches();

        while (batchesRemaining > 0) {
            // Fill crafting grid
            for (int y = 0; y < recipe.getHeight(); y++) {
                for (int x = 0; x < recipe.getWidth(); x++) {
                    ItemIdentifier ingredient = recipe.getSlotIngredient(x, y);
                    if (ingredient == null || "minecraft:air".equals(ingredient.getId())) {
                        continue;
                    }

                    int gridSlot = containerType.gridStartSlot + (y * containerType.gridWidth + x);

                    // Find player inventory slot containing ingredient
                    List<Integer> slots = workingInventory.findSlotsWithItem(ingredient);
                    if (slots.isEmpty()) {
                        // Inventory sync error or missing item
                        continue;
                    }

                    int sourcePlayerSlot = slots.get(0);
                    int sourceContainerSlot = containerType.mapPlayerSlot(sourcePlayerSlot);

                    // 1. Pick up stack from inventory slot
                    list.add(TransactionStep.pickup(sourceContainerSlot, "Pick up " + ingredient.getDisplayName()));

                    // 2. Place 1 item into grid slot
                    list.add(TransactionStep.placeOne(gridSlot, "Place 1x into grid slot " + gridSlot));

                    // 3. Put remainder back into original source slot
                    list.add(TransactionStep.pickup(sourceContainerSlot, "Return cursor remainder to slot " + sourceContainerSlot));

                    // Update working snapshot
                    workingInventory.deductItem(ingredient, 1);
                }
            }

            // Crafting result is now available in result slot (slot 0)
            list.add(TransactionStep.takeResult("Collect crafted " + recipe.getOutput().getDisplayName() + " from result slot"));
            list.add(TransactionStep.delay("Wait for server slot sync acknowledgment"));

            workingInventory.addItem(recipe.getOutput(), recipe.getOutputCount());
            batchesRemaining--;
        }

        return list;
    }
}
