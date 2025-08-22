package com.zerog.neoessentials.managers;

import com.zerog.neoessentials.storage.JsonStorage;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class SocialSpyManager {
    private static final Map<UUID, Boolean> spyData = JsonStorage.loadSocialSpy();

    public static void toggle(ServerPlayer player, boolean enable) {
        spyData.put(player.getUUID(), enable);
        JsonStorage.saveSocialSpy(spyData);
    }

    public static boolean isEnabled(ServerPlayer player) {
        return spyData.getOrDefault(player.getUUID(), false);
    }

    public static Set<UUID> getEnabledPlayers() {
        Set<UUID> enabled = new HashSet<>();
        for (Map.Entry<UUID, Boolean> entry : spyData.entrySet()) {
            if (entry.getValue()) enabled.add(entry.getKey());
        }
        return enabled;
    }
}
