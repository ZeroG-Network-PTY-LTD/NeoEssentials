package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.economy.ShopCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main command manager for NeoEssentials
 * Registers all essential commands similar to EssentialsX
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
        
        // Core teleportation commands
        // HomeCommandNew.register(dispatcher); // DISABLED - Removed problematic command
        SpawnCommand.register(dispatcher);
        WarpCommand.register(dispatcher);
        TpaCommand.register(dispatcher);
        TeleportCommand.register(dispatcher);
        BackCommand.register(dispatcher);
        
        // Essential admin commands
        ItemCommand.register(dispatcher, context);
        RepairCommand.register(dispatcher, context);
        
        // Kit system
        KitCommand.register(dispatcher);
        
        // Messaging system
        MessageCommand.register(dispatcher);
        ReplyCommand.register(dispatcher);
        MailCommand.register(dispatcher);
        
        // Economy commands
    // Economy commands
    BalanceCommand.register(dispatcher);
    PayCommand.register(dispatcher);
    EconomyCommand.register(dispatcher);
        
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
        
        // Utility commands
        NickCommand.register(dispatcher);
        FeedCommand.register(dispatcher);
        HealCommand.register(dispatcher);
        GodCommand.register(dispatcher);
        VanishCommand.register(dispatcher);
        FlyCommand.register(dispatcher);
        SpeedCommand.register(dispatcher);
        GiveCommand.register(dispatcher, context);
        TimeCommand.register(dispatcher);
        WeatherCommand.register(dispatcher);
    WorkbenchCommand.register(dispatcher);
    AnvilCommand.register(dispatcher);
    EnderChestCommand.register(dispatcher);
    InvSeeCommand.register(dispatcher);
    SmithingCommand.register(dispatcher);
    StonecutterCommand.register(dispatcher);
        
        // Text file commands
        MotdCommand.register(dispatcher);
        RulesCommand.register(dispatcher);
        HelpCommand.register(dispatcher);
        InfoCommand.register(dispatcher);
        
        // Shop commands
        CreateShopCommand.register(dispatcher);
        ShopCommand.register(dispatcher);
        
        LOGGER.info("Registered {} essentials commands", 
            dispatcher.getRoot().getChildren().size());
    }
}
