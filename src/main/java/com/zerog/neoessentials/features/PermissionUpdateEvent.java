package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired when a player's permissions, group, prefix, or suffix are updated.
 */
public class PermissionUpdateEvent extends Event {
    private final ServerPlayer player;

    public PermissionUpdateEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
