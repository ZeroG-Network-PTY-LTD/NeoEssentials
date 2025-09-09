package com.zerog.neoessentials.afk;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

public class AFKEventListener {
    
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            AFKManager.getInstance().updateActivity(player);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AFKManager.getInstance().removePlayer(player);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Initialize player activity tracking
            AFKManager.getInstance().updateActivity(player);
        }
    }
}
