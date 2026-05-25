package com.zerog.neoessentials.economy.compat;

import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central manager for per-player economy modifiers.
 *
 * <p>Aggregates modifier values from all registered {@link EconomyModifierProvider}
 * implementations. The highest sell multiplier wins; the most restrictive pay-limit applies;
 * tax-exempt overrides the global rate; and the highest max-balance override is used.
 *
 * <h3>Built-in providers (registered automatically on startup):</h3>
 * <ul>
 *   <li>{@link PermissionBasedModifiers} — permission-node modifiers compatible with FTB Ranks,
 *       LuckPerms, NeoEssentials internal permissions, and any other backend.</li>
 *   <li>{@link LuckPermsEconomyModifiers} — reads LuckPerms user/group meta values when
 *       LuckPerms is present on the server.</li>
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * EconomyModifierManager mods = EconomyModifierManager.getInstance();
 * double multiplier = mods.getSellMultiplier(player.getUUID());
 * boolean taxFree   = mods.isTaxExempt(player.getUUID());
 * }</pre>
 */
public class EconomyModifierManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EconomyModifierManager.class);

    // Bill Pugh singleton — thread-safe without synchronization
    private static class Holder {
        static final EconomyModifierManager INSTANCE = new EconomyModifierManager();
    }

    public static EconomyModifierManager getInstance() {
        return Holder.INSTANCE;
    }

    private final List<EconomyModifierProvider> providers = new ArrayList<>();

    private EconomyModifierManager() {
        // Register built-in providers
        register(new PermissionBasedModifiers());
        register(new LuckPermsEconomyModifiers());
    }

    /**
     * Register a custom modifier provider. Call this during mod initialisation.
     *
     * @param provider the provider to add (skip if null)
     */
    public void register(EconomyModifierProvider provider) {
        if (provider == null) return;
        providers.add(provider);
        LOGGER.debug("[EconomyModifiers] Registered provider: {}", provider.getName());
    }

    // ── Sell multiplier ───────────────────────────────────────────────────────

    /**
     * Return the effective sell multiplier for this player.
     * The highest value among all providers wins; falls back to the global config value.
     */
    public double getSellMultiplier(UUID player) {
        double base = getGlobalSellMultiplier();
        double best = base;
        for (EconomyModifierProvider p : providers) {
            try {
                double v = p.getSellMultiplier(player);
                if (v > best) best = v;
            } catch (Exception ignored) {}
        }
        return best;
    }

    /** Global sell multiplier from economy config. */
    private double getGlobalSellMultiplier() {
        try {
            com.google.gson.JsonObject cfg = ConfigManager.getInstance()
                .getConfig(ConfigManager.MAIN_CONFIG);
            if (cfg.has("economy")) {
                com.google.gson.JsonObject eco = cfg.getAsJsonObject("economy");
                if (eco.has("sellMultiplier")) return eco.get("sellMultiplier").getAsDouble();
            }
        } catch (Exception ignored) {}
        return 1.0;
    }

    // ── Tax exemption ─────────────────────────────────────────────────────────

    /**
     * Return true if the player is exempt from transaction tax (i.e., tax rate = 0).
     */
    public boolean isTaxExempt(UUID player) {
        for (EconomyModifierProvider p : providers) {
            try {
                if (p.isTaxExempt(player)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Return the effective tax rate for this player (0.0 – 100.0).
     * Respects tax exemption before returning the global config rate.
     */
    public double getEffectiveTaxRate(UUID player) {
        if (isTaxExempt(player)) return 0.0;

        // Let providers override the per-player rate; lowest overriding rate wins
        double global = ConfigManager.getTaxPercentage();
        double best = global;
        boolean overridden = false;
        for (EconomyModifierProvider p : providers) {
            try {
                double v = p.getTaxRate(player);
                if (v >= 0 && (!overridden || v < best)) {
                    best = v;
                    overridden = true;
                }
            } catch (Exception ignored) {}
        }
        return best;
    }

    // ── Max balance ───────────────────────────────────────────────────────────

    /**
     * Return the per-player max balance (e.g. from LuckPerms meta or a permission tier).
     * Returns the global config max if no provider overrides it.
     */
    public BigDecimal getMaxBalance(UUID player) {
        BigDecimal global = BigDecimal.valueOf(ConfigManager.getMaxBalance());
        BigDecimal best = global;
        for (EconomyModifierProvider p : providers) {
            try {
                BigDecimal v = p.getMaxBalance(player);
                if (v != null && v.compareTo(best) > 0) best = v;
            } catch (Exception ignored) {}
        }
        return best;
    }

    // ── Pay limit per transaction ─────────────────────────────────────────────

    /**
     * Return the maximum amount a player can send in a single /pay transaction.
     * Returns the global config max transfer amount if no provider overrides it.
     */
    public BigDecimal getPayLimit(UUID player) {
        BigDecimal global = BigDecimal.valueOf(ConfigManager.getMaxTransferAmount());
        BigDecimal best = global;
        for (EconomyModifierProvider p : providers) {
            try {
                BigDecimal v = p.getPayLimit(player);
                // Higher limit wins (providers can expand the limit)
                if (v != null && v.compareTo(best) > 0) best = v;
            } catch (Exception ignored) {}
        }
        return best;
    }

    // ── Pay cooldown ──────────────────────────────────────────────────────────

    /**
     * Whether this player has no pay cooldown (e.g. via a bypass permission).
     */
    public boolean hasNoPayCooldown(UUID player) {
        for (EconomyModifierProvider p : providers) {
            try {
                if (p.hasNoPayCooldown(player)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}

