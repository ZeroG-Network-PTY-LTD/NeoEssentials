package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Back command implementation - /back
 * Teleports players to their last death or teleport location
 */
public class BackCommand {
    
    // Store last locations for players
    private static final Map<UUID, LocationData> lastLocations = new HashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /back [player] - Return to last location
        dispatcher.register(Commands.literal("back")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.BACK))
            .executes(ctx -> teleportBack(ctx, null))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.BACK))
                .executes(ctx -> teleportBack(ctx, EntityArgument.getPlayer(ctx, "player")))
            )
        );
    }
    
    /**
     * Store a player's current location before they teleport or die
     */
    public static void storeLocation(ServerPlayer player, String reason) {
        if (player == null) return;
        
        LocationData location = new LocationData(
            player.getX(),
            player.getY(), 
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            player.level().dimension(),
            reason,
            System.currentTimeMillis()
        );
        
        lastLocations.put(player.getUUID(), location);
    }
    
    /**
     * Get a player's last stored location
     */
    public static LocationData getLastLocation(ServerPlayer player) {
        return lastLocations.get(player.getUUID());
    }
    
    private static int teleportBack(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) throws CommandSyntaxException {
        ServerPlayer player = targetPlayer != null ? targetPlayer : context.getSource().getPlayerOrException();
        LocationData lastLocation = getLastLocation(player);
        
        if (lastLocation == null) {
            if (targetPlayer != null && targetPlayer != context.getSource().getPlayerOrException()) {
                context.getSource().sendFailure(net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getMessage(player, "back.no_previous_location_other", player.getName().getString())));
            } else {
                context.getSource().sendFailure(net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getMessage(player, "back.no_previous_location")));
            }
            return 0;
        }
        
        // Check if the dimension still exists
        ServerLevel targetLevel = context.getSource().getServer().getLevel(lastLocation.dimension);
        if (targetLevel == null) {
            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getMessage(player, "back.dimension_missing")));
            return 0;
        }
        
        // Store current location before teleporting back
        storeLocation(player, "teleport");
        
        // Teleport the player
        if (player.level().dimension() != lastLocation.dimension) {
            // Cross-dimensional teleport
            player.teleportTo(targetLevel, lastLocation.x, lastLocation.y, lastLocation.z, lastLocation.yaw, lastLocation.pitch);
        } else {
            // Same dimension teleport
            player.teleportTo(lastLocation.x, lastLocation.y, lastLocation.z);
            player.setYRot(lastLocation.yaw);
            player.setXRot(lastLocation.pitch);
        }
        
        // Send confirmation messages
        String timeAgo = getTimeAgo(lastLocation.timestamp);
        if (targetPlayer != null && targetPlayer != context.getSource().getPlayerOrException()) {
            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getMessage(player, "back.success_other", player.getName().getString(), lastLocation.reason, timeAgo)), true);
            targetPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getMessage(targetPlayer, "back.success_self", lastLocation.reason, timeAgo)));
        } else {
            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(com.zerog.neoessentials.localization.LanguageManager.getMessage(player, "back.success_self", lastLocation.reason, timeAgo)), false);
        }
        
        return 1;
    }
    
    private static String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " day" + (days > 1 ? "s" : "");
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "");
        if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "");
        return seconds + " second" + (seconds != 1 ? "s" : "");
    }
    
    /**
     * Data class to store location information
     */
    public static class LocationData {
        public final double x, y, z;
        public final float yaw, pitch;
        public final net.minecraft.resources.ResourceKey<Level> dimension;
        public final String reason;
        public final long timestamp;
        
        public LocationData(double x, double y, double z, float yaw, float pitch, 
                          net.minecraft.resources.ResourceKey<Level> dimension, String reason, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.dimension = dimension;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}
