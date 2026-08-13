package com.zerog.neoessentials.shop.pricing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.shop.pricing.rules.BulkTierRule;
import com.zerog.neoessentials.shop.pricing.rules.SupplyDemandRule;
import com.zerog.neoessentials.shop.pricing.rules.TimeDiscountRule;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the active {@link PriceRule} chain for buy and sell price computation.
 *
 * <p>When {@code shop.pricing.enabled = false} (default), the engine returns the raw
 * prices from the {@link ShopData} unchanged — no performance overhead.
 *
 * <p>Rules are applied sequentially; the output of each rule feeds into the next.
 * A rule returning {@code null} is skipped.
 */
public class PricingEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingEngine.class);

    private static final PricingEngine INSTANCE = new PricingEngine();
    public static PricingEngine getInstance() { return INSTANCE; }

    private volatile boolean enabled = false;
    private final List<PriceRule> rules = new ArrayList<>();

    private PricingEngine() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Load / reload config and rebuild the rule list. */
    public void loadConfig() {
        rules.clear();
        try {
            JsonObject shopCfg = getShopPricingConfig();
            if (shopCfg == null) { enabled = false; return; }

            enabled = shopCfg.has("enabled") && shopCfg.get("enabled").getAsBoolean();
            if (!enabled) return;

            // Supply/demand rule
            if (shopCfg.has("supplyDemand")) {
                JsonObject sd = shopCfg.getAsJsonObject("supplyDemand");
                boolean sdEnabled = !sd.has("enabled") || sd.get("enabled").getAsBoolean();
                if (sdEnabled) {
                    int    maxStock      = sd.has("maxStock")      ? sd.get("maxStock").getAsInt()           : 500;
                    double minMultiplier = sd.has("minMultiplier") ? sd.get("minMultiplier").getAsDouble()   : 0.8;
                    double maxMultiplier = sd.has("maxMultiplier") ? sd.get("maxMultiplier").getAsDouble()   : 2.0;
                    rules.add(new SupplyDemandRule(maxStock, minMultiplier, maxMultiplier));
                }
            }

            // Time discount rule
            if (shopCfg.has("timeDiscount")) {
                JsonObject td = shopCfg.getAsJsonObject("timeDiscount");
                boolean tdEnabled = !td.has("enabled") || td.get("enabled").getAsBoolean();
                if (tdEnabled) {
                    int    startHour       = td.has("startHour")       ? td.get("startHour").getAsInt()           : 12;
                    int    endHour         = td.has("endHour")         ? td.get("endHour").getAsInt()             : 14;
                    double discountPercent = td.has("discountPercent") ? td.get("discountPercent").getAsDouble()  : 10.0;
                    rules.add(new TimeDiscountRule(startHour, endHour, discountPercent));
                }
            }

            // Bulk tier rule
            if (shopCfg.has("bulkTiers")) {
                JsonArray arr = shopCfg.getAsJsonArray("bulkTiers");
                List<BulkTierRule.Tier> tiers = new ArrayList<>();
                for (JsonElement el : arr) {
                    JsonObject t = el.getAsJsonObject();
                    tiers.add(new BulkTierRule.Tier(
                            t.get("minQuantity").getAsInt(),
                            t.get("discountPercent").getAsDouble()));
                }
                if (!tiers.isEmpty()) rules.add(new BulkTierRule(tiers));
            }

            LOGGER.info("[ChestShop] PricingEngine: enabled with {} rule(s)", rules.size());
        } catch (Exception e) {
            LOGGER.error("[ChestShop] PricingEngine config load error — dynamic pricing disabled", e);
            enabled = false;
            rules.clear();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Compute the effective buy price for a transaction.
     *
     * @return adjusted buy price, or the raw {@code shop.buyPrice} if pricing is disabled.
     */
    public BigDecimal computeBuyPrice(ShopData shop, UUID actorUUID, int quantity, ServerLevel level) {
        if (!enabled || shop.buyPrice == null) return shop.buyPrice;
        PriceContext ctx = new PriceContext(shop, actorUUID, quantity,
                ShopTransactionEvent.Type.BUY, level);
        return applyRules(shop.buyPrice, ctx);
    }

    /**
     * Compute the effective sell price for a transaction.
     *
     * @return adjusted sell price, or the raw {@code shop.sellPrice} if pricing is disabled.
     */
    public BigDecimal computeSellPrice(ShopData shop, UUID actorUUID, int quantity, ServerLevel level) {
        if (!enabled || shop.sellPrice == null) return shop.sellPrice;
        PriceContext ctx = new PriceContext(shop, actorUUID, quantity,
                ShopTransactionEvent.Type.SELL, level);
        return applyRules(shop.sellPrice, ctx);
    }

    public boolean isEnabled() { return enabled; }
    public int getRuleCount()  { return rules.size(); }

    // ── Internals ─────────────────────────────────────────────────────────────

    private BigDecimal applyRules(BigDecimal base, PriceContext ctx) {
        BigDecimal price = base.setScale(2, RoundingMode.HALF_UP);
        for (PriceRule rule : rules) {
            try {
                BigDecimal adjusted = rule.apply(price, ctx);
                if (adjusted != null && adjusted.compareTo(BigDecimal.ZERO) >= 0) {
                    price = adjusted.setScale(2, RoundingMode.HALF_UP);
                }
            } catch (Exception e) {
                LOGGER.debug("[ChestShop] PriceRule {} threw: {}", rule.getClass().getSimpleName(), e.getMessage());
            }
        }
        return price;
    }

    private static JsonObject getShopPricingConfig() {
        try {
            JsonObject cfg = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("shop")) {
                JsonObject shop = cfg.getAsJsonObject("shop");
                if (shop.has("pricing")) return shop.getAsJsonObject("pricing");
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to read shop.pricing config", e);
        }
        return null;
    }
}

