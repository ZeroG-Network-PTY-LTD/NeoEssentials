package com.zerog.neoessentials.economy.compat;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.UUID;
/**
 * LuckPerms economy modifiers.
 *
 * Reads user/group meta from LuckPerms to provide per-player economy modifiers.
 *
 * Meta keys (set with /lp user <name> meta set <key> <value>
 *                   or /lp group <group> meta set <key> <value>):
 *
 *   neoessentials.sell-multiplier   e.g. "1.5"  -> 1.5x sell multiplier
 *   neoessentials.tax-rate          e.g. "0"    -> 0% tax (implies tax-exempt)
 *   neoessentials.max-balance       e.g. "5000000" -> 5,000,000 max balance
 *   neoessentials.pay-limit         e.g. "100000"  -> $100k max per /pay
 *   neoessentials.no-pay-cooldown   "true"          -> bypass /pay cooldown
 *   neoessentials.tax-exempt        "true"          -> fully tax-exempt
 */
public class LuckPermsEconomyModifiers implements EconomyModifierProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsEconomyModifiers.class);
    private static final String META_SELL_MULTIPLIER  = "neoessentials.sell-multiplier";
    private static final String META_TAX_RATE         = "neoessentials.tax-rate";
    private static final String META_MAX_BALANCE      = "neoessentials.max-balance";
    private static final String META_PAY_LIMIT        = "neoessentials.pay-limit";
    private static final String META_NO_PAY_COOLDOWN  = "neoessentials.no-pay-cooldown";
    private static final String META_TAX_EXEMPT       = "neoessentials.tax-exempt";
    private final boolean available;
    public LuckPermsEconomyModifiers() {
        this.available = ModList.get().isLoaded("luckperms");
        if (available) {
            LOGGER.info("[Economy] LuckPerms economy meta integration active.");
        }
    }
    @Override
    public String getName() { return "LuckPermsMeta"; }
    @Override
    public double getSellMultiplier(UUID player) {
        String val = getMeta(player, META_SELL_MULTIPLIER);
        if (val == null) return 0.0;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                "getSellMultiplier: invalid meta value '" + val + "' for " + META_SELL_MULTIPLIER + ", player=" + player, e);
            return 0.0;
        }
    }
    @Override
    public boolean isTaxExempt(UUID player) {
        String val = getMeta(player, META_TAX_EXEMPT);
        if ("true".equalsIgnoreCase(val)) return true;
        String rateStr = getMeta(player, META_TAX_RATE);
        if (rateStr != null) {
            try { return Double.parseDouble(rateStr) <= 0.0; } catch (NumberFormatException e) {
                NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                    "isTaxExempt: invalid meta value '" + rateStr + "' for " + META_TAX_RATE + ", player=" + player, e);
            }
        }
        return false;
    }
    @Override
    public double getTaxRate(UUID player) {
        String val = getMeta(player, META_TAX_RATE);
        if (val == null) return -1.0;
        try {
            double v = Double.parseDouble(val);
            return (v >= 0 && v <= 100) ? v : -1.0;
        } catch (NumberFormatException e) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                "getTaxRate: invalid meta value '" + val + "' for " + META_TAX_RATE + ", player=" + player, e);
            return -1.0;
        }
    }
    @Override
    public BigDecimal getMaxBalance(UUID player) {
        String val = getMeta(player, META_MAX_BALANCE);
        if (val == null) return null;
        try { return new BigDecimal(val); } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                "getMaxBalance: invalid meta value '" + val + "' for " + META_MAX_BALANCE + ", player=" + player, e);
            return null;
        }
    }
    @Override
    public BigDecimal getPayLimit(UUID player) {
        String val = getMeta(player, META_PAY_LIMIT);
        if (val == null) return null;
        try { return new BigDecimal(val); } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY,
                "getPayLimit: invalid meta value '" + val + "' for " + META_PAY_LIMIT + ", player=" + player, e);
            return null;
        }
    }
    @Override
    public boolean hasNoPayCooldown(UUID player) {
        return "true".equalsIgnoreCase(getMeta(player, META_NO_PAY_COOLDOWN));
    }
    // ── LuckPerms meta lookup ─────────────────────────────────────────────────
    private String getMeta(UUID player, String key) {
        if (!available) return null;
        try {
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = api.getUserManager().getUser(player);
            if (user == null) return null;
            net.luckperms.api.cacheddata.CachedMetaData meta =
                user.getCachedData().getMetaData(net.luckperms.api.query.QueryOptions.defaultContextualOptions());
            return meta.getMetaValue(key);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.ECONOMY, "[LuckPermsEconomy] Error reading meta '{}' for {}: {}", key, player, e.getMessage());
            return null;
        }
    }
}
