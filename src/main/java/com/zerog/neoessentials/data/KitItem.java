package com.zerog.neoessentials.data;

import com.google.gson.annotations.SerializedName;
import com.zerog.neoessentials.util.ItemStackNbtUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

/**
 * Kit item data structure for NeoEssentials
 * Represents an item within a kit with quantity, NBT data, and metadata
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitItem {
    
    @SerializedName("item")
    private String itemId; // minecraft:diamond_sword
    
    @SerializedName("count")
    private int count = 1;
    
    @SerializedName("slot")
    private int slot = -1; // -1 means any available slot
    
    @SerializedName("nbt")
    private String nbtData; // Raw NBT string
    
    @SerializedName("display_name")
    private String displayName;
    
    @SerializedName("lore")
    private String[] lore;
    
    @SerializedName("enchantments")
    private Map<String, Integer> enchantments;
    
    @SerializedName("damage")
    private int damage = 0;
    
    @SerializedName("unbreakable")
    private boolean unbreakable = false;
    
    // Constructors
    public KitItem() {}
    
    public KitItem(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }
    
    public KitItem(String itemId, int count, int slot) {
        this.itemId = itemId;
        this.count = count;
        this.slot = slot;
    }
    
    // Getters and Setters
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = Math.max(1, Math.min(64, count)); // Clamp between 1 and 64
    }
    
    public int getSlot() {
        return slot;
    }
    
    public void setSlot(int slot) {
        this.slot = slot;
    }
    
    public String getNbtData() {
        return nbtData;
    }
    
    public void setNbtData(String nbtData) {
        this.nbtData = nbtData;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String[] getLore() {
        return lore;
    }
    
    public void setLore(String[] lore) {
        this.lore = lore;
    }
    
    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }
    
    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
    }
    
    public boolean isUnbreakable() {
        return unbreakable;
    }
    
    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }
    
    // Utility methods
    
    /**
     * Add an enchantment to this item
     */
    public void addEnchantment(String enchantmentId, int level) {
        if (enchantments == null) {
            enchantments = new java.util.HashMap<>();
        }
        enchantments.put(enchantmentId, level);
    }
    
    /**
     * Convert this KitItem to a Minecraft ItemStack
     * @param registryAccess The registry access for looking up items and enchantments
     */
    public ItemStack toItemStack(RegistryAccess registryAccess) {
        try {
            // Get the item from registry
            ResourceLocation itemResource = ResourceLocation.fromNamespaceAndPath("minecraft", itemId.contains(":") ? itemId.split(":")[1] : itemId);
            if (itemId.contains(":")) {
                String[] parts = itemId.split(":");
                itemResource = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
            }
            Item item = registryAccess.registryOrThrow(Registries.ITEM).get(itemResource);
            if (item == null) {
                throw new IllegalArgumentException("Invalid item ID: " + itemId);
            }
            // Create the ItemStack
            ItemStack itemStack = new ItemStack(item, count);
            // Apply damage
            if (damage > 0 && itemStack.isDamageableItem()) {
                itemStack.setDamageValue(damage);
            }
            // Apply NBT data if present
            if (nbtData != null && !nbtData.trim().isEmpty()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(nbtData);
                    ItemStackNbtUtil.mergeTag(itemStack, nbt);
                } catch (Exception e) {
                    System.err.println("Failed to parse NBT data for item " + itemId + ": " + e.getMessage());
                }
            }
            // Apply custom display name and lore
            if (displayName != null || lore != null || unbreakable) {
                CompoundTag tag = new CompoundTag();
                ItemStackNbtUtil.mergeTag(itemStack, tag);
                CompoundTag display = tag.getCompound("display");
                if (displayName != null) {
                    display.putString("Name", "{\"text\":\"" + displayName + "\"}");
                }
                if (lore != null && lore.length > 0) {
                    StringBuilder loreJson = new StringBuilder("[");
                    for (int i = 0; i < lore.length; i++) {
                        if (i > 0) loreJson.append(",");
                        loreJson.append("{\"text\":\"").append(lore[i]).append("\"}");
                    }
                    loreJson.append("]");
                }
                if (unbreakable) {
                    tag.putBoolean("Unbreakable", true);
                }
                if (!display.isEmpty()) {
                    tag.put("display", display);
                }
            }
            // Apply enchantments using Holder<Enchantment>
            if (enchantments != null && !enchantments.isEmpty()) {
                Registry<Enchantment> enchRegistry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
                for (Map.Entry<String, Integer> enchEntry : enchantments.entrySet()) {
                    try {
                        String enchId = enchEntry.getKey();
                        int level = enchEntry.getValue();
                        ResourceLocation enchResource;
                        if (enchId.contains(":")) {
                            String[] parts = enchId.split(":");
                            enchResource = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
                        } else {
                            enchResource = ResourceLocation.fromNamespaceAndPath("minecraft", enchId);
                        }
                        Holder<Enchantment> enchHolder = enchRegistry.getHolder(enchResource).orElse(null);
                        if (enchHolder != null) {
                            itemStack.enchant(enchHolder, level);
                        } else {
                            System.err.println("Unknown enchantment: " + enchResource);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to apply enchantment " + enchEntry.getKey() + ": " + e.getMessage());
                    }
                }
            }
            return itemStack;
        } catch (Exception e) {
            System.err.println("Failed to create ItemStack for " + itemId + ": " + e.getMessage());
            return new ItemStack(registryAccess.registryOrThrow(Registries.ITEM).get(ResourceLocation.fromNamespaceAndPath("minecraft", "stone")), 1);
        }
    }
    
    /**
     * Create a KitItem from a Minecraft ItemStack
     */
    public static KitItem fromItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        
        KitItem kitItem = new KitItem();
        
        // Basic properties
        ResourceLocation itemResource = itemStack.getItem().builtInRegistryHolder().key().location();
        kitItem.setItemId(itemResource.toString());
        kitItem.setCount(itemStack.getCount());
        
        // Damage
        if (itemStack.isDamaged()) {
            kitItem.setDamage(itemStack.getDamageValue());
        }
        
        // NBT data (simplified - in practice you'd want more selective NBT handling)
        try {
            // Use reflection to get NBT tag since direct methods may not be available
            var tagField = ItemStack.class.getDeclaredField("tag");
            tagField.setAccessible(true);
            CompoundTag tag = (CompoundTag) tagField.get(itemStack);
            
            if (tag != null && !tag.isEmpty()) {
                kitItem.setNbtData(tag.toString());
                
                // Extract unbreakable flag
                if (tag.contains("Unbreakable")) {
                    kitItem.setUnbreakable(tag.getBoolean("Unbreakable"));
                }
            }
        } catch (Exception e) {
            // NBT extraction failed, continue without it
            System.err.println("Failed to extract NBT from ItemStack: " + e.getMessage());
        }
        
        // Enchantments - extract from ItemStack (disabled for MC 1.21.1 API changes)
        try {
            // Skip enchantment extraction for now - the API has changed in MC 1.21.1
            // TODO: Update when stable enchantment API is available for NeoForge 1.21.1
            System.out.println("Skipping enchantment extraction (API changed in MC 1.21.1)");
        } catch (Exception e) {
            // Enchantment extraction failed completely
            System.err.println("Failed to extract enchantments from ItemStack: " + e.getMessage());
        }
        
        return kitItem;
    }
    
    /**
     * Check if this item should be placed in a specific slot
     */
    public boolean hasSpecificSlot() {
        return slot >= 0 && slot <= 40; // 0-35 for inventory, 36-39 for armor, 40 for offhand
    }
    
    /**
     * Check if this is an armor item based on slot
     */
    public boolean isArmorItem() {
        return slot >= 36 && slot <= 39;
    }
    
    /**
     * Check if this should go in the offhand
     */
    public boolean isOffhandItem() {
        return slot == 40;
    }
    
    @Override
    public String toString() {
        return String.format("KitItem{item='%s', count=%d, slot=%d}", itemId, count, slot);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        KitItem kitItem = (KitItem) obj;
        return itemId != null && itemId.equals(kitItem.itemId) && count == kitItem.count;
    }
    
    @Override
    public int hashCode() {
        return itemId != null ? itemId.hashCode() * 31 + count : 0;
    }
}