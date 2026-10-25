package com.smartcrafter.engine.recipe;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.CraftingStep;
import com.smartcrafter.engine.model.ItemIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeGraphSolverTest {
    private RecipeRegistry registry;
    private RecipeGraphSolver solver;

    @BeforeEach
    public void setup() {
        registry = new RecipeRegistry();
        solver = new RecipeGraphSolver(registry);
    }

    @Test
    @DisplayName("Single tier recipe resolution: 1 Oak Log -> 4 Oak Planks")
    public void testSingleTierPlanksResolution() {
        ItemIdentifier oakLog = ItemIdentifier.of("minecraft:oak_log");
        ItemIdentifier oakPlanks = ItemIdentifier.of("minecraft:oak_planks");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(oakLog, 16);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(oakPlanks, 16, inv);

        assertNotNull(plan);
        assertTrue(plan.isFeasible(), "Plan should be feasible with 16 logs in inventory");
        assertEquals(1, plan.getSteps().size(), "Should have exactly 1 crafting step");

        CraftingStep step = plan.getSteps().get(0);
        assertEquals(4, step.getBatches(), "16 planks requires 4 batches of 4");
        assertEquals(16, step.getTotalOutputCount());
        assertEquals(4, step.getInputCounts().get(oakLog).intValue(), "Consumes 4 logs");
        assertFalse(plan.requiresCraftingTable(), "Planks can be crafted in 2x2 grid");
    }

    @Test
    @DisplayName("Recursive multi-tier resolution: Wood -> Planks, + Cobble, Iron, Redstone -> 64 Pistons")
    public void testMultiTierPistonCraftingResolution() {
        ItemIdentifier oakLog = ItemIdentifier.of("minecraft:oak_log");
        ItemIdentifier cobblestone = ItemIdentifier.of("minecraft:cobblestone");
        ItemIdentifier ironIngot = ItemIdentifier.of("minecraft:iron_ingot");
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone");
        ItemIdentifier piston = ItemIdentifier.of("minecraft:piston");

        // Player has: 48 Oak Logs, 256 Cobblestone, 64 Iron Ingot, 64 Redstone Dust
        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(oakLog, 48);
        items.put(cobblestone, 256);
        items.put(ironIngot, 64);
        items.put(redstone, 64);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(piston, 64, inv);

        assertNotNull(plan);
        assertTrue(plan.isFeasible(), "Plan should be completely feasible with supplied raw materials");
        assertTrue(plan.requiresCraftingTable(), "Pistons require 3x3 crafting table");
        assertEquals(2, plan.getSteps().size(), "Must have 2 steps: (1) Craft Planks, (2) Craft Pistons");

        // Step 1: Craft Planks from Logs (64 pistons * 3 planks = 192 planks; 192 / 4 = 48 batches)
        CraftingStep step1 = plan.getSteps().get(0);
        assertEquals("minecraft:oak_planks", step1.getRecipe().getOutput().getId());
        assertEquals(48, step1.getBatches());
        assertEquals(192, step1.getTotalOutputCount());

        // Step 2: Craft Pistons
        CraftingStep step2 = plan.getSteps().get(1);
        assertEquals("minecraft:piston", step2.getRecipe().getOutput().getId());
        assertEquals(64, step2.getBatches());
        assertEquals(64, step2.getTotalOutputCount());
        assertEquals(192, step2.getInputCounts().get(ItemIdentifier.of("minecraft:oak_planks")).intValue());
        assertEquals(256, step2.getInputCounts().get(cobblestone).intValue());
        assertEquals(64, step2.getInputCounts().get(ironIngot).intValue());
        assertEquals(64, step2.getInputCounts().get(redstone).intValue());
    }

    @Test
    @DisplayName("Observer crafting resolution: 128 Observers from Cobble, Redstone, Quartz")
    public void testObserverCraftingResolution() {
        ItemIdentifier cobblestone = ItemIdentifier.of("minecraft:cobblestone");
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone");
        ItemIdentifier quartz = ItemIdentifier.of("minecraft:quartz");
        ItemIdentifier observer = ItemIdentifier.of("minecraft:observer");

        // 128 Observers require: 128*6=768 Cobble, 128*2=256 Redstone, 128*1=128 Quartz
        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(cobblestone, 768);
        items.put(redstone, 256);
        items.put(quartz, 128);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(observer, 128, inv);

        assertNotNull(plan);
        assertTrue(plan.isFeasible());
        assertEquals(1, plan.getSteps().size());
        CraftingStep step = plan.getSteps().get(0);
        assertEquals(128, step.getBatches());
        assertEquals(128, step.getTotalOutputCount());
        assertEquals(768, step.getInputCounts().get(cobblestone).intValue());
        assertEquals(256, step.getInputCounts().get(redstone).intValue());
        assertEquals(128, step.getInputCounts().get(quartz).intValue());
    }

    @Test
    @DisplayName("Firework Rockets resolution: Sugar Cane -> Paper, + Gunpowder -> 64 Fireworks")
    public void testFireworksCraftingResolution() {
        ItemIdentifier sugarCane = ItemIdentifier.of("minecraft:sugar_cane");
        ItemIdentifier gunpowder = ItemIdentifier.of("minecraft:gunpowder");
        ItemIdentifier fireworks = ItemIdentifier.of("minecraft:firework_rocket");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(sugarCane, 64);
        items.put(gunpowder, 64);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(fireworks, 64, inv);

        assertNotNull(plan);
        assertTrue(plan.isFeasible());
        assertEquals(2, plan.getSteps().size(), "Step 1: Paper, Step 2: Firework Rockets");

        // 64 fireworks / 3 = 22 batches = 66 fireworks produced
        CraftingStep fireworksStep = plan.getSteps().get(1);
        assertEquals(22, fireworksStep.getBatches());
        assertEquals(66, fireworksStep.getTotalOutputCount());

        // 22 paper needed / 3 = 8 batches = 24 paper produced
        CraftingStep paperStep = plan.getSteps().get(0);
        assertEquals("minecraft:paper", paperStep.getRecipe().getOutput().getId());
        assertEquals(8, paperStep.getBatches());
        assertEquals(24, paperStep.getTotalOutputCount());
    }

    @Test
    @DisplayName("Intermediate item reuse: does not over-craft when player already has intermediate items")
    public void testIntermediateInventoryReuse() {
        ItemIdentifier oakLog = ItemIdentifier.of("minecraft:oak_log");
        ItemIdentifier oakPlanks = ItemIdentifier.of("minecraft:oak_planks");
        ItemIdentifier cobblestone = ItemIdentifier.of("minecraft:cobblestone");
        ItemIdentifier ironIngot = ItemIdentifier.of("minecraft:iron_ingot");
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone");
        ItemIdentifier piston = ItemIdentifier.of("minecraft:piston");

        // 10 Pistons require 30 Planks.
        // Player already has 20 Planks and 10 Logs.
        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(oakPlanks, 20);
        items.put(oakLog, 10);
        items.put(cobblestone, 100);
        items.put(ironIngot, 50);
        items.put(redstone, 50);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        CraftingPlan plan = solver.resolvePlan(piston, 10, inv);

        assertTrue(plan.isFeasible());
        CraftingStep plankStep = plan.getSteps().get(0);
        // Deficit: 30 needed - 20 in inventory = 10 planks needed.
        // 10 / 4 = 3 batches = 12 planks crafted (surplus: 2 planks).
        assertEquals(3, plankStep.getBatches(), "Should only craft 3 batches of planks to cover the 10 plank deficit");
        assertEquals(12, plankStep.getTotalOutputCount());
        assertEquals(3, plankStep.getInputCounts().get(oakLog).intValue(), "Consumes only 3 logs instead of 8");
    }

    @Test
    @DisplayName("Accurate missing raw ingredient reporting when inventory is deficient")
    public void testMissingRawIngredientsReporting() {
        ItemIdentifier piston = ItemIdentifier.of("minecraft:piston");
        InventorySnapshot emptyInv = new InventorySnapshot();

        CraftingPlan plan = solver.resolvePlan(piston, 64, emptyInv);

        assertFalse(plan.isFeasible(), "Plan must not be feasible with empty inventory");
        Map<ItemIdentifier, Integer> missing = plan.getMissingRawIngredients();

        assertEquals(48, missing.get(ItemIdentifier.of("minecraft:oak_log")).intValue(), "Missing 48 logs");
        assertEquals(256, missing.get(ItemIdentifier.of("minecraft:cobblestone")).intValue(), "Missing 256 cobble");
        assertEquals(64, missing.get(ItemIdentifier.of("minecraft:iron_ingot")).intValue(), "Missing 64 iron");
        assertEquals(64, missing.get(ItemIdentifier.of("minecraft:redstone")).intValue(), "Missing 64 redstone");
    }
}
