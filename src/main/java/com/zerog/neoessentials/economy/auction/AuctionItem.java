package com.zerog.neoessentials.economy.auction;

import com.zerog.neoessentials.economy.Currency;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an auction item
 */
public class AuctionItem {
    
    public enum Status {
        ACTIVE,     // Auction is currently active and accepting bids
        ENDED,      // Auction has ended but payment/delivery is pending
        COMPLETED,  // Auction completed successfully
        CANCELLED   // Auction was cancelled
    }
    
    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack itemStack;
    private final BigDecimal startingPrice;
    private final BigDecimal buyNowPrice; // null if no buy-now option
    private final Currency currency;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String description;
    
    // Current auction state
    private UUID currentBidderId;
    private String currentBidderName;
    private BigDecimal currentBid;
    private Status status;
    private int bidCount;
    
    private AuctionItem(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ID cannot be null");
        this.sellerId = Objects.requireNonNull(builder.sellerId, "Seller ID cannot be null");
        this.sellerName = Objects.requireNonNull(builder.sellerName, "Seller name cannot be null");
        this.itemStack = Objects.requireNonNull(builder.itemStack, "ItemStack cannot be null").copy();
        this.startingPrice = Objects.requireNonNull(builder.startingPrice, "Starting price cannot be null");
        this.buyNowPrice = builder.buyNowPrice;
        this.currency = Objects.requireNonNull(builder.currency, "Currency cannot be null");
        this.startTime = Objects.requireNonNull(builder.startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(builder.endTime, "End time cannot be null");
        this.description = builder.description != null ? builder.description : "";
        
        this.currentBidderId = builder.currentBidderId;
        this.currentBidderName = builder.currentBidderName;
        this.currentBid = builder.currentBid != null ? builder.currentBid : startingPrice;
        this.status = builder.status != null ? builder.status : Status.ACTIVE;
        this.bidCount = builder.bidCount;
        
        // Validation
        if (startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Starting price must be positive");
        }
        
        if (buyNowPrice != null && buyNowPrice.compareTo(startingPrice) <= 0) {
            throw new IllegalArgumentException("Buy now price must be greater than starting price");
        }
        
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public BigDecimal getBuyNowPrice() { return buyNowPrice; }
    public Currency getCurrency() { return currency; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getDescription() { return description; }
    public UUID getCurrentBidderId() { return currentBidderId; }
    public String getCurrentBidderName() { return currentBidderName; }
    public BigDecimal getCurrentBid() { return currentBid; }
    public Status getStatus() { return status; }
    public int getBidCount() { return bidCount; }
    
    // Utility methods
    public boolean isActive() { return status == Status.ACTIVE && LocalDateTime.now().isBefore(endTime); }
    public boolean hasEnded() { return LocalDateTime.now().isAfter(endTime) || status != Status.ACTIVE; }
    public boolean hasBids() { return bidCount > 0; }
    public boolean hasBuyNow() { return buyNowPrice != null; }
    public UUID getHighestBidder() { return getCurrentBidderId(); }
    public String getHighestBidderName() { return getCurrentBidderName(); }
    public long getTimeRemainingMinutes() {
        if (hasEnded()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), endTime).toMinutes();
    }
    
    /**
     * Creates a new auction item with an updated bid
     */
    public AuctionItem withBid(UUID bidderId, String bidderName, BigDecimal bidAmount) {
        return new Builder()
                .from(this)
                .currentBidderId(bidderId)
                .currentBidderName(bidderName)
                .currentBid(bidAmount)
                .bidCount(this.bidCount + 1)
                .build();
    }
    
    /**
     * Creates a new auction item with updated status
     */
    public AuctionItem withStatus(Status newStatus) {
        return new Builder()
                .from(this)
                .status(newStatus)
                .build();
    }
    
    /**
     * Builder class for creating AuctionItem instances
     */
    public static class Builder {
        private UUID id;
        private UUID sellerId;
        private String sellerName;
        private ItemStack itemStack;
        private BigDecimal startingPrice;
        private BigDecimal buyNowPrice;
        private Currency currency;
        private LocalDateTime startTime = LocalDateTime.now();
        private LocalDateTime endTime;
        private String description = "";
        private UUID currentBidderId;
        private String currentBidderName;
        private BigDecimal currentBid;
        private Status status = Status.ACTIVE;
        private int bidCount = 0;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder sellerId(UUID sellerId) {
            this.sellerId = sellerId;
            return this;
        }
        
        public Builder sellerName(String sellerName) {
            this.sellerName = sellerName;
            return this;
        }
        
        public Builder itemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }
        
        public Builder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }
        
        public Builder buyNowPrice(BigDecimal buyNowPrice) {
            this.buyNowPrice = buyNowPrice;
            return this;
        }
        
        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder currentBidderId(UUID currentBidderId) {
            this.currentBidderId = currentBidderId;
            return this;
        }
        
        public Builder currentBidderName(String currentBidderName) {
            this.currentBidderName = currentBidderName;
            return this;
        }
        
        public Builder currentBid(BigDecimal currentBid) {
            this.currentBid = currentBid;
            return this;
        }
        
        public Builder status(Status status) {
            this.status = status;
            return this;
        }
        
        public Builder bidCount(int bidCount) {
            this.bidCount = bidCount;
            return this;
        }
        
        public Builder from(AuctionItem item) {
            this.id = item.id;
            this.sellerId = item.sellerId;
            this.sellerName = item.sellerName;
            this.itemStack = item.itemStack.copy();
            this.startingPrice = item.startingPrice;
            this.buyNowPrice = item.buyNowPrice;
            this.currency = item.currency;
            this.startTime = item.startTime;
            this.endTime = item.endTime;
            this.description = item.description;
            this.currentBidderId = item.currentBidderId;
            this.currentBidderName = item.currentBidderName;
            this.currentBid = item.currentBid;
            this.status = item.status;
            this.bidCount = item.bidCount;
            return this;
        }
        
        public AuctionItem build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            return new AuctionItem(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuctionItem)) return false;
        AuctionItem that = (AuctionItem) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("AuctionItem{id=%s, item=%s, currentBid=%s, status=%s}", 
                id, itemStack.getDisplayName().getString(), currency.format(currentBid), status);
    }
}
