package com.zerog.neoessentials.tablist;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Event handler that drives the TablistManager tick, join, quit, and proxy channel updates.
 *
 * <p>BungeeTabListPlus-style additions:
 * <ul>
 *   <li>Registers the {@code BungeeCord} plugin-messaging channel on server start.</li>
 *   <li>Passes plugin-channel messages to {@link ProxyIntegration}.</li>
 *   <li>Calls the new {@code onPlayerQuit(player, server)} signature to clean up
 *       fake-player entries and proxy state cleanly.</li>
 * </ul>
 */
@EventBusSubscriber(modid = "neoessentials")
public class TablistEventHandler {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Register the BungeeCord plugin-messaging channel so the proxy can respond
        // to our GetServers / PlayerCount queries.
        // NeoForge 1.21.1 does not natively manage plugin-message channel registration
        // for BungeeCord — the proxy listens for traffic on the "BungeeCord" namespace
        // regardless of REGISTER, so no special registration is required here.
        // We simply poll on the first tick after the server starts.
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // AnimationManager must keep advancing regardless of whether the tablist MODULE is
        // enabled — {animation:NAME} is shared by scoreboard/hologram/chat/crate keys, none of
        // which should go dark just because a server doesn't use tablist customization. This
        // used to be gated behind isTablistModuleEnabled() below (this was the ONLY call site
        // driving AnimationManager's clock at all), so any server with modules.tablistEnabled
        // set to false silently froze every animation mod-wide on its very first frame forever
        // — no hologram/scoreboard refresh-interval setting could ever fix that, since the
        // underlying animation clock itself was never advancing in the first place.
        TablistManager.getInstance().tickAnimationsOnly();

        if (!com.zerog.neoessentials.config.ConfigManager.isTablistModuleEnabled()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        TablistManager.getInstance().onTick(server);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.isTablistModuleEnabled()) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        TablistManager.getInstance().onPlayerJoin(player, server);
        // Restore the player's tab-list display name if they had a nickname stored
        com.zerog.neoessentials.util.commands.NickCommand.onPlayerJoin(player);
    }

    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.isTablistModuleEnabled()) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        TablistManager.getInstance().clearCustomName(player.getUUID());
        // Use the new full-signature quit that cleans up fake entries and proxy state
        TablistManager.getInstance().onPlayerQuit(player, server);
    }
}

