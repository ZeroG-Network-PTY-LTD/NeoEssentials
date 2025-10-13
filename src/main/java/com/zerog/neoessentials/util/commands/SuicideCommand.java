package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the /suicide command - Allows players to kill themselves
 * Includes cooldown and confirmation features
 */
public class SuicideCommand {
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> CONFIRMATIONS = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 10000; // 10 seconds
    
    /**
     * Register the /suicide command with aliases
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("suicide")) return;
        
        // Register main command and aliases
        registerSuicideCommand(dispatcher, "suicide");
        registerSuicideCommand(dispatcher, "kill");
        registerSuicideCommand(dispatcher, "die");
    }
    
    private static void registerSuicideCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /suicide confirm - Confirm suicide after initial command
                .then(Commands.literal("confirm")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.suicide");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        return confirmSuicide(player);
                    })
                )
                // /suicide - Initial suicide command (requires confirmation)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.suicide");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    return initiateSuicide(player);
                })
        );
    }
    
    /**
     * Initiate suicide (first step - requires confirmation)
     */
    private static int initiateSuicide(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Check cooldown
        if (COOLDOWNS.containsKey(playerId)) {
            long timeLeft = (COOLDOWNS.get(playerId) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.suicide.cooldown", timeLeft));
                return 0;
            } else {
                COOLDOWNS.remove(playerId);
            }
        }
        
        // Check if player is already in creative or spectator mode
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.suicide.invalid_gamemode"));
            return 0;
        }
        
        // Set confirmation timeout
        CONFIRMATIONS.put(playerId, System.currentTimeMillis() + CONFIRMATION_TIMEOUT);
        
        // Send confirmation message
        player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.suicide.confirmation"));
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.suicide.confirm_instructions"));
        
        return 1;
    }
    
    /**
     * Confirm and execute suicide
     */
    private static int confirmSuicide(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Check if confirmation is valid
        if (!CONFIRMATIONS.containsKey(playerId)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.suicide.no_confirmation"));
            return 0;
        }
        
        long confirmationTime = CONFIRMATIONS.get(playerId);
        if (System.currentTimeMillis() > confirmationTime) {
            CONFIRMATIONS.remove(playerId);
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.suicide.confirmation_expired"));
            return 0;
        }
        
        // Remove confirmation
        CONFIRMATIONS.remove(playerId);
        
        // Set cooldown (default 30 seconds)
        long cooldownTime = 30 * 1000L; // 30 seconds default
        COOLDOWNS.put(playerId, System.currentTimeMillis() + cooldownTime);
        
        // Send final message
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.suicide.executing"));
        
        // Execute suicide with custom damage source
        try {
            DamageSource suicideDamage = new DamageSource(
                player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.GENERIC_KILL)
            );
            
            // Deal massive damage to ensure death
            player.hurt(suicideDamage, Float.MAX_VALUE);
            
            // If somehow they survived, set health to 0
            if (player.isAlive()) {
                player.setHealth(0.0f);
            }
            
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.suicide.failed"));
            return 0;
        }
    }
    
    /**
     * Clean up expired confirmations (should be called periodically)
     */
    public static void cleanupExpiredConfirmations() {
        long currentTime = System.currentTimeMillis();
        CONFIRMATIONS.entrySet().removeIf(entry -> currentTime > entry.getValue());
        COOLDOWNS.entrySet().removeIf(entry -> currentTime > entry.getValue());
    }
}