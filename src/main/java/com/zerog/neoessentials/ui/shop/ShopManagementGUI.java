package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;

import java.util.List;

/**
 * Shop Management GUI for NeoEssentials
 * Provides a user interface for managing shops, inventory, pricing, and employees
 */
public class ShopManagementGUI {
    
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopManagementGUI(Shop shop, ShopManager shopManager) {
        this.shop = shop;
        this.shopManager = shopManager;
    }
    
    /**
     * Opens the main shop management interface
     */
    public void openMainMenu(ServerPlayer player) {
        // Check permissions
        if (!shop.getOwnerId().equals(player.getUUID()) && 
            !shop.getEmployeeManager().hasPermission(player.getUUID(), 
                com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
            MessageUtil.sendMessage(player, "§cYou don't have permission to manage this shop!");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupMainMenu(container);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§6Shop Manager§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopManagementMenu(containerId, playerInventory, container, shop);
            }
        });
    }
    
    /**
     * Sets up the main management menu
     */
    private void setupMainMenu(SimpleContainer container) {
        // Shop status item
        ItemStack statusItem = new ItemStack(shop.isActive() ? Items.GREEN_WOOL : Items.RED_WOOL);
        statusItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§a§lShop Status"));
        statusItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Status: " + (shop.isActive() ? "§aActive" : "§cInactive")),
            Component.literal("§7Owner: §f" + shop.getOwnerId()),
            Component.literal("§7Type: §f" + shop.getShopType().getDisplayName())
        )));
        
        // Store action data using custom data component
        CompoundTag actionData = new CompoundTag();
        actionData.putString("Action", "toggle_status");
        statusItem.set(DataComponents.CUSTOM_DATA, CustomData.of(actionData));
        
        container.setItem(4, statusItem); // Top center
        
        // Inventory management
        ItemStack inventoryItem = new ItemStack(Items.CHEST);
        inventoryItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§e§lInventory Management"));
        inventoryItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Manage shop inventory"),
            Component.literal("§7Add, remove, and organize items"),
            Component.literal("§8Click to open")
        )));
        
        CompoundTag inventoryData = new CompoundTag();
        inventoryData.putString("Action", "inventory_management");
        inventoryItem.set(DataComponents.CUSTOM_DATA, CustomData.of(inventoryData));
        
        container.setItem(20, inventoryItem);
        
        // Pricing management
        ItemStack pricingItem = new ItemStack(Items.GOLD_INGOT);
        pricingItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6§lPricing Management"));
        pricingItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Set item prices"),
            Component.literal("§7Configure bulk discounts"),
            Component.literal("§8Click to open")
        )));
        
        CompoundTag pricingData = new CompoundTag();
        pricingData.putString("Action", "pricing_management");
        pricingItem.set(DataComponents.CUSTOM_DATA, CustomData.of(pricingData));
        
        container.setItem(22, pricingItem);
        
        // Employee management
        ItemStack employeeItem = new ItemStack(Items.PLAYER_HEAD);
        employeeItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§b§lEmployee Management"));
        employeeItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Hire and manage employees"),
            Component.literal("§7Set permissions and roles"),
            Component.literal("§8Click to open")
        )));
        
        CompoundTag employeeData = new CompoundTag();
        employeeData.putString("Action", "employee_management");
        employeeItem.set(DataComponents.CUSTOM_DATA, CustomData.of(employeeData));
        
        container.setItem(24, employeeItem);
        
        // Statistics
        ItemStack statsItem = new ItemStack(Items.BOOK);
        statsItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§d§lShop Statistics"));
        statsItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7View sales history"),
            Component.literal("§7Revenue: §6$" + String.format("%.2f", shop.getTotalRevenue())),
            Component.literal("§7Total Sales: §a" + shop.getTotalSales()),
            Component.literal("§8Click to view details")
        )));
        
        CompoundTag statsData = new CompoundTag();
        statsData.putString("Action", "view_statistics");
        statsItem.set(DataComponents.CUSTOM_DATA, CustomData.of(statsData));
        
        container.setItem(40, statsItem);
        
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
    }
}
