package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last player who messaged each player for /reply functionality.
 */
public class LastMessageManager {
    private static final Map<String, String> lastMessagerMap = new ConcurrentHashMap<>();

    public static void setLastMessager(ServerPlayer recipient, ServerPlayer sender) {
        lastMessagerMap.put(recipient.getName().getString().toLowerCase(), sender.getName().getString().toLowerCase());
    }

    public static ServerPlayer getLastMessager(ServerPlayer player) {
        String last = lastMessagerMap.get(player.getName().getString().toLowerCase());
        if (last == null) return null;
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(last)) return p;
        }
        return null;
    }
}
