package com.zerog.neoessentials.items;

import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for managing ItemStack operations with config-aware stack sizes.
 * Handles oversized stacks and default stack size overrides.
 */
public class ItemStackHelper {
    @SuppressWarnings("unused") // Reserved for future logging features
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemStackHelper.class);
    
    /**
     * Get the effective max stack size for an item, respecting config settings.
     * 
     * @param item The item to check
     * @return The maximum stack size for this item
     */
    public static int getMaxStackSize(Item item) {
        int defaultSize = ConfigManager.getDefaultStackSize();
        int oversizedSize = ConfigManager.getOversizedStackSize();
        
        // If default-stack-size is -1, use vanilla behavior
        if (defaultSize == -1) {
            int vanillaMax = item.getDefaultMaxStackSize();
            // If oversized stacks are enabled and larger than vanilla, use oversized
            return Math.max(vanillaMax, oversizedSize);
        }
        
        // Otherwise use the configured default, capped at oversized max
        return Math.min(defaultSize, oversizedSize);
    }
    
    /**
     * Get the effective max stack size for an ItemStack.
     * 
     * @param stack The ItemStack to check
     * @return The maximum stack size
     */
    public static int getMaxStackSize(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return getMaxStackSize(stack.getItem());
    }
    
    /**
     * Check if a stack can accept more items.
     * 
     * @param stack The stack to check
     * @param amount The amount to add
     * @return true if the stack can accept the amount
     */
    public static boolean canAcceptAmount(ItemStack stack, int amount) {
        if (stack.isEmpty()) {
            return amount <= getMaxStackSize(stack.getItem());
        }
        return stack.getCount() + amount <= getMaxStackSize(stack);
    }
    
    /**
     * Set the count on an ItemStack, capping at the configured max stack size.
     * 
     * @param stack The stack to modify
     * @param count The desired count
     * @return The actual count set (may be less if over max)
     */
    public static int setCount(ItemStack stack, int count) {
        int maxStack = getMaxStackSize(stack);
        int actualCount = Math.min(count, maxStack);
        stack.setCount(actualCount);
        return actualCount;
    }
    
    /**
     * Grow the stack by the specified amount, respecting max stack size.
     * 
     * @param stack The stack to grow
     * @param amount The amount to add
     * @return The actual amount added
     */
    public static int growStack(ItemStack stack, int amount) {
        int maxStack = getMaxStackSize(stack);
        int currentCount = stack.getCount();
        int newCount = Math.min(currentCount + amount, maxStack);
        int actualAdded = newCount - currentCount;
        stack.setCount(newCount);
        return actualAdded;
    }
    
    /**
     * Check if an item is stackable according to config settings.
     * 
     * @param item The item to check
     * @return true if the item can stack
     */
    public static boolean isStackable(Item item) {
        return getMaxStackSize(item) > 1;
    }
}
