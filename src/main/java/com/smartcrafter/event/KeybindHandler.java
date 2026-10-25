package com.smartcrafter.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.smartcrafter.SmartCrafterMod;
import com.smartcrafter.gui.SmartCrafterScreen;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Client keybinding registrations and input polling for SmartCrafter Pro.
 */
public class KeybindHandler {
    private static KeyMapping openGuiKey;
    private static KeyMapping batchCompressKey;
    private static KeyMapping abortCraftKey;

    public static void register() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.smartcrafter.open_gui",
                InputConstants.Type.KEYBOARD,
                GLFW.GLFW_KEY_K,
                KeyMapping.Category.MISC
        ));

        batchCompressKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.smartcrafter.batch_compress",
                InputConstants.Type.KEYBOARD,
                GLFW.GLFW_KEY_B,
                KeyMapping.Category.MISC
        ));

        abortCraftKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.smartcrafter.abort_craft",
                InputConstants.Type.KEYBOARD,
                GLFW.GLFW_KEY_X,
                KeyMapping.Category.MISC
        ));
    }

    public static void handleInput(Minecraft client) {
        if (client.player == null) return;

        // Open GUI
        while (openGuiKey != null && openGuiKey.consumeClick()) {
            if (client.gui != null && client.gui.screen() == null) {
                client.gui.setScreen(new SmartCrafterScreen());
            }
        }

        // Batch Compress
        while (batchCompressKey != null && batchCompressKey.consumeClick()) {
            SmartCrafterMod.getInstance().triggerInstantCompress();
        }

        // Abort Crafting
        while (abortCraftKey != null && abortCraftKey.consumeClick()) {
            SmartCrafterMod.getInstance().abortCrafting();
        }
    }
}
