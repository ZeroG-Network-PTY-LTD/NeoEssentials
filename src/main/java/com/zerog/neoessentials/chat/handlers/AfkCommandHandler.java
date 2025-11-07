package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tracks command usage to detect player activity for AFK system.
 * Excludes certain commands that shouldn't reset AFK status.
 */
@EventBusSubscriber(modid = "neoessentials")
public class AfkCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfkCommandHandler.class);
    
    // Commands that should NOT reset AFK status (now loaded from config via AfkManager)
    
    /**
     * Track command execution as player activity
     */
    @SubscribeEvent
    public static void onCommandExecute(CommandEvent event) {
        // Check if command source is a player
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            String commandName = getCommandName(event.getParseResults().getReader().getString());
            
            // Skip excluded commands (from config)
            if (AfkManager.getInstance().getExcludedCommands().contains(commandName.toLowerCase())) {
                LOGGER.debug("Command '{}' excluded from AFK activity tracking for {}", 
                    commandName, player.getName().getString());
                return;
            }
            
            // Update activity for non-excluded commands
            AfkManager.getInstance().updateActivity(player.getUUID());
            LOGGER.debug("Command activity tracked for {}: /{}", 
                player.getName().getString(), commandName);
        }
    }
    
    /**
     * Extract the base command name from the full command string
     */
    private static String getCommandName(String fullCommand) {
        if (fullCommand == null || fullCommand.isEmpty()) {
            return "";
        }
        
        // Remove leading slash if present
        String command = fullCommand.startsWith("/") ? fullCommand.substring(1) : fullCommand;
        
        // Extract just the command name (before first space)
        int spaceIndex = command.indexOf(' ');
        return spaceIndex != -1 ? command.substring(0, spaceIndex) : command;
    }
}