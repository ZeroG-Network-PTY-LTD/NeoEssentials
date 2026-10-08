package com.zerog.neoessentials.shop.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One entry in an NPC shop's inventory — either an item for sale, or a paid command listing.
 *
 * @param itemId       registry ID, e.g. {@code "minecraft:diamond"} — for a command listing,
 *                     just the icon shown in the shop GUI
 * @param itemNbt      the item's data components (custom name, enchantments, modded data) as
 *                     JSON when it was added from an admin's hand — same format as
 *                     {@code ShopData.itemNbt}; null = plain item
 * @param buyPrice     price customer pays to buy; null = buy disabled
 * @param sellPrice    price customer receives to sell; null = sell disabled (always null for
 *                     a command listing)
 * @param quantity     units per transaction (icon stack size for a command listing)
 * @param displayName  "&"-coded name shown in the GUI instead of the item's own name; null = item name
 * @param commands     null for an item listing; for a command listing, the commands run on
 *                     purchase ({player}/{uuid} placeholders) — may be empty while being set up
 * @param runAsPlayer  command listing only: run as the buyer instead of the console
 * @param permission   command listing only: node required to buy; null = anyone with shop access
 */
public record ShopListing(
        String itemId,
        String itemNbt,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        int quantity,
        String displayName,
        List<String> commands,
        boolean runAsPlayer,
        String permission
) {
    public ShopListing {
        commands = commands == null ? null : List.copyOf(commands);
    }

    /** A plain item listing. */
    public ShopListing(String itemId, String itemNbt, BigDecimal buyPrice, BigDecimal sellPrice, int quantity) {
        this(itemId, itemNbt, buyPrice, sellPrice, quantity, null, null, false, null);
    }

    /** A new command listing with one command. */
    public static ShopListing command(String iconItemId, BigDecimal price, String command) {
        return new ShopListing(iconItemId, null, price, null, 1, null, List.of(command), false, null);
    }

    public boolean canBuy()  { return buyPrice  != null && buyPrice.compareTo(BigDecimal.ZERO)  >= 0; }
    public boolean canSell() { return !isCommandListing() && sellPrice != null && sellPrice.compareTo(BigDecimal.ZERO) >= 0; }

    public boolean isCommandListing() { return commands != null; }

    public ShopListing withDisplayName(String name) {
        return new ShopListing(itemId, itemNbt, buyPrice, sellPrice, quantity, name, commands, runAsPlayer, permission);
    }

    public ShopListing withPrices(BigDecimal buy, BigDecimal sell) {
        return new ShopListing(itemId, itemNbt, buy, isCommandListing() ? null : sell, quantity, displayName, commands, runAsPlayer, permission);
    }

    public ShopListing withAddedCommand(String command) {
        List<String> updated = new ArrayList<>(commands != null ? commands : List.of());
        updated.add(command);
        return new ShopListing(itemId, itemNbt, buyPrice, sellPrice, quantity, displayName, updated, runAsPlayer, permission);
    }

    public ShopListing withNoCommands() {
        return new ShopListing(itemId, itemNbt, buyPrice, sellPrice, quantity, displayName, List.of(), runAsPlayer, permission);
    }

    public ShopListing withRunAsPlayer(boolean asPlayer) {
        return new ShopListing(itemId, itemNbt, buyPrice, sellPrice, quantity, displayName, commands, asPlayer, permission);
    }

    public ShopListing withPermission(String node) {
        return new ShopListing(itemId, itemNbt, buyPrice, sellPrice, quantity, displayName, commands, runAsPlayer, node);
    }
}
