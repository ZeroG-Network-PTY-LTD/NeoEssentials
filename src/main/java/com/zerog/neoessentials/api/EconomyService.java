package com.zerog.neoessentials.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for economy operations.
 */
public interface EconomyService {
    /**
     * Gets the balance for a player.
     * @param playerId Player UUID
     * @return Player's balance, or 0.0 if not found
     */
    double getBalance(UUID playerId);

    /**
     * Gets the balance for a player as an Optional.
     * @param playerId Player UUID
     * @return Optional containing the balance, or empty if not found
     */
    default Optional<Double> getBalanceOptional(UUID playerId) {
        double bal = getBalance(playerId);
        return bal == 0.0 ? Optional.empty() : Optional.of(bal);
    }

    /**
     * Deposits an amount to a player's balance.
     * @param playerId Player UUID
     * @param amount Amount to deposit (must be positive)
     * @return true if successful, false otherwise
     */
    boolean deposit(UUID playerId, double amount);

    /**
     * Withdraws an amount from a player's balance.
     * @param playerId Player UUID
     * @param amount Amount to withdraw (must be positive and less than or equal to balance)
     * @return true if successful, false otherwise
     */
    boolean withdraw(UUID playerId, double amount);

    /**
     * Sets a player's balance to a specific value.
     * @param playerId Player UUID
     * @param amount New balance (must be >= 0)
     * @return true if successful, false otherwise
     */
    boolean setBalance(UUID playerId, double amount);

    /**
     * Resets a player's balance to zero.
     * @param playerId Player UUID
     * @return true if successful, false otherwise
     */
    boolean resetBalance(UUID playerId);
}
