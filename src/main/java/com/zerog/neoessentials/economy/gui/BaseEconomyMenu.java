package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * Base class for economy GUI menus using chest-like interfaces
 */
public abstract class BaseEconomyMenu extends AbstractContainerMenu {
    
    protected final ServerPlayer player;
    protected final Container container;
    protected final int rows;
    protected final ContainerLevelAccess access;
    
    // Callback for when items are clicked
    protected BiConsumer<Integer, ClickType> clickHandler;
    
    public BaseEconomyMenu(MenuType<?> type, int containerId, Inventory playerInventory, int rows) {
        super(type, containerId);
        this.player = (ServerPlayer) playerInventory.player;
        this.rows = rows;
        this.access = ContainerLevelAccess.create(player.level(), player.blockPosition());
        
        // Create virtual container
        this.container = new SimpleContainer(rows * 9) {
            @Override
            public void setChanged() {
                // Do nothing - this is a virtual container
            }
            
            @Override
            public boolean stillValid(@NotNull Player player) {
                return BaseEconomyMenu.this.stillValid(player);
            }
        };
        
        setupSlots(playerInventory);
    }
    
    /**
     * Sets up the slots for this menu
     */
    protected void setupSlots(Inventory playerInventory) {
        // Add container slots
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                addSlot(new EconomySlot(container, index, 8 + col * 18, 18 + row * 18));
            }
        }
        
        // Add player inventory slots
        int playerInvY = 18 + rows * 18 + 14;
        
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
        }
    }
    
    /**
     * Sets the click handler for this menu
     */
    public void setClickHandler(BiConsumer<Integer, ClickType> handler) {
        this.clickHandler = handler;
    }
    
    /**
     * Sets an item in the GUI at the specified slot
     */
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < container.getContainerSize()) {
            container.setItem(slot, item);
        }
    }
    
    /**
     * Gets an item from the GUI at the specified slot
     */
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < container.getContainerSize()) {
            return container.getItem(slot);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Clears all items in the GUI
     */
    public void clear() {
        container.clearContent();
    }
    
    /**
     * Refreshes the menu content
     */
    public abstract void refresh();
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            itemstack = slotItem.copy();
            
            int containerSize = container.getContainerSize();
            
            if (index < containerSize) {
                // Moving from container to player inventory
                if (!this.moveItemStackTo(slotItem, containerSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to container
                if (!this.moveItemStackTo(slotItem, 0, containerSize, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotItem.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }
    
    @Override
    public boolean stillValid(@NotNull Player player) {
        return player == this.player && player.isAlive();
    }
    
    /**
     * Custom slot class that handles clicks
     */
    protected class EconomySlot extends Slot {
        
        public EconomySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        
        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            // Prevent placing items in GUI slots by default
            return false;
        }
        
        @Override
        public boolean mayPickup(@NotNull Player player) {
            // Prevent picking up items from GUI slots by default
            return false;
        }
        
        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
            // Handle click when item is taken
            if (clickHandler != null) {
                clickHandler.accept(this.getSlotIndex(), ClickType.PICKUP);
            }
        }
    }
}
