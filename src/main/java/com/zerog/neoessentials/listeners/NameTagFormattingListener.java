
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
            String nickname = com.zerog.neoessentials.commands.essentials.NickCommand.getNicknameOnly(serverPlayer.getUUID());
            if (nickname != null && !nickname.isEmpty()) {
                displayName = nickname;
                com.zerog.neoessentials.config.MainConfig.ChatSettings chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().chatSettings;
                if (chatConfig.nicknames.allowColors) {
                    displayName = com.zerog.neoessentials.util.ColorUtil.colorize(displayName).getString();
                }
            }
        }
        event.setDisplayname(com.zerog.neoessentials.util.ColorUtil.colorize(displayName));
    }
}
