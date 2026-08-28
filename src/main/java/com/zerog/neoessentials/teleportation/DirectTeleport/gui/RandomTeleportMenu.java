package com.zerog.neoessentials.teleportation.DirectTeleport.gui;

import com.zerog.neoessentials.auctionhouse.gui.AuctionGuiHelper;
import com.zerog.neoessentials.teleportation.DirectTeleport.BiomeIconRegistry;
import com.zerog.neoessentials.teleportation.DirectTeleport.RandomTeleportManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Paginated chest-GUI biome picker for {@code /rtp} when {@code randomTeleportSettings.mode}
 * is {@code "gui"} — structural copy of {@code leaderboard/gui/LeaderboardMenu.java} (itself
 * copied from {@code auctionhouse/gui/GUIAuctionHouse.java}), reusing the same generic
 * nav-button helpers from {@link AuctionGuiHelper}. View/select-only — picking a biome closes
 * the GUI and delegates to {@link RandomTeleportManager}, same as the plain command path.
 *
 * <p>Biomes pinned to a fixed slot via {@code randomTeleportSettings.biomeMenuItems} (see
 * {@link BiomeIconRegistry}) render at that exact slot on page 1 and are removed from the
 * auto-listed pool; every other biome — including any this mod doesn't recognize, e.g. from a
 * biome mod — still fills the remaining slots automatically, in sorted order, continuing onto
 * later pages exactly as if pinning didn't exist.
 */
public class RandomTeleportMenu extends AbstractContainerMenu {
    private static final int PAGE_SIZE     = 45;
    private static final int DISPLAY_SLOTS = 54;
    private static final int ANY_BIOME_SLOT = 53;

    private final SimpleContainer display = new SimpleContainer(DISPLAY_SLOTS);
    private final ServerPlayer viewer;
    private int page = 0;
    private int totalPages = 1;
    private List<Holder<Biome>> currentSlotBiomes = new ArrayList<>(Collections.nCopies(PAGE_SIZE, null));

    private RandomTeleportMenu(int containerId, Inventory playerInv) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = (ServerPlayer) playerInv.player;

        for (int i = 0; i < DISPLAY_SLOTS; i++) {
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
        populateDisplay();
    }

