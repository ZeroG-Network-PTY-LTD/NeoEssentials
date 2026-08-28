package com.zerog.neoessentials.leaderboard.gui;

import com.mojang.authlib.GameProfile;
import com.zerog.neoessentials.auctionhouse.gui.AuctionGuiHelper;
import com.zerog.neoessentials.leaderboard.LeaderboardCache;
import com.zerog.neoessentials.leaderboard.LeaderboardManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.List;
import java.util.UUID;

/**
 * Paginated chest-GUI leaderboard viewer — view-only, structural copy of
 * {@code auctionhouse/gui/GUIAuctionHouse.java} (45 display slots + nav row at 45-53), reusing
 * that same nav-button/name/lore helpers from {@link AuctionGuiHelper} since they're generic,
 * not auction-specific.
 */
public class LeaderboardMenu extends AbstractContainerMenu {
    private static final int PAGE_SIZE     = 45;
    private static final int DISPLAY_SLOTS = 54;

    private final SimpleContainer display = new SimpleContainer(DISPLAY_SLOTS);
    private final ServerPlayer viewer;
    private final String boardId;
    private int page = 0;

    private LeaderboardMenu(int containerId, Inventory playerInv, String boardId) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = (ServerPlayer) playerInv.player;
        this.boardId = boardId;

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

    public static void open(ServerPlayer player, String boardId) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
                String name = cache != null ? cache.getDefinition().displayName() : boardId;
                return Component.literal(MessageUtil.localize("commands.neoessentials.leaderboard.gui.title", name));
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new LeaderboardMenu(id, inv, boardId);
            }
        });
    }

    private void populateDisplay() {
        display.clearContent();
        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
        if (cache == null) { broadcastChanges(); return; }

        var pageEntries = cache.getPage(viewer.level().getServer(), page + 1, PAGE_SIZE);
        int startRank = page * PAGE_SIZE + 1;
        int rank = startRank;
        for (var entry : pageEntries) {
            int slot = rank - startRank;
            if (slot >= PAGE_SIZE) break;
            String formatted = cache.getProvider().formatValue(entry.value());
            display.setItem(slot, buildEntryStack(entry, rank, formatted, cache));
            rank++;
        }

        int totalPages = cache.getTotalPages(PAGE_SIZE);
        for (int i = 45; i < 54; i++) display.setItem(i, ItemStack.EMPTY);
        display.setItem(48, page > 0             ? AuctionGuiHelper.prevPageItem() : AuctionGuiHelper.prevPageBlockedItem());
        display.setItem(49, AuctionGuiHelper.closeItem());
        display.setItem(50, page + 1 < totalPages ? AuctionGuiHelper.nextPageItem() : AuctionGuiHelper.nextPageBlockedItem());
        for (int i : new int[]{45, 46, 47, 51, 52, 53}) display.setItem(i, AuctionGuiHelper.fillerItem());
        broadcastChanges();
    }

    private ItemStack buildEntryStack(LeaderboardCache.Entry entry, int rank, String formatted, LeaderboardCache cache) {
        ItemStack stack = entry.playerUuid() != null
            ? buildPlayerHead(entry.playerUuid(), entry.name())
            : buildIcon(cache.getDefinition().icon());
        if (stack.isEmpty()) return stack;

        stack.set(DataComponents.CUSTOM_NAME, Component.literal("#" + rank + " " + entry.name()).withStyle(ChatFormatting.YELLOW));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(MessageUtil.localize("commands.neoessentials.leaderboard.gui.value", formatted)).withStyle(ChatFormatting.GRAY)
        )));
        return stack;
    }

    /** Same pattern as {@code /skull} (util/commands/ItemCustomisationCommands.java) — real
     *  texture via the profile resolver, name-only fallback if unresolvable.
     *  26.1 port: GameProfileCache was replaced by Services.profileResolver(), which resolves
     *  the full GameProfile (including textures) in one call; ResolvableProfile is now
     *  abstract/sealed, constructed via createResolved(...). */
    private ItemStack buildPlayerHead(UUID playerUuid, String name) {
        ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
        try {
            viewer.level().getServer().services().profileResolver().fetchById(playerUuid)
                .ifPresent(profile -> skull.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile)));
        } catch (Exception ignored) {
            // fall through to the name-only profile below
        }
        if (!skull.has(DataComponents.PROFILE)) {
            skull.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(playerUuid, name)));
        }
        return skull;
    }

    private ItemStack buildIcon(String iconId) {
        try {
            Identifier rl = Identifier.parse(iconId != null ? iconId : "minecraft:paper");
            return new ItemStack(BuiltInRegistries.ITEM.getValue(rl));
        } catch (Exception e) {
            return new ItemStack(Items.PAPER);
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId < 0 || slotId >= DISPLAY_SLOTS) { super.clicked(slotId, button, clickType, player); return; }
        if (clickType == ContainerInput.THROW) return;
        if (slotId < PAGE_SIZE) return; // view-only — no action on entry click

        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
        int totalPages = cache != null ? cache.getTotalPages(PAGE_SIZE) : 1;
        int nav = slotId - PAGE_SIZE;
        switch (nav) {
            case 3 -> { if (page > 0) { page--; populateDisplay(); } }
            case 4 -> ((ServerPlayer) player).closeContainer();
            case 5 -> { if (page + 1 < totalPages) { page++; populateDisplay(); } }
            default -> { }
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}
