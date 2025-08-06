package com.zerog.neoessentials.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Custom GUI Container that handles click actions properly
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class CustomMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomMenu.class);
    
    private final Container container;
    private final int containerRows;
    private final Map<Integer, CustomGuiManager.GuiClickAction> clickActions;
    private final CustomGuiManager.GuiType guiType;
    
    public CustomMenu(int windowId, Inventory playerInventory, Container container, int rows, 
                     Map<Integer, CustomGuiManager.GuiClickAction> clickActions, 
                     CustomGuiManager.GuiType guiType) {
        super(getMenuType(rows), windowId);
        this.container = container;
        this.containerRows = rows;
        this.clickActions = clickActions;
        this.guiType = guiType;
        
        // Add container slots
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new CustomSlot(container, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }
        
        // Add player inventory slots
        int yOffset = 103 + (rows - 4) * 18;
        
        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, yOffset + row * 18));
            }
        }
        
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, yOffset + 58));
        }
    }
    
    private static MenuType<?> getMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
    }
    
    @Override
    public void clicked(int slotIndex, int button, @Nonnull ClickType clickType, @Nonnull Player player) {
        // Handle custom GUI clicks
        if (slotIndex >= 0 && slotIndex < containerRows * 9 && player instanceof ServerPlayer serverPlayer) {
            CustomGuiManager.GuiClickAction action = clickActions.get(slotIndex);
            if (action != null) {
                try {
                    LOGGER.debug("Executing custom click action for slot {} in GUI {}", slotIndex, guiType);
                    action.onClick(serverPlayer);
                    return; // Don't process normal item handling
                } catch (Exception e) {
                    LOGGER.error("Error executing click action for slot {} in GUI {}", slotIndex, guiType, e);
                }
            }
        }
        
        // For non-custom slots or if no action, use default behavior but prevent item taking
        if (slotIndex >= 0 && slotIndex < containerRows * 9) {
            // This is a GUI slot - don't allow normal item manipulation
            return;
        }
        
        // Allow normal inventory management for player inventory slots
        super.clicked(slotIndex, button, clickType, player);
    }
    
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        // Prevent shift-clicking items from GUI to inventory and vice versa
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        return this.container.stillValid(player);
    }
    
    /**
     * Custom slot that prevents item manipulation
     */
    private static class CustomSlot extends Slot {
        public CustomSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        
        @Override
        public boolean mayPickup(@Nonnull Player player) {
            return false; // Prevent taking items
        }
        
        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return false; // Prevent placing items
        }
    }
}
