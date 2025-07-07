package com.zerog.neoessentials.economy.shop;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Utility class for managing default shop items and populating shops
 */
public class ShopUtils {
    
    private static final Currency DEFAULT_CURRENCY = Currency.createBasic("coins", "Coin", "§6", "Coins");
    
    /**
     * Adds some default admin shop items for testing
     */
    public static void addDefaultShopItems(EconomyManager economyManager) {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            if (shopManager == null) {
                NeoEssentials.LOGGER.warn("Cannot add default shop items: ShopManager is null");
                return;
            }
            
            // Add some basic items to the shop for testing
            addAdminShopItem(shopManager, new ItemStack(Items.DIAMOND, 1), 100.0, 1000);
            addAdminShopItem(shopManager, new ItemStack(Items.IRON_INGOT, 1), 10.0, 500);
            addAdminShopItem(shopManager, new ItemStack(Items.GOLD_INGOT, 1), 20.0, 300);
            addAdminShopItem(shopManager, new ItemStack(Items.EMERALD, 1), 50.0, 200);
            addAdminShopItem(shopManager, new ItemStack(Items.BREAD, 1), 2.0, 100);
            addAdminShopItem(shopManager, new ItemStack(Items.COOKED_BEEF, 1), 5.0, 150);
            addAdminShopItem(shopManager, new ItemStack(Items.ARROW, 64), 15.0, 50);
            addAdminShopItem(shopManager, new ItemStack(Items.OAK_LOG, 64), 25.0, 75);
            addAdminShopItem(shopManager, new ItemStack(Items.STONE, 64), 5.0, 200);
            addAdminShopItem(shopManager, new ItemStack(Items.WHEAT_SEEDS, 32), 3.0, 80);
            
            NeoEssentials.LOGGER.info("Added default admin shop items");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to add default shop items", e);
        }
    }
    
    private static void addAdminShopItem(ShopManager shopManager, ItemStack itemStack, double price, int stock) {
        try {
            ShopItem shopItem = new ShopItem.Builder()
                .id(UUID.randomUUID())
                .itemStack(itemStack)
                .type(ShopItem.Type.BUY)
                .buyPrice(BigDecimal.valueOf(price))
                .currency(DEFAULT_CURRENCY)
                .stock(stock)
                .maxStock(stock)
                .createdBy(null) // Admin item
                .createdAt(LocalDateTime.now())
                .description("Admin shop item")
                .adminItem(true)
                .build();
                
            if (!shopManager.addShopItem(shopItem)) {
                NeoEssentials.LOGGER.warn("Failed to add admin shop item: {}", itemStack.getHoverName().getString());
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error adding admin shop item: {}", itemStack.getHoverName().getString(), e);
        }
    }
    
    /**
     * Creates a player shop item from a player's held item
     */
    public static ShopItem createPlayerShopItem(UUID playerId, ItemStack itemStack, double price, int stock) {
        return new ShopItem.Builder()
            .id(UUID.randomUUID())
            .itemStack(itemStack.copy())
            .type(ShopItem.Type.BUY)
            .buyPrice(BigDecimal.valueOf(price))
            .currency(DEFAULT_CURRENCY)
            .stock(stock)
            .maxStock(stock)
            .createdBy(playerId)
            .createdAt(LocalDateTime.now())
            .description("Player shop item")
            .adminItem(false)
            .build();
    }
    
    /**
     * Gets the default currency used in the shop
     */
    public static Currency getDefaultCurrency() {
        return DEFAULT_CURRENCY;
    }
}
