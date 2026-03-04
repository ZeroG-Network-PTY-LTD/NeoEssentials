package com.zerog.neoessentials.vault.api;

import java.util.List;
import java.util.UUID;

/**
 * NeoEssentials Vault Economy API — NeoForge equivalent of Vault's Economy interface.
 * <p>
 * Provides a standardised economy interface that other NeoForge mods can code against
 * without depending on a specific economy implementation.
 * <p>
 * Register an implementation via {@link VaultServiceRegistry#registerEconomy}.
 * Retrieve the active implementation via {@link VaultServiceRegistry#getEconomy()}.
 */
public abstract class VaultEconomy {

    // ── Identity ─────────────────────────────────────────────────────────────

    /** Human-readable name of this economy implementation, e.g. "NeoEssentials Economy". */
    public abstract String getName();

    /** Whether this economy implementation is currently enabled and usable. */
    public abstract boolean isEnabled();

    // ── Formatting ────────────────────────────────────────────────────────────

    /**
     * Format an amount to a human-readable currency string.
     * e.g. {@code format(1000.5)} → {@code "$1,000.50"}
     */
    public abstract String format(double amount);

    /** The singular name of the currency (e.g. "Dollar"). */
    public abstract String currencyNameSingular();

    /** The plural name of the currency (e.g. "Dollars"). */
    public abstract String currencyNamePlural();

    /** How many decimal places this economy uses. -1 = unlimited. */
    public int fractionalDigits() {
        return 2;
    }

    // ── Account existence ─────────────────────────────────────────────────────

    /** Returns true if the given player UUID has an account. */
    public abstract boolean hasAccount(UUID playerId);

    /** Returns true if the given player name has an account. */
    public boolean hasAccount(String playerName) {
        return false; // override if name lookup is supported
    }

    /**
     * Create a new account for the given player UUID.
     * @return true if account was created or already exists
     */
    public abstract boolean createPlayerAccount(UUID playerId);

    /** Create account by name. Default delegates to UUID if possible. */
    public boolean createPlayerAccount(String playerName) {
        return false;
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    /** Get the balance of a player's account. */
    public abstract double getBalance(UUID playerId);

    /** Get the balance by name. Default returns 0. */
    public double getBalance(String playerName) {
        return 0;
    }

    /** Returns true if {@code playerId} has at least {@code amount}. */
    public boolean has(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    /**
     * Withdraw {@code amount} from a player's account.
     * @return result of the transaction
     */
    public abstract EconomyResponse withdrawPlayer(UUID playerId, double amount);

    /**
     * Deposit {@code amount} into a player's account.
     * @return result of the transaction
     */
    public abstract EconomyResponse depositPlayer(UUID playerId, double amount);

    // ── Bank (optional) ───────────────────────────────────────────────────────

    /** Returns true if this economy supports bank accounts. Default false. */
    public boolean hasBankSupport() {
        return false;
    }

    public EconomyResponse createBank(String name, UUID ownerId) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse isBankOwner(String name, UUID playerId) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public EconomyResponse isBankMember(String name, UUID playerId) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    public List<String> getBanks() {
        return List.of();
    }

    // ── Response class ────────────────────────────────────────────────────────

    /**
     * Result of an economy transaction.
     */
    public static class EconomyResponse {
        public enum ResponseType {
            SUCCESS,
            FAILURE,
            NOT_IMPLEMENTED
        }

        /** Amount involved in the transaction. */
        public final double amount;
        /** Player's balance after the transaction. */
        public final double balance;
        /** Whether the transaction succeeded. */
        public final ResponseType type;
        /** Human-readable error message, or empty string on success. */
        public final String errorMessage;

        public EconomyResponse(double amount, double balance, ResponseType type, String errorMessage) {
            this.amount = amount;
            this.balance = balance;
            this.type = type;
            this.errorMessage = errorMessage != null ? errorMessage : "";
        }

        public boolean transactionSuccess() {
            return type == ResponseType.SUCCESS;
        }
    }
}

