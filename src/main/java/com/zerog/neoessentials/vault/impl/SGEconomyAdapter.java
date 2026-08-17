package com.zerog.neoessentials.vault.impl;

import com.zerog.neoessentials.vault.api.VaultEconomy;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.sirgrantd.sg_economy.api.EconomyEventProvider;
import net.sirgrantd.sg_economy.api.SGEconomyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.text.DecimalFormat;
import java.util.UUID;

/**
 * {@link VaultEconomy} bridge for SG Economy API (curseforge.com/minecraft/mc-mods/sg-economy-api),
 * a completely independent economy mod with no knowledge of NeoEssentials. This is a one-directional
 * adapter WE wrote — the same shape as {@code LuckPermsAdapter} for permissions — since SG Economy API
 * has no NeoEssentials-specific integration of its own and nothing generic exists for two unrelated
 * economy mods to auto-discover each other.
 *
 * <p><b>Important limitation, inherent to SG Economy API's own public API, not this adapter:</b> its
 * {@link EconomyEventProvider} only accepts a live {@code Entity}, not a UUID — balances are stored as
 * a NeoForge capability attached to the player entity. This means SG Economy API's balance data can
 * only be read/written while the player is online; there is no offline-balance lookup path exposed by
 * its API at all. Offline calls here return a conservative failure/zero rather than guessing.
 *
 * <p>Registered by {@code VaultManager} at {@link com.zerog.neoessentials.vault.api.VaultServiceRegistry.ServicePriority#HIGH}
 * when {@code economy.useExternalEconomy} is enabled and SG Economy API is detected loaded, so it
 * takes over from NeoEssentials' own {@link NeoEssentialsEconomy} (registered at NORMAL) without any
 * other code needing to change — see the priority-based override design already documented on
 * {@code VaultManager}.
 */
public class SGEconomyAdapter extends VaultEconomy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SGEconomyAdapter.class);
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");

    private EconomyEventProvider provider() {
        return SGEconomyApi.get();
    }

    private ServerPlayer onlinePlayer(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayer(playerId) : null;
    }

    @Override
    public String getName() {
        return "SG Economy API";
    }

    @Override
    public boolean isEnabled() {
        try {
            return provider() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String format(double amount) {
        return fmt.format(amount) + (amount == 1.0 ? " " + currencyNameSingular() : " " + currencyNamePlural());
    }

    @Override
    public String currencyNameSingular() {
        return "Coin";
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public int fractionalDigits() {
        try {
            return provider().isDecimalSystem() ? 2 : 0;
        } catch (Exception e) {
            return 2;
        }
    }

    @Override
    public boolean hasAccount(UUID playerId) {
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) return false; // can't check an offline player's capability data
        try {
            return provider().hasCoinsBag(player);
        } catch (Exception e) {
            LOGGER.error("SGEconomyAdapter: hasAccount error for {}: {}", playerId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean createPlayerAccount(UUID playerId) {
        // The coins-bag capability auto-attaches to every player entity on join; there's nothing
        // to explicitly create. Report success if the player is online (capability will exist),
        // matching what hasAccount() would then report.
        return onlinePlayer(playerId) != null;
    }

    @Override
    public double getBalance(UUID playerId) {
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "SGEconomyAdapter: getBalance requested for offline player {} — SG Economy API has no offline lookup, returning 0", playerId);
            return 0.0;
        }
        try {
            return provider().getBalance(player);
        } catch (Exception e) {
            LOGGER.error("SGEconomyAdapter: getBalance error for {}: {}", playerId, e.getMessage());
            return 0.0;
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(UUID playerId, double amount) {
        if (amount < 0) return fail(amount, "Cannot withdraw a negative amount");
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) return fail(amount, "Player must be online — SG Economy API has no offline account access");
        try {
            boolean ok = provider().withdrawBalance(player, amount);
            if (!ok) return fail(amount, "Insufficient funds");
            return new EconomyResponse(amount, provider().getBalance(player), EconomyResponse.ResponseType.SUCCESS, "");
        } catch (Exception e) {
            LOGGER.error("SGEconomyAdapter: withdrawPlayer error for {}: {}", playerId, e.getMessage());
            return fail(amount, e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(UUID playerId, double amount) {
        if (amount < 0) return fail(amount, "Cannot deposit a negative amount");
        ServerPlayer player = onlinePlayer(playerId);
        if (player == null) return fail(amount, "Player must be online — SG Economy API has no offline account access");
        try {
            boolean ok = provider().depositBalance(player, amount);
            if (!ok) return fail(amount, "Deposit rejected");
            return new EconomyResponse(amount, provider().getBalance(player), EconomyResponse.ResponseType.SUCCESS, "");
        } catch (Exception e) {
            LOGGER.error("SGEconomyAdapter: depositPlayer error for {}: {}", playerId, e.getMessage());
            return fail(amount, e.getMessage());
        }
    }

    private EconomyResponse fail(double amount, String msg) {
        return new EconomyResponse(amount, 0.0, EconomyResponse.ResponseType.FAILURE, msg);
    }
}
