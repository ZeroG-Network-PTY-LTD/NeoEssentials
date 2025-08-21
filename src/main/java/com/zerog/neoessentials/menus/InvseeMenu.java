package com.zerog.neoessentials.menus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
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
        for (int i = 0; i < targetInventory.getContainerSize(); i++) {
            this.targetInventory.setItem(i, targetInventory.getItem(i));
        }

        // Main inventory slots (0-35)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9 + 9;
                this.addSlot(new Slot(this.targetInventory, slot, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                        return canModify;
                    }
                    @Override
                    public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                        return canModify;
                    }
                });
            }
        }
        // Hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(this.targetInventory, i, 8 + i * 18, 90) {
                @Override
                public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                    return canModify;
                }
                @Override
                public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                    return canModify;
                }
            });
        }
        // Armor slots (36-39)
        for (int i = 0; i < 4; i++) {
            final int armorSlot = 36 + i;
            this.addSlot(new Slot(this.targetInventory, armorSlot, 180, 18 + i * 18) {
                @Override
                public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                    return canModify;
                }
                @Override
                public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                    return canModify;
                }
            });
        }
        // Offhand slot (40)
        this.addSlot(new Slot(this.targetInventory, 40, 180, 90) {
            @Override
            public boolean mayPickup(@javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                return canModify;
            }
            @Override
            public boolean mayPlace(@javax.annotation.Nonnull ItemStack stack) {
                return canModify;
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
