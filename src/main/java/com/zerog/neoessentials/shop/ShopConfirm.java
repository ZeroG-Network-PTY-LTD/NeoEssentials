package com.zerog.neoessentials.shop;

import com.zerog.neoessentials.shop.api.ShopEconomyRegistry;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Second-click confirmation for expensive shop purchases ({@code shop.confirmAbove} in
 * config.json) — the first click on a buy above the threshold only asks; clicking the same
 * thing again within {@link #WINDOW_MS} actually buys it. Shared by NPC shops and sign shops.
 */
public final class ShopConfirm {
    private static final long WINDOW_MS = 5000L;
    private static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.valueOf(1000);

    private record Pending(String key, long expiresAt) {}
    private static final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    private ShopConfirm() {}

    /**
     * @param key   identifies exactly what's being bought (shop + slot/sign + amount), so a
     *              confirming click on something else doesn't count
     * @return true to go ahead with the purchase; false if the player was just asked to confirm
     */
    public static boolean confirmed(ServerPlayer player, String key, BigDecimal price, String label) {
        BigDecimal threshold = threshold();
        if (threshold.signum() <= 0 || price.compareTo(threshold) <= 0) return true;

        long now = System.currentTimeMillis();
        Pending previous = pending.get(player.getUUID());
        if (previous != null && previous.key().equals(key) && now <= previous.expiresAt()) {
            pending.remove(player.getUUID());
            return true;
        }
        pending.put(player.getUUID(), new Pending(key, now + WINDOW_MS));
        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.confirm_prompt",
            label, ShopEconomyRegistry.getInstance().getAdapter().format(price)));
        return false;
    }

    private static BigDecimal threshold() {
        try {
            var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance()
                .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("shop")) {
                var shop = cfg.getAsJsonObject("shop");
                if (shop.has("confirmAbove")) return shop.get("confirmAbove").getAsBigDecimal();
            }
        } catch (Exception ignored) {
            // fall through to the default
        }
        return DEFAULT_THRESHOLD;
    }
}
