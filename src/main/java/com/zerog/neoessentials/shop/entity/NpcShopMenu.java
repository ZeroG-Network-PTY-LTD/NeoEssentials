package com.zerog.neoessentials.shop.entity;

import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.api.ShopEconomyAdapter;
import com.zerog.neoessentials.shop.api.ShopEconomyRegistry;
import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
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
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Virtual 6-row chest GUI that displays NPC shop listings.
 *
 * <p>Each occupied slot displays an item from the shop's {@link ShopListing} list.
 * When a player clicks a slot, the system executes a BUY transaction (right-click =
 * single purchase).  Player inventory slots below are real and are needed for
 * Minecraft's container sync but are locked — items cannot be taken out of the
 * display area.
 *
 * <p>Uses the built-in {@link MenuType#GENERIC_9x6} so no custom MenuType
 * registration is required.
 */
public class NpcShopMenu extends AbstractContainerMenu {

    private static final int SHOP_ROWS           = 6;
    private static final int SHOP_SLOTS          = SHOP_ROWS * 9; // 54
    private static final int PLAYER_INV_START    = SHOP_SLOTS;

    private final Container shopContainer;
    private final ShopEntityData shopData;
    private final UUID viewerUUID;

    // ── Constructor (server-side) ─────────────────────────────────────────────

    public NpcShopMenu(int containerId, Inventory playerInventory, ShopEntityData shopData) {
        super(MenuType.GENERIC_9x6, containerId);
        this.shopData      = shopData;
        this.viewerUUID    = playerInventory.player.getUUID();

        // Build a read-only virtual container populated with listing items
        this.shopContainer = buildShopContainer(shopData.listings);

        // Add shop display slots (locked — see overrideSlot)
        for (int row = 0; row < SHOP_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                this.addSlot(new LockedSlot(shopContainer, slotIndex,
                        8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory (rows 1-3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 140 + row * 18));
            }
        }

        // Add hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    // ── Click handling ────────────────────────────────────────────────────────

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < SHOP_SLOTS) {
            // Shop slot clicked — execute a BUY transaction for this listing
            int listingIndex = slotId;
            if (listingIndex < shopData.listings.size()) {
                ShopListing listing = shopData.listings.get(listingIndex);
                if (listing.canBuy() && player instanceof ServerPlayer sp) {
                    executeListing(sp, listing);
                }
            }
            // never move items out of the display
            return;
        }
        // Player inventory interactions pass through normally
        super.clicked(slotId, button, clickType, player);
    }

    /**
     * Shift-click — disabled for the shop display area; normal for the player inventory.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < SHOP_SLOTS) return ItemStack.EMPTY; // lock shop display slots
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            // Standard shift-click into upper container — no-op here since shop is virtual
            slot.getItem(); // satisfy compiler; no actual move
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    // ── Transaction logic ─────────────────────────────────────────────────────

    private void executeListing(ServerPlayer player, ShopListing listing) {
        ShopEconomyAdapter eco = ShopEconomyRegistry.getInstance().getAdapter();

        if (!listing.canBuy()) {
            player.sendSystemMessage(Component.literal("§cThis item is not for sale."));
            return;
        }

        BigDecimal price = listing.buyPrice().setScale(2, RoundingMode.HALF_UP);

        if (!eco.hasBalance(player.getUUID(), price)) {
            player.sendSystemMessage(Component.literal("§cYou don't have enough money. Need " +
                    eco.format(price) + "."));
            return;
        }

        ItemStack template = ShopTransaction.resolveItem(listing.itemId());
        if (template.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cThis item could not be resolved."));
            return;
        }

        ItemStack give = template.copyWithCount(listing.quantity());
        if (!ShopTransaction.hasSpaceInContainer(player.getInventory(), give, listing.quantity())) {
            player.sendSystemMessage(Component.literal("§cYour inventory is full."));
            return;
        }

        if (!eco.debit(player.getUUID(), price)) {
            player.sendSystemMessage(Component.literal("§cPayment failed."));
            return;
        }

        ShopTransaction.giveItems(player, give);

        player.sendSystemMessage(Component.literal(String.format(
                "§aBought §f%dx %s §afor §f%s§a.",
                listing.quantity(),
                listing.itemId().replace("minecraft:", ""),
                eco.format(price))));

        // Fire event using a synthetic ShopData stub so listeners can process it
        NeoForge.EVENT_BUS.post(new ShopTransactionEvent(
                null, player.getUUID(), ShopTransactionEvent.Type.BUY, price, listing.quantity()));
    }

    // ── Virtual container builder ─────────────────────────────────────────────

    private static Container buildShopContainer(List<ShopListing> listings) {
        SimpleContainer container = new SimpleContainer(SHOP_SLOTS);
        int i = 0;
        for (ShopListing listing : listings) {
            if (i >= SHOP_SLOTS) break;
            ItemStack template = ShopTransaction.resolveItem(listing.itemId());
            if (!template.isEmpty()) {
                ItemStack display = template.copyWithCount(listing.quantity());
                // Encode buy/sell price info as item lore
                List<Component> lore = new java.util.ArrayList<>();
                if (listing.canBuy())  lore.add(Component.literal("§aBuy:  " + listing.buyPrice().toPlainString()));
                if (listing.canSell()) lore.add(Component.literal("§6Sell: " + listing.sellPrice().toPlainString()));
                lore.add(Component.literal("§7Click to buy"));
                display.set(net.minecraft.core.component.DataComponents.LORE,
                        new net.minecraft.world.item.component.ItemLore(lore));
                container.setItem(i, display);
            }
            i++;
        }
        return container;
    }

    // ── Locked slot (prevents taking items from display) ─────────────────────

    private static class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    // ── MenuProvider ──────────────────────────────────────────────────────────

    /**
     * MenuProvider that opens this menu for a given {@link ServerPlayer}.
     */
    public static class NpcShopMenuProvider implements MenuProvider {
        private final ShopEntityData shopData;
        private final ServerPlayer   viewer;

        public NpcShopMenuProvider(ShopEntityData shopData, ServerPlayer viewer) {
            this.shopData = shopData;
            this.viewer   = viewer;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("§6" + shopData.shopName);
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new NpcShopMenu(containerId, playerInventory, shopData);
        }
    }
}

