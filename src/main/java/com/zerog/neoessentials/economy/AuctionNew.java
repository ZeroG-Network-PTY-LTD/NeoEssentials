package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Enhanced auction data model with improved features and validation.
 * This is the new version that replaces the original Auction class.
 */
public class AuctionNew {
    
    // Core auction data
    private final UUID auctionId;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack itemStack;
    private final String itemDisplayName;
    private final int quantity;
    private final double startingBid;
    private final long startTime;
    private final long endTime;
    private final AuctionType auctionType;
    private final String category;
    
    // Bidding data
    private double currentBid;
    private UUID currentBidderId;
    private String currentBidderName;
    private final Map<UUID, Double> bidHistory;
    private final Map<UUID, Long> bidTimestamps;
    
    // Status and configuration
    private AuctionStatus status;
    private String description;
    private final Currency currency;
    private double buyoutPrice;
    private double reservePrice;
    private boolean featured;
    
    // Auto-bid data
    private final Map<UUID, AutoBidConfig> autoBids;
    
    // Metadata
    private final Map<String, Object> metadata;
    private final List<String> watchers;
    private int viewCount;
    
    public enum AuctionType {
        STANDARD("Standard Auction", "Traditional bidding auction"),
        BUY_IT_NOW("Buy It Now", "Fixed price immediate purchase"),
        RESERVE("Reserve Auction", "Auction with minimum sale price"),
        DUTCH("Dutch Auction", "Descending price auction"),
        SILENT("Silent Auction", "Hidden current bid auction");
        
        private final String displayName;
        private final String description;
        
