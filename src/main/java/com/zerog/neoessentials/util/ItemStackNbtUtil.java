package com.zerog.neoessentials.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Field;

/**
 * Reflection-based NBT helper for ItemStack (NeoForge/Minecraft 1.19+)
 * Allows merging NBT tags even if mappings do not expose standard methods.
 */
public class ItemStackNbtUtil {
    private static Field tagField;

    static {
        try {
            // The field name may be 'tag', 'mTag', or similar depending on mappings
            tagField = ItemStack.class.getDeclaredField("tag");
            tagField.setAccessible(true);
        } catch (Exception e) {
            tagField = null;
        }
    }

    /**
     * Merge the given CompoundTag into the ItemStack's NBT tag.
     * If no tag exists, sets the tag directly.
     */
    public static void mergeTag(ItemStack stack, CompoundTag nbt) {
        if (stack == null || nbt == null || tagField == null) return;
        try {
            CompoundTag current = (CompoundTag) tagField.get(stack);
            if (current == null) {
                tagField.set(stack, nbt.copy());
            } else {
                current.merge(nbt);
            }
        } catch (Exception ignored) {}
    }
}
