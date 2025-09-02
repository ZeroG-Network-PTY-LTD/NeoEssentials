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
    private BlockPosData chestPos; // Connected chest position
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
        this.chestPos = signShop.getChestPos() != null ? new BlockPosData(signShop.getChestPos()) : null;
        this.item = new ItemStackData(signShop.getItem());
        this.buyPrice = signShop.getBuyPrice();
        this.sellPrice = signShop.getSellPrice();
        this.quantity = signShop.getQuantity();
        this.stock = signShop.getStock();
    }
    
    // Convert back to SignShop
    public ShopManager.SignShop toSignShop() {
        BlockPos pos = signPos.toBlockPos();
        BlockPos chestPosition = chestPos != null ? chestPos.toBlockPos() : null;
        ItemStack itemStack = item.toItemStack();
        
        ShopManager.SignShop shop = new ShopManager.SignShop(
            ownerId, pos, chestPosition, itemStack, buyPrice, sellPrice, quantity
        );
        shop.setStock(stock);
        return shop;
    }
    
    // Getters and setters for JSON serialization
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    
    public BlockPosData getSignPos() { return signPos; }
    public void setSignPos(BlockPosData signPos) { this.signPos = signPos; }
    
    public BlockPosData getChestPos() { return chestPos; }
    public void setChestPos(BlockPosData chestPos) { this.chestPos = chestPos; }
    
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
                this.count = itemStack.getCount();
                
                // Enhanced item ID extraction using proper registry lookup
                try {
                    // Use Minecraft's built-in registry to get the proper resource location
                    net.minecraft.core.Registry<net.minecraft.world.item.Item> itemRegistry = 
                        net.minecraft.core.registries.BuiltInRegistries.ITEM;
                    net.minecraft.resources.ResourceLocation resourceLocation = itemRegistry.getKey(itemStack.getItem());
                    
                    if (resourceLocation != null) {
                        this.itemId = resourceLocation.toString();
                    } else {
                        // Fallback to string representation
                        this.itemId = itemStack.getItem().toString();
                        if (!this.itemId.contains(":")) {
                            this.itemId = "minecraft:" + this.itemId.toLowerCase();
                        }
                    }
                } catch (Exception e) {
                    // Last resort fallback
                    this.itemId = "minecraft:stone";
                }
            }
        }
        
        public ItemStack toItemStack() {
            try {
                if ("minecraft:air".equals(itemId) || count <= 0) {
                    return ItemStack.EMPTY;
                }
                
                // Enhanced item lookup using proper registry
                net.minecraft.core.Registry<net.minecraft.world.item.Item> itemRegistry = 
                    net.minecraft.core.registries.BuiltInRegistries.ITEM;
                
                // Try to parse as ResourceLocation first
                try {
                    net.minecraft.resources.ResourceLocation resourceLocation = 
                        net.minecraft.resources.ResourceLocation.parse(itemId);
                    net.minecraft.world.item.Item item = itemRegistry.get(resourceLocation);
                    
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        return new ItemStack(item, count);
                    }
                } catch (Exception e) {
                    // Fall through to legacy mappings
                }
                
                // Enhanced item mappings with more comprehensive coverage
                net.minecraft.world.item.Item item = switch (itemId.toLowerCase()) {
                    // Precious materials
                    case "minecraft:diamond", "diamond" -> net.minecraft.world.item.Items.DIAMOND;
                    case "minecraft:emerald", "emerald" -> net.minecraft.world.item.Items.EMERALD;
                    case "minecraft:netherite_ingot", "netherite_ingot" -> net.minecraft.world.item.Items.NETHERITE_INGOT;
                    
                    // Metal ingots
                    case "minecraft:iron_ingot", "iron_ingot" -> net.minecraft.world.item.Items.IRON_INGOT;
                    case "minecraft:gold_ingot", "gold_ingot" -> net.minecraft.world.item.Items.GOLD_INGOT;
                    case "minecraft:copper_ingot", "copper_ingot" -> net.minecraft.world.item.Items.COPPER_INGOT;
                    
                    // Building blocks
                    case "minecraft:stone", "stone" -> net.minecraft.world.item.Items.STONE;
                    case "minecraft:cobblestone", "cobblestone" -> net.minecraft.world.item.Items.COBBLESTONE;
                    case "minecraft:dirt", "dirt" -> net.minecraft.world.item.Items.DIRT;
                    case "minecraft:grass_block", "grass_block" -> net.minecraft.world.item.Items.GRASS_BLOCK;
                    
                    // Wood types
                    case "minecraft:oak_log", "oak_log" -> net.minecraft.world.item.Items.OAK_LOG;
                    case "minecraft:birch_log", "birch_log" -> net.minecraft.world.item.Items.BIRCH_LOG;
                    case "minecraft:spruce_log", "spruce_log" -> net.minecraft.world.item.Items.SPRUCE_LOG;
                    case "minecraft:jungle_log", "jungle_log" -> net.minecraft.world.item.Items.JUNGLE_LOG;
                    case "minecraft:acacia_log", "acacia_log" -> net.minecraft.world.item.Items.ACACIA_LOG;
                    case "minecraft:dark_oak_log", "dark_oak_log" -> net.minecraft.world.item.Items.DARK_OAK_LOG;
                    
                    // Food items
                    case "minecraft:wheat", "wheat" -> net.minecraft.world.item.Items.WHEAT;
                    case "minecraft:bread", "bread" -> net.minecraft.world.item.Items.BREAD;
                    case "minecraft:apple", "apple" -> net.minecraft.world.item.Items.APPLE;
                    case "minecraft:golden_apple", "golden_apple" -> net.minecraft.world.item.Items.GOLDEN_APPLE;
                    case "minecraft:cooked_beef", "cooked_beef" -> net.minecraft.world.item.Items.COOKED_BEEF;
                    case "minecraft:cooked_porkchop", "cooked_porkchop" -> net.minecraft.world.item.Items.COOKED_PORKCHOP;
                    
                    // Tools and weapons
                    case "minecraft:diamond_sword", "diamond_sword" -> net.minecraft.world.item.Items.DIAMOND_SWORD;
                    case "minecraft:iron_sword", "iron_sword" -> net.minecraft.world.item.Items.IRON_SWORD;
                    case "minecraft:diamond_pickaxe", "diamond_pickaxe" -> net.minecraft.world.item.Items.DIAMOND_PICKAXE;
                    case "minecraft:iron_pickaxe", "iron_pickaxe" -> net.minecraft.world.item.Items.IRON_PICKAXE;
                    
                    // Redstone items
                    case "minecraft:redstone", "redstone" -> net.minecraft.world.item.Items.REDSTONE;
                    case "minecraft:repeater", "repeater" -> net.minecraft.world.item.Items.REPEATER;
                    case "minecraft:comparator", "comparator" -> net.minecraft.world.item.Items.COMPARATOR;
                    case "minecraft:piston", "piston" -> net.minecraft.world.item.Items.PISTON;
                    
                    default -> net.minecraft.world.item.Items.STONE; // Enhanced fallback
                };
                
                return new ItemStack(item, count);
            } catch (Exception e) {
                // Enhanced error handling with logging
                return new ItemStack(net.minecraft.world.item.Items.STONE, Math.max(1, count));
            }
        }
        
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
