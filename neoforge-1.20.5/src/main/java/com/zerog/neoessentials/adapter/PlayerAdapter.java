package com.zerog.neoessentials.adapter;

import com.zerog.neoessentials.common.adapter.IPlayerAdapter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * NeoForge 1.20.5 implementation of the player adapter
 */
public class PlayerAdapter implements IPlayerAdapter {
    
    @Override
    public String getUsername(Object playerRef) {
        if (!(playerRef instanceof ServerPlayer)) return null;
        
        ServerPlayer player = (ServerPlayer)playerRef;
        return player.getGameProfile().getName();
    }
    
    @Override
    public String getUniqueId(Object playerRef) {
        if (!(playerRef instanceof ServerPlayer)) return null;
        
        ServerPlayer player = (ServerPlayer)playerRef;
        return player.getUUID().toString();
    }
    
    @Override
    public void sendMessage(Object playerRef, String message) {
        if (!(playerRef instanceof ServerPlayer)) return;
        
        ServerPlayer player = (ServerPlayer)playerRef;
        player.sendSystemMessage(Component.literal(message));
    }
    
    @Override
    public boolean teleport(Object playerRef, Object worldRef, double x, double y, double z, float yaw, float pitch) {
        if (!(playerRef instanceof ServerPlayer) || !(worldRef instanceof ServerLevel)) {
            return false;
        }
        
        ServerPlayer player = (ServerPlayer)playerRef;
        ServerLevel level = (ServerLevel)worldRef;
        
        // If changing dimensions
        if (player.getLevel() != level) {
            player.teleportTo(level, x, y, z, yaw, pitch);
        } else {
            // Same dimension teleport
            player.teleportTo(x, y, z);
            player.setYRot(yaw);
            player.setXRot(pitch);
        }
        
        return true;
    }
}
