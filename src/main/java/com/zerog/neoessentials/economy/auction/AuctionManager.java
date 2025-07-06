package com.zerog.neoessentials.economy.auction;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.EconomyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages auction operations and item listings
 */
public class AuctionManager {
    
    private final EconomyManager economyManager;
    private final Map<UUID, AuctionItem> activeAuctions = new ConcurrentHashMap<>();
    private final Map<UUID, AuctionItem> completedAuctions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    
    public AuctionManager(EconomyManager economyManager) {
        this.economyManager = Objects.requireNonNull(economyManager, "Economy manager cannot be null");
        this.scheduler = economyManager.getScheduler();
        
        // Start auction expiration checker
        scheduler.scheduleAtFixedRate(this::processExpiredAuctions, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Creates a new auction
     */
    public AuctionResult createAuction(ServerPlayer seller, ItemStack item, BigDecimal startingPrice, 
                                     BigDecimal buyNowPrice, int durationHours, String description) {
        if (!economyManager.isEnabled()) {
            return new AuctionResult(false, "Economy system is disabled");
        }
        
        if (item.isEmpty()) {
            return new AuctionResult(false, "Cannot auction empty items");
        }
        
        if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new AuctionResult(false, "Starting price must be positive");
        }
        
        if (durationHours < 1 || durationHours > 168) { // 1 hour to 1 week
            return new AuctionResult(false, "Duration must be between 1 and 168 hours");
        }
        
        // Check if player has the item
        if (!hasItemInInventory(seller, item)) {
            return new AuctionResult(false, "You don't have this item in your inventory");
        }
        
        // Remove item from player's inventory
        if (!removeItemFromInventory(seller, item)) {
            return new AuctionResult(false, "Failed to remove item from inventory");
        }
        
        try {
            Currency defaultCurrency = economyManager.getDefaultCurrency();
            LocalDateTime endTime = LocalDateTime.now().plusHours(durationHours);
            
            AuctionItem auction = new AuctionItem.Builder()
                    .sellerId(seller.getUUID())
                    .sellerName(seller.getName().getString())
                    .itemStack(item)
                    .startingPrice(startingPrice)
                    .buyNowPrice(buyNowPrice)
                    .currency(defaultCurrency)
                    .endTime(endTime)
                    .description(description != null ? description : "")
                    .build();
            
            activeAuctions.put(auction.getId(), auction);
            
            NeoEssentials.LOGGER.info("Created auction {} by player {}", auction.getId(), seller.getName().getString());
            
            String message = String.format("Created auction for %s with starting price %s (ID: %s)", 
                    item.getDisplayName().getString(),
                    defaultCurrency.format(startingPrice),
                    auction.getId().toString().substring(0, 8));
            
            return new AuctionResult(true, message, auction);
            
        } catch (Exception e) {
            // Return item to player if auction creation fails
            seller.getInventory().add(item);
            NeoEssentials.LOGGER.error("Failed to create auction", e);
            return new AuctionResult(false, "Failed to create auction: " + e.getMessage());
        }
    }
    
    /**
     * Places a bid on an auction
     */
    public BidResult placeBid(ServerPlayer bidder, UUID auctionId, BigDecimal bidAmount) {
        if (!economyManager.isEnabled()) {
            return new BidResult(false, "Economy system is disabled");
        }
        
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new BidResult(false, "Auction not found or no longer active");
        }
        
        if (!auction.isActive()) {
            return new BidResult(false, "This auction has ended");
        }
        
        if (auction.getSellerId().equals(bidder.getUUID())) {
            return new BidResult(false, "You cannot bid on your own auction");
        }
        
        // Check if bid is higher than current bid
        BigDecimal minimumBid = auction.getCurrentBid().add(BigDecimal.ONE);
        if (bidAmount.compareTo(minimumBid) < 0) {
            return new BidResult(false, "Bid must be at least " + auction.getCurrency().format(minimumBid));
        }
        
        // Check if player has enough money
        EconomyAccount account = economyManager.getOrCreateAccount(bidder.getUUID(), bidder.getName().getString());
        if (account == null) {
            return new BidResult(false, "Could not access your account");
        }
        
        if (!account.hasBalance(auction.getCurrency(), bidAmount)) {
            return new BidResult(false, "Insufficient funds. Required: " + auction.getCurrency().format(bidAmount));
        }
        
        try {
            // Refund previous bidder if any
            if (auction.getCurrentBidderId() != null) {
                economyManager.addMoney(auction.getCurrentBidderId(), auction.getCurrentBid(), 
                        auction.getCurrency(), "Auction bid refund");
            }
            
            // Take money from new bidder
            if (!economyManager.subtractMoney(bidder.getUUID(), bidAmount, auction.getCurrency(), 
                    "Auction bid: " + auction.getItemStack().getDisplayName().getString())) {
                return new BidResult(false, "Failed to process payment");
            }
            
            // Update auction with new bid
            AuctionItem updatedAuction = auction.withBid(bidder.getUUID(), bidder.getName().getString(), bidAmount);
            activeAuctions.put(auctionId, updatedAuction);
            
            String message = String.format("Successfully bid %s on %s", 
                    auction.getCurrency().format(bidAmount),
                    auction.getItemStack().getDisplayName().getString());
            
            return new BidResult(true, message, updatedAuction);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error processing bid", e);
            return new BidResult(false, "An error occurred while processing your bid");
        }
    }
    
