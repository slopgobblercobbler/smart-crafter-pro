package com.smartcrafter;

import com.smartcrafter.config.SmartCrafterConfig;
import com.smartcrafter.engine.compressor.BatchCompressor;
import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.CraftingPlan;
import com.smartcrafter.engine.recipe.RecipeRegistry;
import com.smartcrafter.engine.transaction.ClientPacketDispatcher;
import com.smartcrafter.engine.transaction.TransactionExecutor;
import com.smartcrafter.engine.transaction.TransactionPlanner;
import com.smartcrafter.engine.transaction.TransactionStep;
import com.smartcrafter.event.ClientTickHandler;
import com.smartcrafter.event.KeybindHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main client entrypoint for SmartCrafter Pro.
 * Provides client-side recursive auto-crafting, recipe graph resolution,
 * anti-desync transaction sequencing, and bulk batch compression.
 */
public class SmartCrafterMod implements ClientModInitializer {
    public static final String MOD_ID = "smartcrafter";
    public static final String MOD_NAME = "SmartCrafter Pro";
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static SmartCrafterMod instance;
    private final TransactionExecutor executor = new TransactionExecutor(ClientPacketDispatcher.getInstance());
    private final TransactionPlanner planner = new TransactionPlanner();
    private final BatchCompressor compressor = new BatchCompressor(RecipeRegistry.getInstance());

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("[{}] Initializing client integration (v{})...", MOD_NAME, MOD_VERSION);

        // Load config
        SmartCrafterConfig config = SmartCrafterConfig.getInstance();
        config.load();
        executor.setDelayTicks(config.getActionDelayTicks());

        // Register event handlers
        KeybindHandler.register();
        ClientTickHandler.register();

        LOGGER.info("[{}] SmartCrafter Pro loaded successfully.", MOD_NAME);
    }

    public static SmartCrafterMod getInstance() {
        return instance;
    }

    public TransactionExecutor getExecutor() {
        return executor;
    }

    public TransactionPlanner getPlanner() {
        return planner;
    }

    public BatchCompressor getCompressor() {
        return compressor;
    }

    /**
     * Translates a resolved CraftingPlan into transaction steps and begins execution.
     */
    public void startCraftingPlan(CraftingPlan plan) {
        if (plan == null || !plan.isFeasible()) {
            ClientPacketDispatcher.sendFeedback("§c[SmartCrafter]§r Cannot start: plan is null or infeasible.");
            return;
        }

        InventorySnapshot inv = ClientPacketDispatcher.capturePlayerInventory();
        TransactionPlanner.ContainerType container = plan.requiresCraftingTable()
                ? TransactionPlanner.ContainerType.CRAFTING_TABLE_3X3
                : TransactionPlanner.ContainerType.PLAYER_INVENTORY_2X2;

        List<TransactionStep> steps = planner.planTransactions(plan, inv, container);
        if (steps.isEmpty()) {
            ClientPacketDispatcher.sendFeedback("§e[SmartCrafter]§r No steps required to craft target.");
            return;
        }

        executor.setDelayTicks(SmartCrafterConfig.getInstance().getActionDelayTicks());
        executor.start(steps, plan.getTargetCount() + "x " + plan.getTargetItem().getDisplayName());
        ClientPacketDispatcher.sendFeedback("§a[SmartCrafter]§r Started crafting " + plan.getTargetCount() +
                "x " + plan.getTargetItem().getDisplayName() + " (" + steps.size() + " actions)...");
    }

    /**
     * Executes immediate bulk compression of inventory contents.
     */
    public void triggerInstantCompress() {
        InventorySnapshot inv = ClientPacketDispatcher.capturePlayerInventory();
        BatchCompressor.CompressionResult result = compressor.planCompression(inv,
                SmartCrafterConfig.getInstance().getDefaultCategory(), true);

        if (!result.hasActions()) {
            ClientPacketDispatcher.sendFeedback("§e[SmartCrafter]§r No compressible items found.");
            return;
        }

        startCraftingPlan(result.getPlan());
        ClientPacketDispatcher.sendFeedback("§a[SmartCrafter]§r Compressing " + result.getTotalItemsCompressed() +
                " items into dense blocks (saving ~" + result.getEstimatedSlotsSaved() + " slots)...");
    }

    /**
     * Halts any active transaction immediately.
     */
    public void abortCrafting() {
        if (executor.isRunning()) {
            executor.abort("Cancelled by user keybind");
            ClientPacketDispatcher.sendFeedback("§c[SmartCrafter]§r Crafting cancelled.");
        }
    }
}
