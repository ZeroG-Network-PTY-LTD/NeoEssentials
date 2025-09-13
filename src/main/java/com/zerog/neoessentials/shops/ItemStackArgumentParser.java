package com.zerog.neoessentials.shops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ItemStackArgumentParser {
    // Expects format: 'minecraft:stone' or similar
    public static ItemStack fromString(String s) {
        String id = s.trim();
        var resLoc = ResourceLocation.tryParse(id);
        if (resLoc == null) throw new IllegalArgumentException("Invalid item id: " + id);
        var item = BuiltInRegistries.ITEM.get(resLoc);
        if (item == null) throw new IllegalArgumentException("Invalid item id: " + id);
        return new ItemStack(item);
    }
}