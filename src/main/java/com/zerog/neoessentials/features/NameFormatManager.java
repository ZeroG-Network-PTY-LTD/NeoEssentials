package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;

/**
 * Central manager for player name, prefix, and suffix formatting.
 * Use this for tablist, chat, scoreboard, bossbar, etc.
 */
public class NameFormatManager {
    private static NameFormatManager instance;
    public static NameFormatManager getInstance() {
        if (instance == null) instance = new NameFormatManager();
        return instance;
    }

    public String getPrefix(ServerPlayer player) {
        return com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerPrefix(player.getUUID());
    }
    public String getSuffix(ServerPlayer player) {
        return com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerSuffix(player.getUUID());
    }
    public String getGroup(ServerPlayer player) {
        return com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance().getPlayerGroup(player.getUUID());
    }
    public String getNickname(ServerPlayer player) {
        String nick = com.zerog.neoessentials.commands.essentials.NickCommand.getNickname(player);
        return (nick != null && !nick.isEmpty()) ? nick : player.getGameProfile().getName();
    }
    public String getDisplayName(ServerPlayer player) {
    String nickname = getNickname(player);
    return nickname.replace('&', '\u00A7');
    }
    /**
     * Get formatted name for tablist, chat, etc. using a format string.
     * Supported placeholders: {PREFIX}, {DISPLAYNAME}, {SUFFIX}, {GROUP}
     */
    public String format(ServerPlayer player, String format) {
        String prefix = getPrefix(player);
        String suffix = getSuffix(player);
        String group = getGroup(player);
        String nickname = getNickname(player);
        String result = format
            .replace("{PREFIX}", prefix != null ? prefix : "")
            .replace("{DISPLAYNAME}", nickname)
            .replace("{SUFFIX}", suffix != null ? suffix : "")
            .replace("{GROUP}", group != null ? group : "");
        return result.replace('&', '\u00A7');
    }
}
