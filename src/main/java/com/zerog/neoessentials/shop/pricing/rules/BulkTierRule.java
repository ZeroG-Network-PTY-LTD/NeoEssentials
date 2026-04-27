package com.zerog.neoessentials.shop.pricing.rules;

import com.zerog.neoessentials.shop.pricing.PriceContext;
import com.zerog.neoessentials.shop.pricing.PriceRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies a discount when the transaction quantity meets or exceeds a tier threshold.
 *
 * <p>Tiers are matched by finding the highest {@code minQuantity} that is still
 * {@code <=} the requested quantity.  The deepest matching tier wins.
 *
 * <p>Config key: {@code shop.pricing.bulkTiers} — JSON array of
 * {@code { "minQuantity": 10, "discountPercent": 5 }} objects.
 *
 * <p>Example:
 * <pre>
 *   minQuantity=10 → 5% off
 *   minQuantity=32 → 10% off
 *   minQuantity=64 → 15% off
 * </pre>
 */
public class BulkTierRule implements PriceRule {

    public record Tier(int minQuantity, double discountPercent) {}

    private final List<Tier> tiers;

    public BulkTierRule(List<Tier> tiers) {
        // Sort ascending so we iterate cheaply
        this.tiers = new ArrayList<>(tiers);
        this.tiers.sort(Comparator.comparingInt(Tier::minQuantity));
    }

    public BulkTierRule() {
        this(List.of(
                new Tier(10,  5.0),
                new Tier(32, 10.0),
                new Tier(64, 15.0)
        ));
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, PriceContext ctx) {
        int qty = ctx.requestedQuantity();
        Tier best = null;
        for (Tier t : tiers) {
            if (qty >= t.minQuantity()) best = t;
        }
        if (best == null) return null; // no tier matched

        double multiplier = 1.0 - (best.discountPercent() / 100.0);
        multiplier = Math.max(0.0, Math.min(1.0, multiplier));
        return currentPrice.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
    }
}

