package com.zerog.neoessentials.vault.impl;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.vault.api.VaultEconomy;
import net.neoforged.neoforge.common.NeoForge;
import com.zerog.neoessentials.api.event.EconomyDepositEvent;
import com.zerog.neoessentials.api.event.EconomyWithdrawEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

/**
 * NeoEssentials built-in {@link VaultEconomy} implementation.
 * Delegates all calls to {@link EconomyManager}.
 */
public class NeoEssentialsEconomy extends VaultEconomy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsEconomy.class);
    private final DecimalFormat fmt;

    public NeoEssentialsEconomy() {
        fmt = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
    }

    @Override public String getName() { return "NeoEssentials Economy"; }

    @Override
    public boolean isEnabled() {
        return ConfigManager.isEconomyEnabled() && EconomyManager.getInstance() != null;
    }

    @Override
    public String format(double amount) {
        String symbol = ConfigManager.getCurrencySymbol();
        return symbol + fmt.format(amount);
    }

    @Override public String currencyNameSingular() { return ConfigManager.getCurrencySymbol(); }
    @Override public String currencyNamePlural()   { return ConfigManager.getCurrencySymbol(); }
    @Override public int    fractionalDigits()      { return 2; }

    // ── Account ───────────────────────────────────────────────────────────────

    @Override
    public boolean hasAccount(UUID playerId) {
        try {
            return EconomyManager.getInstance().getAllBalances().containsKey(playerId);
        } catch (Exception e) {
            LOGGER.error("VaultEconomy: hasAccount error for {}: {}", playerId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean createPlayerAccount(UUID playerId) {
        try {
            if (hasAccount(playerId)) return true;
            BigDecimal start = BigDecimal.valueOf(ConfigManager.getEconomyStartingBalance());
            EconomyManager.getInstance().setBalance(playerId, start);
            return true;
        } catch (Exception e) {
            LOGGER.error("VaultEconomy: createPlayerAccount error for {}: {}", playerId, e.getMessage());
            return false;
        }
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    @Override
    public double getBalance(UUID playerId) {
        try {
            return EconomyManager.getInstance().getBalance(playerId).doubleValue();
        } catch (Exception e) {
            LOGGER.error("VaultEconomy: getBalance error for {}: {}", playerId, e.getMessage());
            return 0.0;
        }
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        return getBalance(playerId) >= amount;
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    @Override
    public EconomyResponse withdrawPlayer(UUID playerId, double amount) {
        if (amount < 0)
            return fail(amount, playerId, "Cannot withdraw a negative amount");
        try {
            BigDecimal amt = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
            boolean ok = EconomyManager.getInstance().subtractBalance(playerId, amt);
            if (!ok)
                return fail(amount, playerId, "Insufficient funds");
            NeoForge.EVENT_BUS.post(new EconomyWithdrawEvent(playerId, amount));
            return new EconomyResponse(amount, getBalance(playerId), EconomyResponse.ResponseType.SUCCESS, "");
        } catch (Exception e) {
            LOGGER.error("VaultEconomy: withdrawPlayer error for {}: {}", playerId, e.getMessage());
            return fail(amount, playerId, e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(UUID playerId, double amount) {
        if (amount < 0)
            return fail(amount, playerId, "Cannot deposit a negative amount");
        try {
            if (!hasAccount(playerId)) createPlayerAccount(playerId);
            BigDecimal amt = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
            boolean ok = EconomyManager.getInstance().addBalance(playerId, amt);
            if (!ok)
                return fail(amount, playerId, "Deposit rejected (max balance?)");
            NeoForge.EVENT_BUS.post(new EconomyDepositEvent(playerId, amount));
            return new EconomyResponse(amount, getBalance(playerId), EconomyResponse.ResponseType.SUCCESS, "");
        } catch (Exception e) {
            LOGGER.error("VaultEconomy: depositPlayer error for {}: {}", playerId, e.getMessage());
            return fail(amount, playerId, e.getMessage());
        }
    }

    @Override public boolean hasBankSupport() { return false; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EconomyResponse fail(double amount, UUID playerId, String msg) {
        return new EconomyResponse(amount, getBalance(playerId), EconomyResponse.ResponseType.FAILURE, msg);
    }
}
