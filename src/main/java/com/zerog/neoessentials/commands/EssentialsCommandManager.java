package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.commands.util.CommandRegistryManager;
import com.zerog.neoessentials.economy.ShopCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main command manager for NeoEssentials
 * Registers all essential commands using the standardized registration system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EssentialsCommandManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EssentialsCommandManager.class);
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var context = event.getBuildContext();
        
        LOGGER.info("Registering essentials commands...");
        
        // Create command registry manager for organized registration
        CommandRegistryManager manager = new CommandRegistryManager(dispatcher, context);
        
        // Register essential utility commands
        registerEssentialUtilities(manager);
        
        // Register player management commands
        registerPlayerManagement(manager);
        
        // Register teleportation commands
        registerTeleportation(manager);
        
        // Register economy commands
        registerEconomy(manager);
        
        // Register communication commands
        registerCommunication(manager);
        
        // Print summary
        manager.printRegistrationSummary();
        
        LOGGER.info("All essentials commands registered successfully!");
    }
    
    private void registerEssentialUtilities(CommandRegistryManager manager) {
        manager.registerCommand("Essential Utilities", "heal", "Restore player health and hunger", 
                               HealCommand::register);
        manager.registerCommand("Essential Utilities", "feed", "Restore player hunger and saturation", 
                               FeedCommand::register);
        manager.registerCommand("Essential Utilities", "god", "Toggle invulnerability mode", 
                               GodCommand::register);
        manager.registerCommand("Essential Utilities", "vanish", "Toggle visibility to other players", 
                               VanishCommand::register);
        manager.registerCommand("Essential Utilities", "fly", "Toggle flight ability", 
                               FlyCommand::register);
        manager.registerCommand("Essential Utilities", "speed", "Modify player movement speed", 
                               SpeedCommand::register);
        manager.registerCommand("Essential Utilities", "afk", "Toggle away-from-keyboard status", 
                               AFKCommand::register);
        manager.registerCommand("Essential Utilities", "gamemode", "Change player gamemode", 
                               GameModeCommand::register);
        manager.registerCommand("Essential Utilities", "repair", "Repair items and equipment", 
                               (dispatcher, context) -> RepairCommand.register(dispatcher, context));
        manager.registerCommand("Essential Utilities", "give", "Give items to players", 
                               (dispatcher, context) -> GiveCommand.register(dispatcher, context));
        manager.registerCommand("Essential Utilities", "item", "Advanced item management", 
                               (dispatcher, context) -> ItemCommand.register(dispatcher, context));
    }
    
    private void registerPlayerManagement(CommandRegistryManager manager) {
        manager.registerCommand("Player Management", "list", "List online players", 
                               ListCommand::register);
        manager.registerCommand("Player Management", "whois", "Get player information", 
                               WhoisCommand::register);
        manager.registerCommand("Player Management", "seen", "Check when player was last online", 
                               SeenCommand::register);
        manager.registerCommand("Player Management", "nick", "Change player nickname", 
                               NickCommand::register);
        manager.registerCommand("Player Management", "invsee", "View player inventory", 
                               InvSeeCommand::register);
    }
    
    private void registerTeleportation(CommandRegistryManager manager) {
        manager.registerCommand("Teleportation", "spawn", "Teleport to spawn location", 
                               SpawnCommand::register);
        manager.registerCommand("Teleportation", "warp", "Teleport to predefined locations", 
                               WarpCommand::register);
        manager.registerCommand("Teleportation", "tpa", "Request teleportation to another player", 
                               TpaCommand::register);
        manager.registerCommand("Teleportation", "tp", "Teleport to players or coordinates", 
                               TeleportCommand::register);
        manager.registerCommand("Teleportation", "back", "Return to previous location", 
                               BackCommand::register);
    }
    
    private void registerEconomy(CommandRegistryManager manager) {
        manager.registerCommand("Economy", "balance", "Check player balance", 
                               BalanceCommand::register);
        manager.registerCommand("Economy", "eco", "Economy administration commands", 
                               EconomyCommand::register);
        manager.registerCommand("Economy", "kit", "Access predefined item kits", 
                               KitCommand::register);
        manager.registerCommand("Economy", "shop", "Manage server shops", 
                               ShopCommand::register);
    }
    
    private void registerCommunication(CommandRegistryManager manager) {
        manager.registerCommand("Communication", "msg", "Send private message to player", 
                               MessageCommand::register);
        manager.registerCommand("Communication", "reply", "Reply to last received message", 
                               ReplyCommand::register);
        manager.registerCommand("Communication", "mail", "Offline messaging system", 
                               MailCommand::register);
    }
}
