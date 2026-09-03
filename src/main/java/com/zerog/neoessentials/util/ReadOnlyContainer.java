package com.zerog.neoessentials.util;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link SimpleContainer} that only the owning GUI (via {@link #forceSetItem}) can redecorate
 * — nothing else can ever pull a real item out of it, no matter which of {@code Container}'s
 * mutation methods it goes through.
 *
 * <p>Blocking extraction at the {@code Slot}/{@code Menu} level ({@code Slot#mayPickup}
 * returning {@code false}, overriding {@code quickMoveStack}/{@code clicked}) only closes the
 * normal click-packet path, and refusing just {@link #removeItem}/{@link #removeItemNoUpdate}
 * still leaves {@link #setItem} open — {@code Slot#set(ItemStack)} (and anything else that reads
 * a slot then blanks it) goes through {@code setItem}, not {@code removeItem}, which is exactly
 * how a "pull items out of the open GUI" inventory-utility mod got past the first version of this
 * fix. So the public {@link #setItem} here is a no-op too; the owning menu must call
 * {@link #forceSetItem} instead when it wants to actually change what's displayed. That leaves no
 * public mutator that can ever hand back or blank a real item.
 */
public class ReadOnlyContainer extends SimpleContainer {
    public ReadOnlyContainer(int size) {
        super(size);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Ignored — external code (including non-click extraction paths) must not be able to
        // mutate the display. The owning menu uses forceSetItem instead.
    }

    /** The owning menu's own redraw path — the only way this container's contents ever change. */
    public void forceSetItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
    }
}
