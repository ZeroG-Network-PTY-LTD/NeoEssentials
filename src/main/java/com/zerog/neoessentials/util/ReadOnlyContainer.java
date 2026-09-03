package com.zerog.neoessentials.util;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A {@link SimpleContainer} the owning GUI can freely redecorate via {@link #setItem}, but that
 * can never actually be depleted by anything else — {@link #removeItem}/
 * {@link #removeItemNoUpdate} always refuse and return {@link ItemStack#EMPTY} instead of
 * handing back the real stack.
 *
 * <p>Blocking extraction at the {@code Slot}/{@code Menu} level ({@code Slot#mayPickup}
 * returning {@code false}, overriding {@code quickMoveStack}/{@code clicked}) only closes the
 * normal click-packet path. Several inventory-utility client mods pull items out of "whatever
 * container is behind the currently open GUI" through a capability or other route that never
 * touches a {@code Slot} at all, bypassing all three of those. Refusing at the container's own
 * removal methods is the one choke point every extraction path — present or future — has to go
 * through to actually take an item out, so a purely decorative preview/reveal GUI backed by this
 * class can't leak a real item no matter what mechanism a client mod uses to try.
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
}
