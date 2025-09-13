package com.zerog.neoessentials.shops;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Listens for world load to scan and register sign shops
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopWorldLoadListener {
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // Load persisted shops
        ShopRegistry.loadAll();
        // Scan for any missing sign shops and register them
        ShopEventHandler.scanWorldForSignShops(level);
    }
}