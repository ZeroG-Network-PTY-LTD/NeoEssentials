package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fly command implementation - /fly [player]
 * Toggles flight mode for players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class FlyCommand {
    
    // Set to track players with fly enabled
    private static final Set<UUID> flyingPlayers = new HashSet<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /fly - Toggle fly mode for yourself
        dispatcher.register(Commands.literal("fly")
            .requires(source -> source.hasPermission(2))
            .executes(FlyCommand::toggleFlySelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(FlyCommand::toggleFlyOther)
            )
        );
    }
    
    /**
     * Toggle fly mode for the command executor
     */
    private static int toggleFlySelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean canFly = toggleFly(player);
        
        if (canFly) {
            context.getSource().sendSuccess(() -> Component.literal("§aFlight enabled! You can now fly."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§cFlight disabled! You can no longer fly."), false);
        }
        
        return 1;
    }
    
    /**
     * Toggle fly mode for another player
     */
    private static int toggleFlyOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ServerPlayer executor = context.getSource().getPlayerOrException();
        
        boolean canFly = toggleFly(target);
        
        // Send confirmation to both players
        if (canFly) {
            context.getSource().sendSuccess(() -> Component.literal("§aFlight enabled for " + target.getName().getString() + "!"), true);
            target.sendSystemMessage(Component.literal("§aFlight enabled by " + executor.getName().getString() + "! You can now fly."));
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§cFlight disabled for " + target.getName().getString() + "!"), true);
            target.sendSystemMessage(Component.literal("§cFlight disabled by " + executor.getName().getString() + "! You can no longer fly."));
        }
        
        return 1;
    }
    
    /**
     * Toggle fly mode for a player
     * @return true if fly is now enabled, false if disabled
     */
    private static boolean toggleFly(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Abilities abilities = player.getAbilities();
        
        if (flyingPlayers.contains(playerId)) {
            // Disable flight
            flyingPlayers.remove(playerId);
            
            // Remove flight abilities (unless in creative mode)
            if (!player.isCreative()) {
                abilities.mayfly = false;
                abilities.flying = false;
            }
            
            // Sync abilities to client
            player.onUpdateAbilities();
            
            return false;
        } else {
            // Enable flight
            flyingPlayers.add(playerId);
            
            // Grant flight abilities
            abilities.mayfly = true;
            
            // Sync abilities to client
            player.onUpdateAbilities();
            
            return true;
        }
    }
    
    /**
     * Check if a player has fly enabled
     */
    public static boolean canFly(ServerPlayer player) {
        return flyingPlayers.contains(player.getUUID());
    }
    
    /**
     * Remove player from fly mode (called when player leaves or changes gamemode)
     */
    public static void removePlayer(UUID playerId) {
        flyingPlayers.remove(playerId);
    }
    
    /**
     * Get all players with fly enabled
     */
    public static Set<UUID> getFlyingPlayers() {
        return new HashSet<>(flyingPlayers);
    }
    
    /**
     * Handle gamemode change - preserve fly status if appropriate
     */
    public static void onGameModeChange(ServerPlayer player) {
        // If player changes to creative or spectator, they don't need custom fly
        if (player.isCreative() || player.isSpectator()) {
            // Don't remove from tracking, but don't interfere with abilities
            return;
        }
        
        // If player was in fly mode, restore it
        if (flyingPlayers.contains(player.getUUID())) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }
}
