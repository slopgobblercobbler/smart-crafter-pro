package com.smartcrafter.event;

import com.smartcrafter.SmartCrafterMod;
import com.smartcrafter.engine.transaction.ClientPacketDispatcher;
import com.smartcrafter.engine.transaction.TransactionExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Tick listener driving keybind polling, transaction executor stepping,
 * and HUD overlay notifications.
 */
public class ClientTickHandler {
    private static int lastReportedPercent = -1;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickHandler::onTick);
    }

    private static void onTick(Minecraft client) {
        if (client.player == null) return;

        // Poll keybinds
        KeybindHandler.handleInput(client);

        // Drive transaction executor
        TransactionExecutor executor = SmartCrafterMod.getInstance().getExecutor();
        if (executor != null && executor.isRunning()) {
            executor.onTick();

            // Progress notification
            int percent = (int) (executor.getProgressPercentage() * 100);
            if (percent != lastReportedPercent && percent % 20 == 0) {
                lastReportedPercent = percent;
                ClientPacketDispatcher.sendFeedback("§6[SmartCrafter]§r Crafting " + executor.getCurrentTargetName() +
                        " (" + percent + "%)...");
            }

            if (executor.getState() == TransactionExecutor.ExecutionState.COMPLETED) {
                ClientPacketDispatcher.sendFeedback("§a[SmartCrafter]§r Crafting complete!");
                lastReportedPercent = -1;
            } else if (executor.getState() == TransactionExecutor.ExecutionState.ABORTED) {
                ClientPacketDispatcher.sendFeedback("§c[SmartCrafter]§r Crafting aborted.");
                lastReportedPercent = -1;
            }
        }
    }
}
