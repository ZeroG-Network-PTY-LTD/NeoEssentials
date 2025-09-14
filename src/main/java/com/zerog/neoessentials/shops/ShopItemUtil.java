package com.zerog.neoessentials.shops;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class ShopItemUtil {
    /**
     * Converts a string item name to an ItemStack. Accepts names like "minecraft:diamond_sword" or "diamond_sword".
     * Returns null if not found.
     */
    public static ItemStack itemStackFromName(String name, int amount) {
        if (name == null || name.isEmpty()) return null;
        String fullName = name.contains(":") ? name : "minecraft:" + name;
        ResourceLocation id = ResourceLocation.parse(fullName);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) return null;
        return new ItemStack(item, amount);
    }

    /**
     * Parses a string in the format "amount x item" or "amountxitem" (e.g., "3x minecraft:diamond" or "1x #forge:ingots/iron").
     * Returns a ParsedAmountItem object, or null if invalid.
     */
    public static ParsedAmountItem parseAmountAndItem(String line) {
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.toLowerCase().split("x", 2);
        if (parts.length != 2) return null;
        try {
            int amount = Integer.parseInt(parts[0].trim());
            String itemOrTag = parts[1].trim();
            boolean isTag = itemOrTag.startsWith("#");
            return new ParsedAmountItem(amount, itemOrTag, isTag);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolves a tag (e.g., "#forge:ingots/iron") to a list of Item objects. Returns empty list if not found or not a tag.
     */
    public static java.util.List<Item> resolveTagToItems(String tag) {
        java.util.List<Item> items = new java.util.ArrayList<>();
        if (tag == null || !tag.startsWith("#")) return items;
        try {
            String tagName = tag.substring(1); // Remove '#'
            net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.tryParse(tagName);
            if (tagId == null) return items;
            var tagManager = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagNames();
            // This is a stub; actual tag resolution may require server context or tag registry access
            // For now, just return empty list (implement as needed for your modding environment)
        } catch (Exception e) {
            // Ignore
        }
        return items;
    }

    /**
     * Structure to hold parsed amount and item/tag info.
     */
    public static class ParsedAmountItem {
        public final int amount;
        public final String itemOrTag;
        public final boolean isTag;
        public ParsedAmountItem(int amount, String itemOrTag, boolean isTag) {
            this.amount = amount;
            this.itemOrTag = itemOrTag;
            this.isTag = isTag;
        }
    }
}