package com.smartcrafter.gui;

import com.smartcrafter.SmartCrafterMod;
import com.smartcrafter.engine.compressor.BatchCompressor;
import com.smartcrafter.engine.compressor.CompressionCategory;
import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.Recipe;
import com.smartcrafter.engine.recipe.RecipeGraphSolver;
import com.smartcrafter.engine.recipe.RecipeRegistry;
import com.smartcrafter.engine.transaction.ClientPacketDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Client GUI for SmartCrafter Pro.
 * Allows searching recipes, specifying target quantities, visualizing the resolved
 * tree graph and deficit breakdown, and triggering recursive crafting or batch compression.
 */
public class SmartCrafterScreen extends Screen {
    private final RecipeRegistry registry = RecipeRegistry.getInstance();
    private final RecipeGraphSolver solver = new RecipeGraphSolver(registry);
    private final BatchCompressor compressor = new BatchCompressor(registry);

    private EditBox searchBox;
    private final List<Recipe> matchingRecipes = new ArrayList<>();
    private Recipe selectedRecipe;
    private int targetQuantity = 1;
    private CraftingPlan currentPlan;
    private CompressionCategory selectedCategory = CompressionCategory.ALL;

    private Button craftButton;
    private Button compressButton;
    private Button categoryButton;
    private Button qtyMinusBtn;
    private Button qtyPlusBtn;
    private Button qtyPlus16Btn;
    private Button qtyPlus64Btn;
    private Button qtyMaxBtn;

    public SmartCrafterScreen() {
        super(Component.literal("SmartCrafter Pro"));
    }

    @Override
    protected void init() {
        super.init();
        matchingRecipes.clear();
        matchingRecipes.addAll(registry.getAllRecipes());
        if (!matchingRecipes.isEmpty() && selectedRecipe == null) {
            selectedRecipe = matchingRecipes.get(0);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Search Box (Top Center)
        searchBox = new EditBox(this.font, centerX - 120, 20, 240, 20, Component.literal("Search..."));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        // Quantity controls
        int qtyY = centerY - 30;
        qtyMinusBtn = this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> adjustQuantity(-1))
                .bounds(centerX - 120, qtyY, 30, 20).build());
        qtyPlusBtn = this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> adjustQuantity(1))
                .bounds(centerX - 85, qtyY, 30, 20).build());
        qtyPlus16Btn = this.addRenderableWidget(Button.builder(Component.literal("+16"), b -> adjustQuantity(16))
                .bounds(centerX - 50, qtyY, 35, 20).build());
        qtyPlus64Btn = this.addRenderableWidget(Button.builder(Component.literal("+64"), b -> adjustQuantity(64))
                .bounds(centerX - 10, qtyY, 35, 20).build());
        qtyMaxBtn = this.addRenderableWidget(Button.builder(Component.literal("Max"), b -> setMaxQuantity())
                .bounds(centerX + 30, qtyY, 40, 20).build());

        // Craft Action Button
        craftButton = this.addRenderableWidget(Button.builder(Component.literal("Start Crafting"), b -> onCraftClicked())
                .bounds(centerX - 120, centerY + 20, 115, 22).build());

        // Batch Compress Button
        compressButton = this.addRenderableWidget(Button.builder(Component.literal("Compress Inventory"), b -> onCompressClicked())
                .bounds(centerX + 5, centerY + 20, 115, 22).build());

        // Category Filter Toggle Button
        categoryButton = this.addRenderableWidget(Button.builder(Component.literal("Cat: " + selectedCategory.name()), b -> cycleCategory())
                .bounds(centerX - 120, centerY + 50, 115, 20).build());

        // Close Button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(centerX + 5, centerY + 50, 115, 20).build());

        recalculatePlan();
    }

    private void onSearchChanged(String text) {
        matchingRecipes.clear();
        String query = text.toLowerCase().trim();
        for (Recipe r : registry.getAllRecipes()) {
            if (r.getOutput().getDisplayName().toLowerCase().contains(query) ||
                r.getOutput().getId().toLowerCase().contains(query)) {
                matchingRecipes.add(r);
            }
        }
        if (!matchingRecipes.isEmpty()) {
            selectedRecipe = matchingRecipes.get(0);
        } else {
            selectedRecipe = null;
        }
        recalculatePlan();
    }

    private void adjustQuantity(int delta) {
        this.targetQuantity = Math.max(1, Math.min(1024, this.targetQuantity + delta));
        recalculatePlan();
    }

    private void setMaxQuantity() {
        if (selectedRecipe == null) return;
        InventorySnapshot inv = ClientPacketDispatcher.capturePlayerInventory();
        CraftingPlan singlePlan = solver.resolvePlan(selectedRecipe.getOutput(), selectedRecipe.getOutputCount(), inv);
        int max = 0;
        for (int q = 64; q >= 1; q--) {
            CraftingPlan test = solver.resolvePlan(selectedRecipe.getOutput(), q * selectedRecipe.getOutputCount(), inv);
            if (test.isFeasible()) {
                max = q * selectedRecipe.getOutputCount();
                break;
            }
        }
        this.targetQuantity = Math.max(1, max);
        recalculatePlan();
    }

    private void cycleCategory() {
        CompressionCategory[] cats = CompressionCategory.values();
        int next = (selectedCategory.ordinal() + 1) % cats.length;
        selectedCategory = cats[next];
        if (categoryButton != null) {
            categoryButton.setMessage(Component.literal("Cat: " + selectedCategory.name()));
        }
    }

    private void recalculatePlan() {
        if (selectedRecipe == null) {
            currentPlan = null;
            if (craftButton != null) craftButton.active = false;
            return;
        }

        InventorySnapshot inv = ClientPacketDispatcher.capturePlayerInventory();
        currentPlan = solver.resolvePlan(selectedRecipe.getOutput(), targetQuantity, inv);
        if (craftButton != null) {
            craftButton.active = currentPlan.isFeasible() && !currentPlan.getSteps().isEmpty();
            craftButton.setMessage(Component.literal(currentPlan.isFeasible() ? "Craft " + targetQuantity + "x" : "Missing Ingredients"));
        }
    }

    private void onCraftClicked() {
        if (currentPlan == null || !currentPlan.isFeasible()) return;
        SmartCrafterMod.getInstance().startCraftingPlan(currentPlan);
        onClose();
    }

    private void onCompressClicked() {
        InventorySnapshot inv = ClientPacketDispatcher.capturePlayerInventory();
        BatchCompressor.CompressionResult result = compressor.planCompression(inv, selectedCategory, true);
        if (result.hasActions()) {
            SmartCrafterMod.getInstance().startCraftingPlan(result.getPlan());
            ClientPacketDispatcher.sendFeedback("§a[SmartCrafter]§r Compressing " + result.getTotalItemsCompressed() +
                    " items (saving ~" + result.getEstimatedSlotsSaved() + " slots)...");
        } else {
            ClientPacketDispatcher.sendFeedback("§e[SmartCrafter]§r No compressible items found in inventory.");
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        extractMenuBackground(extractor);
        super.extractRenderState(extractor, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
