package com.zerog.neoessentials.afk;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.zerog.neoessentials.features.TabListManager;
import com.zerog.neoessentials.features.ScoreboardManager;

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
        boolean wasAfk = isAFK(player);
        afkStatus.put(player.getUUID(), isAfk);
        if (isAfk && !wasAfk) {
            MessageUtil.sendTranslatedMessage(player, "neoessentials.afk.now_afk", player.getName().getString());
        } else if (!isAfk && wasAfk) {
            MessageUtil.sendTranslatedMessage(player, "neoessentials.afk.back", player.getName().getString());
        }
        // Always update tablist and scoreboard when AFK status changes
        TabListManager.getInstance().updateTabList(java.util.Collections.singletonList(player));
        ScoreboardManager.getInstance().updateScoreboard(player);
    }

    public boolean isAFK(ServerPlayer player) {
        return afkStatus.getOrDefault(player.getUUID(), false);
    }

    public void updateActivity(ServerPlayer player) {
        lastActivity.put(player.getUUID(), System.currentTimeMillis());
        if (isAFK(player)) setAFK(player, false);
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
    public void updatePlayerActivity(ServerPlayer player) {
        AFKManager.getInstance().updateActivity(player);
    }
    // Call this on a timer (e.g., every minute) to check auto-AFK
    public void checkAllPlayersAutoAFK() {
        AFKManager.getInstance().checkAutoAFK();
    }

    public static class AFKEventListener {
        public AFKEventListener() {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
        }

        // Removed invalid event handler methods
    }
}
