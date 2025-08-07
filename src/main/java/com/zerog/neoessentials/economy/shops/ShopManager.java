package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.web.WebDashboardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Shop Management System
 * Handles player shops, admin shops, sign shops, and shop analytics
 */
public class ShopManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopManager.class);
    private static ShopManager instance;
    
    private final EconomyManager economyManager;
    private final WebDashboardManager webDashboard;
    
    // Shop storage
    private final Map<String, PlayerShop> playerShops = new ConcurrentHashMap<>();
    private final Map<String, AdminShop> adminShops = new ConcurrentHashMap<>();
    private final Map<BlockPos, SignShop> signShops = new ConcurrentHashMap<>();
    
    // Shop analytics
    private int dailyTransactions = 0;
    private double dailyRevenue = 0.0;
    private final Map<String, Integer> categoryStats = new ConcurrentHashMap<>();
    
    private ShopManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.webDashboard = WebDashboardManager.getInstance();
    }
    
    public static ShopManager getInstance() {
        return instance;
    }
    
    public static void createInstance(EconomyManager economyManager) {
        if (instance == null) {
            instance = new ShopManager(economyManager);
        }
    }
    
    public void initialize() {
        LOGGER.info("Initializing Shop Management System...");
        
        // Initialize default admin shops
        createDefaultAdminShops();
        
        // Load existing shops from storage (TODO: implement persistence)
        loadShopsFromStorage();
        
        // Update web dashboard with initial data
        updateWebDashboardMetrics();
        
        LOGGER.info("Shop Management System initialized with {} player shops, {} admin shops, {} sign shops", 
                   playerShops.size(), adminShops.size(), signShops.size());
    }
    
    public void shutdown() {
        LOGGER.info("Shutting down Shop Management System...");
        
        // Save shops to storage (TODO: implement persistence)
        saveShopsToStorage();
        
        // Clear caches
        playerShops.clear();
        adminShops.clear();
        signShops.clear();
        
        LOGGER.info("Shop Management System shutdown complete");
    }
    
    /**
     * Create a new player shop
     */
    public boolean createPlayerShop(String playerId, String shopName, String category, BlockPos location) {
        if (playerShops.containsKey(shopName.toLowerCase())) {
            return false; // Shop name already exists
        }
        
        PlayerShop shop = new PlayerShop(shopName, playerId, category, location);
        playerShops.put(shopName.toLowerCase(), shop);
        
        updateWebDashboardMetrics();
        webDashboard.addRealTimeEvent("SHOP", "Player shop '" + shopName + "' created by " + playerId, "INFO");
        
        LOGGER.info("Created player shop '{}' for player {}", shopName, playerId);
        return true;
    }
    
    /**
     * Create a new sign shop
     */
    public boolean createSignShop(Player player, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
        if (signShops.containsKey(signPos)) {
            return false; // Sign shop already exists at this location
        }
        
        SignShop signShop = new SignShop(player.getStringUUID(), signPos, item, buyPrice, sellPrice, quantity);
        signShops.put(signPos, signShop);
        
        // Save shops to storage after creating a new one
        saveShopsToStorage();
        
        updateWebDashboardMetrics();
        webDashboard.addRealTimeEvent("SHOP", "Sign shop created by " + player.getName().getString() + 
                                     " for " + item.getDisplayName().getString(), "INFO");
        
        LOGGER.info("Created sign shop at {} for player {}", signPos, player.getName().getString());
        return true;
    }
    
    /**
     * Update sign shop stock and save to storage
     */
    public void updateSignShopStock(BlockPos signPos, int newStock) {
        SignShop signShop = signShops.get(signPos);
        if (signShop != null) {
            signShop.setStock(newStock);
            // Save shops to storage after stock update
            saveShopsToStorage();
            LOGGER.debug("Updated stock for sign shop at {} to {}", signPos, newStock);
        }
    }
    
    /**
     * Remove a sign shop and save to storage
     */
    public boolean removeSignShop(BlockPos signPos) {
        SignShop removed = signShops.remove(signPos);
        if (removed != null) {
            saveShopsToStorage();
            LOGGER.info("Removed sign shop at {}", signPos);
            return true;
        }
        return false;
    }
    
    /**
     * Process a shop transaction
     */
    public boolean processTransaction(String playerId, String shopId, ItemStack item, int quantity, double totalPrice) {
        // Process transaction using the simple EconomyManager API
        boolean success = economyManager.withdrawBalance(UUID.fromString(playerId), BigDecimal.valueOf(totalPrice), 
            "Shop purchase: " + quantity + "x " + item.getDisplayName().getString());
        
        LOGGER.info("Processing shop transaction: Player {}, Amount: {}, Success: {}", 
                   playerId, totalPrice, success);
        
        if (success) {
            dailyTransactions++;
            dailyRevenue += totalPrice;
            
            // Update category stats
            String category = getCategoryForItem(item);
            categoryStats.merge(category, quantity, Integer::sum);
            
            updateWebDashboardMetrics();
            webDashboard.addRealTimeEvent("TRANSACTION", 
                    "Shop purchase: " + quantity + "x " + item.getDisplayName().getString() + " for $" + totalPrice, "INFO");
            
            LOGGER.debug("Processed shop transaction: {} bought {}x {} for ${}", 
                        playerId, quantity, item.getDisplayName().getString(), totalPrice);
        }
        
        return success;
    }
    
    /**
     * Get all player shops
     */
    public Collection<PlayerShop> getPlayerShops() {
        return playerShops.values();
    }
    
    /**
     * Get all admin shops
     */
    public Collection<AdminShop> getAdminShops() {
        return adminShops.values();
    }
    
    /**
     * Get all sign shops
     */
    public Collection<SignShop> getSignShops() {
        return signShops.values();
    }
    
    /**
     * Get shops by category
     */
    public List<PlayerShop> getShopsByCategory(String category) {
        return playerShops.values().stream()
                .filter(shop -> shop.getCategory().equalsIgnoreCase(category))
                .toList();
    }
    
    /**
     * Get shop statistics
     */
    public Map<String, Object> getShopStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_shops", playerShops.size() + adminShops.size());
        stats.put("player_shops", playerShops.size());
        stats.put("admin_shops", adminShops.size());
        stats.put("sign_shops", signShops.size());
        stats.put("daily_transactions", dailyTransactions);
        stats.put("daily_revenue", dailyRevenue);
        stats.put("category_stats", new HashMap<>(categoryStats));
        return stats;
    }
    
    private void createDefaultAdminShops() {
        // Create a default server economy shop
        AdminShop economyHub = new AdminShop("Server Economy Hub", "SERVER", "all", BlockPos.ZERO);
        adminShops.put("server_economy", economyHub);
        
        // Create category-specific admin shops
        AdminShop weaponShop = new AdminShop("Server Weapon Shop", "SERVER", "weapons", BlockPos.ZERO);
        adminShops.put("server_weapons", weaponShop);
        
        AdminShop rareItemsShop = new AdminShop("Rare Items Emporium", "SERVER", "rare", BlockPos.ZERO);
        adminShops.put("server_rare", rareItemsShop);
        
        LOGGER.info("Created {} default admin shops", adminShops.size());
    }
    
    private void loadShopsFromStorage() {
        LOGGER.info("Loading sign shops from storage...");
        
        try {
            com.zerog.neoessentials.storage.StorageManager storageManager = 
                com.zerog.neoessentials.storage.StorageManager.getInstance();
            
            // Load sign shops asynchronously
            storageManager.loadDataAsync("shops", "signshops.json", java.util.Map.class)
                .thenAccept(data -> {
                    if (data != null) {
                        try {
                            // Clear existing sign shops
                            signShops.clear();
                            
                            // Convert the loaded data back to SignShop objects
                            for (Object entry : data.values()) {
                                if (entry instanceof java.util.Map<?, ?> shopData) {
                                    try {
                                        // Convert Map to SignShopData using Gson
                                        com.google.gson.Gson gson = new com.google.gson.Gson();
                                        String json = gson.toJson(shopData);
                                        SignShopData signShopData = gson.fromJson(json, SignShopData.class);
                                        
                                        // Convert to SignShop and add to collection
                                        ShopManager.SignShop signShop = signShopData.toSignShop();
                                        signShops.put(signShop.getSignPos(), signShop);
                                        
                                        LOGGER.debug("Loaded sign shop at {} for item {}", 
                                                    signShop.getSignPos(), 
                                                    signShop.getItem().getDisplayName().getString());
                                    } catch (Exception e) {
                                        LOGGER.error("Failed to deserialize sign shop data: {}", e.getMessage());
                                    }
                                }
                            }
                            
                            LOGGER.info("Successfully loaded {} sign shops from storage", signShops.size());
                        } catch (Exception e) {
                            LOGGER.error("Failed to process loaded sign shop data", e);
                        }
                    } else {
                        LOGGER.info("No existing sign shop data found - starting with empty shop list");
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Failed to load sign shops from storage", throwable);
                    return null;
                });
        } catch (Exception e) {
            LOGGER.error("Failed to initialize sign shop loading", e);
        }
    }
    
    private void saveShopsToStorage() {
        LOGGER.debug("Saving {} sign shops to storage...", signShops.size());
        
        try {
            com.zerog.neoessentials.storage.StorageManager storageManager = 
                com.zerog.neoessentials.storage.StorageManager.getInstance();
            
            // Convert SignShop objects to serializable data
            java.util.Map<String, SignShopData> shopDataMap = new java.util.HashMap<>();
            
            for (ShopManager.SignShop signShop : signShops.values()) {
                String key = signShop.getSignPos().toShortString();
                SignShopData shopData = new SignShopData(signShop);
                shopDataMap.put(key, shopData);
            }
            
            // Save asynchronously
            storageManager.saveDataAsync("shops", "signshops.json", shopDataMap)
                .thenAccept(success -> {
                    if (success) {
                        LOGGER.debug("Successfully saved {} sign shops to storage", shopDataMap.size());
                    } else {
                        LOGGER.error("Failed to save sign shops to storage");
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Exception while saving sign shops to storage", throwable);
                    return null;
                });
        } catch (Exception e) {
            LOGGER.error("Failed to initialize sign shop saving", e);
        }
    }
    
    private void updateWebDashboardMetrics() {
        int totalShops = playerShops.size() + adminShops.size();
        int activeShops = (int) playerShops.values().stream().filter(PlayerShop::isActive).count() + adminShops.size();
        
        webDashboard.updateShopMetrics(totalShops, activeShops, dailyTransactions, dailyRevenue);
    }
    
    private String getCategoryForItem(ItemStack item) {
        // Simple category determination based on item type
        String itemName = item.getItem().toString().toLowerCase();
        
        if (itemName.contains("sword") || itemName.contains("bow") || itemName.contains("axe")) {
            return "weapons";
        } else if (itemName.contains("helmet") || itemName.contains("chestplate") || itemName.contains("leggings") || itemName.contains("boots")) {
            return "armor";
        } else if (itemName.contains("bread") || itemName.contains("meat") || itemName.contains("apple") || itemName.contains("food")) {
            return "food";
        } else if (itemName.contains("stone") || itemName.contains("wood") || itemName.contains("dirt") || itemName.contains("block")) {
            return "blocks";
        } else if (itemName.contains("redstone") || itemName.contains("piston") || itemName.contains("repeater")) {
            return "redstone";
        } else if (itemName.contains("diamond") || itemName.contains("emerald") || itemName.contains("netherite")) {
            return "rare";
        }
        
        return "general";
    }
    
    // Shop classes
    public static class PlayerShop {
        private final String name;
        private final String ownerId;
        private final String category;
        private final BlockPos location;
        private boolean active = true;
        private double rating = 0.0;
        private int transactions = 0;
        
        public PlayerShop(String name, String ownerId, String category, BlockPos location) {
            this.name = name;
            this.ownerId = ownerId;
            this.category = category;
            this.location = location;
        }
        
        public String getName() { return name; }
        public String getOwnerId() { return ownerId; }
        public String getCategory() { return category; }
        public BlockPos getLocation() { return location; }
        public boolean isActive() { return active; }
        public double getRating() { return rating; }
        public int getTransactions() { return transactions; }
        
        public void setActive(boolean active) { this.active = active; }
        public void addTransaction() { this.transactions++; }
        public void setRating(double rating) { this.rating = rating; }
    }
    
    public static class AdminShop {
        private final String name;
        private final String category;
        private final String owner;
        private final BlockPos location;
        
        public AdminShop(String name, String owner, String category, BlockPos location) {
            this.name = name;
            this.owner = owner;
            this.category = category;
            this.location = location;
        }
        
        public String getName() { return name; }
        public String getOwner() { return owner; }
        public String getCategory() { return category; }
        public BlockPos getLocation() { return location; }
    }
    
    public static class SignShop {
        private final String ownerId;
        private final BlockPos signPos;
        private final ItemStack item;
        private final double buyPrice;
        private final double sellPrice;
        private final int quantity;
        private int stock = 64; // Default stock
        
        public SignShop(String ownerId, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
            this.ownerId = ownerId;
            this.signPos = signPos;
            this.item = item;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.quantity = quantity;
        }
        
        public String getOwnerId() { return ownerId; }
        public BlockPos getSignPos() { return signPos; }
        public ItemStack getItem() { return item; }
        public double getBuyPrice() { return buyPrice; }
        public double getSellPrice() { return sellPrice; }
        public int getQuantity() { return quantity; }
        public int getStock() { return stock; }
        
        public void setStock(int stock) { this.stock = stock; }
        public boolean hasStock() { return stock > 0; }
    }
}
