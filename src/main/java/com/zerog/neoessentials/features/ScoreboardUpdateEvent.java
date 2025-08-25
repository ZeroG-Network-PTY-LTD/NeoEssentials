



package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class ScoreboardUpdateEvent extends Event {
    private final ServerPlayer player;
    private final int newScore;

    public ScoreboardUpdateEvent(ServerPlayer player, int newScore) {
        this.player = player;
        this.newScore = newScore;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public int getNewScore() {
        return newScore;
    }
}
