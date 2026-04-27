package com.zerog.neoessentials.shop.entity;

import java.math.BigDecimal;

/**
 * One item entry in an NPC shop's inventory.
 *
 * @param itemId       registry ID, e.g. {@code "minecraft:diamond"}
 * @param buyPrice     price customer pays to buy; null = buy disabled
 * @param sellPrice    price customer receives to sell; null = sell disabled
 * @param quantity     units per transaction
 */
public record ShopListing(
        String itemId,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        int quantity
) {
    public boolean canBuy()  { return buyPrice  != null && buyPrice.compareTo(BigDecimal.ZERO)  >= 0; }
    public boolean canSell() { return sellPrice != null && sellPrice.compareTo(BigDecimal.ZERO) >= 0; }
}

