package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.Shop.ShopItem;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.utils.MessageUtil;
import com.zerog.neoessentials.NeoEssentials;
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
import java.util.ArrayList;
import java.util.Map;

/**
 * Shop Interface GUI for buying and selling items
 * Provides a user-friendly interface for shop transactions
 */
public class ShopInterfaceGUI {
    
    private final Shop shop;
    private final EconomyManager economyManager;
    
    public ShopInterfaceGUI(Shop shop, EconomyManager economyManager) {
        this.shop = shop;
        this.economyManager = economyManager;
    }
    
    /**
     * Opens the main shop interface for buying/selling
     */
    public void openShopInterface(ServerPlayer player) {
        // Check if player can view the shop
        if (!shop.isActive()) {
            MessageUtil.sendErrorMessage(player, "This shop is currently inactive.");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupShopInterface(container, player);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§2Shop§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopInterfaceMenu(containerId, playerInventory, container, shop);
            }
        });
    }
    
    /**
     * Sets up the shop interface with available items
     */
    private void setupShopInterface(SimpleContainer container, ServerPlayer player) {
        // Shop info item
        setupShopInfoItem(container);
        
        // Player balance info
        setupBalanceItem(container, player);
        
        // Available items for purchase
        setupShopItems(container, player);
        
        // Navigation and action items
        setupNavigationItems(container);
    }
    
    /**
     * Sets up the shop information display
     */
    private void setupShopInfoItem(SimpleContainer container) {
        ItemStack infoItem = new ItemStack(Items.EMERALD);
        infoItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§a§l" + shop.getShopName()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Owner: §f" + (shop.getOwnerId() != null ? "Player" : "Server")));
        lore.add(Component.literal("§7Category: §f" + shop.getCategory()));
        lore.add(Component.literal("§7Type: §f" + shop.getShopType().getDisplayName()));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§7Available Items: §e" + shop.getInventory().size()));
        lore.add(Component.literal("§7Status: " + (shop.isActive() ? "§aActive" : "§cInactive")));
        
        infoItem.set(DataComponents.LORE, new ItemLore(lore));
        container.setItem(4, infoItem); // Top center
    }
    
    /**
     * Sets up the player balance display
     */
    private void setupBalanceItem(SimpleContainer container, ServerPlayer player) {
        ItemStack balanceItem = new ItemStack(Items.GOLD_INGOT);
        balanceItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§e§lYour Balance"));
        
        double walletBalance = economyManager.getWalletManager().getCashBalance(player.getUUID());
        com.zerog.neoessentials.economy.Currency defaultCurrency = com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
        double bankBalance = economyManager.getBankManager().getTotalPlayerBalance(player.getUUID(), defaultCurrency);
        double totalBalance = walletBalance + bankBalance;
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Wallet: §e$" + String.format("%.2f", walletBalance)));
        lore.add(Component.literal("§7Bank: §e$" + String.format("%.2f", bankBalance)));
        lore.add(Component.literal("§7Total: §a$" + String.format("%.2f", totalBalance)));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§8Left-click an item to buy"));
        lore.add(Component.literal("§8Right-click an item to sell"));
        
        balanceItem.set(DataComponents.LORE, new ItemLore(lore));
        container.setItem(8, balanceItem); // Top right
    }
    
    /**
     * Sets up available shop items for purchase/sale
     */
    private void setupShopItems(SimpleContainer container, ServerPlayer player) {
        Map<String, ShopItem> inventory = shop.getInventory();
        int slot = 18; // Start from second row
        
        for (Map.Entry<String, ShopItem> entry : inventory.entrySet()) {
            if (slot >= 45) break; // Don't exceed available space
            
            String itemName = entry.getKey();
            ShopItem shopItem = entry.getValue();
            
            // Create display item
            ItemStack displayItem = createItemFromString(itemName);
            if (displayItem.isEmpty()) continue;
            
            // Set custom name and lore
            displayItem.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§f" + shopItem.getItemName()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("§7Stock: §e" + shopItem.getQuantity()));
            lore.add(Component.literal("§7Buy Price: §a$" + String.format("%.2f", shop.getItemPrice(itemName))));
            
            // For sell price, we'll use 70% of buy price as default
            double sellPrice = shop.getItemPrice(itemName) * 0.7;
            if (sellPrice > 0) {
                lore.add(Component.literal("§7Sell Price: §c$" + String.format("%.2f", sellPrice)));
            }
            
            lore.add(Component.literal(""));
            
            // Check if player can afford
            com.zerog.neoessentials.economy.Currency defaultCurrency = com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
            double totalBalance = economyManager.getTotalAvailableFunds(player.getUUID(), defaultCurrency);
            double buyPrice = shop.getItemPrice(itemName);
            if (totalBalance >= buyPrice) {
                lore.add(Component.literal("§a§lLEFT-CLICK to BUY (1x)"));
                lore.add(Component.literal("§a§lSHIFT+LEFT-CLICK to BUY (5x)"));
            } else {
                lore.add(Component.literal("§c§lCANNOT AFFORD"));
            }
            
            // Check if player has items to sell
            int playerItemCount = getPlayerItemCount(player, itemName);
            if (playerItemCount > 0 && sellPrice > 0) {
                lore.add(Component.literal("§e§lRIGHT-CLICK to SELL (1x)"));
                lore.add(Component.literal("§e§lSHIFT+RIGHT-CLICK to SELL (5x)"));
                lore.add(Component.literal("§7You have: §e" + playerItemCount));
            } else if (sellPrice > 0) {
                lore.add(Component.literal("§7§lNO ITEMS TO SELL"));
            } else {
                lore.add(Component.literal("§7§lSHOP DOESN'T BUY THIS ITEM"));
            }
            
            displayItem.set(DataComponents.LORE, new ItemLore(lore));
            
            // Store action data
            CompoundTag actionData = new CompoundTag();
            actionData.putString("Action", "shop_item");
            actionData.putString("ItemName", itemName);
            actionData.putDouble("BuyPrice", buyPrice);
            actionData.putDouble("SellPrice", sellPrice);
            actionData.putInt("Stock", shopItem.getQuantity());
            displayItem.set(DataComponents.CUSTOM_DATA, CustomData.of(actionData));
            
            container.setItem(slot, displayItem);
            slot++;
        }
    }
    
    /**
     * Sets up navigation and action items
     */
    private void setupNavigationItems(SimpleContainer container) {
        // Close button
        ItemStack closeItem = new ItemStack(Items.BARRIER);
        closeItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§c§lClose Shop"));
        closeItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Click to close the shop interface")
        )));
        
        CompoundTag closeData = new CompoundTag();
        closeData.putString("Action", "close");
        closeItem.set(DataComponents.CUSTOM_DATA, CustomData.of(closeData));
        
        container.setItem(49, closeItem); // Bottom center
        
        // Refresh button
        ItemStack refreshItem = new ItemStack(Items.CLOCK);
        refreshItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6§lRefresh"));
        refreshItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Click to refresh shop inventory"),
            Component.literal("§7and update prices")
        )));
        
        CompoundTag refreshData = new CompoundTag();
        refreshData.putString("Action", "refresh");
        refreshItem.set(DataComponents.CUSTOM_DATA, CustomData.of(refreshData));
        
        container.setItem(45, refreshItem); // Bottom left
    }
    
    /**
     * Creates an ItemStack from a string item name
     */
    private ItemStack createItemFromString(String itemName) {
        try {
            // Remove minecraft: prefix if present
            if (itemName.startsWith("minecraft:")) {
                itemName = itemName.substring(10);
            }
            
            // Get the item from registry
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("minecraft:" + itemName));
            
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // Item not found, use barrier as placeholder
        }
        
        return new ItemStack(Items.BARRIER);
    }
    
    /**
     * Gets the count of a specific item in player's inventory
     */
    private int getPlayerItemCount(ServerPlayer player, String itemName) {
        try {
            ItemStack targetItem = createItemFromString(itemName);
            if (targetItem.isEmpty()) return 0;
            
            int count = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItem(stack, targetItem)) {
                    count += stack.getCount();
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
