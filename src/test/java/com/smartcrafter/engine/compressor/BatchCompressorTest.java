package com.smartcrafter.engine.compressor;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.recipe.RecipeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BatchCompressorTest {
    private RecipeRegistry registry;
    private BatchCompressor compressor;

    @BeforeEach
    public void setup() {
        registry = new RecipeRegistry();
        compressor = new BatchCompressor(registry);
    }

    @Test
    @DisplayName("Recursive compression: 81 Iron Nuggets -> 9 Iron Ingots -> 1 Iron Block")
    public void testRecursiveNuggetToBlockCompression() {
        ItemIdentifier ironNugget = ItemIdentifier.of("minecraft:iron_nugget");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(ironNugget, 81);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        BatchCompressor.CompressionResult result = compressor.planCompression(inv, CompressionCategory.ALL, true);

        assertNotNull(result);
        assertTrue(result.hasActions());
        assertEquals(2, result.getPlan().getSteps().size(), "Should have 2 steps: Nuggets->Ingots, Ingots->Block");

        // Step 1: 81 nuggets -> 9 ingots
        assertEquals("minecraft:iron_ingot", result.getPlan().getSteps().get(0).getRecipe().getOutput().getId());
        assertEquals(9, result.getPlan().getSteps().get(0).getBatches());

        // Step 2: 9 ingots -> 1 block
        assertEquals("minecraft:iron_block", result.getPlan().getSteps().get(1).getRecipe().getOutput().getId());
        assertEquals(1, result.getPlan().getSteps().get(1).getBatches());

        // Total items consumed: 81 nuggets + 9 ingots = 90
        assertEquals(90, result.getTotalItemsCompressed());
        // Total products: 9 ingots + 1 block = 10
        assertEquals(10, result.getTotalDenseBlocksProduced());
    }

    @Test
    @DisplayName("Bulk mixed resource compression (Wheat, Redstone, Diamond)")
    public void testMixedResourceCompression() {
        ItemIdentifier wheat = ItemIdentifier.of("minecraft:wheat");
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone");
        ItemIdentifier diamond = ItemIdentifier.of("minecraft:diamond");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(wheat, 64);
        items.put(redstone, 128);
        items.put(diamond, 32);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        BatchCompressor.CompressionResult result = compressor.planCompression(inv, CompressionCategory.ALL, false);

        assertTrue(result.hasActions());
        assertEquals(3, result.getPlan().getSteps().size());

        // 64 wheat / 9 = 7 hay bales (63 wheat consumed)
        // 128 redstone / 9 = 14 redstone blocks (126 consumed)
        // 32 diamonds / 9 = 3 diamond blocks (27 consumed)
        int expectedCondensed = 63 + 126 + 27; // 216 items
        assertEquals(expectedCondensed, result.getTotalItemsCompressed());
        assertEquals(7 + 14 + 3, result.getTotalDenseBlocksProduced());
    }

    @Test
    @DisplayName("Category filtering: METALS category only compresses iron/gold/copper")
    public void testCategoryFiltering() {
        ItemIdentifier wheat = ItemIdentifier.of("minecraft:wheat");
        ItemIdentifier ironIngot = ItemIdentifier.of("minecraft:iron_ingot");

        Map<ItemIdentifier, Integer> items = new HashMap<>();
        items.put(wheat, 64);
        items.put(ironIngot, 64);
        InventorySnapshot inv = InventorySnapshot.ofMap(items);

        BatchCompressor.CompressionResult result = compressor.planCompression(inv, CompressionCategory.METALS, false);

        assertTrue(result.hasActions());
        assertEquals(1, result.getPlan().getSteps().size(), "Only iron should be compressed");
        assertEquals("minecraft:iron_block", result.getPlan().getSteps().get(0).getRecipe().getOutput().getId());
        assertEquals(7, result.getPlan().getSteps().get(0).getBatches()); // 7 * 9 = 63 iron ingots
    }
}
