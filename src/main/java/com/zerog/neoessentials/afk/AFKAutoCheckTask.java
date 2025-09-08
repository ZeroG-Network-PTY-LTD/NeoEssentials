package com.zerog.neoessentials.afk;

public class AFKAutoCheckTask implements Runnable {
    @Override
    public void run() {
        AFKManager.getInstance().checkAutoAFK();
    }

    public static void start(Object server) {
        // Temporarily simplified to resolve import issues
        // TODO: Restore MinecraftServer integration when imports are stable
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(new AFKAutoCheckTask(), 0, 60, java.util.concurrent.TimeUnit.SECONDS);
    }
}
