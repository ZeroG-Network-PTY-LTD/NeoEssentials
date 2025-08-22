package com.zerog.neoessentials.menus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Custom menu for /invsee showing main inventory, armor, and offhand
 */
public class InvseeMenu extends AbstractContainerMenu {
    private final Container targetInventory;
    private final boolean canModify;
    public InvseeMenu(int windowId, Inventory viewerInventory, Inventory targetInventory, boolean canModify) {
        super(MenuType.GENERIC_9x6, windowId);
        this.targetInventory = new SimpleContainer(targetInventory.getContainerSize());
        this.canModify = canModify;
        // Copy items for display
        for (int idx = 0; idx < targetInventory.getContainerSize(); idx++) {
            this.targetInventory.setItem(idx, targetInventory.getItem(idx));
        }
        // Main inventory slots (0-35)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIdx = col + row * 9 + 9;
                this.addSlot(new Slot(this.targetInventory, slotIdx, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                        return InvseeMenu.this.canModify;
                    }
                    @Override
                    public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                        return InvseeMenu.this.canModify;
                    }
                });
            }
        }
        // Hotbar (0-8)
        for (int hotbarIdx = 0; hotbarIdx < 9; hotbarIdx++) {
            this.addSlot(new Slot(this.targetInventory, hotbarIdx, 8 + hotbarIdx * 18, 90) {
                @Override
                public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                    return InvseeMenu.this.canModify;
                }
                @Override
                public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                    return InvseeMenu.this.canModify;
                }
            });
        }
        // Armor slots (36-39)
        for (int armorIdx = 0; armorIdx < 4; armorIdx++) {
            final int armorSlot = 36 + armorIdx;
            this.addSlot(new Slot(this.targetInventory, armorSlot, 180, 18 + armorIdx * 18) {
                @Override
                public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                    return InvseeMenu.this.canModify;
                }
                @Override
                public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                    return InvseeMenu.this.canModify;
                }
            });
        }
        // Offhand slot (40)
        this.addSlot(new Slot(this.targetInventory, 40, 180, 90) {
            @Override
            public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                return InvseeMenu.this.canModify;
            }
            @Override
            public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                return InvseeMenu.this.canModify;
            }
        });
    }

    @Override
    public boolean stillValid(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player, int index) {
        return ItemStack.EMPTY;
    }
}
