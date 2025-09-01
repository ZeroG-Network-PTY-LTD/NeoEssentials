package com.zerog.neoessentials.afk;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AFKEventListener {
    public AFKEventListener() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            AFKManager.getInstance().updateActivity(player);
        }
    }

    // Workaround: Use a scheduled executor to periodically check AFK status for all online players
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    {
        scheduler.scheduleAtFixedRate(() -> {
            AFKManager.getInstance().checkAllPlayersAutoAFK();
        }, 0, 1, TimeUnit.SECONDS); // Check every second
    }
}
