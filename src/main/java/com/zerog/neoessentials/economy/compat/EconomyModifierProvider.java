package com.zerog.neoessentials.economy.compat;
import java.math.BigDecimal;
import java.util.UUID;
/**
 * SPI: provides per-player economy modifier values.
 *
 * All methods have safe default implementations that signal "no override" so
 * partial implementations are easy to write.
 *
 * Register with EconomyModifierManager#register() to activate.
 */
public interface EconomyModifierProvider {
    String getName();
    default double getSellMultiplier(UUID player) { return 0.0; }
    default boolean isTaxExempt(UUID player) { return false; }
    default double getTaxRate(UUID player) { return -1.0; }
    default BigDecimal getMaxBalance(UUID player) { return null; }
    default BigDecimal getPayLimit(UUID player) { return null; }
    default boolean hasNoPayCooldown(UUID player) { return false; }
}
