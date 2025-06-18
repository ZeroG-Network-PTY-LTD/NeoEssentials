package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements AFK (away from keyboard) commands and functionality.
 */
public class AfkCommands {

    // Store AFK players with their status message
    private final Map<UUID, String> afkPlayers = new ConcurrentHashMap<>();
    
    // Store the last active time for players
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    
    // Auto-AFK settings
    private boolean autoAfkEnabled = true;
    private long autoAfkTime = 300000; // 5 minutes in milliseconds
    
    /**
     * Constructor to register event handlers
     */
    public AfkCommands() {
        // Register event handlers
        NeoForge.EVENT_BUS.register(this);
    }
    
    /**
     * Register all AFK commands
     * 
     * @param dispatcher Command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /afk [message]
        dispatcher.register(Commands.literal("afk")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.afk"))
            .executes(context -> toggleAfk(context, null))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> toggleAfk(
                    context,
                    StringArgumentType.getString(context, "message")
                ))
            )
        );
        
        // /afkautotoggle
        dispatcher.register(Commands.literal("afkautotoggle")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.afkautotoggle"))
            .executes(context -> toggleAutoAfk(context))
        );
        
        // /afktime
        dispatcher.register(Commands.literal("afktime")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.afktime"))
            .then(Commands.argument("time", StringArgumentType.word())
                .executes(context -> setAfkTime(context, StringArgumentType.getString(context, "time")))
            )
        );
        
        // /back
        dispatcher.register(Commands.literal("back")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.back"))
            .executes(context -> goBack(context))
        );
    }
    
    /**
     * Toggle AFK status for a player
     */
    private int toggleAfk(CommandContext<CommandSourceStack> context, String message) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            UUID playerId = player.getUUID();
            
            if (afkPlayers.containsKey(playerId)) {
                // Player is already AFK, set them back to active
                afkPlayers.remove(playerId);
                lastActivity.put(playerId, System.currentTimeMillis());
                
                // Broadcast that player is no longer AFK
                String playerName = player.getScoreboardName();
                context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(playerName + " is no longer AFK."),
                    false
                );
                
                return 1;
            } else {
                // Set player as AFK
                afkPlayers.put(playerId, message != null ? message : "");
                
                // Broadcast that player is now AFK
                String playerName = player.getScoreboardName();
                if (message != null && !message.isEmpty()) {
                    String formattedMessage = TextUtil.formatText(message);
                    context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(playerName + " is now AFK: " + formattedMessage),
                        false
                    );
                } else {
                    context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(playerName + " is now AFK."),
                        false
                    );
                }
                
                return 1;
            }
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("You must be a player to use this command."));
            return 0;
        }
    }
    
    /**
     * Toggle auto-AFK functionality
     */
    private int toggleAutoAfk(CommandContext<CommandSourceStack> context) {
        autoAfkEnabled = !autoAfkEnabled;
        
        if (autoAfkEnabled) {
            context.getSource().sendSuccess(() -> Component.literal("Auto-AFK detection enabled."), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Auto-AFK detection disabled."), true);
        }
        
        return 1;
    }
    
    /**
     * Set the auto-AFK time
     */
    private int setAfkTime(CommandContext<CommandSourceStack> context, String timeStr) {
        try {
            // Parse time in minutes
            int minutes = Integer.parseInt(timeStr);
            if (minutes < 1) {
                context.getSource().sendFailure(Component.literal("AFK time must be at least 1 minute."));
                return 0;
            }
            
            // Convert to milliseconds
            autoAfkTime = minutes * 60000L;
            
            context.getSource().sendSuccess(() -> 
                Component.literal("Auto-AFK time set to " + minutes + " minutes."), true);
            
            return 1;
        } catch (NumberFormatException e) {
            context.getSource().sendFailure(Component.literal("Invalid time format. Use a number of minutes."));
            return 0;
        }
    }
    
    /**
     * Go back to previous location
     * Placeholder for now, will need to integrate with TeleportHistory
     */    private int goBack(CommandContext<CommandSourceStack> context) {
        try {
            // Get player but don't assign to variable since we're not using it yet
            context.getSource().getPlayerOrException();
            
            // This should be integrated with the TeleportHistory system
            context.getSource().sendFailure(Component.literal("Back command is not yet implemented."));
            return 0;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("You must be a player to use this command."));
            return 0;
        }
    }
    
    /**
     * Check if a player is AFK
     */
    public boolean isPlayerAfk(UUID playerId) {
        return afkPlayers.containsKey(playerId);
    }
    
    /**
     * Get the AFK message for a player
     */
    public String getAfkMessage(UUID playerId) {
        return afkPlayers.get(playerId);
    }
    
    /**
     * Update a player's last activity time
     */
    public void updateActivity(ServerPlayer player) {
        if (player == null) return;
        
        UUID playerId = player.getUUID();
        
        // Update last activity time
        lastActivity.put(playerId, System.currentTimeMillis());
        
        // If player is AFK, set them back to active
        if (afkPlayers.containsKey(playerId)) {
            afkPlayers.remove(playerId);
              // Broadcast that player is no longer AFK
            String playerName = player.getScoreboardName();
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(playerName + " is no longer AFK."),
                    false
                );
            }
        }
    }
    
    /**
     * Check for players who have been inactive for too long
     */
    public void checkForInactivePlayers() {
        if (!autoAfkEnabled) return;
        
        long currentTime = System.currentTimeMillis();
        
        for (ServerPlayer player : NeoEssentials.getInstance().getServer().getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            
            // Skip players who are already AFK
            if (afkPlayers.containsKey(playerId)) continue;
            
            // Skip players who don't have recorded activity
            if (!lastActivity.containsKey(playerId)) {
                lastActivity.put(playerId, currentTime);
                continue;
            }
            
            // Check if player has been inactive for too long
            long lastActiveTime = lastActivity.get(playerId);
            if (currentTime - lastActiveTime > autoAfkTime) {
                // Set player as AFK
                afkPlayers.put(playerId, "Auto-AFK after " + (autoAfkTime / 60000) + " minutes");
                  // Broadcast that player is now AFK
                String playerName = player.getScoreboardName();
                if (player.getServer() != null) {
                    player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(playerName + " is now AFK."),
                        false
                    );
                }
            }
        }
    }
    
    /**
     * Handle player movement to update activity status
     */
    @SubscribeEvent
    public void onPlayerMove(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            updateActivity((ServerPlayer) event.getEntity());
        }
    }
    
    /**
     * Handle player chat to update activity status
     */
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        updateActivity(event.getPlayer());
    }
}
