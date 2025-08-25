package com.zerog.neoessentials.features;


import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

public class UIEventHandler {
    @SubscribeEvent
    private void onPermissionUpdate(com.zerog.neoessentials.features.PermissionUpdateEvent event) {
    ServerPlayer player = event.getPlayer();
    String displayName = DisplayNameManager.getDisplayName(player);
    tabListManager.updateHeaderFooter(player, displayName);
    tabListManager.updatePlayerEntry(player);
    scoreboardManager.updateScoreboard(player);
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
    scoreboardManager.updateScoreboard(player);
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
