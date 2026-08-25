package com.zerog.neoessentials.api;

import com.zerog.neoessentials.config.ConfigManager;
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
     * This operation is ATOMIC to prevent double-spending exploits.
     */
    public static boolean payPlayer(UUID sender, UUID receiver, BigDecimal amount) {
        return payPlayer(sender, receiver, amount, ConfigManager.getTaxPercentage());
    }

    /**
     * Pay another player with a custom tax rate (0.0–100.0).
     * Use this overload to apply per-player tax-exempt/modifier logic before calling.
     * The fee is removed from the system (burned).
     * This operation is ATOMIC to prevent double-spending exploits.
     */
    public static boolean payPlayer(UUID sender, UUID receiver, BigDecimal amount, double taxRate) {
        if (sender.equals(receiver)) return false; // Prevent self-payment
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false; // No negative or zero payments

        EconomyManager manager = EconomyManager.getInstance();

        // Calculate tax upfront using the provided (per-player) tax rate
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(taxRate / 100.0));
        BigDecimal netAmount = amount.subtract(fee);
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) return false; // Fee too high for amount

        // ATOMIC: subtract from sender (fails if insufficient funds)
        boolean senderSuccess = manager.subtractBalance(sender, amount);
        if (!senderSuccess) {
            return false; // Insufficient funds or negative balance not allowed
        }

        // Add to receiver
        boolean receiverSuccess = manager.addBalance(receiver, netAmount);
        if (!receiverSuccess) {
            // Extremely unlikely — refund the sender. addBalance() can itself be rejected by
            // the same cancellable EconomyDepositEvent, so a plain retry here could silently
            // lose the sender's money (already subtracted above) without it reaching either
            // party. Fall back to a guaranteed, non-cancellable settlement on rejection,
            // matching AuctionHouseManager/ShopTransaction's existing fix for the same
            // failure mode.
            boolean refunded = manager.addBalance(sender, amount);
            if (!refunded) {
                manager.setBalance(sender, manager.getBalance(sender).add(amount));
            }
            return false;
        }

        return true;
    }
}