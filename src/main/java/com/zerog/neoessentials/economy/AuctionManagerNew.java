package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Enhanced auction management system for NeoEssentials.
 * Handles all auction operations, persistence, and background tasks.
 */
public class AuctionManagerNew {
    
    private static AuctionManagerNew instance;
    
    // Auction storage
    private final Map<UUID, AuctionNew> activeAuctions;
    private final Map<UUID, AuctionNew> completedAuctions;
    private final Map<UUID, List<AuctionNew>> playerAuctions;
    private final Map<String, List<AuctionNew>> categoryAuctions;
    
    // Background services
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executorService;
    
    // Configuration
    private final AuctionConfig config;
    
    // File management
    private final Gson gson;
    private final Path auctionDataPath;
    
    // Statistics
    private final AuctionStatistics statistics;
    
    /**
     * Auction configuration class
     */
    public static class AuctionConfig {
        public int maxActiveAuctionsPerPlayer = 5;
        public int maxAuctionDurationHours = 168; // 1 week
        public int minAuctionDurationMinutes = 5;
        public double auctionHouseFeeRate = 0.05; // 5%
        public double featuredAuctionFee = 100.0;
        public boolean enableAutoBidding = true;
        public boolean enableBuyoutFeature = true;
        public boolean enableReserveAuctions = true;
        public boolean enableDutchAuctions = false;
        public boolean enableSilentAuctions = false;
        public int autoExpireCheckIntervalMinutes = 5;
        public int maxBidHistoryEntries = 50;
        public boolean notifyWatchersOnBid = true;
        public List<String> allowedCategories = Arrays.asList(
            "weapons", "tools", "armor", "blocks", "items", "food", "misc", "rare"
        );
        
        // Item restrictions
        public List<String> bannedItems = new ArrayList<>();
        public Map<String, Double> categoryMinPrices = new HashMap<>();
        public Map<String, Double> categoryMaxPrices = new HashMap<>();
    }
    
    /**
     * Auction statistics tracking
     */
    public static class AuctionStatistics {
        private long totalAuctionsCreated = 0;
        private long totalAuctionsCompleted = 0;
        private long totalAuctionsCancelled = 0;
        private double totalVolume = 0.0;
        private double totalFees = 0.0;
        private Map<String, Long> categoryStats = new ConcurrentHashMap<>();
        private Map<UUID, Long> playerStats = new ConcurrentHashMap<>();
        
        public void recordAuctionCreated(String category) {
            totalAuctionsCreated++;
            categoryStats.merge(category, 1L, Long::sum);
        }
        
        public void recordAuctionCompleted(double amount, UUID seller, UUID buyer) {
            totalAuctionsCompleted++;
            totalVolume += amount;
            playerStats.merge(seller, 1L, Long::sum);
        }
        
        public void recordAuctionCancelled() {
            totalAuctionsCancelled++;
        }
        
        public void recordFee(double fee) {
            totalFees += fee;
        }
        
        // Getters
        public long getTotalAuctionsCreated() { return totalAuctionsCreated; }
        public long getTotalAuctionsCompleted() { return totalAuctionsCompleted; }
        public long getTotalAuctionsCancelled() { return totalAuctionsCancelled; }
        public double getTotalVolume() { return totalVolume; }
        public double getTotalFees() { return totalFees; }
        public Map<String, Long> getCategoryStats() { return new HashMap<>(categoryStats); }
        public Map<UUID, Long> getPlayerStats() { return new HashMap<>(playerStats); }
    }
    
    private AuctionManagerNew() {
        this.activeAuctions = new ConcurrentHashMap<>();
        this.completedAuctions = new ConcurrentHashMap<>();
        this.playerAuctions = new ConcurrentHashMap<>();
        this.categoryAuctions = new ConcurrentHashMap<>();
        
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.executorService = Executors.newFixedThreadPool(4);
        
        this.config = loadConfig();
        this.statistics = new AuctionStatistics();
        
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
        
        this.auctionDataPath = Paths.get("config", "neoessentials", "auctions");
        
        // Create data directory
        try {
            Files.createDirectories(auctionDataPath);
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to create auction data directory", e);
        }
        
        // Start background tasks
        startBackgroundTasks();
        
        // Load existing auctions
        loadAuctions();
        
        NeoEssentials.LOGGER.info("AuctionManagerNew initialized with {} active auctions", 
            activeAuctions.size());
    }
    
    public static AuctionManagerNew getInstance() {
        if (instance == null) {
            instance = new AuctionManagerNew();
        }
        return instance;
    }
    
