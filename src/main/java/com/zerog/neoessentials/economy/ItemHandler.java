package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles item operations for the shop system including validation,
 * inventory management, and item delivery.
 */
public class ItemHandler {
    
    /**
     * Represents an item with quantity for shop operations
     */
    public static class ShopItem {
        private final String itemId;
        private final int quantity;
        private final CompoundTag nbt;
        private final String displayName;
        
        public ShopItem(String itemId, int quantity) {
            this(itemId, quantity, null, null);
        }
        
        public ShopItem(String itemId, int quantity, CompoundTag nbt, String displayName) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.nbt = nbt;
            this.displayName = displayName;
        }
        
        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public CompoundTag getNbt() { return nbt; }
        public String getDisplayName() { return displayName != null ? displayName : itemId; }
        
        public ItemStack toItemStack() {
            Item item = getItemFromId(itemId);
            if (item == null) return ItemStack.EMPTY;
            
            ItemStack stack = new ItemStack(item, quantity);
            if (nbt != null) {
                stack.setTag(nbt.copy());
            }
            return stack;
        }
        
        public static ShopItem fromItemStack(ItemStack stack) {
            if (stack.isEmpty()) return null;
            
            String itemId = getItemId(stack.getItem());
            CompoundTag nbt = stack.hasTag() ? stack.getTag().copy() : null;
            String displayName = stack.hasCustomHoverName() ? stack.getHoverName().getString() : null;
            
            return new ShopItem(itemId, stack.getCount(), nbt, displayName);
        }
    }
    
    /**
     * Get item from string ID (e.g., "minecraft:diamond_sword")
     */
    public static Item getItemFromId(String itemId) {
        try {
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            return BuiltInRegistries.ITEM.get(resourceLocation);
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Invalid item ID: {}", itemId);
            return null;
        }
    }
    
    /**
     * Get string ID from item
     */
    public static String getItemId(Item item) {
        ResourceLocation resourceLocation = BuiltInRegistries.ITEM.getKey(item);
        return resourceLocation != null ? resourceLocation.toString() : "minecraft:air";
    }
    
    /**
     * Validate if an item ID exists in the game
     */
    public static boolean isValidItem(String itemId) {
        return getItemFromId(itemId) != null && !getItemFromId(itemId).equals(Items.AIR);
    }
    
    /**
     * Get all available item IDs for tab completion
     */
    public static List<String> getAllItemIds() {
        return BuiltInRegistries.ITEM.keySet().stream()
            .map(ResourceLocation::toString)
            .filter(id -> !id.equals("minecraft:air"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Search for items by name (partial matching)
     */
    public static List<String> searchItems(String query) {
        String lowerQuery = query.toLowerCase();
        return getAllItemIds().stream()
            .filter(id -> id.toLowerCase().contains(lowerQuery))
            .limit(50)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if player has enough of an item in their inventory
     */
    public static boolean hasEnoughItems(ServerPlayer player, String itemId, int requiredQuantity) {
        Item item = getItemFromId(itemId);
        if (item == null) return false;
        
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                totalCount += stack.getCount();
                if (totalCount >= requiredQuantity) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Remove items from player inventory
     */
    public static boolean removeItemsFromPlayer(ServerPlayer player, String itemId, int quantity) {
        Item item = getItemFromId(itemId);
        if (item == null || quantity <= 0) return false;
        
        if (!hasEnoughItems(player, itemId, quantity)) {
            return false;
        }
        
        int remainingToRemove = quantity;
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingToRemove > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int stackCount = stack.getCount();
                if (stackCount <= remainingToRemove) {
                    // Remove entire stack
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    remainingToRemove -= stackCount;
                } else {
                    // Remove partial stack
                    stack.shrink(remainingToRemove);
                    remainingToRemove = 0;
                }
            }
        }
        
        player.inventoryMenu.broadcastChanges();
        return remainingToRemove == 0;
    }
    
    /**
     * Give items to player inventory
     */
    public static boolean giveItemsToPlayer(ServerPlayer player, String itemId, int quantity) {
        Item item = getItemFromId(itemId);
        if (item == null || quantity <= 0) return false;
        
        ItemStack itemStack = new ItemStack(item, quantity);
        return giveItemStackToPlayer(player, itemStack);
    }
    
    /**
     * Give ItemStack to player inventory
     */
    public static boolean giveItemStackToPlayer(ServerPlayer player, ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;
        
        // Try to add to existing stacks first
        int remainingCount = itemStack.getCount();
        ItemStack workingStack = itemStack.copy();
        
        // First pass: try to add to existing stacks
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingCount > 0; i++) {
            ItemStack existingStack = player.getInventory().getItem(i);
            if (!existingStack.isEmpty() && existingStack.getItem() == itemStack.getItem() && 
                ItemStack.isSameItemSameTags(existingStack, workingStack)) {
                
                int maxStackSize = existingStack.getMaxStackSize();
                int existingCount = existingStack.getCount();
                int canAdd = Math.min(remainingCount, maxStackSize - existingCount);
                
                if (canAdd > 0) {
                    existingStack.grow(canAdd);
                    remainingCount -= canAdd;
                }
            }
        }
        
        // Second pass: add to empty slots
        for (int i = 0; i < player.getInventory().getContainerSize() && remainingCount > 0; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) {
                int toAdd = Math.min(remainingCount, workingStack.getMaxStackSize());
                ItemStack newStack = workingStack.copy();
                newStack.setCount(toAdd);
                player.getInventory().setItem(i, newStack);
                remainingCount -= toAdd;
            }
        }
        
        // If there are still items remaining, drop them
        if (remainingCount > 0) {
            ItemStack dropStack = workingStack.copy();
            dropStack.setCount(remainingCount);
            player.drop(dropStack, false);
            NeoEssentials.LOGGER.debug("Dropped {} items for player {} (inventory full)", remainingCount, player.getName().getString());
        }
        
        player.inventoryMenu.broadcastChanges();
        return true;
    }
    
    /**
     * Get the amount of a specific item in player's inventory
     */
    public static int getItemCount(ServerPlayer player, String itemId) {
        Item item = getItemFromId(itemId);
        if (item == null) return 0;
        
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                totalCount += stack.getCount();
            }
        }
        return totalCount;
    }
    
    /**
     * Get player's inventory contents as ShopItems
     */
    public static List<ShopItem> getInventoryContents(ServerPlayer player) {
        Map<String, Integer> itemCounts = new HashMap<>();
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String itemId = getItemId(stack.getItem());
                itemCounts.merge(itemId, stack.getCount(), Integer::sum);
            }
        }
        
        return itemCounts.entrySet().stream()
            .map(entry -> new ShopItem(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
    
    /**
     * Format item name for display (convert minecraft:diamond_sword to "Diamond Sword")
     */
    public static String formatItemName(String itemId) {
        if (itemId == null) return "Unknown Item";
        
        // Remove namespace (minecraft:)
        String name = itemId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }
        
        // Replace underscores with spaces and capitalize
        name = name.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }
                formatted.append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Get category for an item (used for shop categorization)
     */
    public static String getItemCategory(String itemId) {
        if (itemId == null) return "misc";
        
        String lowerId = itemId.toLowerCase();
        
        // Weapons and tools
        if (lowerId.contains("sword") || lowerId.contains("bow") || lowerId.contains("crossbow") || 
            lowerId.contains("trident") || lowerId.contains("arrow")) {
            return "weapons";
        }
        if (lowerId.contains("pickaxe") || lowerId.contains("axe") || lowerId.contains("shovel") || 
            lowerId.contains("hoe") || lowerId.contains("shears")) {
            return "tools";
        }
        
        // Armor
        if (lowerId.contains("helmet") || lowerId.contains("chestplate") || lowerId.contains("leggings") || 
            lowerId.contains("boots") || lowerId.contains("shield")) {
            return "armor";
        }
        
        // Building blocks
        if (lowerId.contains("stone") || lowerId.contains("wood") || lowerId.contains("plank") || 
            lowerId.contains("brick") || lowerId.contains("block") || lowerId.contains("slab") || 
            lowerId.contains("stair") || lowerId.contains("wall")) {
            return "blocks";
        }
        
        // Food
        if (lowerId.contains("bread") || lowerId.contains("meat") || lowerId.contains("fish") || 
            lowerId.contains("apple") || lowerId.contains("carrot") || lowerId.contains("potato") || 
            lowerId.contains("beetroot") || lowerId.contains("soup") || lowerId.contains("pie")) {
            return "food";
        }
        
        // Materials and crafting
        if (lowerId.contains("ingot") || lowerId.contains("nugget") || lowerId.contains("gem") || 
            lowerId.contains("dust") || lowerId.contains("rod") || lowerId.contains("string") || 
            lowerId.contains("leather") || lowerId.contains("wool")) {
            return "materials";
        }
        
        // Redstone and technical
        if (lowerId.contains("redstone") || lowerId.contains("piston") || lowerId.contains("repeater") || 
            lowerId.contains("comparator") || lowerId.contains("observer") || lowerId.contains("hopper")) {
            return "redstone";
        }
        
        // Default category
        return "misc";
    }
}
