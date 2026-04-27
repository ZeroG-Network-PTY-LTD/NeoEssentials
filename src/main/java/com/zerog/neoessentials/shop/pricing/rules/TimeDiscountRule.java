package com.zerog.neoessentials.shop.pricing.rules;

import com.zerog.neoessentials.shop.pricing.PriceContext;
import com.zerog.neoessentials.shop.pricing.PriceRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Applies a percentage discount during a configured daily time window.
 *
 * <p>Config keys (under {@code shop.pricing.timeDiscount}):
 * <ul>
 *   <li>{@code startHour}       — hour (0–23) the discount begins (default 12)</li>
 *   <li>{@code endHour}         — hour (0–23) the discount ends (default 14)</li>
 *   <li>{@code discountPercent} — percent to subtract (default 10 = 10% off)</li>
 * </ul>
 *
 * <p>The check uses the JVM's system time-zone. For cross-server consistency,
 * set the server's JVM timezone with {@code -Duser.timezone=UTC}.
 */
public class TimeDiscountRule implements PriceRule {

    private final int    startHour;
    private final int    endHour;
    private final double discountPercent;

    public TimeDiscountRule(int startHour, int endHour, double discountPercent) {
        this.startHour      = startHour;
        this.endHour        = endHour;
        this.discountPercent = discountPercent;
    }

    /** Default: 10% off between 12:00 and 14:00 server time. */
    public TimeDiscountRule() {
        this(12, 14, 10.0);
    }

    @Override
    public BigDecimal apply(BigDecimal currentPrice, PriceContext ctx) {
        ZonedDateTime now = Instant.ofEpochMilli(ctx.serverTimeMs())
                .atZone(ZoneId.systemDefault());
        int hour = now.getHour();

        boolean inWindow = startHour <= endHour
                ? (hour >= startHour && hour < endHour)               // same-day window
                : (hour >= startHour || hour < endHour);              // overnight window

        if (!inWindow) return null;

        double multiplier = 1.0 - (discountPercent / 100.0);
        multiplier = Math.max(0.0, Math.min(1.0, multiplier));
        return currentPrice.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP);
    }
}