    /**
     * Create a new auction
     */
    public CreateAuctionResult createAuction(ServerPlayer seller, ItemStack itemStack, 
                                           double startingBid, int durationMinutes, 
                                           AuctionNew.AuctionType auctionType, String category) {
        
        // Validate seller
        if (seller == null) {
            return new CreateAuctionResult(false, "Invalid seller", null);
        }
        
        UUID sellerId = seller.getUUID();
        String sellerName = seller.getName().getString();
        
        // Check auction limits
        int currentAuctions = getPlayerActiveAuctionCount(sellerId);
        if (currentAuctions >= config.maxActiveAuctionsPerPlayer) {
            return new CreateAuctionResult(false, 
                "Maximum active auctions reached (" + config.maxActiveAuctionsPerPlayer + ")", null);
        }
        
        // Validate item
        if (itemStack == null || itemStack.isEmpty()) {
            return new CreateAuctionResult(false, "Invalid item", null);
        }
        
        // Check banned items
        String itemId = ItemHandler.getItemId(itemStack.getItem());
        if (config.bannedItems.contains(itemId)) {
            return new CreateAuctionResult(false, "This item cannot be auctioned", null);
        }
        
        // Validate duration
        if (durationMinutes < config.minAuctionDurationMinutes || 
            durationMinutes > config.maxAuctionDurationHours * 60) {
            return new CreateAuctionResult(false, 
                "Duration must be between " + config.minAuctionDurationMinutes + " minutes and " + 
                config.maxAuctionDurationHours + " hours", null);
        }
        
        // Validate starting bid
        if (startingBid <= 0) {
            return new CreateAuctionResult(false, "Starting bid must be positive", null);
        }
        
        // Check category price limits
        String validCategory = validateCategory(category);
        if (config.categoryMinPrices.containsKey(validCategory) && 
            startingBid < config.categoryMinPrices.get(validCategory)) {
            return new CreateAuctionResult(false, 
                "Minimum starting bid for " + validCategory + " is " + 
                config.categoryMinPrices.get(validCategory), null);
        }
        
        // Check auction type availability
        if (!isAuctionTypeEnabled(auctionType)) {
            return new CreateAuctionResult(false, 
                "Auction type " + auctionType.getDisplayName() + " is not enabled", null);
        }
        
        // Remove item from player inventory
        if (!removeItemFromPlayer(seller, itemStack)) {
            return new CreateAuctionResult(false, "Could not remove item from inventory", null);
        }
        
        // Create the auction
        long durationMs = durationMinutes * 60 * 1000L;
        AuctionNew auction = new AuctionNew(sellerId, sellerName, itemStack, 
            startingBid, durationMs, auctionType, validCategory);
        
        // Store the auction
        activeAuctions.put(auction.getAuctionId(), auction);
        playerAuctions.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(auction);
        categoryAuctions.computeIfAbsent(validCategory, k -> new ArrayList<>()).add(auction);
        
        // Update statistics
        statistics.recordAuctionCreated(validCategory);
        
        // Save to disk
        saveAuction(auction);
        
        // Notify seller
        seller.sendSystemMessage(Component.literal(
            "§aAuction created successfully! ID: §e" + auction.getAuctionId().toString().substring(0, 8) + 
            "§a Duration: §e" + durationMinutes + " minutes"));
        
        NeoEssentials.LOGGER.info("Player {} created auction {} for {}", 
            sellerName, auction.getAuctionId().toString().substring(0, 8), 
            auction.getItemDisplayName());
        
        return new CreateAuctionResult(true, "Auction created successfully", auction);
    }
    
    /**
     * Place a bid on an auction
     */
    public AuctionNew.BidResult placeBid(UUID bidderId, String bidderName, 
                                        UUID auctionId, double bidAmount) {
        
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new AuctionNew.BidResult(false, "Auction not found");
        }
        
        // Delegate to auction object
        AuctionNew.BidResult result = auction.placeBid(bidderId, bidderName, bidAmount, false);
        
        if (result.isSuccess()) {
            // Notify watchers
            notifyWatchers(auction, "New bid placed: " + 
                auction.getCurrency().format(bidAmount) + " by " + bidderName);
            
            // Save updated auction
            saveAuction(auction);
        }
        
