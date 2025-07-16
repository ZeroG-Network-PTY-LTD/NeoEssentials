package com.zerog.neoessentials.economy.shop;

import com.zerog.neoessentials.economy.Currency;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an item listing in a shop
 */
public class ShopItem {
    
    public enum Type {
        BUY,    // Players can buy this item from the shop
        SELL,   // Players can sell this item to the shop
        BOTH    // Both buy and sell operations are allowed
    }
    
    private final UUID id;
    private final ItemStack itemStack;
    private final Type type;
    private final BigDecimal buyPrice;
    private final BigDecimal sellPrice;
    private final Currency currency;
    private final int stock;
    private final int maxStock;
    private final UUID createdBy; // UUID of player who created this listing (null for admin)
    private final LocalDateTime createdAt;
    private final String description;
    private final boolean adminItem; // Whether this is an admin-created item
    
    private ShopItem(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ID cannot be null");
        this.itemStack = Objects.requireNonNull(builder.itemStack, "ItemStack cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Type cannot be null");
        this.buyPrice = builder.buyPrice;
        this.sellPrice = builder.sellPrice;
        this.currency = Objects.requireNonNull(builder.currency, "Currency cannot be null");
        // Allow negative stock for admin items (infinite stock = -1)
        this.stock = builder.adminItem ? builder.stock : Math.max(0, builder.stock);
        this.maxStock = builder.adminItem ? builder.maxStock : Math.max(0, builder.maxStock);
        this.createdBy = builder.createdBy;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created date cannot be null");
        this.description = builder.description != null ? builder.description : "";
        this.adminItem = builder.adminItem;
        
        // Validation
        if (type == Type.BUY || type == Type.BOTH) {
            Objects.requireNonNull(buyPrice, "Buy price cannot be null for buyable items");
            if (buyPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Buy price must be positive");
            }
        }
        
        if (type == Type.SELL || type == Type.BOTH) {
            Objects.requireNonNull(sellPrice, "Sell price cannot be null for sellable items");
            if (sellPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Sell price must be positive");
            }
        }
    }
    
    // Getters
    public UUID getId() { return id; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public Type getType() { return type; }
    public BigDecimal getBuyPrice() { return buyPrice; }
    public BigDecimal getSellPrice() { return sellPrice; }
    public Currency getCurrency() { return currency; }
    public int getStock() { return stock; }
    public int getMaxStock() { return maxStock; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getDescription() { return description; }
    public boolean isAdminItem() { return adminItem; }
    
    /**
     * Gets the appropriate price based on the context (buy price for purchases)
     */
    public BigDecimal getPrice() {
        return canBuy() ? buyPrice : sellPrice;
    }
    
    // Utility methods
    public boolean canBuy() { return type == Type.BUY || type == Type.BOTH; }
    public boolean canSell() { return type == Type.SELL || type == Type.BOTH; }
    public boolean hasStock() { return stock > 0 || (adminItem && stock < 0); }
    public boolean canAddStock() { return stock < maxStock; }
    
    /**
<<<<<<< HEAD
     * Validates that this shop item is properly configured and usable
     */
    public boolean isValid() {
        try {
            // Basic null checks
            if (id == null || itemStack == null || itemStack.isEmpty()) {
                return false;
            }
            
            if (type == null || currency == null || createdAt == null) {
                return false;
            }
            
            // Validate prices
            if (canBuy() && (buyPrice == null || buyPrice.compareTo(BigDecimal.ZERO) <= 0)) {
                return false;
            }
            
            if (canSell() && (sellPrice == null || sellPrice.compareTo(BigDecimal.ZERO) <= 0)) {
                return false;
            }
            
            // Validate stock for non-admin items
            if (!adminItem && stock < 0) {
                return false;
            }
            
            // Validate max stock
            if (!adminItem && maxStock < 0) {
                return false;
            }
            
            // For non-admin items, stock should not exceed max stock
            if (!adminItem && stock > maxStock) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
=======
>>>>>>> 4fee73b0b24b6301947b09da0d1e52696e353f1d
     * Creates a new ShopItem with updated stock
     */
    public ShopItem withStock(int newStock) {
        return new Builder()
                .id(this.id)
                .itemStack(this.itemStack)
                .type(this.type)
                .buyPrice(this.buyPrice)
                .sellPrice(this.sellPrice)
                .currency(this.currency)
                .stock(newStock)
                .maxStock(this.maxStock)
                .createdBy(this.createdBy)
                .createdAt(this.createdAt)
                .description(this.description)
                .adminItem(this.adminItem)
                .build();
    }
    
    /**
     * Builder class for creating ShopItem instances
     */
    public static class Builder {
        private UUID id;
        private ItemStack itemStack;
        private Type type;
        private BigDecimal buyPrice;
        private BigDecimal sellPrice;
        private Currency currency;
        private int stock = 0;
        private int maxStock = 999;
        private UUID createdBy;
        private LocalDateTime createdAt = LocalDateTime.now();
        private String description = "";
        private boolean adminItem = false;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder itemStack(ItemStack itemStack) {
            this.itemStack = itemStack.copy();
            return this;
        }
        
        public Builder type(Type type) {
            this.type = type;
            return this;
        }
        
        public Builder buyPrice(BigDecimal buyPrice) {
            this.buyPrice = buyPrice;
            return this;
        }
        
        public Builder sellPrice(BigDecimal sellPrice) {
            this.sellPrice = sellPrice;
            return this;
        }
        
        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder stock(int stock) {
            this.stock = stock;
            return this;
        }
        
        public Builder maxStock(int maxStock) {
            this.maxStock = maxStock;
            return this;
        }
        
        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder adminItem(boolean adminItem) {
            this.adminItem = adminItem;
            return this;
        }
        
        public ShopItem build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            return new ShopItem(this);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShopItem)) return false;
        ShopItem shopItem = (ShopItem) o;
        return Objects.equals(id, shopItem.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("ShopItem{id=%s, item=%s, type=%s, buyPrice=%s, sellPrice=%s, stock=%d}", 
                id, itemStack.getDisplayName().getString(), type, buyPrice, sellPrice, stock);
    }
}
