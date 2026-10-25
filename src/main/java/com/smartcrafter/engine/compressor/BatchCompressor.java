package com.smartcrafter.engine.compressor;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.CraftingStep;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.Recipe;
import com.smartcrafter.engine.recipe.RecipeRegistry;

import java.util.*;

/**
 * Engine for one-click bulk compression of inventory contents.
 * Automatically condenses loose nuggets, ingots, gems, and farm products
 * into dense blocks (e.g. Iron Nuggets -> Iron Ingots -> Iron Blocks).
 */
public class BatchCompressor {
    private final RecipeRegistry registry;

    public static class CompressionResult {
        private final CraftingPlan plan;
        private final int totalItemsCompressed;
        private final int totalDenseBlocksProduced;
        private final int estimatedSlotsSaved;

        public CompressionResult(CraftingPlan plan, int totalItemsCompressed, int totalDenseBlocksProduced, int estimatedSlotsSaved) {
            this.plan = plan;
            this.totalItemsCompressed = totalItemsCompressed;
            this.totalDenseBlocksProduced = totalDenseBlocksProduced;
            this.estimatedSlotsSaved = estimatedSlotsSaved;
        }

        public CraftingPlan getPlan() {
            return plan;
        }

        public int getTotalItemsCompressed() {
            return totalItemsCompressed;
        }

        public int getTotalDenseBlocksProduced() {
            return totalDenseBlocksProduced;
        }

        public int getEstimatedSlotsSaved() {
            return estimatedSlotsSaved;
        }

        public boolean hasActions() {
            return plan != null && !plan.getSteps().isEmpty();
        }
    }

    public BatchCompressor(RecipeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
    }

    public BatchCompressor() {
        this(RecipeRegistry.getInstance());
    }

