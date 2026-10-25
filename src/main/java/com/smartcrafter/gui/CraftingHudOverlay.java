package com.smartcrafter.gui;

import com.smartcrafter.engine.transaction.TransactionExecutor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Visual HUD feedback overlay for active crafting transactions.
 * Designed to provide clear progress visibility while remaining non-intrusive.
 */
public class CraftingHudOverlay {
    private final TransactionExecutor executor;

    public CraftingHudOverlay(TransactionExecutor executor) {
        this.executor = executor;
    }

    public void render(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        // Active crafting feedback is primarily conveyed through actionbar overlay messages
        // to prevent screen clutter during intense gameplay or minigames.
    }
}
