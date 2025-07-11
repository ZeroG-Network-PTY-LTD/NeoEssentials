package com.zerog.neoessentials.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public class GuiUtils {
    public static ItemStack createItem(Object itemOrItems, String... lore) {
        // Minimal stub: always returns a stone for now
        ItemStack stack = new ItemStack(Items.STONE);
        if (lore.length > 0) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(lore[0]));
        }
        return stack;
    }
}
