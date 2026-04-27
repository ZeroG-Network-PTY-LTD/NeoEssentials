package com.zerog.neoessentials.shop.pricing;

import java.math.BigDecimal;

/**
 * A single pluggable pricing rule.
 *
 * <p>Rules are applied in order by {@link PricingEngine}. Each rule receives the
 * current (possibly already-adjusted) price and returns a new adjusted price.
 * Returning {@code null} means "skip — leave the price unchanged".
 */
@FunctionalInterface
public interface PriceRule {

    /**
     * Apply this rule to the given price.
     *
     * @param currentPrice the price after all previously applied rules
     * @param ctx          contextual information for the current transaction
     * @return adjusted price, or {@code null} to leave the price unchanged
     */
    BigDecimal apply(BigDecimal currentPrice, PriceContext ctx);
}

