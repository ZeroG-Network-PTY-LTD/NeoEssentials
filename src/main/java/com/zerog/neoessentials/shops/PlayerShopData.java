package com.zerog.neoessentials.shops;

import net.minecraft.core.BlockPos;

/**
 * Serializable data structure for player shops
 * Used for persistence to/from JSON storage
 */
public class PlayerShopData {
    private String name;
    private String ownerId;
    private String category;
    private BlockPosData location;
    private boolean active;
    private double rating;
    private int transactions;

    public PlayerShopData() {}

    public PlayerShopData(ShopManager.PlayerShop shop) {
        this.name = shop.getName();
        this.ownerId = shop.getOwnerId();
        this.category = shop.getCategory();
        this.location = new BlockPosData(shop.getLocation());
        this.active = shop.isActive();
        this.rating = shop.getRating();
        this.transactions = shop.getTransactions();
    }

    public ShopManager.PlayerShop toPlayerShop() {
        ShopManager.PlayerShop shop = new ShopManager.PlayerShop(
            name, ownerId, category, location.toBlockPos()
        );
        shop.setActive(active);
        shop.setRating(rating);
        for (int i = 0; i < transactions; i++) shop.addTransaction();
        return shop;
    }

    // Getters and setters for JSON serialization
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BlockPosData getLocation() { return location; }
    public void setLocation(BlockPosData location) { this.location = location; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getTransactions() { return transactions; }
    public void setTransactions(int transactions) { this.transactions = transactions; }

    /**
     * Serializable BlockPos data
     */
    public static class BlockPosData {
        private int x, y, z;
        public BlockPosData() {}
        public BlockPosData(BlockPos pos) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getZ() { return z; }
        public void setZ(int z) { this.z = z; }
    }
}
