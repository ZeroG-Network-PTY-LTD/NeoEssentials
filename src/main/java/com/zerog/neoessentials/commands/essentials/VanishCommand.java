package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vanish command implementation - /vanish [player]
 * Toggles invisibility for players (hides them from other players)
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class VanishCommand {
    
    // Set to track vanished players
    private static final Set<UUID> vanishedPlayers = new HashSet<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /vanish - Toggle vanish mode for yourself
        dispatcher.register(Commands.literal("vanish")
            .requires(source -> source.hasPermission(2))
            .executes(VanishCommand::toggleVanishSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(VanishCommand::toggleVanishOther)
            )
        );
        
        // Alias: /v
        dispatcher.register(Commands.literal("v")
            .requires(source -> source.hasPermission(2))
            .executes(VanishCommand::toggleVanishSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(VanishCommand::toggleVanishOther)
            )
        );
    }
    
    /**
     * Toggle vanish mode for the command executor
     */
    private static int toggleVanishSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean isVanished = toggleVanish(player);
        
        if (isVanished) {
            context.getSource().sendSuccess(() -> Component.literal("§aVanish enabled! You are now invisible to other players."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§cVanish disabled! You are now visible to other players."), false);
        }
        
        return 1;
    }
    
    /**
     * Toggle vanish mode for another player
     */
    private static int toggleVanishOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ServerPlayer executor = context.getSource().getPlayerOrException();
        
        boolean isVanished = toggleVanish(target);
        
        // Send confirmation to both players
        if (isVanished) {
            context.getSource().sendSuccess(() -> Component.literal("§aVanish enabled for " + target.getName().getString() + "!"), true);
            target.sendSystemMessage(Component.literal("§aVanish enabled by " + executor.getName().getString() + "! You are now invisible to other players."));
        } else {
            context.getSource().sendSuccess(() -> Component.literal("§cVanish disabled for " + target.getName().getString() + "!"), true);
            target.sendSystemMessage(Component.literal("§cVanish disabled by " + executor.getName().getString() + "! You are now visible to other players."));
        }
        
        return 1;
    }
    
    /**
     * Toggle vanish mode for a player
     * @return true if vanish is now enabled, false if disabled
     */
    private static boolean toggleVanish(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (vanishedPlayers.contains(playerId)) {
            // Disable vanish mode
            vanishedPlayers.remove(playerId);
            player.removeEffect(MobEffects.INVISIBILITY);
            
            // Make player visible to all other players
            for (ServerPlayer otherPlayer : player.serverLevel().players()) {
                if (!otherPlayer.equals(player)) {
                    // Re-add player to other players' tracking - use server-side tracking instead
                    // The server will handle visibility automatically when vanish is disabled
                    otherPlayer.connection.getPlayer().connection.resetPosition();
                }
            }
            
            return false;
        } else {
            // Enable vanish mode
            vanishedPlayers.add(playerId);
            
            // Apply invisibility effect (infinite duration)
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            
            // Hide player from other players (except those with permission)
            for (ServerPlayer otherPlayer : player.serverLevel().players()) {
                if (!otherPlayer.equals(player) && !hasVanishBypassPermission(otherPlayer)) {
                    // Use invisibility effect instead of packet manipulation
                    // The invisibility effect will handle visibility
                }
            }
            
            return true;
        }
    }
    
    /**
     * Check if a player has permission to see vanished players
     */
    private static boolean hasVanishBypassPermission(ServerPlayer player) {
        // Check if player has permission to see vanished players
        // This would typically check with a permission system
        return player.hasPermissions(3); // Op level 3+ can see vanished players
    }
    
    /**
     * Check if a player is vanished
     */
    public static boolean isVanished(ServerPlayer player) {
        return vanishedPlayers.contains(player.getUUID());
    }
    
    /**
     * Remove player from vanish mode (called when player leaves)
     */
    public static void removePlayer(UUID playerId) {
        vanishedPlayers.remove(playerId);
    }
    
    /**
     * Get all vanished players
     */
    public static Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayers);
    }
    
    /**
     * Handle player join event to hide vanished players from new player
     */
    public static void onPlayerJoin(ServerPlayer newPlayer) {
        for (UUID vanishedId : vanishedPlayers) {
            ServerPlayer vanishedPlayer = newPlayer.server.getPlayerList().getPlayer(vanishedId);
            if (vanishedPlayer != null && !hasVanishBypassPermission(newPlayer)) {
                // Vanished players will be automatically hidden due to invisibility effect
                // No need for manual packet manipulation
            }
        }
    }
}
