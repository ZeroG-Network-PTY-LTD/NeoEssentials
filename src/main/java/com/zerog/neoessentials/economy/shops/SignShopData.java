package com.zerog.neoessentials.economy.shops;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
     */
    public static class ItemStackData {
        private String nbtData;
        
        public ItemStackData() {}
        
        public ItemStackData(ItemStack itemStack) {
            // Serialize ItemStack to NBT string
            CompoundTag tag = new CompoundTag();
            itemStack.save(tag);
            this.nbtData = tag.toString();
        }
        
        public ItemStack toItemStack() {
            try {
                // Deserialize ItemStack from NBT string
                CompoundTag tag = CompoundTag.parseString(nbtData);
                return ItemStack.of(tag);
            } catch (Exception e) {
                // Fallback to empty stack if deserialization fails
                return ItemStack.EMPTY;
            }
        }
        
        public String getNbtData() { return nbtData; }
        public void setNbtData(String nbtData) { this.nbtData = nbtData; }
    }
}
