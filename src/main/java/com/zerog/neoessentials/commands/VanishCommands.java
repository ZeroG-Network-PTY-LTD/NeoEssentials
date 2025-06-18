package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the vanish command and functionality to make players invisible to others.
 */
public class VanishCommands {

    // Store vanished players
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * Constructor to register event handlers
     */
    public VanishCommands() {
        // Register event handlers
        NeoForge.EVENT_BUS.register(this);
    }
    
    /**
     * Register all vanish commands
     * 
     * @param dispatcher Command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /vanish [player]
        dispatcher.register(Commands.literal("vanish")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.vanish"))
            .executes(context -> toggleVanish(context, null))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.vanish.others"))
                .executes(context -> toggleVanish(
                    context,
                    EntityArgument.getPlayer(context, "player")
                ))
            )
        );
        
        // /v (alias for /vanish)
        dispatcher.register(Commands.literal("v")
            .requires(source -> PermissionUtil.hasPermission(source, "essentials.vanish"))
            .executes(context -> toggleVanish(context, null))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, "essentials.vanish.others"))
                .executes(context -> toggleVanish(
                    context,
                    EntityArgument.getPlayer(context, "player")
                ))
            )
        );
    }
    
    /**
     * Toggle vanish status for a player
     */
    private int toggleVanish(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        try {
            ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
            
            // If no target is provided, use the source player
            if (targetPlayer == null) {
                targetPlayer = sourcePlayer;
            }
            
            UUID targetId = targetPlayer.getUUID();
            boolean isNowVanished;
            
            if (vanishedPlayers.contains(targetId)) {
                // Player is vanished, make them visible
                vanishedPlayers.remove(targetId);
                isNowVanished = false;
                  // Make player visible to all players
                targetPlayer.setInvisible(false);
                
                // Update visibility for all online players
                updateVanishStatusForPlayer(targetPlayer);
            } else {                // Make player vanished
                vanishedPlayers.add(targetId);
                isNowVanished = true;
                
                // Make the player invisible
                targetPlayer.setInvisible(true);
                
                // Update visibility for all online players
                updateVanishStatusForPlayer(targetPlayer);
            }
            
            // Send a message to the player who toggled vanish
            if (targetPlayer.equals(sourcePlayer)) {
                sourcePlayer.sendSystemMessage(
                    Component.literal(isNowVanished ? "You are now vanished." : "You are no longer vanished.")
                );
            } else {
                sourcePlayer.sendSystemMessage(
                    Component.literal(targetPlayer.getScoreboardName() + 
                                  (isNowVanished ? " is now vanished." : " is no longer vanished."))
                );
                
                targetPlayer.sendSystemMessage(
                    Component.literal(sourcePlayer.getScoreboardName() + 
                                  " has " + (isNowVanished ? "vanished" : "un-vanished") + " you.")
                );
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("You must be a player to use this command."));
            return 0;
        }
    }
    
    /**
     * Check if a player is vanished
     */
    public boolean isVanished(UUID playerId) {
        return vanishedPlayers.contains(playerId);
    }
    
    /**
     * Get all currently vanished players
     */
    public Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayers);
    }
    
    /**
     * Check if a player can see vanished players
     */
    public boolean canSeeVanished(ServerPlayer player) {
        return PermissionUtil.hasPermission(player, "essentials.vanish.see");
    }
      /**
     * Update the vanish status for a player
     */
    public void updateVanishStatusForPlayer(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        
        boolean isVanished = isVanished(player.getUUID());
        
        // Set player invisibility based on vanish status
        player.setInvisible(isVanished);
        
        // Update visibility for other players
        for (ServerPlayer otherPlayer : player.getServer().getPlayerList().getPlayers()) {
            if (player.equals(otherPlayer)) continue;
            
            if (isVanished && !canSeeVanished(otherPlayer)) {
                // Hide the player from this player
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(
                    java.util.List.of(player.getUUID())
                ));
            } else {
                // Ensure player is visible to this player
                java.util.List<net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry> entries = 
                    java.util.List.of(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry(
                        player.getUUID(), player.getGameProfile(), true, 
                        player.connection.latency, player.gameMode.getGameModeForPlayer(), 
                        player.getDisplayName(), null
                    ));
                otherPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                    java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), entries
                ));
            }
        }
    }
    
    /**
     * Handle player login to update vanish status
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Update the vanish status for the player who just logged in
        updateVanishStatusForPlayer(player);
        
        // Hide all vanished players from this player if they can't see vanished
        if (!canSeeVanished(player)) {
            for (ServerPlayer otherPlayer : player.getServer().getPlayerList().getPlayers()) {
                if (isVanished(otherPlayer.getUUID())) {
                    player.connection.send(
                        new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                            net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.REMOVE_PLAYER,
                            otherPlayer
                        )
                    );
                }
            }
        }
    }
}
