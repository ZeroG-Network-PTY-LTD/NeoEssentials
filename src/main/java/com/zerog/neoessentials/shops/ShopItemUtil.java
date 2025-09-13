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
}