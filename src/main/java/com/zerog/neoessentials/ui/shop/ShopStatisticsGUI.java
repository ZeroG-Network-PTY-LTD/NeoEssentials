package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.Shop.ShopItem;
import com.zerog.neoessentials.economy.ShopEmployeeManager;
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
 * Shop Statistics GUI
 * Shows shop performance metrics and analytics
 */
public class ShopStatisticsGUI {
    
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopStatisticsGUI(Shop shop, ShopManager shopManager) {
        this.shop = shop;
        this.shopManager = shopManager;
    }
    
    /**
     * Opens the shop statistics interface
     */
    public void openStatisticsMenu(ServerPlayer player) {
        // Check permissions
        if (!shop.getOwnerId().equals(player.getUUID()) && 
            !shop.getEmployeeManager().hasPermission(player.getUUID(), 
                com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.VIEW_STATISTICS)) {
            MessageUtil.sendMessage(player, "§cYou don't have permission to view this shop's statistics!");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupStatisticsMenu(container);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§6Shop Stats§8] §f" + shop.getShopName());
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
        
        // Overall statistics
        ItemStack overallStats = new ItemStack(Items.EMERALD);
        overallStats.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lOverall Statistics"));
        overallStats.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Total Revenue: §6$" + String.format("%.2f", shop.getTotalRevenue())),
            Component.literal("§7Total Sales: §a" + shop.getTotalSales()),
            Component.literal("§7Shop Status: " + (shop.isActive() ? "§aActive" : "§cInactive")),
            Component.literal("§7Shop Type: §f" + shop.getShopType().getDisplayName()),
            Component.literal("§7Total Items: §b" + shop.getInventory().getItems().size())
        )));
        container.setItem(4, overallStats);
        
        // Inventory value
        double totalInventoryValue = calculateInventoryValue();
        ItemStack inventoryValue = new ItemStack(Items.DIAMOND);
        inventoryValue.set(DataComponents.CUSTOM_NAME, Component.literal("§b§lInventory Value"));
        inventoryValue.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Total Stock Value: §6$" + String.format("%.2f", totalInventoryValue)),
            Component.literal("§7Average Item Value: §6$" + 
                String.format("%.2f", shop.getInventory().getItems().isEmpty() ? 0 : 
                    totalInventoryValue / shop.getInventory().getItems().size())),
            Component.literal("§7Click for detailed breakdown")
        )));
        
        CompoundTag valueData = new CompoundTag();
        valueData.putString("Action", "show_inventory_breakdown");
        inventoryValue.set(DataComponents.CUSTOM_DATA, CustomData.of(valueData));
        container.setItem(8, inventoryValue);
        
        // Top selling items section
        ItemStack topSellingTitle = new ItemStack(Items.GOLD_INGOT);
        topSellingTitle.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lTop Performing Items"));
        topSellingTitle.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Items with highest stock levels"),
            Component.literal("§7and best profit margins")
        )));
        container.setItem(22, topSellingTitle);
        
        // Display top items
        Map<String, ShopItem> shopItems = shop.getInventory().getItems();
        int[] topSlots = {28, 29, 30, 32, 33, 34}; // Around the center
        int slotIndex = 0;
        
        for (Map.Entry<String, ShopItem> entry : shopItems.entrySet()) {
            if (slotIndex >= topSlots.length) break;
            
            ShopItem shopItem = entry.getValue();
            ItemStack displayStack = shopItem.getItemStack().copy();
            
            // Calculate profit margin and total value
            double margin = shopItem.getSellPrice() > 0 ? 
                ((shopItem.getBuyPrice() - shopItem.getSellPrice()) / shopItem.getBuyPrice()) * 100 : 0;
            double stockValue = shopItem.getStock() * shopItem.getBuyPrice();
            
            displayStack.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + shopItem.getDisplayName()));
            
            displayStack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Stock: §a" + shopItem.getStock()),
                Component.literal("§7Stock Value: §6$" + String.format("%.2f", stockValue)),
                Component.literal("§7Buy Price: §6$" + String.format("%.2f", shopItem.getBuyPrice())),
                Component.literal("§7Sell Price: §6$" + String.format("%.2f", shopItem.getSellPrice())),
                Component.literal("§7Profit Margin: " + 
                    (margin > 0 ? "§a" : margin < 0 ? "§c" : "§7") + 
                    String.format("%.1f%%", margin))
            )));
            
            container.setItem(topSlots[slotIndex], displayStack);
            slotIndex++;
        }
        
        // Performance indicators
        ItemStack performanceItem = new ItemStack(Items.CLOCK);
        performanceItem.set(DataComponents.CUSTOM_NAME, Component.literal("§d§lPerformance Metrics"));
        performanceItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Average Transaction: §6$" + 
                String.format("%.2f", shop.getTotalSales() > 0 ? shop.getTotalRevenue() / shop.getTotalSales() : 0)),
            Component.literal("§7Revenue per Item Type: §6$" + 
                String.format("%.2f", shop.getInventory().getItems().isEmpty() ? 0 : 
                    shop.getTotalRevenue() / shop.getInventory().getItems().size())),
            Component.literal("§7Shop Efficiency: " + getShopEfficiencyRating())
        )));
        container.setItem(40, performanceItem);
        
        // Tips and recommendations
        ItemStack tipsItem = new ItemStack(Items.KNOWLEDGE_BOOK);
        tipsItem.set(DataComponents.CUSTOM_NAME, Component.literal("§b§lBusiness Tips"));
        tipsItem.set(DataComponents.LORE, new ItemLore(getBusinessTips()));
        container.setItem(49, tipsItem);
        
        // Add border items
        ItemStack borderItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        borderItem.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        
        // Fill borders
        int[] borderSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, borderItem);
            }
        }
    }
    
    /**
     * Calculates total inventory value
     */
    private double calculateInventoryValue() {
        double total = 0.0;
        for (ShopItem item : shop.getInventory().getItems().values()) {
            total += item.getStock() * item.getBuyPrice();
        }
        return total;
    }
    
    /**
     * Gets shop efficiency rating
     */
    private String getShopEfficiencyRating() {
        double revenue = shop.getTotalRevenue();
        int itemTypes = shop.getInventory().getItems().size();
        
        if (revenue > 10000 && itemTypes > 10) return "§a§lExcellent";
        if (revenue > 5000 && itemTypes > 5) return "§6§lGood";
        if (revenue > 1000 && itemTypes > 2) return "§e§lFair";
        return "§c§lNeeds Improvement";
    }
    
    /**
     * Gets business tips based on shop performance
     */
    private List<Component> getBusinessTips() {
        List<Component> tips = List.of(
            Component.literal("§7• Stock popular items in bulk"),
            Component.literal("§7• Set competitive but profitable prices"),
            Component.literal("§7• Monitor your profit margins"),
            Component.literal("§7• Diversify your inventory"),
            Component.literal("§7• Keep your shop active and stocked")
        );
        
        // Add specific tips based on shop state
        double revenue = shop.getTotalRevenue();
        if (revenue < 1000) {
            return List.of(
                Component.literal("§7• Your shop is just starting out!"),
                Component.literal("§7• Focus on stocking basic items"),
                Component.literal("§7• Set reasonable prices to attract customers"),
                Component.literal("§7• Consider advertising your shop")
            );
        }
        
        return tips;
    }
}
