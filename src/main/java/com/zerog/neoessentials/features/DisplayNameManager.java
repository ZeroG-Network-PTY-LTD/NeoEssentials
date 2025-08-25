package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;

public class DisplayNameManager {
    public static String getDisplayName(ServerPlayer player) {
        // Use NickCommand to get nickname or player name
        String name = com.zerog.neoessentials.commands.essentials.NickCommand.getNickname(player);
        return name.replace('&', '§');
    }
}
