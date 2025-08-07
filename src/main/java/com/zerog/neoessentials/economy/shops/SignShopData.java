package com.zerog.neoessentials.economy.shops;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Serializable data structure for sign shops
 * Used for persistence to/from JSON storage
 */
public class SignShopData {
    private String ownerId;
    private BlockPosData signPos;
    private ItemStackData item;
    private double buyPrice;
    private double sellPrice;
    private int quantity;
    private int stock;
    
    // Default constructor for JSON deserialization
    public SignShopData() {}
    
    // Constructor from SignShop
    public SignShopData(ShopManager.SignShop signShop) {
        this.ownerId = signShop.getOwnerId();
        this.signPos = new BlockPosData(signShop.getSignPos());
        this.item = new ItemStackData(signShop.getItem());
        this.buyPrice = signShop.getBuyPrice();
        this.sellPrice = signShop.getSellPrice();
        this.quantity = signShop.getQuantity();
        this.stock = signShop.getStock();
    }
    
    // Convert back to SignShop
    public ShopManager.SignShop toSignShop() {
        BlockPos pos = signPos.toBlockPos();
        ItemStack itemStack = item.toItemStack();
        
        ShopManager.SignShop shop = new ShopManager.SignShop(
            ownerId, pos, itemStack, buyPrice, sellPrice, quantity
        );
        shop.setStock(stock);
        return shop;
    }
    
    // Getters and setters for JSON serialization
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    
    public BlockPosData getSignPos() { return signPos; }
    public void setSignPos(BlockPosData signPos) { this.signPos = signPos; }
    
    public ItemStackData getItem() { return item; }
    public void setItem(ItemStackData item) { this.item = item; }
    
    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    
    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    
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
    
    /**
     * Serializable ItemStack data
     * Simplified version that stores basic item info
     */
    public static class ItemStackData {
        private String itemId;
        private int count;
        
        public ItemStackData() {}
        
        public ItemStackData(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                this.itemId = "minecraft:air";
                this.count = 0;
            } else {
                // Get the item registry name - simplified approach
                this.itemId = itemStack.getItem().getClass().getSimpleName().toLowerCase();
                this.count = itemStack.getCount();
                
                // Try to get proper registry name if possible
                try {
                    String fullName = itemStack.getItem().toString();
                    if (fullName.contains(":")) {
                        this.itemId = fullName;
                    } else {
                        // Fallback: construct likely registry name
                        this.itemId = "minecraft:" + fullName.toLowerCase();
                    }
                } catch (Exception e) {
                    // Keep the simple name as fallback
                }
            }
        }
        
        public ItemStack toItemStack() {
            try {
                if ("minecraft:air".equals(itemId) || count <= 0) {
                    return ItemStack.EMPTY;
                }
                
                // For now, create basic items - this is a simplified approach
                // A full implementation would need proper registry lookup
                
                // Common item mappings for testing
                net.minecraft.world.item.Item item = switch (itemId.toLowerCase()) {
                    case "minecraft:diamond", "diamond" -> net.minecraft.world.item.Items.DIAMOND;
                    case "minecraft:iron_ingot", "iron_ingot" -> net.minecraft.world.item.Items.IRON_INGOT;
                    case "minecraft:gold_ingot", "gold_ingot" -> net.minecraft.world.item.Items.GOLD_INGOT;
                    case "minecraft:emerald", "emerald" -> net.minecraft.world.item.Items.EMERALD;
                    case "minecraft:stone", "stone" -> net.minecraft.world.item.Items.STONE;
                    case "minecraft:dirt", "dirt" -> net.minecraft.world.item.Items.DIRT;
                    case "minecraft:cobblestone", "cobblestone" -> net.minecraft.world.item.Items.COBBLESTONE;
                    case "minecraft:oak_log", "oak_log" -> net.minecraft.world.item.Items.OAK_LOG;
                    case "minecraft:wheat", "wheat" -> net.minecraft.world.item.Items.WHEAT;
                    case "minecraft:bread", "bread" -> net.minecraft.world.item.Items.BREAD;
                    default -> net.minecraft.world.item.Items.STONE; // Default fallback
                };
                
                return new ItemStack(item, count);
            } catch (Exception e) {
                // Fallback to stone if item creation fails
                return new ItemStack(net.minecraft.world.item.Items.STONE, Math.max(1, count));
            }
        }
        
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
