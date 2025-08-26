package com.zerog.neoessentials.features;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public class UIEventHandler {
    @SubscribeEvent
    public void onPermissionUpdate(com.zerog.neoessentials.features.PermissionUpdateEvent event) {
        com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] DEBUG: onPermissionUpdate fired. TabListManager instance: " + tabListManager);
        ServerPlayer player = event.getPlayer();
        com.zerog.neoessentials.util.DebugUtil.debugLog("[UIEventHandler] onPermissionUpdate called for " + player.getName().getString() + " (UUID: " + player.getUUID() + ")");
        com.zerog.neoessentials.util.DebugUtil.debugLog("[NeoEssentials] PermissionUpdateEvent received for player " + player.getUUID() + ". Updating tablist for affected player only.");
        if (player != null) {
            tabListManager.updateTabList(java.util.Collections.singletonList(player)); // Only update for affected player
            tabListManager.updateHeaderFooter(player, com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player));
            tabListManager.updatePlayerEntry(player);
            scoreboardManager.updateScoreboard(player);
        }
        String displayName = DisplayNameManager.getDisplayName(player);
        bossBarManager.showBossBar(player, displayName, 1.0f, 0x00FF00);
    }
    private final TabListManager tabListManager;
    private final ScoreboardManager scoreboardManager;
    private final BossBarManager bossBarManager;

    public UIEventHandler(TabListManager tabListManager, ScoreboardManager scoreboardManager, BossBarManager bossBarManager, PlaceholderManager placeholderManager) {
    this.tabListManager = tabListManager;
    this.scoreboardManager = scoreboardManager;
    this.bossBarManager = bossBarManager;
    }

    @SubscribeEvent
    private void onPlayerJoin(PlayerLoggedInEvent event) {
    ServerPlayer player = (ServerPlayer) event.getEntity();
    String displayName = DisplayNameManager.getDisplayName(player);
    tabListManager.updateHeaderFooter(player, displayName);
    tabListManager.updatePlayerEntry(player);
    if (player.getServer() != null) {
        scoreboardManager.updateScoreboard(player);
    }
    bossBarManager.showBossBar(player, displayName, 1.0f, 0x00FF00);
    }

    @SubscribeEvent
    private void onScoreUpdate(ScoreboardUpdateEvent event) {
    // ScoreboardManager no longer supports setPlayerScore; implement score logic if needed
    }

    @SubscribeEvent
    private void onBossBarEvent(CustomBossBarEvent event) {
        bossBarManager.showBossBar(event.getPlayer(), event.getTitle(), event.getProgress(), event.getColor());
    }
}
