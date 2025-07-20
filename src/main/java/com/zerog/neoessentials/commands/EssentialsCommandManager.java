package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.config.EssentialsConfig;

import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Main command manager for NeoEssentials
 * Registers all essential commands similar to EssentialsX
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EssentialsCommandManager {
    
    private final EssentialsConfig config;
    
    public EssentialsCommandManager(EssentialsConfig config) {
        this.config = config;
        NeoForge.EVENT_BUS.register(this);
        NeoEssentials.LOGGER.info("Essentials command manager initialized");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        
        NeoEssentials.LOGGER.info("Registering essentials commands...");
        
        // Core teleportation commands
        HomeCommand.register(dispatcher);
        SpawnCommand.register(dispatcher);
        WarpCommand.register(dispatcher);
        TpaCommand.register(dispatcher);
        TeleportCommand.register(dispatcher);
        BackCommand.register(dispatcher);
        
        // Kit system
        KitCommand.register(dispatcher);
        
        // Messaging system
        MessageCommand.register(dispatcher);
        ReplyCommand.register(dispatcher);
        MailCommand.register(dispatcher);
        
        // Economy commands
        if (config.economy.enabled) {
            BalanceCommand.register(dispatcher);
            PayCommand.register(dispatcher);
            EconomyCommand.register(dispatcher);
        }
        
        // Moderation commands
        KickCommand.register(dispatcher);
        BanCommand.register(dispatcher);
        TempBanCommand.register(dispatcher);
        MuteCommand.register(dispatcher);
        
        // Information commands
        WhoisCommand.register(dispatcher);
        SeenCommand.register(dispatcher);
        ListCommand.register(dispatcher);
        
        // Admin commands
        GameModeCommand.register(dispatcher);
        SpawnerCommand.register(dispatcher);
        
        // Utility commands
        NickCommand.register(dispatcher);
        FeedCommand.register(dispatcher);
        HealCommand.register(dispatcher);
        
        // Text file commands
        MotdCommand.register(dispatcher);
        RulesCommand.register(dispatcher);
        HelpCommand.register(dispatcher);
        InfoCommand.register(dispatcher);
        
        NeoEssentials.LOGGER.info("Registered {} essentials commands", 
            dispatcher.getRoot().getChildren().size());
    }
}
