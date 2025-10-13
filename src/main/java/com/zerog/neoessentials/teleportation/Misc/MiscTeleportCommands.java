package com.zerog.neoessentials.teleportation.Misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands for miscellaneous teleportation: /back, /top, /jump, /jumpto, /tpr
 */
public class MiscTeleportCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscTeleportCommands.class);
    
    // Permission nodes for misc teleportation
    private static final String PERMISSION_BACK = "neoessentials.teleport.back";
    private static final String PERMISSION_TOP = "neoessentials.teleport.top";
    private static final String PERMISSION_JUMP = "neoessentials.teleport.jump";
    private static final String PERMISSION_JUMPTO = "neoessentials.teleport.jumpto";
    private static final String PERMISSION_TPR = "neoessentials.teleport.tpr";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        // Only register if teleportation module and back command are enabled
        if (config.isTeleportationEnabled() && config.isCommandEnabled("back")) {
            // /back - Teleport to previous location
        dispatcher.register(
            Commands.literal("back")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_BACK);
                    }
                    return false; // Console can't use teleportation
                })
                .executes(context -> executeBack(context))
        );
        
        // /top - Teleport to highest block above current position
        dispatcher.register(
            Commands.literal("top")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TOP);
                    }
                    return false; // Console can't use teleportation
                })
                .executes(context -> executeTop(context))
        );
        
        // /jump - Teleport to the block you're looking at
        dispatcher.register(
            Commands.literal("jump")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_JUMP);
                    }
                    return false; // Console can't use teleportation
                })
                .executes(context -> executeJump(context))
        );
        
        // /jumpto - Teleport to the block you're looking at (alias for /jump)
        dispatcher.register(
            Commands.literal("jumpto")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_JUMPTO);
                    }
                    return false; // Console can't use teleportation
                })
                .executes(context -> executeJump(context))
        );
        
        // /tpr - Random teleport
        dispatcher.register(
            Commands.literal("tpr")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TPR);
                    }
                    return false; // Console can't use teleportation
                })
                .executes(context -> executeTpr(context))
            );
            
            LOGGER.info("Registered misc teleport commands: /back, /top, /jump, /jumpto, /tpr");
        }
    }
    
    /**
     * Execute /back command
     */
    private static int executeBack(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            MiscTeleportManager manager = MiscTeleportManager.getInstance();
            boolean success = manager.teleportBack(player);
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /back", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /back command", e);
            return 0;
        }
    }
    
    /**
     * Execute /top command
     */
    private static int executeTop(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Note: teleportToTop method needs to be implemented in MiscTeleportManager
            // For now, show error message
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.not_implemented", "top"));
            boolean success = false;
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /top", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /top command", e);
            return 0;
        }
    }
    
    /**
     * Execute /jump command
     */
    private static int executeJump(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Note: teleportToLookingAt method needs to be implemented in MiscTeleportManager
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.not_implemented", "jump"));
            boolean success = false;
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /jump", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /jump command", e);
            return 0;
        }
    }
    
    /**
     * Execute /tpr command
     */
    private static int executeTpr(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Note: randomTeleport method needs to be implemented in MiscTeleportManager
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.misc.not_implemented", "tpr"));
            boolean success = false;
            
            return success ? 1 : 0;
            
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /tpr", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /tpr command", e);
            return 0;
        }
    }
}