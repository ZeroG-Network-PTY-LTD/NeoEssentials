package com.zerog.neoessentials.auctionhouse.gui;
import com.zerog.neoessentials.auctionhouse.AuctionConfig;
import com.zerog.neoessentials.auctionhouse.AuctionHouseManager;
import com.zerog.neoessentials.auctionhouse.AuctionItem;
import com.zerog.neoessentials.util.MessageUtil;
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
import java.util.List;
/**
 * Main Auction House browse screen.
 * MenuType.GENERIC_9x6 - Slots 0-44=item grid, 45-53=nav, 54-89=player inventory.
 */
public class GUIAuctionHouse extends AbstractContainerMenu {
    private static final int PAGE_SIZE     = 45;
    private static final int DISPLAY_SLOTS = 54;
    private final SimpleContainer display = new SimpleContainer(DISPLAY_SLOTS);
    private final ServerPlayer viewer;
    protected int page = 0;
    private GUIAuctionHouse(int containerId, Inventory playerInv) {
        super(MenuType.GENERIC_9x6, containerId);
        this.viewer = (ServerPlayer) playerInv.player;
        for (int i = 0; i < DISPLAY_SLOTS; i++)
            addSlot(new Slot(display, i, 0, 0) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 232));
        populateDisplay();
    }
    public static void open(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.literal(MessageUtil.localize("commands.neoessentials.ah.gui.title_auction_house")); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new GUIAuctionHouse(id, inv);
            }
        });
    }
    protected void populateDisplay() {
        display.clearContent();
        List<AuctionItem> items = AuctionHouseManager.getInstance().getAllActive();
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = page * PAGE_SIZE + i;
            if (idx < items.size()) display.setItem(i, AuctionGuiHelper.buildListingStack(items.get(idx)));
        }
        int total = items.size();
        int totalPages = Math.max(1, Math.min(AuctionConfig.get().getMaxPages(), (total + PAGE_SIZE - 1) / PAGE_SIZE));
        for (int i = 45; i < 54; i++) display.setItem(i, ItemStack.EMPTY);
        display.setItem(48, page > 0             ? AuctionGuiHelper.prevPageItem()         : AuctionGuiHelper.prevPageBlockedItem());
        display.setItem(49, AuctionGuiHelper.closeItem());
        display.setItem(50, page + 1 < totalPages ? AuctionGuiHelper.nextPageItem()        : AuctionGuiHelper.nextPageBlockedItem());
        display.setItem(53, AuctionGuiHelper.myListingsItem());
        for (int i : new int[]{45,46,47,51,52}) display.setItem(i, AuctionGuiHelper.fillerItem());
        broadcastChanges();
    }
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= DISPLAY_SLOTS) { super.clicked(slotId, button, clickType, player); return; }
        if (clickType == ClickType.THROW) return;
        ServerPlayer sp = (ServerPlayer) player;
        List<AuctionItem> items = AuctionHouseManager.getInstance().getAllActive();
        int totalPages = Math.max(1, Math.min(AuctionConfig.get().getMaxPages(), (items.size() + PAGE_SIZE - 1) / PAGE_SIZE));
        if (slotId < PAGE_SIZE) {
            int idx = page * PAGE_SIZE + slotId;
            if (idx < items.size()) { sp.closeContainer(); GUIAuctionItem.open(sp, items.get(idx), false); }
        } else {
            int nav = slotId - PAGE_SIZE;
            switch (nav) {
                case 3 -> { if (page > 0) { page--; populateDisplay(); } }
                case 4 -> sp.closeContainer();
                case 5 -> { if (page + 1 < totalPages) { page++; populateDisplay(); } }
                case 8 -> { sp.closeContainer(); GUIPersonalAuctionHouse.open(sp); }
            }
        }
    }
    @Override public ItemStack quickMoveStack(Player player, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}