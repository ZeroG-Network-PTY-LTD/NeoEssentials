package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.ShopEmployeeManager;
import com.zerog.neoessentials.util.LanguageUtil;
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
import java.util.ArrayList;
import java.util.Map;

/**
 * Shop Statistics GUI for NeoEssentials
 * Provides a user interface for viewing shop statistics, sales history, and analytics
 */
public class ShopStatisticsGUI {
    
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopStatisticsGUI(Shop shop, ShopManager shopManager) {
        this.shop = shop;
        this.shopManager = shopManager;
    }
    
    /**
     * Opens the statistics interface
     */
    public void openStatisticsMenu(ServerPlayer player) {
        // Check permissions
        if (!shop.getOwnerId().equals(player.getUUID()) && 
            !shop.getEmployeeManager().hasPermission(player.getUUID(), 
                ShopEmployeeManager.ShopPermission.VIEW_STATS)) {
            LanguageUtil.sendMessage(player, "§cYou don't have permission to view shop statistics!");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupStatisticsMenu(container);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§dStatistics§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopStatisticsMenu(containerId, playerInventory, container, shop, shopManager);
            }
        });
    }
    
    /**
     * Sets up the statistics menu
     */
    private void setupStatisticsMenu(SimpleContainer container) {
        // Calculate statistics
        double totalInventoryValue = 0;
        Map<String, Shop.ShopItem> shopItems = shop.getInventory();
        
        for (Map.Entry<String, Shop.ShopItem> entry : shopItems.entrySet()) {
            Shop.ShopItem item = entry.getValue();
            double price = shop.getItemPrice(entry.getKey());
            totalInventoryValue += item.getQuantity() * price;
        }
        
        // Overall statistics item
        ItemStack overallStats = new ItemStack(Items.DIAMOND);
        overallStats.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§d§lOverall Statistics"));
        
        List<Component> overallLore = new ArrayList<>();
        overallLore.add(Component.literal("§7Total Revenue: §6$" + String.format("%.2f", shop.getTotalRevenue())));
        overallLore.add(Component.literal("§7Total Sales: §a" + shop.getTotalSales()));
        overallLore.add(Component.literal("§7Total Items: §b" + shopItems.size()));
        overallLore.add(Component.literal("§7Shop Status: " + (shop.isActive() ? "§aActive" : "§cInactive")));
        
        overallStats.set(DataComponents.LORE, new ItemLore(overallLore));
        container.setItem(4, overallStats); // Top center
        
        // Inventory value item
        ItemStack inventoryValue = new ItemStack(Items.CHEST);
        inventoryValue.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6§lInventory Value"));
        
        List<Component> inventoryLore = new ArrayList<>();
        inventoryLore.add(Component.literal("§7Total Value: §6$" + String.format("%.2f", totalInventoryValue)));
        inventoryLore.add(Component.literal("§7Average Value: §6$" + 
            String.format("%.2f", shopItems.isEmpty() ? 0 : totalInventoryValue / shopItems.size())));
        
        inventoryValue.set(DataComponents.LORE, new ItemLore(inventoryLore));
        container.setItem(20, inventoryValue);
        
        // Popular items display
        setupPopularItems(container, shopItems);
        
        // Performance metrics
        ItemStack performance = new ItemStack(Items.BOOK);
        performance.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§e§lPerformance"));
        
        List<Component> performanceLore = new ArrayList<>();
        performanceLore.add(Component.literal("§7Revenue/Sale: §6$" + 
            String.format("%.2f", shop.getTotalSales() > 0 ? shop.getTotalRevenue() / shop.getTotalSales() : 0)));
        
        performance.set(DataComponents.LORE, new ItemLore(performanceLore));
        container.setItem(24, performance);
        
        // Add border items
        ItemStack borderItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        borderItem.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        
        // Fill border
        for (int i = 0; i < 9; i++) {
            if (i != 4) container.setItem(i, borderItem); // Top row except center
        }
        for (int i = 45; i < 54; i++) {
            container.setItem(i, borderItem); // Bottom row
        }
        for (int i = 9; i < 45; i += 9) {
            container.setItem(i, borderItem); // Left column
            container.setItem(i + 8, borderItem); // Right column
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Items.BARRIER);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lBack"));
        backItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Return to shop management")
        )));
        
        CompoundTag backData = new CompoundTag();
        backData.putString("Action", "back_to_main");
        backItem.set(DataComponents.CUSTOM_DATA, CustomData.of(backData));
        
        container.setItem(45, backItem);
    }
    
    /**
     * Sets up popular items display
     */
    private void setupPopularItems(SimpleContainer container, Map<String, Shop.ShopItem> shopItems) {
        // Start at slot 28 (middle area)
        int slot = 28;
        int itemCount = 0;
        
        for (Map.Entry<String, Shop.ShopItem> entry : shopItems.entrySet()) {
            if (itemCount >= 8) break; // Limit to 8 items
            
            Shop.ShopItem item = entry.getValue();
            String itemId = entry.getKey();
            double price = shop.getItemPrice(itemId);
            
            // Create a simple display item (since we don't have getItemStack)
            ItemStack displayStack = new ItemStack(Items.PAPER);
            displayStack.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + item.getItemName()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Stock: §a" + item.getQuantity()));
            lore.add(Component.literal("§7Price: §6$" + String.format("%.2f", price)));
            lore.add(Component.literal("§7Item ID: §f" + itemId));
            
            displayStack.set(DataComponents.LORE, new ItemLore(lore));
            
            container.setItem(slot, displayStack);
            
            // Move to next slot, skip borders
            slot++;
            if (slot % 9 == 8) slot += 2; // Skip to next row if at right border
            
            itemCount++;
        }
    }
}
