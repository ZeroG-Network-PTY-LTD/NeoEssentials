package com.zerog.neoessentials.shop.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shop economy abstraction layer.
 *
 * <p>Allows the shop system to be decoupled from {@code EconomyManager} directly,
 * enabling future integration with external economy systems (Vault, etc.) simply
 * by registering a higher-priority adapter via {@link ShopEconomyRegistry}.
 */
public interface ShopEconomyAdapter {

    /** @return human-readable provider name for logging / dashboard display. */
    String getProviderName();

    /** @return current balance for the given player. */
    BigDecimal getBalance(UUID player);

    /** @return true if the player has at least {@code amount} in their account. */
    boolean hasBalance(UUID player, BigDecimal amount);

    /**
     * Deduct {@code amount} from the player's balance.
     *
     * @return true on success, false if insufficient funds or provider error.
     */
    boolean debit(UUID player, BigDecimal amount);

    /**
     * Credit {@code amount} to the player's balance.
     *
     * @return true on success, false on provider error.
     */
    boolean credit(UUID player, BigDecimal amount);

    /**
     * Format an amount using this provider's currency symbol/format.
     *
     * @param amount amount to format
     * @return formatted string, e.g. "$10.50"
     */
    String format(BigDecimal amount);
}