    /**
     * Executes buy-now purchase
     */
    public BuyNowResult buyNow(ServerPlayer buyer, UUID auctionId) {
        if (!economyManager.isEnabled()) {
            return new BuyNowResult(false, "Economy system is disabled");
        }
        
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new BuyNowResult(false, "Auction not found or no longer active");
        }
        
        if (!auction.isActive()) {
            return new BuyNowResult(false, "This auction has ended");
        }
        
        if (!auction.hasBuyNow()) {
            return new BuyNowResult(false, "This auction does not have a buy-now option");
        }
        
        if (auction.getSellerId().equals(buyer.getUUID())) {
            return new BuyNowResult(false, "You cannot buy your own auction");
        }
        
        BigDecimal buyNowPrice = auction.getBuyNowPrice();
        
        // Check if player has enough money
        EconomyAccount account = economyManager.getOrCreateAccount(buyer.getUUID(), buyer.getName().getString());
        if (account == null) {
            return new BuyNowResult(false, "Could not access your account");
        }
        
        if (!account.hasBalance(auction.getCurrency(), buyNowPrice)) {
            return new BuyNowResult(false, "Insufficient funds. Required: " + auction.getCurrency().format(buyNowPrice));
        }
        
        try {
            // Process buy-now purchase
            if (!economyManager.subtractMoney(buyer.getUUID(), buyNowPrice, auction.getCurrency(), 
                    "Auction buy-now: " + auction.getItemStack().getDisplayName().getString())) {
                return new BuyNowResult(false, "Failed to process payment");
            }
            
            // Pay seller
            economyManager.addMoney(auction.getSellerId(), buyNowPrice, auction.getCurrency(), 
                    "Auction sale: " + auction.getItemStack().getDisplayName().getString());
            
            // Give item to buyer
            buyer.getInventory().add(auction.getItemStack());
            
            // Refund any existing bidder
            if (auction.getCurrentBidderId() != null) {
                economyManager.addMoney(auction.getCurrentBidderId(), auction.getCurrentBid(), 
                        auction.getCurrency(), "Auction bid refund (buy-now)");
            }
            
            // Move auction to completed
            AuctionItem completedAuction = auction.withStatus(AuctionItem.Status.COMPLETED);
            activeAuctions.remove(auctionId);
            completedAuctions.put(auctionId, completedAuction);
            
            String message = String.format("Successfully purchased %s for %s", 
                    auction.getItemStack().getDisplayName().getString(),
                    auction.getCurrency().format(buyNowPrice));
            
            return new BuyNowResult(true, message, completedAuction);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error processing buy-now", e);
            return new BuyNowResult(false, "An error occurred while processing your purchase");
        }
    }
    
    /**
     * Cancels an auction (only by seller or admin)
     */
    public AuctionResult cancelAuction(ServerPlayer player, UUID auctionId, boolean isAdmin) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return new AuctionResult(false, "Auction not found or no longer active");
        }
        
        if (!isAdmin && !auction.getSellerId().equals(player.getUUID())) {
            return new AuctionResult(false, "You can only cancel your own auctions");
        }
        
        if (auction.hasBids() && !isAdmin) {
            return new AuctionResult(false, "Cannot cancel auction with existing bids");
        }
        
        try {
            // Refund current bidder if any
            if (auction.getCurrentBidderId() != null) {
                economyManager.addMoney(auction.getCurrentBidderId(), auction.getCurrentBid(), 
                        auction.getCurrency(), "Auction cancelled - bid refund");
            }
            
            // Return item to seller
            // Try to find seller online first
            ServerPlayer seller = economyManager.getServer().getPlayerList().getPlayer(auction.getSellerId());
            if (seller != null) {
                seller.getInventory().add(auction.getItemStack());
            } else {
                // Store item for offline collection (simplified - in production, implement offline storage)
                NeoEssentials.LOGGER.warn("Seller {} is offline, item needs to be stored for collection", auction.getSellerName());
            }
            
            // Move to completed with cancelled status
            AuctionItem cancelledAuction = auction.withStatus(AuctionItem.Status.CANCELLED);
            activeAuctions.remove(auctionId);
            completedAuctions.put(auctionId, cancelledAuction);
            
            return new AuctionResult(true, "Auction cancelled successfully", cancelledAuction);
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error cancelling auction", e);
            return new AuctionResult(false, "Failed to cancel auction");
        }
    }
    
    /**
     * Gets all active auctions
     */
    public List<AuctionItem> getActiveAuctions() {
        return activeAuctions.values().stream()
                .filter(AuctionItem::isActive)
                .sorted(Comparator.comparing(AuctionItem::getEndTime))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets auctions by seller
     */
    public List<AuctionItem> getAuctionsBySeller(UUID sellerId) {
        List<AuctionItem> result = new ArrayList<>();
        result.addAll(activeAuctions.values().stream()
                .filter(auction -> auction.getSellerId().equals(sellerId))
                .collect(Collectors.toList()));
        result.addAll(completedAuctions.values().stream()
                .filter(auction -> auction.getSellerId().equals(sellerId))
                .collect(Collectors.toList()));
        return result;
    }
    
    /**
     * Gets completed auctions
     */
    public List<AuctionItem> getCompletedAuctions() {
        return new ArrayList<>(completedAuctions.values());
    }
    
    /**
     * Gets auctions for a specific player (seller)
     */
    public List<AuctionItem> getPlayerAuctions(UUID playerId) {
        return getAuctionsBySeller(playerId);
    }
    
    /**
     * Gets auctions where the player has placed bids (is the current highest bidder)
     */
    public List<AuctionItem> getBiddedAuctions(UUID playerId) {
        return activeAuctions.values().stream()
                .filter(auction -> playerId.equals(auction.getCurrentBidderId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a specific auction by ID
     */
    public AuctionItem getAuction(UUID auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction != null) return auction;
        return completedAuctions.get(auctionId);
    }
    
    /**
     * Gets global auction statistics
     */
    public AuctionStatistics getGlobalStatistics() {
        // For now, return a basic implementation
        // In production, this should calculate actual statistics
        return new AuctionStatistics(
            activeAuctions.size(),
            completedAuctions.size(),
            (int) activeAuctions.values().stream()
                .filter(a -> a.getTimeRemainingMinutes() < 60)
                .count()
        );
    }
    
    /**
     * Forces an auction to end (admin command)
     */
    public boolean forceEndAuction(UUID auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) return false;
        
        try {
            // Process the auction ending
            processAuctionEnd(auction);
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error force-ending auction", e);
            return false;
        }
    }
    
    /**
     * Cleans up expired auctions
     */
    public int cleanupExpiredAuctions() {
        List<AuctionItem> expired = activeAuctions.values().stream()
            .filter(auction -> !auction.isActive())
            .collect(Collectors.toList());
            
        for (AuctionItem auction : expired) {
            processAuctionEnd(auction);
        }
        
        return expired.size();
    }

    /**
     * Gets the statistics
     */
    public AuctionStatistics getStatistics() {
        int totalActive = activeAuctions.size();
        int totalCompleted = completedAuctions.size();
        long expiringSoon = activeAuctions.values().stream()
                .filter(auction -> auction.getTimeRemainingMinutes() < 60)
                .count();
        
        return new AuctionStatistics(totalActive, totalCompleted, (int) expiringSoon);
    }
    
    /**
     * Processes expired auctions
     */
    private void processExpiredAuctions() {
        List<AuctionItem> expiredAuctions = activeAuctions.values().stream()
                .filter(AuctionItem::hasEnded)
                .collect(Collectors.toList());
        
        for (AuctionItem auction : expiredAuctions) {
            try {
                processAuctionEnd(auction);
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error processing expired auction {}", auction.getId(), e);
            }
        }
    }
    
    private void processAuctionEnd(AuctionItem auction) {
        if (auction.hasBids()) {
            // Auction won - transfer money and item
            economyManager.addMoney(auction.getSellerId(), auction.getCurrentBid(), 
                    auction.getCurrency(), "Auction sale: " + auction.getItemStack().getDisplayName().getString());
            
            // Give item to winner
            ServerPlayer winner = economyManager.getServer().getPlayerList().getPlayer(auction.getCurrentBidderId());
            if (winner != null) {
                winner.getInventory().add(auction.getItemStack());
            } else {
                // Store item for offline collection
                NeoEssentials.LOGGER.warn("Auction winner {} is offline, item needs to be stored", auction.getCurrentBidderName());
            }
        } else {
            // No bids - return item to seller
            ServerPlayer seller = economyManager.getServer().getPlayerList().getPlayer(auction.getSellerId());
            if (seller != null) {
                seller.getInventory().add(auction.getItemStack());
            } else {
                // Store item for offline collection
                NeoEssentials.LOGGER.warn("Seller {} is offline, item needs to be stored", auction.getSellerName());
            }
        }
        
        // Move to completed
        AuctionItem completedAuction = auction.withStatus(AuctionItem.Status.COMPLETED);
        activeAuctions.remove(auction.getId());
        completedAuctions.put(auction.getId(), completedAuction);
        
        NeoEssentials.LOGGER.info("Processed expired auction {}", auction.getId());
    }
    
    // Helper methods
    private boolean hasItemInInventory(ServerPlayer player, ItemStack item) {
        int needed = item.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, item)) {
                needed -= slotStack.getCount();
                if (needed <= 0) return true;
            }
        }
        return false;
    }
    
    private boolean removeItemFromInventory(ServerPlayer player, ItemStack item) {
        int toRemove = item.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize() && toRemove > 0; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, item)) {
                int removeFromSlot = Math.min(toRemove, slotStack.getCount());
                slotStack.shrink(removeFromSlot);
                toRemove -= removeFromSlot;
            }
        }
        return toRemove == 0;
    }
    
    // Result classes
    public static class AuctionResult {
        private final boolean success;
        private final String message;
        private final AuctionItem auction;
        
        public AuctionResult(boolean success, String message) {
            this(success, message, null);
        }
        
        public AuctionResult(boolean success, String message, AuctionItem auction) {
            this.success = success;
            this.message = message;
            this.auction = auction;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AuctionItem getAuction() { return auction; }
    }
    
    public static class BidResult {
        private final boolean success;
        private final String message;
        private final AuctionItem auction;
        
        public BidResult(boolean success, String message) {
            this(success, message, null);
        }
        
        public BidResult(boolean success, String message, AuctionItem auction) {
            this.success = success;
            this.message = message;
            this.auction = auction;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AuctionItem getAuction() { return auction; }
    }
    
    public static class BuyNowResult {
        private final boolean success;
        private final String message;
        private final AuctionItem auction;
        
        public BuyNowResult(boolean success, String message) {
            this(success, message, null);
        }
        
        public BuyNowResult(boolean success, String message, AuctionItem auction) {
            this.success = success;
            this.message = message;
            this.auction = auction;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AuctionItem getAuction() { return auction; }
    }
    
    public static class AuctionStatistics {
        private final int totalActive;
        private final int totalCompleted;
        private final int expiringSoon;
        private final BigDecimal totalValue;
        private final BigDecimal averageValue;
        private final int totalAuctions;
        
        public AuctionStatistics(int totalActive, int totalCompleted, int expiringSoon) {
            this.totalActive = totalActive;
            this.totalCompleted = totalCompleted;
            this.expiringSoon = expiringSoon;
            this.totalAuctions = totalActive + totalCompleted;
            this.totalValue = BigDecimal.ZERO; // TODO: Calculate from actual data
            this.averageValue = totalAuctions > 0 ? totalValue.divide(BigDecimal.valueOf(totalAuctions)) : BigDecimal.ZERO;
        }
        
        public int getTotalActive() { return totalActive; }
        public int getTotalCompleted() { return totalCompleted; }
        public int getExpiringSoon() { return expiringSoon; }
        public int getTotalAuctions() { return totalAuctions; }
        public int getActiveAuctions() { return totalActive; }
        public int getCompletedAuctions() { return totalCompleted; }
        public BigDecimal getTotalValue() { return totalValue; }
        public BigDecimal getAverageValue() { return averageValue; }
    }
}
