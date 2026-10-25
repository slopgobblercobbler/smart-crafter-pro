package com.smartcrafter.engine.transaction;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.recipe.RecipeGraphSolver;
import com.smartcrafter.engine.recipe.RecipeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionPlannerTest {
    private RecipeRegistry registry;
    private RecipeGraphSolver solver;
    private TransactionPlanner planner;

    @BeforeEach
    public void setup() {
        registry = new RecipeRegistry();
        solver = new RecipeGraphSolver(registry);
        planner = new TransactionPlanner();
    }

    @Test
    @DisplayName("Container slot mapping matches vanilla Minecraft container layout")
    public void testContainerSlotMapping() {
        TransactionPlanner.ContainerType table = TransactionPlanner.ContainerType.CRAFTING_TABLE_3X3;
        TransactionPlanner.ContainerType inv2x2 = TransactionPlanner.ContainerType.PLAYER_INVENTORY_2X2;

        // 3x3 Crafting Table:
        // Hotbar 0..8 -> 37..45
        assertEquals(37, table.mapPlayerSlot(0));
        assertEquals(45, table.mapPlayerSlot(8));
        // Main Inventory 9..35 -> 10..36
        assertEquals(10, table.mapPlayerSlot(9));
        assertEquals(36, table.mapPlayerSlot(35));

        // 2x2 Player Inventory:
        // Hotbar 0..8 -> 36..44
        assertEquals(36, inv2x2.mapPlayerSlot(0));
        assertEquals(44, inv2x2.mapPlayerSlot(8));
        // Main Inventory 9..35 -> 9..35
        assertEquals(9, inv2x2.mapPlayerSlot(9));
        assertEquals(35, inv2x2.mapPlayerSlot(35));
    }

    @Test
    @DisplayName("Transaction sequence generation for crafting table: pick up, place, return cursor, take result")
    public void testTransactionSequenceGeneration() {
        ItemIdentifier oakPlanks = ItemIdentifier.of("minecraft:oak_planks");
        ItemIdentifier craftingTable = ItemIdentifier.of("minecraft:crafting_table");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(oakPlanks, 4); // Placed in slot 0 (hotbar)
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(craftingTable, 1, inv);
        assertTrue(plan.isFeasible());

        List<TransactionStep> steps = planner.planTransactions(plan, inv, TransactionPlanner.ContainerType.CRAFTING_TABLE_3X3);

        assertNotNull(steps);
        assertFalse(steps.isEmpty());

        // Must end with TAKE_RESULT and DELAY
        TransactionStep takeResultStep = steps.get(steps.size() - 2);
        assertEquals(SlotActionType.TAKE_RESULT, takeResultStep.getActionType());

        TransactionStep delayStep = steps.get(steps.size() - 1);
        assertEquals(SlotActionType.DELAY, delayStep.getActionType());
    }

    @Test
    @DisplayName("TransactionExecutor executes steps with cooldown and completes cleanly")
    public void testTransactionExecutorPacing() {
        List<String> actions = new ArrayList<>();
        boolean[] soundPlayed = new boolean[1];

        TransactionExecutor.SlotClickDispatcher mockDispatcher = new TransactionExecutor.SlotClickDispatcher() {
            @Override
            public void clickSlot(int slotId, int button, SlotActionType actionType) {
                actions.add(actionType + ":" + slotId);
            }

            @Override
            public boolean isContainerOpen() {
                return true;
            }

            @Override
            public void playCraftSound() {
                soundPlayed[0] = true;
            }
        };

        TransactionExecutor executor = new TransactionExecutor(mockDispatcher);
        executor.setDelayTicks(1);

        List<TransactionStep> steps = Arrays.asList(
                TransactionStep.pickup(10, "Pick up item"),
                TransactionStep.placeOne(1, "Place in slot 1"),
                TransactionStep.takeResult("Take result")
        );

        executor.start(steps, "Test Craft");
        assertEquals(TransactionExecutor.ExecutionState.RUNNING, executor.getState());
        assertEquals(3, executor.getTotalSteps());

        // Tick 1: Step 1 (PICKUP) executes, sets cooldown = 1
        executor.onTick();
        assertEquals(1, actions.size());
        assertEquals("PICKUP:10", actions.get(0));

        // Tick 2: In cooldown, nothing executed
        executor.onTick();
        assertEquals(1, actions.size());

        // Tick 3: Step 2 (PLACE_ONE) executes, sets cooldown = 1
        executor.onTick();
        assertEquals(2, actions.size());
        assertEquals("PLACE_ONE:1", actions.get(1));

        // Tick 4: Cooldown
        executor.onTick();

        // Tick 5: Step 3 (TAKE_RESULT) executes
        executor.onTick();
        assertEquals(3, actions.size());

        // Tick 6: Cooldown
        executor.onTick();

        // Tick 7: Queue empty -> Completed & sound played
        executor.onTick();
        assertEquals(TransactionExecutor.ExecutionState.COMPLETED, executor.getState());
        assertTrue(soundPlayed[0], "Completion sound should have been triggered");
    }
}
