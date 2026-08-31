package com.zerog.neoessentials.crates.gui;

import com.zerog.neoessentials.auctionhouse.gui.AuctionGuiHelper;
import com.zerog.neoessentials.crates.CrateDefinition;
import com.zerog.neoessentials.crates.CrateReward;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** Read-only reward-odds viewer — {@code /crate preview <name>}, no key cost. */
public class CratePreviewMenu extends AbstractContainerMenu {
    private static final int PAGE_SIZE = 45;

    private final SimpleContainer display = new SimpleContainer(54);
    private final ServerPlayer viewer;
    private final CrateDefinition crate;
    private int page = 0;

    private CratePreviewMenu(int containerId, Inventory playerInv, CrateDefinition crate) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = (ServerPlayer) playerInv.player;
        this.crate = crate;

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
        populateDisplay();
    }

    public static void open(ServerPlayer player, CrateDefinition crate) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.literal(MessageUtil.localize("commands.neoessentials.crate.gui.preview_title", crate.displayName));
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new CratePreviewMenu(id, inv, crate);
            }
        });
    }

    private void populateDisplay() {
        display.clearContent();
        List<CrateReward> rewards = crate.rewards;
        double totalWeight = rewards.stream().mapToDouble(r -> r.weight).filter(w -> w > 0).sum();

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= rewards.size()) break;
            display.setItem(i, buildRewardStack(rewards.get(idx), totalWeight));
        }

        int totalPages = Math.max(1, (int) Math.ceil(rewards.size() / (double) PAGE_SIZE));
        for (int i = 45; i < 54; i++) display.setItem(i, ItemStack.EMPTY);
        display.setItem(48, page > 0             ? AuctionGuiHelper.prevPageItem() : AuctionGuiHelper.prevPageBlockedItem());
        display.setItem(49, AuctionGuiHelper.closeItem());
        display.setItem(50, page + 1 < totalPages ? AuctionGuiHelper.nextPageItem() : AuctionGuiHelper.nextPageBlockedItem());
        for (int i : new int[]{45, 46, 47, 51, 52, 53}) display.setItem(i, AuctionGuiHelper.fillerItem());
        broadcastChanges();
    }

    private ItemStack buildRewardStack(CrateReward reward, double totalWeight) {
        if (reward.item.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = reward.item.copy();
        double chance = totalWeight > 0 ? (reward.weight / totalWeight) * 100.0 : 0;
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(String.format("%.2f%% chance", chance)).withStyle(ChatFormatting.GRAY)
        )));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId < 0 || slotId >= 54) { super.clicked(slotId, button, clickType, player); return; }
        if (clickType == ContainerInput.THROW) return;
        if (slotId < PAGE_SIZE) return; // preview is view-only

        ServerPlayer sp = (ServerPlayer) player;
        int totalPages = Math.max(1, (int) Math.ceil(crate.rewards.size() / (double) PAGE_SIZE));
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
