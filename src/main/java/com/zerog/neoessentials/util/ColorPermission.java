package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerPlayer;

public class ColorPermission {
    private final com.zerog.neoessentials.integration.LuckPermsIntegration luckPerms = new com.zerog.neoessentials.integration.LuckPermsIntegration();
    private final com.zerog.neoessentials.config.MainConfig mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig();

    public boolean canAnyColor(ServerPlayer p) {
        // LuckPerms node: neoessentials.color.any
        if (luckPerms.isAvailable()) {
            return luckPerms.hasPermission(p, "neoessentials.color.any");
        }
        // Fallback to config
    return mainConfig.colorPermissionsConfig != null && mainConfig.colorPermissionsConfig.chat;
    }

    public boolean canColor(ServerPlayer p, char code) {
        // LuckPerms node: neoessentials.color.{code}
        if (luckPerms.isAvailable()) {
            return luckPerms.hasPermission(p, "neoessentials.color." + code);
        }
        // Fallback to config
    return mainConfig.colorPermissionsConfig != null && mainConfig.colorPermissionsConfig.chat;
    }

    public boolean canFormat(ServerPlayer p, char code) {
        // LuckPerms node: neoessentials.format.{code}
        if (luckPerms.isAvailable()) {
            return luckPerms.hasPermission(p, "neoessentials.format." + code);
        }
        // Fallback to config
    return mainConfig.colorPermissionsConfig != null && mainConfig.colorPermissionsConfig.chat;
    }

    public boolean permitsRgb(ServerPlayer p) {
        // LuckPerms node: neoessentials.color.rgb
        if (luckPerms.isAvailable()) {
            return luckPerms.hasPermission(p, "neoessentials.color.rgb");
        }
        // Fallback to config
        return mainConfig.colorPermissionsConfig != null && mainConfig.colorPermissionsConfig.rgb;
    }
}
