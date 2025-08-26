package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;

public class DisplayNameManager {
    public static String getDisplayName(ServerPlayer player) {
        String displayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player);
        com.zerog.neoessentials.util.DebugUtil.debugLog("[DisplayNameManager] Player: " + player.getGameProfile().getName() + ", DisplayName: '" + displayName + "'");
        return displayName;
    }
}
