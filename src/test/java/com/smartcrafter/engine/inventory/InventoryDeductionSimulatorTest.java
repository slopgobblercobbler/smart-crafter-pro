package com.smartcrafter.engine.inventory;

import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.ItemStackSim;
import com.smartcrafter.engine.recipe.RecipeGraphSolver;
import com.smartcrafter.engine.recipe.RecipeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryDeductionSimulatorTest {
    private RecipeRegistry registry;
    private RecipeGraphSolver solver;
    private InventoryDeductionSimulator simulator;

    @BeforeEach
    public void setup() {
        registry = new RecipeRegistry();
        solver = new RecipeGraphSolver(registry);
        simulator = new InventoryDeductionSimulator();
    }

    @Test
    @DisplayName("Dry-run simulation succeeds when player has required inventory resources")
    public void testSuccessfulSimulation() {
        ItemIdentifier oakLog = ItemIdentifier.of("minecraft:oak_log");
        ItemIdentifier cobble = ItemIdentifier.of("minecraft:cobblestone");
        ItemIdentifier iron = ItemIdentifier.of("minecraft:iron_ingot");
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone");
        ItemIdentifier piston = ItemIdentifier.of("minecraft:piston");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(oakLog, 48);
        items.put(cobble, 256);
        items.put(iron, 64);
        items.put(redstone, 64);
        InventorySnapshot initial = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(piston, 64, initial);
        assertTrue(plan.isFeasible());

        InventoryDeductionSimulator.SimulationResult result = simulator.simulate(initial, plan);

        assertTrue(result.isSuccess(), "Simulation should succeed without error");
        assertEquals(-1, result.getFailedStepIndex());
        assertNotNull(result.getFinalInventory());

        // Verify final inventory has 64 pistons
        assertEquals(64, result.getFinalInventory().countItem(piston));
        // Verify logs were consumed
        assertEquals(0, result.getFinalInventory().countItem(oakLog));
    }

    @Test
    @DisplayName("Simulation detects inventory overflow when slots are full")
    public void testInventoryOverflowDetection() {
        ItemIdentifier oakLog = ItemIdentifier.of("minecraft:oak_log");
        ItemIdentifier dirt = ItemIdentifier.of("minecraft:dirt");
        ItemIdentifier oakPlanks = ItemIdentifier.of("minecraft:oak_planks");

        // Fill all 36 slots: slot 0 has 64 logs, slots 1-35 have 64 dirt each!
        InventorySnapshot fullInv = new InventorySnapshot();
        fullInv.setSlot(0, new ItemStackSim(oakLog, 64));
        for (int i = 1; i < InventorySnapshot.MAIN_SIZE; i++) {
            fullInv.setSlot(i, new ItemStackSim(dirt, 64));
        }

        // 64 logs -> 256 planks (which require 4 slots, but only 1 slot will be freed by consuming the log stack!)
        CraftingPlan plan = solver.resolvePlan(oakPlanks, 256, fullInv);
        assertTrue(plan.isFeasible(), "Plan is feasible by raw material count");

        InventoryDeductionSimulator.SimulationResult result = simulator.simulate(fullInv, plan);

        assertFalse(result.isSuccess(), "Simulation should catch overflow because 256 planks cannot fit into 1 freed slot");
        assertTrue(result.getErrorMessage().contains("Inventory full") || result.getErrorMessage().contains("Cannot fit"));
    }
}
