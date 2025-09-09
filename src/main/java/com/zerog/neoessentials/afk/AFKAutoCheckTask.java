package com.zerog.neoessentials.afk;

import net.minecraft.server.MinecraftServer;

public class AFKAutoCheckTask implements Runnable {
    @Override
    public void run() {
        AFKManager.getInstance().checkAutoAFK();
    }

    public static void start(MinecraftServer server) {
        // Start AFK auto-check task that runs every minute
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(new AFKAutoCheckTask(), 0, 60, java.util.concurrent.TimeUnit.SECONDS);
    }
}
