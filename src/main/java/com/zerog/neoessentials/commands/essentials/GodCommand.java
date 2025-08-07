package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * God command implementation - /god [player]
 * Toggles invincibility (god mode) for players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GodCommand {
    
    // Set to track players in god mode
    private static final Set<UUID> godModePlayers = new HashSet<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /god - Toggle god mode for yourself
        dispatcher.register(Commands.literal("god")
            .requires(source -> source.hasPermission(2))
            .executes(GodCommand::toggleGodSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(GodCommand::toggleGodOther)
            )
        );
    }
    
    /**
     * Toggle god mode for the command executor
     */
    private static int toggleGodSelf(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "toggle god mode",
            "neoessentials.god", 
            (source) -> {
                ServerPlayer player = source.getPlayerOrException();
                boolean isGodMode = toggleGodMode(player);
                
                if (isGodMode) {
                    source.sendSuccess(() -> Component.literal("§a⚡ God mode enabled! You are now invincible and untouchable."), false);
                } else {
                    source.sendSuccess(() -> Component.literal("§c🛡️ God mode disabled! You are now mortal again."), false);
                }
                
                return 1;
            }
        );
    }
    
    /**
     * Toggle god mode for another player
     */
    private static int toggleGodOther(CommandContext<CommandSourceStack> context) {
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "toggle god mode for others",
            "neoessentials.god.others", 
            (source) -> {
                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                ServerPlayer executor = source.getPlayerOrException();
                
                boolean isGodMode = toggleGodMode(target);
                
                // Send confirmation to both players
                if (isGodMode) {
                    source.sendSuccess(() -> Component.literal("§a⚡ God mode enabled for " + target.getName().getString() + "!"), true);
                    target.sendSystemMessage(Component.literal("§a⚡ God mode enabled by " + executor.getName().getString() + "! You are now invincible and untouchable."));
                } else {
                    source.sendSuccess(() -> Component.literal("§c🛡️ God mode disabled for " + target.getName().getString() + "!"), true);
                    target.sendSystemMessage(Component.literal("§c🛡️ God mode disabled by " + executor.getName().getString() + "! You are now mortal again."));
                }
                
                return 1;
            }
        );
    }
    
    /**
     * Toggle god mode for a player
     * @return true if god mode is now enabled, false if disabled
     */
    private static boolean toggleGodMode(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        if (godModePlayers.contains(playerId)) {
            // Disable god mode
            godModePlayers.remove(playerId);
            player.setInvulnerable(false);
            return false;
        } else {
            // Enable god mode
            godModePlayers.add(playerId);
            player.setInvulnerable(true);
            return true;
        }
    }
    
    /**
     * Check if a player is in god mode
     */
    public static boolean isInGodMode(ServerPlayer player) {
        return godModePlayers.contains(player.getUUID());
    }
    
    /**
     * Remove player from god mode (called when player leaves)
     */
    public static void removePlayer(UUID playerId) {
        godModePlayers.remove(playerId);
    }
    
    /**
     * Get all players currently in god mode
     */
    public static Set<UUID> getGodModePlayers() {
        return new HashSet<>(godModePlayers);
    }
}
