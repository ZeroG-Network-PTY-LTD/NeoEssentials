package com.zerog.neoessentials.shops;

import net.minecraft.core.BlockPos;

/**
 * Serializable data structure for admin shops
 * Used for persistence to/from JSON storage
 */
public class AdminShopData {
    private String name;
    private String owner;
    private String category;
    private BlockPosData location;

    public AdminShopData() {}

    public AdminShopData(ShopManager.AdminShop shop) {
        this.name = shop.getName();
        this.owner = shop.getOwner();
        this.category = shop.getCategory();
        this.location = new BlockPosData(shop.getLocation());
    }

    public ShopManager.AdminShop toAdminShop() {
        return new ShopManager.AdminShop(name, owner, category, location.toBlockPos());
    }

    // Getters and setters for JSON serialization
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BlockPosData getLocation() { return location; }
    public void setLocation(BlockPosData location) { this.location = location; }

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
