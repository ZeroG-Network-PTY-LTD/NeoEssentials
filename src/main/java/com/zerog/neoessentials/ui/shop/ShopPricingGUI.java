package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.ShopItem;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;

import java.util.List;
import java.util.Map;

/**
 * Shop Pricing Management GUI
 * Allows shop owners to manage item prices and pricing strategies
 */
public class ShopPricingGUI {
    
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopPricingGUI(Shop shop, ShopManager shopManager) {
        this.shop = shop;
        this.shopManager = shopManager;
    }
    
    /**
     * Opens the shop pricing management interface
     */
    public void openPricingMenu(ServerPlayer player) {
        // Check permissions
        if (!shop.getOwnerId().equals(player.getUUID()) && 
            !shop.getEmployeeManager().hasPermission(player.getUUID(), 
                com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
            MessageUtil.sendMessage(player, "§cYou don't have permission to manage this shop's pricing!");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupPricingMenu(container);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§6Shop Pricing§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopPricingMenu(containerId, playerInventory, container, shop, shopManager);
            }
        });
    }
    
    /**
     * Sets up the pricing management menu
     */
    private void setupPricingMenu(SimpleContainer container) {
        // Back button
        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lBack to Main Menu"));
        backItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Return to shop management")
        )));
        
        CompoundTag backData = new CompoundTag();
        backData.putString("Action", "back_to_main");
        backItem.set(DataComponents.CUSTOM_DATA, CustomData.of(backData));
        container.setItem(0, backItem);
        
        // Pricing strategy info
        ItemStack strategyItem = new ItemStack(Items.WRITABLE_BOOK);
        strategyItem.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lPricing Instructions"));
        strategyItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Use these commands to set prices:"),
            Component.literal("§e/shop setprice <shop> <item> <buy> [sell]"),
            Component.literal("§7"),
            Component.literal("§7Click on items below to see current prices"),
            Component.literal("§7and get the exact command to modify them.")
        )));
        container.setItem(4, strategyItem);
        
        // Auto-pricing toggle (future feature)
        ItemStack autoPrice = new ItemStack(Items.REDSTONE);
        autoPrice.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lAuto-Pricing"));
        autoPrice.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Automatically adjust prices"),
            Component.literal("§7based on supply and demand"),
            Component.literal("§c§lComing Soon!")
        )));
        container.setItem(8, autoPrice);
        
        // Display current shop items with pricing info
        Map<String, ShopItem> shopItems = shop.getInventory().getItems();
        int slot = 18; // Start from second row
        
        for (Map.Entry<String, ShopItem> entry : shopItems.entrySet()) {
            if (slot >= 45) break; // Don't fill bottom row
            
            ShopItem shopItem = entry.getValue();
            ItemStack displayStack = shopItem.getItemStack().copy();
            
            // Calculate profit margin
            double margin = shopItem.getSellPrice() > 0 ? 
                ((shopItem.getBuyPrice() - shopItem.getSellPrice()) / shopItem.getBuyPrice()) * 100 : 0;
            
            // Update display name and lore
            displayStack.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + shopItem.getDisplayName()));
            
            displayStack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Stock: §a" + shopItem.getStock()),
                Component.literal("§7Buy Price: §6$" + String.format("%.2f", shopItem.getBuyPrice())),
                Component.literal("§7Sell Price: §6$" + String.format("%.2f", shopItem.getSellPrice())),
                Component.literal("§7Profit Margin: " + 
                    (margin > 0 ? "§a" : margin < 0 ? "§c" : "§7") + 
                    String.format("%.1f%%", margin)),
                Component.literal(""),
                Component.literal("§8Click to get pricing command")
            )));
            
            // Add action data
            CompoundTag itemData = new CompoundTag();
            itemData.putString("Action", "show_pricing_command");
            itemData.putString("ItemId", entry.getKey());
            itemData.putString("ItemName", shopItem.getDisplayName());
            itemData.putDouble("BuyPrice", shopItem.getBuyPrice());
            itemData.putDouble("SellPrice", shopItem.getSellPrice());
            displayStack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData));
            
            container.setItem(slot, displayStack);
            slot++;
        }
        
        // Add information panel at bottom
        ItemStack infoItem = new ItemStack(Items.KNOWLEDGE_BOOK);
        infoItem.set(DataComponents.CUSTOM_NAME, Component.literal("§b§lPricing Tips"));
        infoItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7• Buy Price: What customers pay you"),
            Component.literal("§7• Sell Price: What you pay customers"),
            Component.literal("§7• Set sell price to -1 to disable selling"),
            Component.literal("§7• Higher buy price = more profit"),
            Component.literal("§7• Competitive prices attract customers")
        )));
        container.setItem(49, infoItem);
        
        // Add border items
        ItemStack borderItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        borderItem.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        
        // Fill borders
        for (int i = 9; i < 18; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, borderItem);
            }
        }
        
        for (int i = 45; i < 54; i++) {
            if (i != 49 && container.getItem(i).isEmpty()) { // Skip info item
                container.setItem(i, borderItem);
            }
        }
    }
}