    public static void open(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.literal(MessageUtil.localize("commands.neoessentials.teleport.misc.rtp_gui_title"));
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new RandomTeleportMenu(id, inv);
            }
        });
    }

    private void populateDisplay() {
        display.clearContent();
        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(viewer);
        List<Holder<Biome>> allBiomes = RandomTeleportManager.getInstance().getPossibleBiomes(level);

        Map<String, Holder<Biome>> byId = new LinkedHashMap<>();
        for (Holder<Biome> h : allBiomes) {
            h.unwrapKey().ifPresent(k -> byId.put(k.location().toString(), h));
        }

        // Resolve configured pins — only entries with a valid slot AND a biome id that actually
        // exists in this dimension count; anything else falls through to auto-listing.
        Map<Integer, Holder<Biome>> pinnedSlots = new HashMap<>();
        Set<String> pinnedIds = new HashSet<>();
        for (BiomeIconRegistry.MenuItemConfig entry : BiomeIconRegistry.getConfiguredMenuItems()) {
            if (entry.slot() == null || entry.slot() < 0 || entry.slot() >= PAGE_SIZE) continue;
            Holder<Biome> holder = byId.get(entry.biomeId());
            if (holder == null) continue;
            pinnedSlots.put(entry.slot(), holder);
            pinnedIds.add(entry.biomeId());
        }

        List<Holder<Biome>> autoFill = new ArrayList<>();
        for (Holder<Biome> h : allBiomes) {
            String id = h.unwrapKey().map(k -> k.location().toString()).orElse(null);
            if (id != null && !pinnedIds.contains(id)) autoFill.add(h);
        }

        int autoFillOffset = autoFillSlotsBeforePage(page, pinnedSlots.size());
        currentSlotBiomes = new ArrayList<>(Collections.nCopies(PAGE_SIZE, null));
        int autoCursor = 0;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            Holder<Biome> holder;
            if (page == 0 && pinnedSlots.containsKey(slot)) {
                holder = pinnedSlots.get(slot);
            } else {
                int autoIdx = autoFillOffset + autoCursor;
                autoCursor++;
                holder = autoIdx < autoFill.size() ? autoFill.get(autoIdx) : null;
            }
            currentSlotBiomes.set(slot, holder);
            display.setItem(slot, holder != null ? buildBiomeStack(holder) : ItemStack.EMPTY);
        }

        totalPages = Math.max(1, (int) Math.ceil(allBiomes.size() / (double) PAGE_SIZE));
        for (int i = 45; i < 54; i++) display.setItem(i, ItemStack.EMPTY);
        display.setItem(48, page > 0             ? AuctionGuiHelper.prevPageItem() : AuctionGuiHelper.prevPageBlockedItem());
        display.setItem(49, AuctionGuiHelper.closeItem());
        display.setItem(50, page + 1 < totalPages ? AuctionGuiHelper.nextPageItem() : AuctionGuiHelper.nextPageBlockedItem());
        for (int i : new int[]{45, 46, 47, 51, 52}) display.setItem(i, AuctionGuiHelper.fillerItem());
        display.setItem(ANY_BIOME_SLOT, buildAnyBiomeStack());
        broadcastChanges();
    }

    /** Page 1 has {@code PAGE_SIZE - page0PinCount} auto-fill slots (pins take the rest); every
     *  later page is fully auto-fill — used to keep a single continuous, non-duplicating cursor
     *  into the auto-fill pool across pages. */
    private static int autoFillSlotsBeforePage(int targetPage, int page0PinCount) {
        if (targetPage <= 0) return 0;
        return (PAGE_SIZE - page0PinCount) + (targetPage - 1) * PAGE_SIZE;
    }

    private ItemStack buildBiomeStack(Holder<Biome> holder) {
        String id = holder.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        ItemStack stack = BiomeIconRegistry.iconFor(id);
        if (stack.isEmpty()) return stack;

        stack.set(DataComponents.CUSTOM_NAME, Component.literal(formatBiomeName(id)).withStyle(ChatFormatting.YELLOW));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(id).withStyle(ChatFormatting.DARK_GRAY),
            Component.literal(MessageUtil.localize("commands.neoessentials.teleport.misc.rtp_gui_click")).withStyle(ChatFormatting.GRAY)
        )));
        return stack;
    }

    private ItemStack buildAnyBiomeStack() {
        ItemStack stack = new ItemStack(Items.ENDER_PEARL);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
            MessageUtil.localize("commands.neoessentials.teleport.misc.rtp_gui_any_biome")).withStyle(ChatFormatting.AQUA));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(MessageUtil.localize("commands.neoessentials.teleport.misc.rtp_gui_click")).withStyle(ChatFormatting.GRAY)
        )));
        return stack;
    }

    private static String formatBiomeName(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= DISPLAY_SLOTS) { super.clicked(slotId, button, clickType, player); return; }
        if (clickType == ClickType.THROW) return;
        ServerPlayer sp = (ServerPlayer) player;

        if (slotId == ANY_BIOME_SLOT) {
            sp.closeContainer();
            RandomTeleportManager.getInstance().randomTeleport(sp, "");
            return;
        }
        if (slotId < PAGE_SIZE) {
            Holder<Biome> holder = currentSlotBiomes.get(slotId);
            if (holder != null) {
                holder.unwrapKey().ifPresent(key -> {
                    sp.closeContainer();
                    RandomTeleportManager.getInstance().randomTeleportToBiome(sp, key);
                });
            }
            return;
        }

        int nav = slotId - PAGE_SIZE;
        switch (nav) {
            case 3 -> { if (page > 0) { page--; populateDisplay(); } }
            case 4 -> sp.closeContainer();
            case 5 -> { if (page + 1 < totalPages) { page++; populateDisplay(); } }
            default -> { }
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}