        AuctionType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public enum AuctionStatus {
        ACTIVE("Active", "§a"),
        COMPLETED("Completed", "§2"),
        CANCELLED("Cancelled", "§c"),
        EXPIRED("Expired", "§7"),
        PENDING_PAYMENT("Pending Payment", "§e"),
        PENDING_DELIVERY("Pending Delivery", "§6");
        
        private final String displayName;
        private final String colorCode;
        
        AuctionStatus(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColorCode() { return colorCode; }
        public String getFormattedName() { return colorCode + displayName + "§r"; }
    }
    
    public static class AutoBidConfig {
        private final UUID bidderId;
        private final double maxAmount;
        private final double increment;
        private final boolean enabled;
        private double totalSpent;
        
        public AutoBidConfig(UUID bidderId, double maxAmount, double increment) {
            this.bidderId = bidderId;
            this.maxAmount = maxAmount;
            this.increment = increment;
            this.enabled = true;
            this.totalSpent = 0.0;
        }
        
        // Getters
        public UUID getBidderId() { return bidderId; }
        public double getMaxAmount() { return maxAmount; }
        public double getIncrement() { return increment; }
        public boolean isEnabled() { return enabled; }
        public double getTotalSpent() { return totalSpent; }
        public double getRemainingAmount() { return maxAmount - totalSpent; }
        
        public void addSpent(double amount) { this.totalSpent += amount; }
    }
    
    /**
     * Create a new auction with full configuration
     */
    public AuctionNew(UUID sellerId, String sellerName, ItemStack itemStack, 
                     double startingBid, long durationMs, AuctionType auctionType, String category) {
        this.auctionId = UUID.randomUUID();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemStack = itemStack.copy();
        this.itemDisplayName = ItemHandler.getItemDisplayName(ItemHandler.getItemId(itemStack.getItem()));
        this.quantity = itemStack.getCount();
        this.startingBid = startingBid;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + durationMs;
        this.auctionType = auctionType;
        this.category = category != null ? category : "misc";
        
        this.currentBid = startingBid;
        this.currentBidderId = null;
        this.currentBidderName = null;
        this.bidHistory = new ConcurrentHashMap<>();
        this.bidTimestamps = new ConcurrentHashMap<>();
        
        this.status = AuctionStatus.ACTIVE;
        this.description = "";
        this.currency = CurrencyManager.getInstance().getDefaultCurrency();
        this.buyoutPrice = 0.0;
        this.reservePrice = 0.0;
        this.featured = false;
        
        this.autoBids = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.watchers = new ArrayList<>();
        this.viewCount = 0;
    }
    
    /**
     * Place a bid on this auction with comprehensive validation
     */
    public BidResult placeBid(UUID bidderId, String bidderName, double bidAmount, boolean isAutoBid) {
        // Pre-bid validation
        BidResult validation = validateBid(bidderId, bidAmount);
        if (!validation.isSuccess()) {
            return validation;
        }
        
        // Refund previous bidder if exists
        if (currentBidderId != null && !isAutoBid) {
            refundPreviousBidder();
        }
        
        // Deduct funds from new bidder
        EconomyManager economyManager = EconomyManager.getInstance();
        if (!economyManager.makeSmartPayment(bidderId, bidAmount, currency, 
            "Auction bid: " + itemDisplayName)) {
            return new BidResult(false, "Insufficient funds for bid");
        }
        
        // Record the bid
        recordBid(bidderId, bidderName, bidAmount);
        
        // Trigger auto-bids from other players
        if (!isAutoBid) {
            processAutoBids(bidAmount);
        }
        
        return new BidResult(true, "Bid placed successfully");
    }
    
    /**
     * Validate if a bid can be placed
     */
    private BidResult validateBid(UUID bidderId, double bidAmount) {
        if (status != AuctionStatus.ACTIVE) {
            return new BidResult(false, "Auction is not active");
        }
        
        if (System.currentTimeMillis() > endTime) {
            return new BidResult(false, "Auction has expired");
        }
        
        if (bidderId.equals(sellerId)) {
            return new BidResult(false, "Cannot bid on your own auction");
        }
        
        if (bidAmount <= currentBid) {
            return new BidResult(false, "Bid must be higher than current bid of " + 
                currency.format(currentBid));
        }
        
        // Check minimum bid increment (1% of current bid, minimum 1.0)
        double minIncrement = Math.max(1.0, currentBid * 0.01);
        if (bidAmount < currentBid + minIncrement) {
            return new BidResult(false, "Minimum bid increment is " + 
                currency.format(minIncrement));
        }
        
        // Check if bidder has sufficient funds
        EconomyManager economyManager = EconomyManager.getInstance();
        double totalAvailable = economyManager.getTotalAvailableFunds(bidderId, currency);
        if (totalAvailable < bidAmount) {
            return new BidResult(false, "Insufficient funds. Available: " + 
                currency.format(totalAvailable));
        }
        
        return new BidResult(true, "Bid validation passed");
    }
    
    /**
     * Record a successful bid
     */
    private void recordBid(UUID bidderId, String bidderName, double bidAmount) {
        currentBid = bidAmount;
        currentBidderId = bidderId;
        currentBidderName = bidderName;
        
        bidHistory.put(bidderId, bidAmount);
        bidTimestamps.put(bidderId, System.currentTimeMillis());
        
        // Record transaction
        EconomyManager economyManager = EconomyManager.getInstance();
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            bidderId,
            null, // held by auction system
            bidAmount,
            currency,
            "Auction bid: " + itemDisplayName + " (ID: " + auctionId.toString().substring(0, 8) + ")",
            Transaction.TransactionType.PURCHASE,
            System.currentTimeMillis()
        );
        economyManager.getTransactionManager().recordTransaction(transaction);
    }
    
    /**
     * Refund the previous bidder
     */
    private void refundPreviousBidder() {
        if (currentBidderId == null) return;
        
        EconomyManager economyManager = EconomyManager.getInstance();
        economyManager.addBalance(currentBidderId, currency, currentBid, 
            "Auction refund: Outbid on " + itemDisplayName);
        
        // Record refund transaction
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            null, // from auction system
            currentBidderId,
            currentBid,
            currency,
            "Auction refund: Outbid on " + itemDisplayName,
            Transaction.TransactionType.REFUND,
            System.currentTimeMillis()
        );
        economyManager.getTransactionManager().recordTransaction(transaction);
    }
    