    /**
     * Scans inventory snapshot and calculates a complete recursive compression plan.
     */
    public CompressionResult planCompression(InventorySnapshot inventory, CompressionCategory category, boolean recursive) {
        if (inventory == null) {
            return new CompressionResult(new CraftingPlan(ItemIdentifier.of("minecraft:air"), 0,
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()), 0, 0, 0);
        }

        InventorySnapshot workingInv = inventory.copy();
        List<CraftingStep> allSteps = new ArrayList<>();
        int totalItemsCondensed = 0;
        int totalBlocksFormed = 0;

        // Compression pass list (ordered tier 1 -> tier 2)
        List<CompressionDefinition> definitions = getDefinitions(category);

        boolean progressed;
        int maxPasses = recursive ? 3 : 1;
        int passes = 0;

        do {
            progressed = false;
            passes++;

            for (CompressionDefinition def : definitions) {
                int count = workingInv.countItem(def.input);
                int batchSize = def.ratio; // 9 for 3x3, 4 for 2x2
                if (count >= batchSize) {
                    int batches = count / batchSize;
                    int consumed = batches * batchSize;
                    int produced = batches * def.recipe.getOutputCount();

                    workingInv.deductItem(def.input, consumed);
                    workingInv.addItem(def.output, produced);

                    allSteps.add(new CraftingStep(def.recipe, batches, passes - 1));
                    totalItemsCondensed += consumed;
                    totalBlocksFormed += produced;
                    progressed = true;
                }
            }
        } while (recursive && progressed && passes < maxPasses);

        Map<ItemIdentifier, Integer> requiredRaw = new LinkedHashMap<>();
        for (CraftingStep step : allSteps) {
            for (Map.Entry<ItemIdentifier, Integer> entry : step.getInputCounts().entrySet()) {
                requiredRaw.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        int slotsBefore = countOccupiedSlots(inventory);
        int slotsAfter = countOccupiedSlots(workingInv);
        int slotsSaved = Math.max(0, slotsBefore - slotsAfter);

        ItemIdentifier summaryItem = ItemIdentifier.of("smartcrafter:batch_compression_pack", "Batch Compressed Materials");
        CraftingPlan plan = new CraftingPlan(summaryItem, totalBlocksFormed, allSteps, requiredRaw, Collections.emptyMap(), Collections.emptyMap());

        return new CompressionResult(plan, totalItemsCondensed, totalBlocksFormed, slotsSaved);
    }

    private int countOccupiedSlots(InventorySnapshot inv) {
        int occupied = 0;
        for (int i = 0; i < InventorySnapshot.MAIN_SIZE; i++) {
            if (!inv.getSlot(i).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    private static class CompressionDefinition {
        final ItemIdentifier input;
        final ItemIdentifier output;
        final int ratio;
        final Recipe recipe;
        final CompressionCategory category;

        CompressionDefinition(ItemIdentifier input, ItemIdentifier output, int ratio, Recipe recipe, CompressionCategory category) {
            this.input = input;
            this.output = output;
            this.ratio = ratio;
            this.recipe = recipe;
            this.category = category;
        }
    }

    private List<CompressionDefinition> getDefinitions(CompressionCategory filter) {
        List<CompressionDefinition> list = new ArrayList<>();

        // Helper
        addDef(list, filter, "minecraft:iron_nugget", "minecraft:iron_ingot", 9, "minecraft:iron_ingot_from_nuggets", CompressionCategory.METALS);
        addDef(list, filter, "minecraft:gold_nugget", "minecraft:gold_ingot", 9, "minecraft:gold_ingot_from_nuggets", CompressionCategory.METALS);

        addDef(list, filter, "minecraft:iron_ingot", "minecraft:iron_block", 9, "minecraft:iron_block", CompressionCategory.METALS);
        addDef(list, filter, "minecraft:gold_ingot", "minecraft:gold_block", 9, "minecraft:gold_block", CompressionCategory.METALS);
        addDef(list, filter, "minecraft:copper_ingot", "minecraft:copper_block", 9, "minecraft:copper_block", CompressionCategory.METALS);
        addDef(list, filter, "minecraft:netherite_ingot", "minecraft:netherite_block", 9, "minecraft:netherite_block", CompressionCategory.METALS);

        addDef(list, filter, "minecraft:raw_iron", "minecraft:raw_iron_block", 9, "minecraft:raw_iron_block", CompressionCategory.METALS);
        addDef(list, filter, "minecraft:raw_gold", "minecraft:raw_gold_block", 9, "minecraft:raw_gold_block", CompressionCategory.METALS);
        addDef(list, filter, "minecraft:raw_copper", "minecraft:raw_copper_block", 9, "minecraft:raw_copper_block", CompressionCategory.METALS);

        addDef(list, filter, "minecraft:diamond", "minecraft:diamond_block", 9, "minecraft:diamond_block", CompressionCategory.GEMS);
        addDef(list, filter, "minecraft:emerald", "minecraft:emerald_block", 9, "minecraft:emerald_block", CompressionCategory.GEMS);
        addDef(list, filter, "minecraft:quartz", "minecraft:quartz_block", 4, "minecraft:quartz_block", CompressionCategory.GEMS);
        addDef(list, filter, "minecraft:amethyst_shard", "minecraft:amethyst_block", 4, "minecraft:amethyst_block", CompressionCategory.GEMS);

        addDef(list, filter, "minecraft:redstone", "minecraft:redstone_block", 9, "minecraft:redstone_block", CompressionCategory.MINERALS);
        addDef(list, filter, "minecraft:lapis_lazuli", "minecraft:lapis_block", 9, "minecraft:lapis_block", CompressionCategory.MINERALS);
        addDef(list, filter, "minecraft:coal", "minecraft:coal_block", 9, "minecraft:coal_block", CompressionCategory.MINERALS);
        addDef(list, filter, "minecraft:glowstone_dust", "minecraft:glowstone", 4, "minecraft:glowstone", CompressionCategory.MINERALS);

        addDef(list, filter, "minecraft:wheat", "minecraft:hay_block", 9, "minecraft:hay_block", CompressionCategory.CROPS);
        addDef(list, filter, "minecraft:bone_meal", "minecraft:bone_block", 9, "minecraft:bone_block", CompressionCategory.CROPS);

        addDef(list, filter, "minecraft:slime_ball", "minecraft:slime_block", 9, "minecraft:slime_block", CompressionCategory.MOB_DROPS);

        return list;
    }

    private void addDef(List<CompressionDefinition> list, CompressionCategory filter,
                        String inputId, String outputId, int ratio, String recipeId, CompressionCategory cat) {
        if (filter != CompressionCategory.ALL && filter != cat) {
            return;
        }
        Recipe r = registry.getRecipeById(recipeId);
        if (r != null) {
            list.add(new CompressionDefinition(ItemIdentifier.of(inputId), ItemIdentifier.of(outputId), ratio, r, cat));
        }
    }
}