        return result;
    }
    
    /**
     * Buy out an auction immediately
     */
    public AuctionNew.BuyoutResult buyoutAuction(UUID buyerId, String buyerName, UUID auctionId) {
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new AuctionNew.BuyoutResult(false, "Auction not found");
        }
        
        AuctionNew.BuyoutResult result = auction.buyout(buyerId, buyerName);
        
        if (result.isSuccess()) {
            // Move to completed auctions
            activeAuctions.remove(auctionId);
            completedAuctions.put(auctionId, auction);
            
            // Update statistics
            statistics.recordAuctionCompleted(auction.getCurrentBid(), 
                auction.getSellerId(), buyerId);
            
            // Notify watchers
            notifyWatchers(auction, "Auction bought out by " + buyerName + 
                " for " + auction.getCurrency().format(auction.getBuyoutPrice()));
            
            // Save updated auction
            saveAuction(auction);
        }
        
        return result;
    }
    
    /**
     * Cancel an auction
     */
    public boolean cancelAuction(UUID sellerId, UUID auctionId) {
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return false;
        }
        
        if (!auction.getSellerId().equals(sellerId)) {
            return false; // Not the seller
        }
        
        if (auction.cancelAuction()) {
            // Remove from active auctions
            activeAuctions.remove(auctionId);
            
            // Return item to seller
            returnItemToSeller(auction);
            
            // Update statistics
            statistics.recordAuctionCancelled();
            
            // Notify watchers
            notifyWatchers(auction, "Auction cancelled by seller");
            
            // Save updated auction
            saveAuction(auction);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Set auto-bid for a player on an auction
     */
    public boolean setAutoBid(UUID bidderId, UUID auctionId, double maxAmount, double increment) {
        if (!config.enableAutoBidding) {
            return false;
        }
        
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return false;
        }
        
        return auction.setAutoBid(bidderId, maxAmount, increment);
    }
    
    /**
     * Remove auto-bid for a player on an auction
     */
    public boolean removeAutoBid(UUID bidderId, UUID auctionId) {
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return false;
        }
        
        return auction.removeAutoBid(bidderId);
    }
    
    /**
     * Get all active auctions
     */
    public List<AuctionNew> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }
    
    /**
     * Get active auctions by category
     */
    public List<AuctionNew> getActiveAuctionsByCategory(String category) {
        return activeAuctions.values().stream()
            .filter(auction -> auction.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
    
    /**
     * Get active auctions by player
     */
    public List<AuctionNew> getActiveAuctionsByPlayer(UUID playerId) {
        return activeAuctions.values().stream()
            .filter(auction -> auction.getSellerId().equals(playerId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get auction by ID
     */
    public AuctionNew getAuction(UUID auctionId) {
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction == null) {
            auction = completedAuctions.get(auctionId);
        }
        return auction;
    }
    
    /**
     * Search auctions by item name
     */
    public List<AuctionNew> searchAuctions(String searchTerm) {
        String lowerSearch = searchTerm.toLowerCase();
        return activeAuctions.values().stream()
            .filter(auction -> auction.getItemDisplayName().toLowerCase().contains(lowerSearch) ||
                             auction.getDescription().toLowerCase().contains(lowerSearch))
            .collect(Collectors.toList());
    }
    
    /**
     * Get player's active auction count
     */
    public int getPlayerActiveAuctionCount(UUID playerId) {
        return (int) activeAuctions.values().stream()
            .filter(auction -> auction.getSellerId().equals(playerId))
            .count();
    }
    
    /**
     * Get auctions ending soon (within specified minutes)
     */
    public List<AuctionNew> getAuctionsEndingSoon(int withinMinutes) {
        long withinMs = withinMinutes * 60 * 1000L;
        long currentTime = System.currentTimeMillis();
        
        return activeAuctions.values().stream()
            .filter(auction -> (auction.getEndTime() - currentTime) <= withinMs)
            .sorted((a, b) -> Long.compare(a.getTimeRemaining(), b.getTimeRemaining()))
            .collect(Collectors.toList());
    }
    
    /**
     * Add watcher to auction
     */
    public boolean addWatcher(UUID auctionId, String playerName) {
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.addWatcher(playerName);
            return true;
        }
        return false;
    }
    
    /**
     * Remove watcher from auction
     */
    public boolean removeWatcher(UUID auctionId, String playerName) {
        AuctionNew auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.removeWatcher(playerName);
            return true;
        }
        return false;
    }
    
    /**
     * Get auction statistics
     */
    public AuctionStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Get auction configuration
     */
    public AuctionConfig getConfig() {
        return config;
    }
    
    // Private helper methods
    
    private void startBackgroundTasks() {
        // Expire auctions task
        scheduler.scheduleAtFixedRate(this::processExpiredAuctions, 
            config.autoExpireCheckIntervalMinutes, 
            config.autoExpireCheckIntervalMinutes, 
            TimeUnit.MINUTES);
        
        // Save statistics task
        scheduler.scheduleAtFixedRate(this::saveStatistics, 10, 10, TimeUnit.MINUTES);
        
        NeoEssentials.LOGGER.info("Auction background tasks started");
    }
    
    private void processExpiredAuctions() {
        List<AuctionNew> expiredAuctions = activeAuctions.values().stream()
            .filter(auction -> !auction.isActive())
            .collect(Collectors.toList());
        
        for (AuctionNew auction : expiredAuctions) {
            // Remove from active auctions
            activeAuctions.remove(auction.getAuctionId());
            
            // Handle completion
            if (auction.getStatus() == AuctionNew.AuctionStatus.COMPLETED) {
                completedAuctions.put(auction.getAuctionId(), auction);
                statistics.recordAuctionCompleted(auction.getCurrentBid(), 
                    auction.getSellerId(), auction.getCurrentBidderId());
            } else {
                // Return item to seller for expired auctions
                returnItemToSeller(auction);
            }
            
            // Notify watchers
            notifyWatchers(auction, "Auction has ended");
            
            // Save updated auction
            saveAuction(auction);
        }
        
        if (!expiredAuctions.isEmpty()) {
            NeoEssentials.LOGGER.info("Processed {} expired auctions", expiredAuctions.size());
        }
    }
    
    private boolean removeItemFromPlayer(ServerPlayer player, ItemStack itemStack) {
        // Implementation would interact with player inventory
        // For now, assume it works
        return true;
    }
    
    private void returnItemToSeller(AuctionNew auction) {
        // Implementation would return item to player or store in claim system
        // For now, just log
        NeoEssentials.LOGGER.info("Returning item {} to seller {}", 
            auction.getItemDisplayName(), auction.getSellerName());
    }
    
    private void notifyWatchers(AuctionNew auction, String message) {
        if (!config.notifyWatchersOnBid) return;
        
        // Implementation would send messages to all watchers
        // For now, just log
        if (!auction.getWatchers().isEmpty()) {
            NeoEssentials.LOGGER.info("Notifying {} watchers of auction {}: {}", 
                auction.getWatchers().size(), 
                auction.getAuctionId().toString().substring(0, 8), 
                message);
        }
    }
    
    private String validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "misc";
        }
        
        String lowerCategory = category.toLowerCase().trim();
        if (config.allowedCategories.contains(lowerCategory)) {
            return lowerCategory;
        }
        
        return "misc";
    }
    
    private boolean isAuctionTypeEnabled(AuctionNew.AuctionType auctionType) {
        switch (auctionType) {
            case STANDARD:
                return true;
            case BUY_IT_NOW:
                return config.enableBuyoutFeature;
            case RESERVE:
                return config.enableReserveAuctions;
            case DUTCH:
                return config.enableDutchAuctions;
            case SILENT:
                return config.enableSilentAuctions;
            default:
                return false;
        }
    }
    
    private AuctionConfig loadConfig() {
        // Load configuration from file or return defaults
        // For now, return defaults
        return new AuctionConfig();
    }
    
    private void saveAuction(AuctionNew auction) {
        executorService.submit(() -> {
            try {
                String filename = auction.getAuctionId().toString() + ".json";
                Path auctionFile = auctionDataPath.resolve(filename);
                String json = gson.toJson(auction);
                Files.write(auctionFile, json.getBytes());
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to save auction " + 
                    auction.getAuctionId().toString().substring(0, 8), e);
            }
        });
    }
    
    private void loadAuctions() {
        try {
            if (!Files.exists(auctionDataPath)) {
                return;
            }
            
            Files.list(auctionDataPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadAuctionFromFile);
                
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load auctions", e);
        }
    }
    
    private void loadAuctionFromFile(Path auctionFile) {
        try {
            String json = new String(Files.readAllBytes(auctionFile));
            AuctionNew auction = gson.fromJson(json, AuctionNew.class);
            
            if (auction != null) {
                if (auction.isActive()) {
                    activeAuctions.put(auction.getAuctionId(), auction);
                } else {
                    completedAuctions.put(auction.getAuctionId(), auction);
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load auction from " + auctionFile, e);
        }
    }
    
    private void saveStatistics() {
        // Save statistics to file
        executorService.submit(() -> {
            try {
                Path statsFile = auctionDataPath.resolve("statistics.json");
                String json = gson.toJson(statistics);
                Files.write(statsFile, json.getBytes());
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Failed to save auction statistics", e);
            }
        });
    }
    
    public void shutdown() {
        NeoEssentials.LOGGER.info("Shutting down AuctionManagerNew...");
        
        // Save all pending data
        saveStatistics();
        
        // Shutdown executors
        scheduler.shutdown();
        executorService.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        NeoEssentials.LOGGER.info("AuctionManagerNew shutdown complete");
    }
    
    /**
     * Result class for auction creation
     */
    public static class CreateAuctionResult {
        private final boolean success;
        private final String message;
        private final AuctionNew auction;
        
        public CreateAuctionResult(boolean success, String message, AuctionNew auction) {
            this.success = success;
            this.message = message;
            this.auction = auction;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AuctionNew getAuction() { return auction; }
    }
}
