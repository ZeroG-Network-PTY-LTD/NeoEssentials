package com.zerog.neoessentials.shop.pricing.rules;

import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import com.zerog.neoessentials.shop.pricing.PriceContext;
import com.zerog.neoessentials.shop.pricing.PriceRule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Adjusts price based on remaining stock in the linked chest.
 *
 * <p>When stock is low the buy price rises (and sell price falls) according to
 * a configurable multiplier range.  When stock is high the opposite applies.
 *
 * <p>Config keys (under {@code shop.pricing.supplyDemand}):
 * <ul>
 *   <li>{@code maxStock}       — stock level considered "full" (default 500)</li>
 *   <li>{@code minMultiplier}  — applied when stock is at max (default 0.8 = 20% cheaper)</li>
 *   <li>{@code maxMultiplier}  — applied when stock is 0 (default 2.0 = 2× more expensive)</li>
 * </ul>
 */
public class SupplyDemandRule implements PriceRule {

    private final int    maxStock;
    private final double minMultiplier;
    private final double maxMultiplier;

    public SupplyDemandRule(int maxStock, double minMultiplier, double maxMultiplier) {
        this.maxStock       = Math.max(1, maxStock);
        this.minMultiplier  = minMultiplier;
        this.maxMultiplier  = maxMultiplier;
    }

    /** Default configuration. */
    public SupplyDemandRule() {
        this(500, 0.8, 2.0);
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, PriceContext ctx) {
        // Only applies to player sign shops with a chest
        if (ctx.shop().isAdminShop() || !ctx.shop().hasChest) return null;

        int stock = getStock(ctx);
        if (stock < 0) return null; // chest not found — skip

        // Ratio 0.0 = empty, 1.0 = full
        double ratio = Math.min(1.0, (double) stock / maxStock);

        double multiplier;
        if (ctx.transactionType() == ShopTransactionEvent.Type.BUY) {
            // Low stock → higher buy price (price ↑ when ratio ↓)
            multiplier = maxMultiplier - (maxMultiplier - minMultiplier) * ratio;
        } else {
            // Low stock → lower sell price (shop can't accept much)
            multiplier = minMultiplier + (maxMultiplier - minMultiplier) * ratio;
        }

        multiplier = Math.max(0.01, multiplier); // never negative
        return currentPrice.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
    }

    private int getStock(PriceContext ctx) {
        if (ctx.level() == null) return -1;
        BlockPos chestPos = ctx.shop().getChestPos();
        if (chestPos == null) return -1;
        // For a double chest, getContainerAt returns the combined 54-slot container covering
        // both halves (same fix as ShopTransaction.getChest) — reading only chestPos's own
        // ChestBlockEntity would price a shop as if half its actual stock didn't exist.
        Container chest = HopperBlockEntity.getContainerAt(ctx.level(), chestPos);
        if (chest == null) return -1;

        String itemId = ctx.shop().itemId;
        if (itemId == null) return -1;
        var template = ShopParser.resolveItem(itemId);
        if (template.isEmpty()) return -1;

        int total = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            var slot = chest.getItem(i);
            if (!slot.isEmpty() && net.minecraft.world.item.ItemStack.isSameItem(slot, template)) {
                total += slot.getCount();
            }
        }
        return total;
    }
}

