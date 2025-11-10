package com.zerog.neoessentials.teleportation.Misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands for miscellaneous teleportation: /back
 * Note: /top, /jump, /jumpto, /tpr are implemented in DirectTeleportCommands
 */
public class MiscTeleportCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscTeleportCommands.class);
    
    // Permission nodes for misc teleportation
    private static final String PERMISSION_BACK = "neoessentials.teleport.back";
    
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
            
            LOGGER.info("Registered misc teleport commands: /back");
            LOGGER.info("Note: /top, /jump, /jumpto, /tpr are registered in DirectTeleportCommands");
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
}