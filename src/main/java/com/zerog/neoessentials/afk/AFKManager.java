package com.zerog.neoessentials.afk;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public void setAFK(ServerPlayer player, boolean isAfk) {
        UUID playerId = player.getUUID();
        afkStatus.put(playerId, isAfk);
        
        // Update tablist to show AFK status
        String afkIndicator = isAfk ? " §7[AFK]" : "";
        String displayName = player.getName().getString() + afkIndicator;
        
        // Send message to player
        if (isAfk) {
            MessageUtil.sendMessage(player, "§eYou are now AFK.");
        } else {
            MessageUtil.sendMessage(player, "§eYou are no longer AFK.");
        }
    }

    public boolean isAFK(ServerPlayer player) {
        return afkStatus.getOrDefault(player.getUUID(), false);
    }

    public void updateActivity(ServerPlayer player) {
        UUID playerId = player.getUUID();
        lastActivity.put(playerId, System.currentTimeMillis());
        
        // If player was AFK, mark them as no longer AFK
        if (isAFK(player)) {
            setAFK(player, false);
        }
    }

    public void checkAutoAFK() {
        long now = System.currentTimeMillis();
        for (UUID uuid : lastActivity.keySet()) {
            long last = lastActivity.get(uuid);
            if (now - last > AFK_TIMEOUT_MS && !afkStatus.getOrDefault(uuid, false)) {
                afkStatus.put(uuid, true);
                // Note: In a real server, you'd need to get ServerPlayer from UUID to send message
                // This is just for tracking AFK status when the auto-timer runs
            }
        }
    }

    // Call this from movement/chat event listeners to update activity  
    public void updatePlayerActivity(ServerPlayer player) {
        AFKManager.getInstance().updateActivity(player);
    }
    
    // Call this on a timer (e.g., every minute) to check auto-AFK
    public void checkAllPlayersAutoAFK() {
        AFKManager.getInstance().checkAutoAFK();
    }

    public void removePlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        afkStatus.remove(playerId);
        lastActivity.remove(playerId);
    }
}
