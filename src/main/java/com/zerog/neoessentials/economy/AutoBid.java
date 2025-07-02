package com.zerog.neoessentials.economy;

import java.util.UUID;

/**
 * Represents an automatic bidding configuration for auction house
 */
public class AutoBid {
    private final UUID auctionId;
    private final UUID playerId;
    private final double maxAmount;
    private final double increment;
    private final long createdTime;
    private boolean active;
    
    /**
     * Creates a new auto-bid configuration
     * 
     * @param auctionId The auction to auto-bid on
     * @param playerId The player setting up auto-bidding
     * @param maxAmount Maximum amount to bid
     * @param increment Amount to increment bids by
     */
    public AutoBid(UUID auctionId, UUID playerId, double maxAmount, double increment) {
        this.auctionId = auctionId;
        this.playerId = playerId;
        this.maxAmount = maxAmount;
        this.increment = increment;
        this.createdTime = System.currentTimeMillis();
        this.active = true;
    }
    
    // Getters
    public UUID getAuctionId() { return auctionId; }
    public UUID getPlayerId() { return playerId; }
    public double getMaxAmount() { return maxAmount; }
    public double getIncrement() { return increment; }
    public long getCreatedTime() { return createdTime; }
    public boolean isActive() { return active; }
    
    // Setters
    public void setActive(boolean active) { this.active = active; }
    
    /**
     * Calculate the next bid amount based on current highest bid
     * 
     * @param currentBid The current highest bid
     * @return The next bid amount, or -1 if exceeds max amount
     */
    public double calculateNextBid(double currentBid) {
        double nextBid = currentBid + increment;
        return nextBid <= maxAmount ? nextBid : -1;
    }
    
    @Override
    public String toString() {
        return String.format("AutoBid{auction=%s, player=%s, max=%.2f, increment=%.2f, active=%s}", 
                auctionId.toString().substring(0, 8), playerId.toString().substring(0, 8), 
                maxAmount, increment, active);
    }
}
