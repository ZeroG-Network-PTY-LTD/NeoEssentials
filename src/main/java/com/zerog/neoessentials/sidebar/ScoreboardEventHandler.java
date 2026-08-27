package com.zerog.neoessentials.sidebar;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Drives {@link ScoreboardManager}'s tick/join/quit lifecycle, mirroring TablistEventHandler. */
@EventBusSubscriber(modid = "neoessentials")
public class ScoreboardEventHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!com.zerog.neoessentials.config.ConfigManager.isScoreboardModuleEnabled()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ScoreboardManager.getInstance().onTick(server);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.isScoreboardModuleEnabled()) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ScoreboardManager.getInstance().onPlayerJoin(player, server);
        server.execute(() -> ScoreboardManager.getInstance().updatePlayer(player, server));
    }

    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.isScoreboardModuleEnabled()) return;
        ScoreboardManager.getInstance().onPlayerQuit(player);
    }
}
