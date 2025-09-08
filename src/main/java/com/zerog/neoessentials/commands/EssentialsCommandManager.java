package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.commands.util.CommandRegistryManager;
import com.zerog.neoessentials.economy.ShopCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("NeoEssentials command registration deferred to server startup...");
        // Commands will be registered through the main mod class during server startup
        // This approach is preferred to avoid early initialization issues
    }
    
    /**
     * Static method to register all commands - called from main mod class during server startup
     */
    public static void registerAllCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher, net.minecraft.commands.CommandBuildContext context) {
        LOGGER.info("Registering essentials commands...");
        
        try {
            // Create command registry manager for organized registration
            CommandRegistryManager manager = new CommandRegistryManager(dispatcher, context);
            
            // Create instance to access instance methods
            EssentialsCommandManager instance = new EssentialsCommandManager();
            
            // Register essential utility commands
            instance.registerEssentialUtilities(manager);
            
            // Register player management commands
            instance.registerPlayerManagement(manager);
            
            // Register teleportation commands
            instance.registerTeleportation(manager);
            
            // Register economy commands
            instance.registerEconomy(manager);
            
            // Register communication commands
            instance.registerCommunication(manager);
            
            // Register administration commands  
            instance.registerAdministration(manager);
            
            // Print summary
            manager.printRegistrationSummary();
            
            LOGGER.info("All essentials commands registered successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize CommandRegistryManager: " + e.getMessage());
            LOGGER.info("Falling back to direct command registration...");
            // Fall back to direct registration if CommandRegistryManager fails
            EssentialsCommandManager instance = new EssentialsCommandManager();
            instance.registerCommandsDirect(dispatcher);
        }
    }
    
    private void registerCommandsDirect(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        LOGGER.info("Registering commands directly due to CommandRegistryManager issues...");
        try {
            // Register essential commands directly (those that don't need context)
            HealCommand.register(dispatcher);
            FeedCommand.register(dispatcher);
            GodCommand.register(dispatcher);
            VanishCommand.register(dispatcher);
            FlyCommand.register(dispatcher);
            // Add more as needed...
        } catch (Exception e) {
            LOGGER.error("Failed to register commands directly: " + e.getMessage());
        }
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
    
    private void registerAdministration(CommandRegistryManager manager) {
        // manager.registerCommand("Administration", "sudo", "Execute command as another player", 
        //                        SudoCommand::register);
        // manager.registerCommand("Administration", "powertool", "Bind commands to items", 
        //                        PowertoolCommand::register);
        manager.registerCommand("Administration", "kitadmin", "Advanced kit management", 
                               com.zerog.neoessentials.commands.admin.KitAdminCommand::register);
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
