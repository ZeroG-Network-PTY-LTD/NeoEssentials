package com.zerog.neoessentials.economy;

import java.util.UUID;

/**
 * Represents an auction in the NeoEssentials auction house system.
 * Supports timed auctions with bidding functionality.
 */
public class Auction {
    private final UUID auctionId;
    private final UUID sellerId;
    private final String itemId;
    private final String itemName;
    private final int quantity;
    private final double startingBid;
    private final long startTime;
    private final long endTime;
    private final AuctionType auctionType;
    
    private double currentBid;
    private UUID currentBidder;
    private AuctionStatus status;
    private String description;
    private final Currency currency;
    private double buyoutPrice; // For BUY_IT_NOW type auctions
    private UUID winnerId; // The final winner of the auction

    public enum AuctionType {
        STANDARD,       // Regular auction with bidding
        BUY_IT_NOW,     // Fixed price, immediate purchase
        RESERVE,        // Auction with minimum sale price
        DUTCH          // Descending price auction
    }
    
    public enum AuctionStatus {
        ACTIVE,         // Auction is ongoing
        COMPLETED,      // Auction ended with winner
        CANCELLED,      // Auction cancelled by seller
        EXPIRED         // Auction ended without bids
    }
    
    /**
     * Create a new auction
     * 
     * @param sellerId The seller's UUID
     * @param itemId The item identifier
     * @param itemName The item display name
     * @param quantity The quantity being auctioned
     * @param startingBid The starting bid amount
     * @param duration Duration in milliseconds
     */
    public Auction(UUID sellerId, String itemId, String itemName, int quantity, 
                  double startingBid, long duration) {
        this.auctionId = UUID.randomUUID();
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.startingBid = startingBid;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + duration;
        this.auctionType = AuctionType.STANDARD;
        this.currentBid = startingBid;
        this.currentBidder = null;
        this.status = AuctionStatus.ACTIVE;
        this.description = "";
        this.currency = CurrencyManager.getInstance().getDefaultCurrency();
        this.buyoutPrice = 0; // Default to 0 for non BUY_IT_NOW auctions
        this.winnerId = null;
    }
    
    /**
     * Place a bid on the auction
     * 
     * @param bidderId The bidder's UUID
     * @param bidAmount The bid amount
     * @return true if bid was successful
     */
    public boolean placeBid(UUID bidderId, double bidAmount) {
        if (status != AuctionStatus.ACTIVE || System.currentTimeMillis() > endTime) {
            return false;
        }
        
        if (bidderId.equals(sellerId)) {
            return false; // Seller cannot bid on own auction
        }
        
        if (bidAmount <= currentBid) {
            return false; // Bid must be higher than current bid
        }
        
        // Check if bidder has sufficient funds
        EconomyManager economyManager = EconomyManager.getInstance();
        if (economyManager.getBalance(bidderId, currency) < bidAmount) {
            return false;
        }
        
        // Refund previous bidder if exists
        if (currentBidder != null) {
            economyManager.addBalance(currentBidder, currency, currentBid, 
                "Auction refund: Outbid on " + itemName);
        }
        
        // Hold the new bid amount
        if (!economyManager.removeBalance(bidderId, currency, bidAmount, 
            "Auction bid: " + itemName)) {
            return false;
        }
        
        currentBid = bidAmount;
        currentBidder = bidderId;
        
        return true;
    }
    
    /**
     * Complete the auction
     * 
     * @return true if auction was completed successfully
     */
    public boolean completeAuction() {
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }
        
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (currentBidder != null) {
            // Transfer money to seller
            economyManager.addBalance(sellerId, currency, currentBid, 
                "Auction sale: " + itemName);
            
            // Mark as completed
            status = AuctionStatus.COMPLETED;
        } else {
            // No bids - mark as expired
            status = AuctionStatus.EXPIRED;
        }
        
        return true;
    }
    
    /**
     * Cancel the auction (seller only)
     * 
     * @return true if auction was cancelled successfully
     */
    public boolean cancelAuction() {
        if (status != AuctionStatus.ACTIVE) {
            return false;
        }
        
        // Refund current bidder if exists
        if (currentBidder != null) {
            EconomyManager economyManager = EconomyManager.getInstance();
            economyManager.addBalance(currentBidder, currency, currentBid, 
                "Auction refund: Cancelled auction for " + itemName);
        }
        
        status = AuctionStatus.CANCELLED;
        return true;
    }
    
    /**
     * Check if auction is still active
     * 
     * @return true if auction is active and not expired
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
     * 
     * @return Time remaining in milliseconds, or 0 if expired
     */
    public long getTimeRemaining() {
        if (status != AuctionStatus.ACTIVE) {
            return 0;
        }
        
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    /**
     * Format time remaining as human-readable string
     * 
     * @return Formatted time string
     */
    public String getFormattedTimeRemaining() {
        long remaining = getTimeRemaining();
        if (remaining <= 0) {
            return "Expired";
        }
        
        long hours = remaining / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    /**
     * Get the buyout price for immediate purchase
     * 
     * @return The buyout price, or 0 if not available
     */
    public double getBuyoutPrice() {
        return buyoutPrice;
    }

    /**
     * Set the buyout price
     * 
     * @param buyoutPrice The new buyout price
     */
    public void setBuyoutPrice(double buyoutPrice) {
        this.buyoutPrice = buyoutPrice;
    }

    /**
     * Get the winner of the auction
     * 
     * @return The winner's UUID, or null if no winner yet
     */
    public UUID getWinnerId() {
        return winnerId;
    }

    /**
     * Set the winner of the auction
     * 
     * @param winnerId The winner's UUID
     */
    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    /**
     * Set the current bid amount
     * 
     * @param currentBid The new current bid
     */
    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }
    
    // Getters
    public UUID getAuctionId() { return auctionId; }
    public UUID getSellerId() { return sellerId; }
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public double getStartingBid() { return startingBid; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public AuctionType getAuctionType() { return auctionType; }
    public double getCurrentBid() { return currentBid; }
    public UUID getCurrentBidder() { return currentBidder; }
    public AuctionStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public Currency getCurrency() { return currency; }
    
    // Setters
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return "Auction{" +
                "auctionId=" + auctionId +
                ", itemName='" + itemName + '\'' +
                ", quantity=" + quantity +
                ", currentBid=" + currency.format(currentBid) +
                ", status=" + status +
                ", timeRemaining='" + getFormattedTimeRemaining() + '\'' +
                '}';
    }
}
