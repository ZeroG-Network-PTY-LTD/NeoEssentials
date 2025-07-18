package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.commands.essentials.EssentialsCommands;
import com.zerog.neoessentials.commands.economy.EconomyCommands;
import com.zerog.neoessentials.commands.homes.HomeCommands;
import com.zerog.neoessentials.commands.warps.WarpCommands;
import com.zerog.neoessentials.commands.kits.KitCommands;
import com.zerog.neoessentials.commands.moderation.ModerationCommands;
import com.zerog.neoessentials.commands.messaging.MessagingCommands;
import com.zerog.neoessentials.commands.teleport.TeleportCommands;
import com.zerog.neoessentials.commands.utility.UtilityCommands;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Central command manager for NeoEssentials
 * 
 * Handles registration and coordination of all mod commands including:
 * - EssentialsX-style commands (/spawn, /nick, /msg, etc.)
 * - Economy commands (/balance, /pay, /eco, etc.)
 * - Home/Warp commands (/home, /sethome, /warp, etc.)
 * - Moderation commands (/kick, /ban, /mute, etc.)
 * - Utility commands (/tpa, /back, /kit, etc.)
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@EventBusSubscriber(modid = NeoEssentials.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class CommandManager {
    
    private final NeoEssentials mod;
    
    public CommandManager(NeoEssentials mod) {
        this.mod = mod;
    }
    
    /**
     * Register all commands when the server starts
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        NeoEssentials.LOGGER.info("Registering NeoEssentials commands...");
        
        try {
            // Register essential commands
            EssentialsCommands.register(dispatcher);
            
            // Register economy commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("economy")) {
                EconomyCommands.register(dispatcher);
            }
            
            // Register home commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("homes")) {
                HomeCommands.register(dispatcher);
            }
            
            // Register warp commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("warps")) {
                WarpCommands.register(dispatcher);
            }
            
            // Register kit commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("kits")) {
                KitCommands.register(dispatcher);
            }
            
            // Register moderation commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("moderation")) {
                ModerationCommands.register(dispatcher);
            }
            
            // Register messaging commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("messaging")) {
                MessagingCommands.register(dispatcher);
            }
            
            // Register teleport commands if enabled
            if (NeoEssentials.getInstance().getFeatureManager().isFeatureEnabled("teleport")) {
                TeleportCommands.register(dispatcher);
            }
            
            // Register utility commands
            UtilityCommands.register(dispatcher);
            
            NeoEssentials.LOGGER.info("Successfully registered all NeoEssentials commands!");
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to register commands", e);
        }
    }
    
    /**
     * Check if a command source has a specific permission
     * 
     * @param source The command source to check
     * @param permission The permission node to check
     * @return true if the source has the permission, false otherwise
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        return hasPermission(source, permission, 2);
    }
    
    /**
     * Check if a command source has a specific permission with a custom level
     * 
     * @param source The command source to check
     * @param permission The permission node to check
     * @param level The minimum permission level required
     * @return true if the source has the permission, false otherwise
     */
    public static boolean hasPermission(CommandSourceStack source, String permission, int level) {
        try {
            // For now, use basic permission level checking
            // This can be extended to integrate with permission mods later
            return source.hasPermission(level);
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Error checking permission {} (level {}) for source: {}", 
                permission, level, e.getMessage());
            return false;
        }
    }
}
