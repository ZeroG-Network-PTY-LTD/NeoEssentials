package com.zerog.neoessentials.auctionhouse.gui;
import com.zerog.neoessentials.auctionhouse.AuctionHouseManager;
import com.zerog.neoessentials.auctionhouse.AuctionItem;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.List;
/**
 * Detail / buy (or cancel) screen for a single Auction House listing.
 * Uses a 1-row chest (MenuType.GENERIC_9x1 = 9 display slots).
 */
public class GUIAuctionItem extends AbstractContainerMenu {
    private static final int DISPLAY_SLOTS = 9;
    private final SimpleContainer display = new SimpleContainer(DISPLAY_SLOTS);
    private final AuctionItem item;
    private final boolean fromPersonal;
    private final ServerPlayer viewer;
    private GUIAuctionItem(int containerId, Inventory playerInv, AuctionItem item, boolean fromPersonal) {
        super(MenuType.GENERIC_9x1, containerId);
        this.item = item;
        this.fromPersonal = fromPersonal;
        this.viewer = (ServerPlayer) playerInv.player;
        for (int i = 0; i < DISPLAY_SLOTS; i++)
            addSlot(new Slot(display, i, 0, 0) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        populateDisplay();
    }
    public static void open(ServerPlayer player, AuctionItem item, boolean fromPersonal) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.literal(fromPersonal ? MessageUtil.localize("commands.neoessentials.ah.gui.title_my_listing") : MessageUtil.localize("commands.neoessentials.ah.gui.title_buy_item")); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new GUIAuctionItem(id, inv, item, fromPersonal);
            }
        });
    }
    private void populateDisplay() {
        display.clearContent();
        boolean isSeller = viewer.getStringUUID().equals(item.getUuid());
        ItemStack preview = item.getItemStack().copy();
        if (!preview.isEmpty()) {
            preview.set(net.minecraft.core.component.DataComponents.LORE, new ItemLore(List.of(
                Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.seller", item.getOwner())).withStyle(ChatFormatting.GRAY),
                Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.price", String.format("%.2f", item.getPrice()))).withStyle(ChatFormatting.GOLD),
                Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.time", item.getTimeLeft())).withStyle(ChatFormatting.DARK_PURPLE)
            )));
        }
        display.setItem(0, preview);
        display.setItem(4, buildActionButton(isSeller));
        display.setItem(8, AuctionGuiHelper.backItem(MessageUtil.localize("commands.neoessentials.ah.gui.back")));
        for (int i : new int[]{1,2,3,5,6,7}) display.setItem(i, AuctionGuiHelper.fillerItem());
        broadcastChanges();
    }
    private ItemStack buildActionButton(boolean isSeller) {
        if (isSeller)
            return AuctionGuiHelper.named(new ItemStack(Items.RED_CONCRETE),
                    Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.cancel_listing")).withStyle(ChatFormatting.RED));
        double bal = com.zerog.neoessentials.api.EconomyAPI.getBalance(viewer.getUUID()).doubleValue();
        boolean canAfford = bal >= item.getPrice();
        ItemStack btn = new ItemStack(canAfford ? Items.EMERALD : Items.BARRIER);
        btn.set(net.minecraft.core.component.DataComponents.LORE, new ItemLore(List.of(
            Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.your_balance", String.format("%.2f", bal))).withStyle(ChatFormatting.GRAY)
        )));
        return AuctionGuiHelper.named(btn, Component.literal(canAfford ? MessageUtil.localize("commands.neoessentials.ah.gui.buy") : MessageUtil.localize("commands.neoessentials.ah.gui.not_enough_money"))
                .withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= DISPLAY_SLOTS) { super.clicked(slotId, button, clickType, player); return; }
        if (clickType == ClickType.THROW) return;
        ServerPlayer sp = (ServerPlayer) player;
        sp.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 1f, 1f);
        boolean isSeller = sp.getStringUUID().equals(item.getUuid());
        switch (slotId) {
            case 4 -> {
                if (isSeller) {
                    boolean ok = AuctionHouseManager.getInstance().cancelListing(item, sp);
                    sp.sendSystemMessage(ok ? MessageUtil.component("commands.neoessentials.ah.gui.listing_cancelled") : MessageUtil.component("commands.neoessentials.ah.gui.cancel_failed"));
                    sp.closeContainer(); GUIPersonalAuctionHouse.open(sp);
                } else {
                    double bal = com.zerog.neoessentials.api.EconomyAPI.getBalance(sp.getUUID()).doubleValue();
                    if (bal >= item.getPrice()) {
                        boolean ok = AuctionHouseManager.getInstance().buyItem(item, sp);
                        sp.sendSystemMessage(ok
                            ? MessageUtil.component("commands.neoessentials.ah.gui.purchased", item.getItemKey(), String.format("%.2f", item.getPrice()))
                            : MessageUtil.component("commands.neoessentials.ah.gui.purchase_failed"));
                    }
                    sp.closeContainer();
                    if (fromPersonal) GUIPersonalAuctionHouse.open(sp); else GUIAuctionHouse.open(sp);
                }
            }
            case 8 -> { sp.closeContainer(); if (fromPersonal) GUIPersonalAuctionHouse.open(sp); else GUIAuctionHouse.open(sp); }
        }
    }
    @Override public ItemStack quickMoveStack(Player player, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}