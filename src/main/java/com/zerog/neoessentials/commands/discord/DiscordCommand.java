package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub Discord Command for compatibility
 */
public class DiscordCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("Discord command registration called but Discord integration is disabled");
        // No command registration - Discord features are disabled
    }
}
