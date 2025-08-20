
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
        String name = player.getScoreboardName();
        String displayName = name;
        if (player instanceof ServerPlayer serverPlayer) {
            // If you want to use nickname, fetch it here
            try {
                java.lang.reflect.Field nicknamesField = com.zerog.neoessentials.commands.essentials.NickCommand.class.getDeclaredField("nicknames");
                nicknamesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<java.util.UUID, String> nicknames = (java.util.Map<java.util.UUID, String>) nicknamesField.get(null);
                String nickname = nicknames.get(serverPlayer.getUUID());
                if (nickname != null && !nickname.isEmpty()) {
                    displayName = nickname;
                    // Always colorize nickname if allowed
                    com.zerog.neoessentials.config.ChatConfig chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getChatConfig();
                    if (chatConfig.nicknames.allowColors) {
                        displayName = com.zerog.neoessentials.util.ColorUtil.colorize(displayName).getString();
                    }
                }
            } catch (Exception e) {
                // Ignore, fallback to raw name
            }
        }
        // Use ColorUtil for all name tag coloring
        event.setDisplayname(com.zerog.neoessentials.util.ColorUtil.colorize(displayName));
    }
}
