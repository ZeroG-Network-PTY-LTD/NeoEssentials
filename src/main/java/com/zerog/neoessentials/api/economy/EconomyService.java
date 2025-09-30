package com.zerog.neoessentials.api.economy;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for economy operations.
 */
public interface EconomyService {
    double getBalance(UUID playerId);
    default Optional<Double> getBalanceOptional(UUID playerId) {
        double bal = getBalance(playerId);
        return bal == 0.0 ? Optional.empty() : Optional.of(bal);
    }
    boolean deposit(UUID playerId, double amount);
    boolean withdraw(UUID playerId, double amount);
    boolean setBalance(UUID playerId, double amount);
    boolean resetBalance(UUID playerId);
    boolean hasAccount(UUID playerId);
    boolean createAccount(UUID playerId);
    boolean deleteAccount(UUID playerId);
    String format(double amount);
    String getCurrencySymbol();
}
