package com.smartcrafter.engine.transaction;

import com.smartcrafter.engine.inventory.InventorySnapshot;
import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.ItemStackSim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Concrete client packet dispatcher executing transaction steps
 * through Minecraft's MultiPlayerGameMode and inventory manager.
 */
public class ClientPacketDispatcher implements TransactionExecutor.SlotClickDispatcher {
    private static final ClientPacketDispatcher INSTANCE = new ClientPacketDispatcher();

    public static ClientPacketDispatcher getInstance() {
        return INSTANCE;
    }

    @Override
    public void clickSlot(int slotId, int button, SlotActionType actionType) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || client.player.containerMenu == null) {
            return;
        }

        int containerId = client.player.containerMenu.containerId;
        ContainerInput input;

        switch (actionType) {
            case QUICK_MOVE, TAKE_RESULT -> input = ContainerInput.QUICK_MOVE;
            case PLACE_ONE, PICKUP, RESTORE_CURSOR -> input = ContainerInput.PICKUP;
            default -> input = ContainerInput.PICKUP;
        }

        client.gameMode.handleContainerInput(containerId, slotId, button, input, client.player);
    }

    @Override
    public boolean isContainerOpen() {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.containerMenu != null;
    }

    @Override
    public void playCraftSound() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            client.player.playSound(SoundEvents.ITEM_PICKUP, 0.5f, 1.2f);
        }
    }

    /**
     * Reads current player inventory from client player entity.
     */
    public static InventorySnapshot capturePlayerInventory() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return new InventorySnapshot();
        }

        Inventory inv = client.player.getInventory();
        ItemStackSim[] slots = new ItemStackSim[InventorySnapshot.TOTAL_SLOTS];

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String name = stack.getHoverName().getString();
                slots[i] = new ItemStackSim(ItemIdentifier.of(id, name), stack.getCount());
            } else {
                slots[i] = ItemStackSim.EMPTY;
            }
        }

        return new InventorySnapshot(slots);
    }

    /**
     * Sends an unobtrusive actionbar notification to the player.
     */
    public static void sendFeedback(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(message));
        }
    }
}
