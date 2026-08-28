package com.zerog.neoessentials.leaderboard.adapters;

import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.leaderboard.NamedStatProvider;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.entity.ShopEntityData;
import com.zerog.neoessentials.shop.entity.ShopEntityManager;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ranks shops (sign/chest {@link ShopData} and NPC {@link ShopEntityData}) by total revenue —
 * the "which shop sells the most" board a user asked for, since the leaderboard system used to
 * only support ranking real players. Unions both shop sources into one {@code shop_sales}
 * board, keyed by each shop's own id rather than a player UUID.
 */
public class ShopSalesStatProvider implements NamedStatProvider {
    @Override
    public Map<String, NamedEntry> getAllNamedValues(MinecraftServer server) {
        Map<String, NamedEntry> out = new LinkedHashMap<>();

        for (ShopData shop : ShopManager.getInstance().getAllShops()) {
            if (shop.totalRevenueCents <= 0) continue;
            String key = shop.shopId != null ? shop.shopId.toString() : shop.toKey();
            String name = shop.ownerName != null && !shop.ownerName.isBlank()
                ? shop.ownerName : "Shop @ " + shop.toKey();
            out.put(key, new NamedEntry(name, shop.totalRevenueCents / 100.0));
        }

        for (ShopEntityData npc : ShopEntityManager.getInstance().getAll()) {
            if (npc.totalRevenueCents <= 0) continue;
            String name = npc.shopName != null && !npc.shopName.isBlank() ? npc.shopName : npc.toKey();
            out.put(npc.toKey(), new NamedEntry(name, npc.totalRevenueCents / 100.0));
        }

        return out;
    }

    @Override
    public String formatValue(Number value) {
        return EconomyManager.getInstance().getCurrencySymbol() + String.format("%.2f", value.doubleValue());
    }
}