    /**
     * Process auto-bids from other players
     */
    private void processAutoBids(double newBidAmount) {
        List<AutoBidConfig> eligibleAutoBids = autoBids.values().stream()
            .filter(config -> !config.getBidderId().equals(currentBidderId))
            .filter(config -> config.isEnabled())
            .filter(config -> config.getRemainingAmount() > newBidAmount)
            .sorted((a, b) -> Double.compare(b.getMaxAmount(), a.getMaxAmount()))
            .collect(Collectors.toList());
        
        for (AutoBidConfig config : eligibleAutoBids) {
            double nextBid = newBidAmount + config.getIncrement();
            if (nextBid <= config.getRemainingAmount()) {
                // Place auto-bid
                String bidderName = "AutoBid"; // Could look up actual name
                BidResult result = placeBid(config.getBidderId(), bidderName, nextBid, true);
                if (result.isSuccess()) {
                    config.addSpent(nextBid);
                    break; // Only one auto-bid wins
                }
            }
        }
    }
    
    /**
     * Complete the auction and handle payouts
     */
    public boolean completeAuction() {
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }
        
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (currentBidderId != null) {
            // Check if reserve price is met
            if (reservePrice > 0 && currentBid < reservePrice) {
                // Reserve not met - refund and expire
                refundPreviousBidder();
                status = AuctionStatus.EXPIRED;
                return true;
            }
            
            // Transfer money to seller
            double sellerAmount = currentBid;
            
            // Apply auction house fee if configured
            double feeRate = getFeeRate();
            if (feeRate > 0) {
                double fee = sellerAmount * feeRate;
                sellerAmount -= fee;
                // Fee goes to server/auction house
            }
            
            economyManager.addBalance(sellerId, currency, sellerAmount,
                "Auction sale: " + itemDisplayName);
            
            status = AuctionStatus.COMPLETED;
            
            // Record completion transaction
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                null, // from auction system
                sellerId,
                sellerAmount,
                currency,
                "Auction sale: " + itemDisplayName + " to " + currentBidderName,
                Transaction.TransactionType.SALE,
                System.currentTimeMillis()
            );
            economyManager.getTransactionManager().recordTransaction(transaction);
            
        } else {
            // No bids - return item to seller
            status = AuctionStatus.EXPIRED;
        }
        
        return true;
    }
    
    /**
     * Cancel the auction
     */
    public boolean cancelAuction() {
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }
        
        // Refund current bidder if exists
        if (currentBidderId != null) {
            refundPreviousBidder();
        }
        
        status = AuctionStatus.CANCELLED;
        return true;
    }
    
    /**
     * Buy out the auction immediately (if available)
     */
    public BuyoutResult buyout(UUID buyerId, String buyerName) {
        if (buyoutPrice <= 0) {
            return new BuyoutResult(false, "This auction does not support buyout");
        }
        
        if (status != AuctionStatus.ACTIVE) {
            return new BuyoutResult(false, "Auction is not active");
        }
        
        if (buyerId.equals(sellerId)) {
            return new BuyoutResult(false, "Cannot buy out your own auction");
        }
        
        EconomyManager economyManager = EconomyManager.getInstance();
        
        // Check funds
        if (economyManager.getTotalAvailableFunds(buyerId, currency) < buyoutPrice) {
            return new BuyoutResult(false, "Insufficient funds for buyout");
        }
        
        // Refund current bidder
        if (currentBidderId != null) {
            refundPreviousBidder();
        }
        
        // Charge buyer
        if (!economyManager.makeSmartPayment(buyerId, buyoutPrice, currency, 
            "Auction buyout: " + itemDisplayName)) {
            return new BuyoutResult(false, "Payment failed");
        }
        
        // Set as winner
        currentBidderId = buyerId;
        currentBidderName = buyerName;
        currentBid = buyoutPrice;
        
        // Complete immediately
        completeAuction();
        
        return new BuyoutResult(true, "Buyout successful");
    }
    
    /**
     * Add or update auto-bid configuration
     */
    public boolean setAutoBid(UUID bidderId, double maxAmount, double increment) {
        if (bidderId.equals(sellerId)) {
            return false;
        }
        
        if (maxAmount <= currentBid) {
            return false;
        }
        
        autoBids.put(bidderId, new AutoBidConfig(bidderId, maxAmount, increment));
        return true;
    }
    
    /**
     * Remove auto-bid configuration
     */
    public boolean removeAutoBid(UUID bidderId) {
        return autoBids.remove(bidderId) != null;
    }
    
    /**
     * Check if auction is still active
     */
    public boolean isActive() {
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }
        
        if (System.currentTimeMillis() > endTime) {
            completeAuction();
            return false;
        }
        
        return true;
    }
    
    /**
     * Get time remaining in auction
     */
    public long getTimeRemaining() {
        if (status != AuctionStatus.ACTIVE) {
            return 0;
        }
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    /**
     * Format time remaining as human-readable string
     */
    public String getFormattedTimeRemaining() {
        long remaining = getTimeRemaining();
        if (remaining <= 0) {
            return "§7Expired§r";
        }
        
        long days = remaining / (24 * 60 * 60 * 1000);
        long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    /**
     * Get auction house fee rate
     */
    private double getFeeRate() {
        // Could be configurable per category or seller status
        return 0.05; // 5% default fee
    }
    
    /**
     * Add a watcher to this auction
     */
    public void addWatcher(String playerName) {
        if (!watchers.contains(playerName)) {
            watchers.add(playerName);
        }
    }
    
    /**
     * Remove a watcher from this auction
     */
    public void removeWatcher(String playerName) {
        watchers.remove(playerName);
    }
    
    /**
     * Increment view count
     */
    public void incrementViewCount() {
        viewCount++;
    }
    
    // Result classes for better return handling
    public static class BidResult {
        private final boolean success;
        private final String message;
        
        public BidResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    public static class BuyoutResult {
        private final boolean success;
        private final String message;
        
        public BuyoutResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    // Comprehensive getters
    public UUID getAuctionId() { return auctionId; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public String getItemDisplayName() { return itemDisplayName; }
    public int getQuantity() { return quantity; }
    public double getStartingBid() { return startingBid; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public AuctionType getAuctionType() { return auctionType; }
    public String getCategory() { return category; }
    public double getCurrentBid() { return currentBid; }
    public UUID getCurrentBidderId() { return currentBidderId; }
    public String getCurrentBidderName() { return currentBidderName; }
    public Map<UUID, Double> getBidHistory() { return new HashMap<>(bidHistory); }
    public AuctionStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public Currency getCurrency() { return currency; }
    public double getBuyoutPrice() { return buyoutPrice; }
    public double getReservePrice() { return reservePrice; }
    public boolean isFeatured() { return featured; }
    public Map<UUID, AutoBidConfig> getAutoBids() { return new HashMap<>(autoBids); }
    public List<String> getWatchers() { return new ArrayList<>(watchers); }
    public int getViewCount() { return viewCount; }
    
    // Setters for configuration
    public void setDescription(String description) { this.description = description; }
    public void setBuyoutPrice(double buyoutPrice) { this.buyoutPrice = buyoutPrice; }
    public void setReservePrice(double reservePrice) { this.reservePrice = reservePrice; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    
    @Override
    public String toString() {
        return "AuctionNew{" +
                "id=" + auctionId.toString().substring(0, 8) + "..." +
                ", item='" + itemDisplayName + '\'' +
                ", quantity=" + quantity +
                ", currentBid=" + currency.format(currentBid) +
                ", status=" + status.getDisplayName() +
                ", timeRemaining='" + getFormattedTimeRemaining() + '\'' +
                ", type=" + auctionType.getDisplayName() +
                '}';
    }
}
