package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.util.CompoundTagCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Marks (and purges) display-only "ghost" item stacks — the reward-icon stacks shown in
 * {@code /crate preview} and the opening reveal GUI.
 *
 * <p>{@link com.zerog.neoessentials.util.ReadOnlyContainer} blocks every normal way of removing
 * an item from those GUIs (the click-packet path, and every {@code Container} mutation method),
 * but that only stops code that tries to <i>take</i> an item out. Some client-side
 * inventory-utility mods (confirmed: Quark's item-pull feature) don't take anything from the
 * container at all — they just read a slot's item and copy it straight into the player's real
 * inventory, a path no container-level defense can close without also breaking the ability to
 * render the preview in the first place.
 *
 * <p>So instead of trying to stop the copy, every ghost stack this mod displays carries a hidden
 * marker, and {@link #sweep} strips any stack carrying it out of a player's real inventory —
 * wherever it came from, however it got there. Both crate GUIs run a sweep on a short timer
 * while open, then {@link #sweepWithGracePeriod} a few more times over the couple of seconds
 * after they close, so an exfiltrated copy never survives long enough to be useful. This is
 * deliberately mod-agnostic: it doesn't matter which mod or mechanism did the copying.
 *
 * <p>The grace period exists because closing the GUI stops the periodic sweep immediately, but
 * doesn't guarantee the extraction itself has already been fully processed server-side — a
 * player clicking a pull button and then immediately hitting E/Esc to close could still win the
 * race and keep a copy that lands a tick or two after the close-triggered sweep already ran,
 * with no menu left open for the next periodic sweep to ever catch it.
 */
public final class CrateGhostItemGuard {
    private static final String GHOST_TAG = "neoessentials_crate_ghost";
    private static final long SWEEP_INTERVAL_MS = 250L;
    /** Extra one-off sweeps after the GUI closes, catching an extraction that resolves
     *  server-side slightly after the close-triggered sweep already ran. */
    private static final long[] GRACE_SWEEP_DELAYS_MS = {200L, 500L, 1000L, 2000L, 4000L};

    private CrateGhostItemGuard() {}

    /** Tags {@code stack} as a ghost — never call this on a stack that's actually being granted
     *  to a player, only on the display copy shown in a crate GUI. */
    public static void mark(ItemStack stack) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.has(DataComponents.CUSTOM_DATA)
            ? stack.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        tag.putBoolean(GHOST_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isGhost(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) return false;
        return CompoundTagCompat.getBoolean(stack.get(DataComponents.CUSTOM_DATA).copyTag(), GHOST_TAG);
    }

    /** Removes any ghost-tagged stack found anywhere in the player's real inventory. Silent —
     *  these stacks should never have left the display in the first place, so there's nothing
     *  legitimate to explain to the player. */
    public static void sweep(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isGhost(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    /** Starts a short-interval background sweep of {@code player}'s inventory for as long as a
     *  crate preview/opening GUI is open — the caller must {@code shutdown()} the returned
     *  scheduler and call {@link #sweepWithGracePeriod} (not just {@link #sweep}) when the menu
     *  closes. */
    public static ScheduledExecutorService startWatch(ServerPlayer player) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Crate-Ghost-Guard");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            var server = player.level().getServer();
            if (server == null) return;
            server.execute(() -> sweep(player));
        }, SWEEP_INTERVAL_MS, SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        return scheduler;
    }

    /** Sweeps immediately, then again a few more times over the next several seconds — call this
     *  (instead of a single {@link #sweep}) when a crate GUI closes, to catch an extraction that
     *  resolves server-side just after the close itself. */
    public static void sweepWithGracePeriod(ServerPlayer player) {
        sweep(player);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Crate-Ghost-Guard-Grace");
            t.setDaemon(true);
            return t;
        });
        for (long delay : GRACE_SWEEP_DELAYS_MS) {
            scheduler.schedule(() -> {
                var server = player.level().getServer();
                if (server == null) return;
                server.execute(() -> sweep(player));
            }, delay, TimeUnit.MILLISECONDS);
        }
        long lastDelay = GRACE_SWEEP_DELAYS_MS[GRACE_SWEEP_DELAYS_MS.length - 1];
        scheduler.schedule(scheduler::shutdown, lastDelay + 100L, TimeUnit.MILLISECONDS);
    }
}
