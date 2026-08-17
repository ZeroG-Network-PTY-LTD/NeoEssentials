package com.zerog.neoessentials.vault.impl;

import com.zerog.neoessentials.shop.api.ShopEconomyAdapter;
import com.zerog.neoessentials.vault.api.VaultEconomy;
import com.zerog.neoessentials.vault.api.VaultServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * A {@link ShopEconomyAdapter} that dynamically delegates to the highest-priority
 * active {@link VaultEconomy} registered in {@link VaultServiceRegistry}.
 *
 * <h3>Purpose</h3>
 * This bridge adapter is registered with {@link com.zerog.neoessentials.shop.api.ShopEconomyRegistry}
 * so that any third-party NeoForge mod that registers a {@code VaultEconomy} at
 * {@link VaultServiceRegistry.ServicePriority#HIGH HIGH} or {@code HIGHEST} priority automatically
 * takes over as the shop's economy provider — no extra integration work required.
 *
 * <h3>Usage for third-party mods</h3>
 * <pre>{@code
 * // Register your economy provider at HIGH priority to override NeoEssentials
 * VaultServiceRegistry.getInstance().registerEconomy(
 *     myEconomy, ServicePriority.HIGH, "mymod");
 * // That's it — the shop system will now use your economy automatically.
 * }</pre>
 *
 * <p>If no third-party economy is registered, this adapter falls back to the standard
 * {@link com.zerog.neoessentials.shop.api.NeoEssentialsShopEconomy NeoEssentials built-in economy}.
 */
public class VaultShopEconomyAdapter implements ShopEconomyAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultShopEconomyAdapter.class);

    /** Fallback used when VaultServiceRegistry has no active economy. */
    private final ShopEconomyAdapter fallback;

    /**
     * @param fallback the adapter to delegate to when no VaultEconomy is active
     */
    public VaultShopEconomyAdapter(ShopEconomyAdapter fallback) {
        this.fallback = fallback;
    }

    @Override
    public String getProviderName() {
        return activeEconomy()
            .map(e -> "VaultBridge[" + e.getName() + "]")
            .orElseGet(fallback::getProviderName);
    }

    @Override
    public BigDecimal getBalance(UUID player) {
        Optional<VaultEconomy> eco = activeEconomy();
        if (eco.isPresent()) {
            return BigDecimal.valueOf(eco.get().getBalance(player));
        }
        return fallback.getBalance(player);
    }

    @Override
    public boolean hasBalance(UUID player, BigDecimal amount) {
        Optional<VaultEconomy> eco = activeEconomy();
        if (eco.isPresent()) {
            return eco.get().has(player, amount.doubleValue());
        }
        return fallback.hasBalance(player, amount);
    }

    @Override
    public boolean debit(UUID player, BigDecimal amount) {
        Optional<VaultEconomy> eco = activeEconomy();
        if (eco.isPresent()) {
            VaultEconomy.EconomyResponse resp = eco.get().withdrawPlayer(player, amount.doubleValue());
            if (!resp.transactionSuccess()) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "[VaultShopAdapter] debit failed for {}: {}", player, resp.errorMessage);
            }
            return resp.transactionSuccess();
        }
        return fallback.debit(player, amount);
    }

    @Override
    public boolean credit(UUID player, BigDecimal amount) {
        Optional<VaultEconomy> eco = activeEconomy();
        if (eco.isPresent()) {
            VaultEconomy.EconomyResponse resp = eco.get().depositPlayer(player, amount.doubleValue());
            if (!resp.transactionSuccess()) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "[VaultShopAdapter] credit failed for {}: {}", player, resp.errorMessage);
            }
            return resp.transactionSuccess();
        }
        return fallback.credit(player, amount);
    }

    @Override
    public String format(BigDecimal amount) {
        Optional<VaultEconomy> eco = activeEconomy();
        if (eco.isPresent()) {
            return eco.get().format(amount.doubleValue());
        }
        return fallback.format(amount);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Optional<VaultEconomy> activeEconomy() {
        return VaultServiceRegistry.getInstance().getEconomy();
    }
}

