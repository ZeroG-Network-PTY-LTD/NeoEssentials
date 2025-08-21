package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerPlayer;

public class ColorPermission {
    public boolean canAnyColor(ServerPlayer p) {
        // TODO: Integrate with LuckPerms or fallback to config
        return true;
    }
    public boolean canColor(ServerPlayer p, char code) {
        // TODO: Integrate with LuckPerms or fallback to config
        return true;
    }
    public boolean canFormat(ServerPlayer p, char code) {
        // TODO: Integrate with LuckPerms or fallback to config
        return true;
    }
    public boolean permitsRgb(ServerPlayer p) {
        // TODO: Integrate with LuckPerms or fallback to config
        return true;
    }
}
