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
    public PlayerInventoryContainerMenu(int id, Inventory viewerInventory, ServerPlayer targetPlayer) {
        super(null, id); // MenuType is null for custom menu; registered slot layout below
        Container targetInventory = targetPlayer.getInventory();
        int slotCount = targetInventory.getContainerSize();

        // ── Target inventory slots (live — changes are reflected immediately) ──
        int x = 8, y = 18;
        for (int i = 0; i < slotCount; i++) {
            int row = i / 9;
            int col = i % 9;
            this.addSlot(new Slot(targetInventory, i, x + col * 18, y + row * 18));
        }

        // ── Viewer's own inventory slots ──────────────────────────────────────
        // Adding these prevents desync when the viewer tries to move items between
        // the target's inventory and their own (e.g. click-to-transfer).
        int targetRows = (int) Math.ceil(slotCount / 9.0);
        int invY = y + targetRows * 18 + 14;   // 14 px gap between grids
        for (int i = 0; i < 27; i++) {         // main inventory (rows 1-3)
            this.addSlot(new Slot(viewerInventory, i + 9, x + (i % 9) * 18, invY + (i / 9) * 18));
        }
        int hotbarY = invY + 3 * 18 + 4;       // 4 px gap between inventory and hotbar
        for (int i = 0; i < 9; i++) {           // hotbar
            this.addSlot(new Slot(viewerInventory, i, x + i * 18, hotbarY));
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
