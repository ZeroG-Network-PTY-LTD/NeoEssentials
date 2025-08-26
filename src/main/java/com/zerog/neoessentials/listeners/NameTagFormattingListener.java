
package com.zerog.neoessentials.listeners;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

public class NameTagFormattingListener {
    public NameTagFormattingListener() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        com.zerog.neoessentials.config.TablistConfig config = com.zerog.neoessentials.features.TabListManager.getInstance().config;
        if (config == null || !config.enableNametag) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[NameTagFormattingListener] Nametag is disabled in config, skipping display name for " + player.getName().getString());
            return;
        }
        String displayName;
        if (player instanceof ServerPlayer serverPlayer) {
            displayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(serverPlayer);
        } else {
            displayName = player.getScoreboardName();
        }
        event.setDisplayname(com.zerog.neoessentials.util.ColorUtil.colorize(displayName));
    }
}
