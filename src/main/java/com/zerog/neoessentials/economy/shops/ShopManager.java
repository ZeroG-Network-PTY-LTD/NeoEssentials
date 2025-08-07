package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.economy.EconomyManager;
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
        
        updateWebDashboardMetrics();
        webDashboard.addRealTimeEvent("SHOP", "Sign shop created by " + player.getName().getString() + 
                                     " for " + item.getDisplayName().getString(), "INFO");
        
        LOGGER.info("Created sign shop at {} for player {}", signPos, player.getName().getString());
        return true;
    }
    
    /**
     * Process a shop transaction
     */
    public boolean processTransaction(String playerId, String shopId, ItemStack item, int quantity, double totalPrice) {
        // Process transaction logic here
        boolean success = economyManager.removeBalance(UUID.fromString(playerId), "NEOESSENTIALS", BigDecimal.valueOf(totalPrice));
        
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
        // TODO: Implement shop persistence loading
        // For now, just log that we would load from storage
        LOGGER.debug("Loading shops from storage (not implemented yet)");
    }
    
    private void saveShopsToStorage() {
        // TODO: Implement shop persistence saving
        // For now, just log that we would save to storage
        LOGGER.debug("Saving shops to storage (not implemented yet)");
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
