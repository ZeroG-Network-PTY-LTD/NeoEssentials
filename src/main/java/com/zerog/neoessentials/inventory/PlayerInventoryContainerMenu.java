package com.zerog.neoessentials.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

public class PlayerInventoryContainerMenu extends AbstractContainerMenu {
    public PlayerInventoryContainerMenu(int id, Inventory ignored, ServerPlayer targetPlayer) {
        super(null, id); // MenuType is null for now, should be registered for full compatibility
        Container targetInventory = targetPlayer.getInventory();
        int slotCount = targetInventory.getContainerSize();

        int x = 8, y = 18;
        for (int i = 0; i < slotCount; i++) {
            int row = i / 9;
            int col = i % 9;
            this.addSlot(new Slot(targetInventory, i, x + col * 18, y + row * 18));
        }
    }

    @Override
    public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player player) {
        return true; // Always valid for viewing
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull net.minecraft.world.entity.player.Player player, int index) {
        // Disable shift-click moving for safety
        return ItemStack.EMPTY;
    }

    public static Component getTitle(ServerPlayer target) {
        return Component.literal(target.getName().getString() + "'s Inventory (Editable)");
    }
}
