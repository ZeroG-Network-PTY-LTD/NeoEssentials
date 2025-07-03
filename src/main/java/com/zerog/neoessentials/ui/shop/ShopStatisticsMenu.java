package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;

/**
 * Menu handler for Shop Statistics GUI
 */
public class ShopStatisticsMenu extends AbstractContainerMenu {
    
    private final SimpleContainer container;
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopStatisticsMenu(int containerId, Inventory playerInventory, SimpleContainer container, 
                             Shop shop, ShopManager shopManager) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.shop = shop;
        this.shopManager = shopManager;
        
        // Add container slots
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, row * 9 + col, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Don't allow placing items
                    }
                });
            }
        }
        
        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        
        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY; // Don't allow quick move
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= container.getContainerSize()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        
        ItemStack clickedItem = container.getItem(slotId);
        if (clickedItem.isEmpty() || !clickedItem.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        
        CustomData customData = clickedItem.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        
        CompoundTag actionData = customData.copyTag();
        String action = actionData.getString("Action");
        
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        handleAction(action, actionData, serverPlayer);
    }
    
    /**
     * Handles GUI actions
     */
    private void handleAction(String action, CompoundTag actionData, ServerPlayer player) {
        switch (action) {
            case "back_to_main":
                new ShopManagementGUI(shop, shopManager).openMainMenu(player);
                break;
                
            default:
                LanguageUtil.sendMessage(player, "§cUnknown action: " + action);
                break;
        }
    }
}
