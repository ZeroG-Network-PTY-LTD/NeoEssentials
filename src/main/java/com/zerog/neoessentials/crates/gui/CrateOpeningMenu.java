package com.zerog.neoessentials.crates.gui;

import com.zerog.neoessentials.crates.CrateAnimation;
import com.zerog.neoessentials.crates.CrateDefinition;
import com.zerog.neoessentials.crates.CrateManager;
import com.zerog.neoessentials.crates.CrateReward;
import com.zerog.neoessentials.crates.WeightedRandomPicker;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The crate-opening reveal GUI — the reward is already determined ({@link CrateManager}
 * consumes the key and resolves the winner before this menu even opens) so nothing here can
 * change the outcome; this purely animates revealing it, styled per {@link CrateAnimation}.
 *
 * <p>The {@code ROULETTE} animation ticks via its own {@link ScheduledExecutorService} (same
 * shape as {@code HologramScheduler} — mutations marshalled back to the main thread via
 * {@code server.execute()}), sliding a 9-wide window across a long randomized "reel" with the
 * real winning reward spliced in near the end, and decelerates by widening the delay between
 * shifts as it approaches landing rather than continuous physics easing — much simpler to get
 * right in a scheduler-tick model.
 */
public class CrateOpeningMenu extends AbstractContainerMenu {
    private static final int REEL_LENGTH = 45;
    private static final int TARGET_INDEX = 40; // lands with a few slots of "runway" after it
    private static final Random RANDOM = new Random();

    private final SimpleContainer display = new SimpleContainer(54);
    private final ServerPlayer viewer;
    private final CrateDefinition crate;
    private final CrateReward wonReward;
    private ScheduledExecutorService scheduler;

    private CrateOpeningMenu(int containerId, Inventory playerInv, CrateDefinition crate, CrateReward wonReward) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = (ServerPlayer) playerInv.player;
        this.crate = crate;
        this.wonReward = wonReward;

        for (int i = 0; i < 54; i++) {
            addSlot(new Slot(display, i, 0, 0) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 232));
        }

        runAnimation();
    }

    public static void open(ServerPlayer player, CrateDefinition crate, CrateReward wonReward) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.literal(MessageUtil.localize("commands.neoessentials.crate.gui.opening_title", crate.displayName));
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new CrateOpeningMenu(id, inv, crate, wonReward);
            }
        });
    }

    private void runAnimation() {
        switch (crate.animation) {
            case INSTANT -> finish();
            case SEQUENTIAL -> runSequential();
            case ROULETTE -> runRoulette();
        }
    }

    private void runSequential() {
        int flickers = 10;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Crate-Sequential");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < flickers; i++) {
            boolean last = i == flickers - 1;
            long delayMs = 150L * (i + 1);
            scheduler.schedule(() -> {
                var server = viewer.getServer();
                if (server == null) return;
                server.execute(() -> {
                    ItemStack shown = last ? displayStack(wonReward) : displayStack(randomReward());
                    for (int slot : new int[]{20, 21, 22, 23, 24, 29, 30, 31, 32, 33}) {
                        display.setItem(slot, shown);
                    }
                    broadcastChanges();
                    if (last) finish();
                });
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void runRoulette() {
        List<CrateReward> reel = new ArrayList<>(REEL_LENGTH);
        for (int i = 0; i < REEL_LENGTH; i++) {
            reel.add(i == TARGET_INDEX ? wonReward : randomReward());
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Crate-Roulette");
            t.setDaemon(true);
            return t;
        });
        scheduleStep(reel, 0, 60L);
    }

    private void scheduleStep(List<CrateReward> reel, int index, long delayMs) {
        scheduler.schedule(() -> {
            var server = viewer.getServer();
            if (server == null) return;
            server.execute(() -> {
                renderReelWindow(reel, index);
                if (index >= TARGET_INDEX) {
                    finish();
                    return;
                }
                // Decelerate: delay grows as the target approaches, giving a natural slow-down
                // feel without needing continuous-easing math.
                int remaining = TARGET_INDEX - index;
                long nextDelay = remaining <= 8 ? delayMs + 40L : delayMs;
                scheduleStep(reel, index + 1, Math.min(nextDelay, 400L));
            });
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void renderReelWindow(List<CrateReward> reel, int centerIndex) {
        for (int i = 0; i < 9; i++) {
            int reelIdx = centerIndex - 4 + i;
            ItemStack stack = (reelIdx >= 0 && reelIdx < reel.size()) ? displayStack(reel.get(reelIdx)) : ItemStack.EMPTY;
            display.setItem(9 + i, stack); // second row, so the "pointer" row is visually centered
        }
        broadcastChanges();
    }

    private void finish() {
        for (int i = 0; i < 54; i++) display.setItem(i, ItemStack.EMPTY);
        display.setItem(22, displayStack(wonReward));
        broadcastChanges();

        CrateManager.getInstance().grantReward(viewer, crate, wonReward);
        viewer.sendSystemMessage(MessageUtil.success("commands.neoessentials.crate.opened",
            crate.displayName, wonReward.item.isEmpty() ? wonReward.id : wonReward.item.getHoverName().getString()));

        if (scheduler != null) scheduler.shutdown();
    }

    private CrateReward randomReward() {
        if (crate.rewards.isEmpty()) return wonReward;
        return crate.rewards.get(RANDOM.nextInt(crate.rewards.size()));
    }

    private static ItemStack displayStack(CrateReward reward) {
        if (reward == null || reward.item.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = reward.item.copy();
        stack.set(DataComponents.CUSTOM_NAME, stack.getHoverName().copy());
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // View-only — the reveal can't be interacted with, only watched.
    }

    @Override public ItemStack quickMoveStack(Player player, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (scheduler != null) scheduler.shutdown();
    }
}
