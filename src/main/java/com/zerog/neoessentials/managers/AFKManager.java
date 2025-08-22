package com.zerog.neoessentials.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AFKManager {
    private static AFKManager instance;
    private final Set<UUID> afkPlayers = new HashSet<>();

    public static AFKManager getInstance() {
        if (instance == null) instance = new AFKManager();
        return instance;
    }

    public boolean isAFK(UUID player) {
        return afkPlayers.contains(player);
    }

    public void setAFK(UUID player, boolean afk) {
        if (afk) afkPlayers.add(player);
        else afkPlayers.remove(player);
    }
}