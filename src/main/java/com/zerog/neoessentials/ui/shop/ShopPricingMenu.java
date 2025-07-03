package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Container menu for shop pricing management interface
 */
public class ShopPricingMenu extends AbstractContainerMenu {
    
    private final Container container;
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopPricingMenu(int containerId, Inventory playerInventory, Container container, 
                         Shop shop, ShopManager shopManager) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.shop = shop;
        this.shopManager = shopManager;
        
        // Add container slots
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // GUI items cannot be moved
                    }
                });
            }
        }
        
        // Add player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + 36));
            }
        }
        
        // Add player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142 + 36));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // Disable shift-clicking
    }
    
    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < container.getContainerSize()) {
            ItemStack clickedItem = container.getItem(slotId);
            
            // Check if the item has an action
            if (clickedItem.has(DataComponents.CUSTOM_DATA)) {
                CustomData customData = clickedItem.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("Action")) {
                        handleActionClick(tag.getString("Action"), tag, (ServerPlayer) player);
                        return;
                    }
                }
            }
        }
        
        // For other slots (like player inventory), allow normal behavior
        if (slotId >= container.getContainerSize()) {
            super.clicked(slotId, button, clickType, player);
        }
    }
    
    /**
     * Handles action clicks from GUI items
     */
    private void handleActionClick(String action, CompoundTag actionData, ServerPlayer player) {
        switch (action) {
            case "back_to_main":
                new ShopManagementGUI(shop, shopManager).openMainMenu(player);
                break;
                
            case "show_pricing_command":
                showPricingCommand(player, actionData);
                break;
                
            default:
                LanguageUtil.sendMessage(player, "§cUnknown action: " + action);
                break;
        }
    }
    
    /**
     * Shows the pricing command for a specific item
     */
    private void showPricingCommand(ServerPlayer player, CompoundTag actionData) {
        String itemName = actionData.getString("ItemName");
        double buyPrice = actionData.getDouble("BuyPrice");
        double sellPrice = actionData.getDouble("SellPrice");
        
        LanguageUtil.sendMessage(player, "§6§l=== Pricing for " + itemName + " ===");
        LanguageUtil.sendMessage(player, "§7Current Buy Price: §6$" + String.format("%.2f", buyPrice));
        LanguageUtil.sendMessage(player, "§7Current Sell Price: §6$" + String.format("%.2f", sellPrice));
        LanguageUtil.sendMessage(player, "");
        LanguageUtil.sendMessage(player, "§eCommands to modify pricing:");
        LanguageUtil.sendMessage(player, "§f/shop setprice \"" + shop.getShopName() + "\" \"" + itemName + "\" <buy-price> [sell-price]");
        LanguageUtil.sendMessage(player, "");
        LanguageUtil.sendMessage(player, "§7Example:");
        LanguageUtil.sendMessage(player, "§f/shop setprice \"" + shop.getShopName() + "\" \"" + itemName + "\" " + 
            String.format("%.2f", buyPrice * 1.1) + " " + String.format("%.2f", sellPrice * 0.9));
        LanguageUtil.sendMessage(player, "§8(Sets buy price 10% higher, sell price 10% lower)");
    }
}
