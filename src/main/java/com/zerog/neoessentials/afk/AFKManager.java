package com.zerog.neoessentials.afk;

import com.zerog.neoessentials.util.MessageUtil;
// import net.minecraft.server.level.ServerPlayer; // Temporarily disabled due to import issues

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
// import com.zerog.neoessentials.features.TabListManager; // Temporarily disabled due to import issues

public class AFKManager {
    private static AFKManager instance;
    private final Map<UUID, Boolean> afkStatus = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private static final long AFK_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private AFKManager() {}

    public static AFKManager getInstance() {
        if (instance == null) instance = new AFKManager();
        return instance;
    }

    // Temporarily use Object instead of ServerPlayer to avoid import issues
    public void setAFK(Object player, boolean isAfk) {
        // TODO: Restore proper ServerPlayer handling when imports work
        System.out.println("AFK status change - player: " + player + ", isAfk: " + isAfk);
    }

    public boolean isAFK(Object player) {
        // TODO: Restore proper ServerPlayer handling when imports work
        return false; // Placeholder
    }

    public void updateActivity(Object player) {
        // TODO: Restore proper ServerPlayer handling when imports work
        System.out.println("Activity update for player: " + player);
    }

    public void checkAutoAFK() {
        long now = System.currentTimeMillis();
        for (UUID uuid : lastActivity.keySet()) {
            long last = lastActivity.get(uuid);
            if (now - last > AFK_TIMEOUT_MS) {
                afkStatus.put(uuid, true);
            }
        }
    }

    // Call this from movement/chat event listeners to update activity  
    public void updatePlayerActivity(Object player) {
        AFKManager.getInstance().updateActivity(player);
    }
    
    // Call this on a timer (e.g., every minute) to check auto-AFK
    public void checkAllPlayersAutoAFK() {
        AFKManager.getInstance().checkAutoAFK();
    }

    public void removePlayer(Object player) {
        // TODO: Restore proper ServerPlayer handling when imports work
        System.out.println("Removing player: " + player);
    }
}
