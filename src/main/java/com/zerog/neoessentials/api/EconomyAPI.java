package com.zerog.neoessentials.api;

import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import com.zerog.neoessentials.economy.managers.TransactionHistoryManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public API for other mods to interact with NeoEssentials Economy.
 * All methods are thread-safe and delegate to the internal managers.
 */
public class EconomyAPI {
    /**
     * Get a player's current balance.
     */
    public static BigDecimal getBalance(UUID player) {
        return EconomyManager.getInstance().getBalance(player);
    }

    /**
     * Set a player's balance. Respects negative balance config.
     */
    public static void setBalance(UUID player, BigDecimal amount) {
        EconomyManager.getInstance().setBalance(player, amount);
    }

    /**
     * Deposit money to a player's account. Returns true if successful.
     */
    public static boolean deposit(UUID player, BigDecimal amount) {
        return EconomyManager.getInstance().addBalance(player, amount);
    }

    /**
     * Withdraw money from a player's account. Returns true if successful.
     */
    public static boolean withdraw(UUID player, BigDecimal amount) {
        return EconomyManager.getInstance().subtractBalance(player, amount);
    }

    /**
     * Check if a player is accepting payments (paytoggle).
     */
    public static boolean isPayToggled(UUID player) {
        return PayToggleManager.getInstance().getPayToggle(player);
    }

    /**
     * Set a player's paytoggle state.
     */
    public static void setPayToggle(UUID player, boolean enabled) {
        PayToggleManager.getInstance().setPayToggle(player, enabled);
    }

    /**
     * Get a player's recent transaction history (as strings).
     */
    public static List<String> getTransactionHistory(UUID player) {
        return TransactionHistoryManager.getInstance().getHistory(player);
    }

    /**
     * Pay another player with a transaction fee applied. Returns true if successful.
     * The fee is removed from the system (no bank accounts).
     */
    public static boolean payPlayer(UUID sender, UUID receiver, BigDecimal amount) {
        if (sender.equals(receiver)) return false; // Prevent self-payment
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false; // No negative or zero payments
        EconomyManager manager = EconomyManager.getInstance();
        BigDecimal senderBalance = manager.getBalance(sender);
        if (senderBalance.compareTo(amount) < 0) return false; // Insufficient funds
        double taxPercent = manager.getConfig().taxPercentage;
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(taxPercent / 100.0));
        BigDecimal netAmount = amount.subtract(fee);
        if (netAmount.compareTo(BigDecimal.ZERO) < 0) return false; // Fee too high for amount
        manager.subtractBalance(sender, amount);
        manager.addBalance(receiver, netAmount);
        // Optionally, log the transaction or notify players here
        return true;
    }
}