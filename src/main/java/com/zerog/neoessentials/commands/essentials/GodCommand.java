package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
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
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.GOD_SELF))
            .executes(GodCommand::toggleGodSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.GOD_OTHERS))
                .executes(GodCommand::toggleGodOther)
            )
        );
        // Alias: /g
        dispatcher.register(Commands.literal("g")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.GOD_SELF))
            .executes(GodCommand::toggleGodSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.GOD_OTHERS))
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
                    source.sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(player, "neoessentials.god.enabled_self"), false);
                } else {
                    source.sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(player, "neoessentials.god.disabled_self"), false);
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
                if (isGodMode) {
                    source.sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(executor, "neoessentials.god.enabled_other", target.getName().getString()), true);
                    target.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.translatable(target, "neoessentials.god.enabled_by_other", executor.getName().getString()));
                } else {
                    source.sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(executor, "neoessentials.god.disabled_other", target.getName().getString()), true);
                    target.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.translatable(target, "neoessentials.god.disabled_by_other", executor.getName().getString()));
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