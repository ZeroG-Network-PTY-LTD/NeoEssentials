package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.NeoEssentialsCompat;
import com.zerog.neoessentials.commands.essentials.*;
import com.zerog.neoessentials.commands.discord.DiscordCommand;
import com.zerog.neoessentials.commands.GuiCommand;
import com.zerog.neoessentials.commands.NeoEssentialsCommand;
import com.zerog.neoessentials.config.EssentialsConfig;
import com.neoessentials.api.home.HomeService;
import com.neoessentials.language.LanguageManager;

import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
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
    
    private final EssentialsConfig config;
    private HomeService homeService;
    private LanguageManager languageManager;
    
    public EssentialsCommandManager(EssentialsConfig config) {
        this.config = config;
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Essentials command manager initialized");
    }
    
    /**
     * Initialize services for commands that need them
     */
    public void initializeServices(HomeService homeService, LanguageManager languageManager) {
        this.homeService = homeService;
        this.languageManager = languageManager;
        
        // Initialize the functional home commands with services
        HomeCommandNew.initialize(homeService, languageManager);
        
        LOGGER.info("Command services initialized");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        var context = event.getBuildContext();
        
        LOGGER.info("Registering essentials commands...");
        
        // Core teleportation commands
        HomeCommandNew.register(dispatcher);
        SpawnCommand.register(dispatcher);
        WarpCommand.register(dispatcher);
        TpaCommand.register(dispatcher);
        TeleportCommand.register(dispatcher);
        BackCommand.register(dispatcher);
        
        // Essential admin commands
        ItemCommand.register(dispatcher, context);
        RepairCommand.register(dispatcher, context);
        NotificationCommand.register(dispatcher);
        
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
        
        // Text file commands
        MotdCommand.register(dispatcher);
        RulesCommand.register(dispatcher);
        HelpCommand.register(dispatcher);
        InfoCommand.register(dispatcher);
        
        // Discord integration commands
        DiscordCommand.register(dispatcher);
        
        // GUI commands
        GuiCommand.register(dispatcher);
        
        // Main info command
        NeoEssentialsCommand.register(dispatcher);
        
        LOGGER.info("Registered {} essentials commands", 
            dispatcher.getRoot().getChildren().size());
    }
}
