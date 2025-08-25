



package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class CustomBossBarEvent extends Event {
    private final ServerPlayer player;
    private final String title;
    private final float progress;
    private final int color;

    public CustomBossBarEvent(ServerPlayer player, String title, float progress, int color) {
        this.player = player;
        this.title = title;
        this.progress = progress;
        this.color = color;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getTitle() {
        return title;
    }

    public float getProgress() {
        return progress;
    }

    public int getColor() {
        return color;
    }
}
