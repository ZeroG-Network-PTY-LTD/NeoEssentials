package com.zerog.neoessentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Constructs a {@link HoverEvent} from an action/value pair.
 *
 * <p>As of Minecraft 26.1, {@code HoverEvent} is a sealed interface implemented by
 * per-action records ({@code ShowText}, {@code ShowItem}, {@code ShowEntity}) rather
 * than a single concrete class with a generic {@code Action<V>} constructor.</p>
 */
public final class HoverEventCompat {
    private HoverEventCompat() {}

    public static HoverEvent create(HoverEvent.Action action, Component value) {
        if (action != HoverEvent.Action.SHOW_TEXT) {
            throw new IllegalArgumentException("Expected SHOW_TEXT action for a Component value, got " + action);
        }
        return new HoverEvent.ShowText(value);
    }

    public static HoverEvent create(HoverEvent.Action action, ItemStack value) {
        if (action != HoverEvent.Action.SHOW_ITEM) {
            throw new IllegalArgumentException("Expected SHOW_ITEM action for an ItemStack value, got " + action);
        }
        return new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(value));
    }

    public static HoverEvent create(HoverEvent.Action action, HoverEvent.EntityTooltipInfo value) {
        if (action != HoverEvent.Action.SHOW_ENTITY) {
            throw new IllegalArgumentException("Expected SHOW_ENTITY action for entity tooltip info, got " + action);
        }
        return new HoverEvent.ShowEntity(value);
    }
}
