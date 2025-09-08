package com.zerog.neoessentials.economy.shops;

import com.zerog.neoessentials.web.WebDashboardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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
    
    private final com.zerog.neoessentials.managers.EconomyManager economyManager;
    private final WebDashboardManager webDashboard;
    
    // Shop storage
    private final Map<String, PlayerShop> playerShops = new ConcurrentHashMap<>();
    private final Map<String, AdminShop> adminShops = new ConcurrentHashMap<>();
    private final Map<BlockPos, SignShop> signShops = new ConcurrentHashMap<>();
    
    // Auto-save system to prevent data loss
    private final java.util.concurrent.ScheduledExecutorService autoSaveExecutor;
    private final java.util.concurrent.ScheduledFuture<?> autoSaveTask;
    
    // Shop analytics
    private int dailyTransactions = 0;
    private double dailyRevenue = 0.0;
    private final Map<String, Integer> categoryStats = new ConcurrentHashMap<>();
    
    private ShopManager(com.zerog.neoessentials.managers.EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.webDashboard = WebDashboardManager.getInstance();
        
        // Initialize auto-save system
        this.autoSaveExecutor = java.util.concurrent.Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "ShopManager-AutoSave");
            t.setDaemon(true);
            return t;
        });
        
        // Auto-save every 5 minutes to prevent data loss
        this.autoSaveTask = autoSaveExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (!signShops.isEmpty()) {
                    saveShopsToStorage();
                    LOGGER.debug("Auto-saved {} shops", signShops.size());
                }
            } catch (Exception e) {
                LOGGER.error("Error during auto-save", e);
            }
        }, 5, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        LOGGER.info("Shop auto-save system initialized (5-minute intervals)");
    }
    
    public static ShopManager getInstance() {
        return instance;
    }
    
    public static void createInstance(com.zerog.neoessentials.managers.EconomyManager economyManager) {
        if (instance == null) {
            instance = new ShopManager(economyManager);
        }
    }
    
    public void initialize() {
        LOGGER.info("Initializing Shop Management System...");
        
        // Initialize default admin shops
        createDefaultAdminShops();
        
    // Load existing shops from storage
    loadShopsFromStorage();
        
        // Update web dashboard with initial data
        updateWebDashboardMetrics();
        
        LOGGER.info("Shop Management System initialized with {} player shops, {} admin shops, {} sign shops", 
                   playerShops.size(), adminShops.size(), signShops.size());
    }
    
    public void shutdown() {
        LOGGER.info("Shutting down Shop Management System...");
        
        // Stop auto-save system
        if (autoSaveTask != null) {
            autoSaveTask.cancel(false);
        }
        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdown();
            try {
                if (!autoSaveExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Save shops to storage SYNCHRONOUSLY to ensure completion
        saveShopsToStorageSync();
        
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
     * Create a new sign shop (with admin shop support)
     */
    public boolean createSignShop(Player player, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity, boolean isAdminShop) {
        if (signShops.containsKey(signPos)) {
            LOGGER.warn("Attempted to create sign shop at {} but shop already exists there", signPos);
            return false; // Sign shop already exists at this location
        }
        
        // Find a nearby chest for the shop (skip for admin shops)
        BlockPos chestPos = null;
        if (!isAdminShop) {
            chestPos = findNearbyChest(player.level(), signPos);
            if (chestPos == null) {
                player.sendSystemMessage(Component.literal("§cNo chest found near the sign! Place a chest within 3 blocks."));
                LOGGER.warn("SHOP CREATION: No chest found near sign at {} for player {}", signPos, player.getName().getString());
                return false;
            }
            LOGGER.info("SHOP CREATION: Found chest at {} for shop at {} by {}", chestPos, signPos, player.getName().getString());
        } else {
            LOGGER.info("SHOP CREATION: Admin shop at {} - no chest required", signPos);
        }
        
        // Set owner based on shop type
        String ownerId = isAdminShop ? "SERVER" : player.getStringUUID();
        
        try {
            SignShop signShop = new SignShop(ownerId, signPos, chestPos, item, buyPrice, sellPrice, quantity);
            signShops.put(signPos, signShop);
            
            LOGGER.info("SHOP STORAGE: Stored shop with key {} -> Owner: {}, Chest: {}, Item: {}", 
                       signPos, ownerId, chestPos, item.getDisplayName().getString());
            
            // Save shops to storage after creating a new one
            saveShopsToStorage();
            
            updateWebDashboardMetrics();
            String shopType = isAdminShop ? "admin" : "player";
            if (webDashboard != null) {
                webDashboard.addRealTimeEvent("SHOP", shopType + " shop created by " + player.getName().getString() + 
                                             " for " + item.getDisplayName().getString(), "INFO");
            }
            
            LOGGER.info("Created {} sign shop at {} for player {}", shopType, signPos, player.getName().getString());
            return true;
        } catch (Exception e) {
            LOGGER.error("Error creating sign shop at {} for player {}", signPos, player.getName().getString(), e);
            return false;
        }
    }
    
    /**
     * Create a new sign shop (backward compatibility)
     */
    public boolean createSignShop(Player player, BlockPos signPos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
        return createSignShop(player, signPos, item, buyPrice, sellPrice, quantity, false);
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
     * Get a specific sign shop by position (more efficient than streaming)
     */
    public SignShop getSignShop(BlockPos signPos) {
        SignShop shop = signShops.get(signPos);
        if (shop != null) {
            LOGGER.debug("SHOP LOOKUP: Found shop at {} -> Owner: {}, Chest: {}, Item: {}", 
                        signPos, shop.getOwnerId(), shop.getChestPos(), shop.getItem().getDisplayName().getString());
        } else {
            LOGGER.debug("SHOP LOOKUP: No shop found at {}", signPos);
        }
        return shop;
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
            storageManager.loadDataAsync("shops", "signshops", java.util.Map.class)
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
                                        
                                        // Validate shop data before converting
                                        if (signShopData == null || signShopData.getSignPos() == null) {
                                            LOGGER.warn("Skipping invalid shop data - missing position data");
                                            continue;
                                        }
                                        
                                        // Convert to SignShop and add to collection
                                        ShopManager.SignShop signShop = signShopData.toSignShop();
                                        if (signShop != null && signShop.getSignPos() != null) {
                                            signShops.put(signShop.getSignPos(), signShop);
                                            
                                            LOGGER.debug("Loaded sign shop at {} for item {}", 
                                                        signShop.getSignPos(), 
                                                        signShop.getItem().getDisplayName().getString());
                                        } else {
                                            LOGGER.warn("Skipping shop with invalid data after conversion");
                                        }
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
    
    public void saveShopsToStorage() {
        if (signShops.isEmpty()) {
            LOGGER.debug("No shops to save - skipping storage operation");
            return;
        }
        
        LOGGER.debug("Saving {} sign shops to storage...", signShops.size());
        
        try {
            com.zerog.neoessentials.storage.StorageManager storageManager = 
                com.zerog.neoessentials.storage.StorageManager.getInstance();
            
            // Enhanced: Convert SignShop objects to serializable data with error handling
            java.util.Map<String, SignShopData> shopDataMap = new java.util.HashMap<>();
            
            for (ShopManager.SignShop signShop : signShops.values()) {
                try {
                    String key = signShop.getSignPos().toShortString();
                    SignShopData shopData = new SignShopData(signShop);
                    shopDataMap.put(key, shopData);
                } catch (Exception e) {
                    LOGGER.warn("Failed to serialize shop at {}: {}", signShop.getSignPos(), e.getMessage());
                    // Continue with other shops instead of failing completely
                }
            }
            
            LOGGER.debug("Successfully serialized {} out of {} shops for storage", 
                        shopDataMap.size(), signShops.size());
            
            // Enhanced async save with better error handling
            storageManager.saveDataAsync("shops", "signshops", shopDataMap)
                .thenAccept(success -> {
                    if (success) {
                        LOGGER.debug("Successfully saved {} sign shops to storage", shopDataMap.size());
                    } else {
                        LOGGER.error("Failed to save sign shops to storage - storage operation returned false");
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.error("Exception while saving sign shops to storage: {}", throwable.getMessage());
                    return null;
                });
        } catch (Exception e) {
            LOGGER.error("Failed to initialize sign shop saving: {}", e.getMessage(), e);
        }
    }
    
    /**
     * SYNCHRONOUS save method for use during shutdown
     * This ensures data is saved before the server stops
     */
    public void saveShopsToStorageSync() {
        if (signShops.isEmpty()) {
            LOGGER.debug("No shops to save - skipping storage operation");
            return;
        }
        
        LOGGER.info("Synchronously saving {} sign shops to storage...", signShops.size());
        
        try {
            com.zerog.neoessentials.storage.StorageManager storageManager = 
                com.zerog.neoessentials.storage.StorageManager.getInstance();
            
            // Convert SignShop objects to serializable data
            java.util.Map<String, SignShopData> shopDataMap = new java.util.HashMap<>();
            int successCount = 0;
            int errorCount = 0;
            
            for (ShopManager.SignShop signShop : signShops.values()) {
                try {
                    String key = signShop.getSignPos().toShortString();
                    SignShopData shopData = new SignShopData(signShop);
                    shopDataMap.put(key, shopData);
                    successCount++;
                } catch (Exception e) {
                    LOGGER.error("Failed to serialize shop at {}: {}", signShop.getSignPos(), e.getMessage(), e);
                    errorCount++;
                }
            }
            
            LOGGER.info("Serialized {} shops successfully, {} failed", successCount, errorCount);
            
            // Use synchronous save with timeout
            boolean saveResult = storageManager.saveDataAsync("shops", "signshops", shopDataMap)
                .get(10, java.util.concurrent.TimeUnit.SECONDS); // 10 second timeout
            
            if (saveResult) {
                LOGGER.info("Successfully saved {} sign shops to storage synchronously", shopDataMap.size());
            } else {
                LOGGER.error("Failed to save sign shops to storage - storage operation returned false");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save sign shops synchronously: {}", e.getMessage(), e);
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
    
    /**
     * Record a shop transaction for analytics
     */
    public void recordShopTransaction(SignShop shop, String transactionType, double amount, int quantity) {
        try {
            dailyTransactions++;
            
            if ("buy".equals(transactionType)) {
                dailyRevenue += amount;
            } else if ("sell".equals(transactionType)) {
                // For sell transactions, this is money going to the player, not revenue
                // But we still track it as shop activity
            }
            
            // Update category stats
            String category = getCategoryForItem(shop.getItem());
            categoryStats.merge(category, quantity, Integer::sum);
            
            // Update web dashboard
            updateWebDashboardMetrics();
            
            // Log real-time event
            String shopOwner = "SERVER".equals(shop.getOwnerId()) ? "Admin Shop" : "Player Shop";
            webDashboard.addRealTimeEvent("TRANSACTION", 
                shopOwner + " " + transactionType + ": " + quantity + "x " + 
                shop.getItem().getDisplayName().getString() + " for $" + String.format("%.2f", amount), 
                "INFO");
            
            LOGGER.debug("Recorded {} transaction: {}x {} for ${}", 
                        transactionType, quantity, shop.getItem().getDisplayName().getString(), amount);
        } catch (Exception e) {
            LOGGER.error("Failed to record shop transaction", e);
        }
    }
    
    /**
     * Find a chest within 3 blocks of the given position
     */
    /**
     * Generate a unique shop key similar to ChestShop plugin approach
     * Format: "dimension_name§x§y§z"
     */
        // Method removed as it was unused
    
    /**
     * Find the chest that this sign is meant to be connected to
     * Enhanced with better performance and more intelligent detection
     */
    private BlockPos findNearbyChest(Level level, BlockPos signPos) {
        // First, try to find a chest the sign is directly attached to (for wall signs)
        net.minecraft.world.level.block.state.BlockState signState = level.getBlockState(signPos);
        
        if (signState.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock) {
            // For wall signs, check the block it's attached to
            net.minecraft.core.Direction facing = signState.getValue(net.minecraft.world.level.block.WallSignBlock.FACING);
            BlockPos attachedPos = signPos.relative(facing.getOpposite());
            
            LOGGER.debug("CHEST DETECTION: Wall sign at {} facing {}, checking attached block at {}", 
                       signPos, facing, attachedPos);
            
            if (level.getBlockState(attachedPos).getBlock() instanceof ChestBlock) {
                LOGGER.info("CHEST DETECTION: Found chest attached to wall sign at {}", attachedPos);
                return attachedPos;
            }
        }
        
        // Enhanced search pattern: prioritize closer positions first
        // Check directly adjacent blocks first (6 directions)
        BlockPos[] adjacentPositions = {
            signPos.north(), signPos.south(), signPos.east(), 
            signPos.west(), signPos.below(), signPos.above()
        };
        
        for (BlockPos checkPos : adjacentPositions) {
            if (level.getBlockState(checkPos).getBlock() instanceof ChestBlock) {
                LOGGER.info("CHEST DETECTION: Found adjacent chest at {} for sign at {}", checkPos, signPos);
                return checkPos;
            }
        }
        
        // Enhanced search pattern: check 2-block radius with intelligent ordering
        // Search in layers: first all positions 2 blocks away, then corners
        for (int distance = 2; distance <= 3; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int y = -1; y <= 1; y++) { // Limit Y search to reasonable range
                    for (int z = -distance; z <= distance; z++) {
                        // Skip already checked positions
                        if (Math.abs(x) <= distance - 1 && Math.abs(z) <= distance - 1) continue;
                        if (x == 0 && y == 0 && z == 0) continue; // Skip sign position
                        
                        BlockPos checkPos = signPos.offset(x, y, z);
                        if (level.getBlockState(checkPos).getBlock() instanceof ChestBlock) {
                            LOGGER.info("CHEST DETECTION: Found nearby chest at {} (distance {}) for sign at {}", 
                                      checkPos, distance, signPos);
                            return checkPos;
                        }
                    }
                }
            }
        }
        
        LOGGER.warn("CHEST DETECTION: No chest found within 3 blocks for sign at {}", signPos);
        return null; // No chest found
    }
    
    /**
     * Add items to a chest
     */
    private boolean addItemsToChest(Level level, BlockPos chestPos, ItemStack items) {
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chestEntity) {
            // Try to add items to the chest
            for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                ItemStack slotStack = chestEntity.getItem(i);
                if (slotStack.isEmpty()) {
                    // Empty slot - put the items here
                    chestEntity.setItem(i, items.copy());
                    chestEntity.setChanged();
                    return true;
                } else if (ItemStack.isSameItem(slotStack, items) && slotStack.getCount() + items.getCount() <= slotStack.getMaxStackSize()) {
                    // Same item type and can fit
                    slotStack.grow(items.getCount());
                    chestEntity.setChanged();
                    return true;
                }
            }
        }
        return false; // Chest is full or couldn't add items
    }
    
    /**
     * Add items to a shop's connected chest
     */
    public boolean addItemsToShopChest(Level level, SignShop signShop, ItemStack items) {
        if (signShop.getChestPos() == null) {
            return false; // No chest connected
        }
        return addItemsToChest(level, signShop.getChestPos(), items);
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
        private final BlockPos chestPos; // Connected chest for item storage
        private final ItemStack item;
        private final double buyPrice;
        private final double sellPrice;
        private final int quantity;
        private int stock = 64; // Default stock
        
        public SignShop(String ownerId, BlockPos signPos, BlockPos chestPos, ItemStack item, double buyPrice, double sellPrice, int quantity) {
            this.ownerId = ownerId;
            this.signPos = signPos;
            this.chestPos = chestPos;
            this.item = item;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.quantity = quantity;
        }
        
        public String getOwnerId() { return ownerId; }
        public BlockPos getSignPos() { return signPos; }
        public BlockPos getChestPos() { return chestPos; }
        public ItemStack getItem() { return item; }
        public double getBuyPrice() { return buyPrice; }
        public double getSellPrice() { return sellPrice; }
        public int getQuantity() { return quantity; }
        public int getStock() { return stock; }
        
        public void setStock(int stock) { this.stock = stock; }
        public boolean hasStock() { return stock > 0; }
        
        /**
         * Check if this is an admin shop (owned by SERVER)
         */
        public boolean isAdminShop() {
            return "SERVER".equals(ownerId);
        }
        
        /**
         * Get owner UUID, returns null for admin shops
         */
        public java.util.UUID getOwner() {
            if (isAdminShop()) {
                return null;
            }
            try {
                return java.util.UUID.fromString(ownerId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
