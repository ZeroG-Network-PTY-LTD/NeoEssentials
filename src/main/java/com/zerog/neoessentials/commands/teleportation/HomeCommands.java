package com.zerog.neoessentials.commands.teleportation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.teleportation.HomeManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for the home teleportation system:
 * - /home [name] - Teleport to home
 * - /sethome <name> - Set a home
 * - /delhome <name> - Delete a home  
 * - /homes - List all homes
 */
public class HomeCommands {
    
    // Permission nodes for home commands (matching PermissionRegistry)
    private static final String PERMISSION_HOME = "neoessentials.teleport.home";
    private static final String PERMISSION_SETHOME = "neoessentials.teleport.home.set";
    private static final String PERMISSION_DELHOME = "neoessentials.teleport.home.delete";
    private static final String PERMISSION_HOMES = "neoessentials.teleport.home.list";
    
    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            HomeManager homeManager = HomeManager.getInstance();
            return SharedSuggestionProvider.suggest(homeManager.getHomeNames(player), builder);
        }
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        // Only register if teleportation module is enabled
        if (config.isTeleportationEnabled()) {
            // Register individual commands based on their command settings
            if (config.isCommandEnabled("home")) {
                registerHomeCommand(dispatcher);
            }
            if (config.isCommandEnabled("sethome")) {
                registerSetHomeCommand(dispatcher);
            }
            if (config.isCommandEnabled("delhome")) {
                registerDelHomeCommand(dispatcher);
            }
            if (config.isCommandEnabled("listhomes")) {
                registerHomesCommand(dispatcher);
            }
        }
    }
    
    /**
     * Register /home [name] command
     */
    private static void registerHomeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register main command
        registerHomeCommandWithName(dispatcher, "home");
        // Register alias
        registerHomeCommandWithName(dispatcher, "h");
    }
    
    private static void registerHomeCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_HOME);
                }
                return false; // Console can't use homes
            })
            .executes(HomeCommands::executeHomeDefault)
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HOME_SUGGESTIONS)
                .executes(HomeCommands::executeHome)
            )
        );
    }
    
    /**
     * Register /sethome <name> command with aliases
     */
    private static void registerSetHomeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerSetHomeCommandWithName(dispatcher, "sethome");
        registerSetHomeCommandWithName(dispatcher, "createhome");
    }
    
    private static void registerSetHomeCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETHOME);
                }
                return false; // Console can't use homes
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommands::executeSetHome)
            )
        );
    }
    
    /**
     * Register /delhome <name> command with aliases
     */
    private static void registerDelHomeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerDelHomeCommandWithName(dispatcher, "delhome");
        registerDelHomeCommandWithName(dispatcher, "deletehome");
        registerDelHomeCommandWithName(dispatcher, "removehome");
        registerDelHomeCommandWithName(dispatcher, "rhome");
    }
    
    private static void registerDelHomeCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_DELHOME);
                }
                return false; // Console can't use homes
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HOME_SUGGESTIONS)
                .executes(HomeCommands::executeDelHome)
            )
        );
    }
    
    /**
     * Register /homes command with aliases
     */
    private static void registerHomesCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerHomesCommandWithName(dispatcher, "homes");
        registerHomesCommandWithName(dispatcher, "listhomes");
        registerHomesCommandWithName(dispatcher, "homelist");
    }
    
    private static void registerHomesCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_HOMES);
                }
                return false; // Console can't use homes
            })
            .executes(HomeCommands::executeHomes)
        );
    }
    
    /**
     * Execute /home (go to default home)
     */
    private static int executeHomeDefault(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        HomeManager homeManager = HomeManager.getInstance();
        
        if (!homeManager.hasHomes(player)) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.home.none_set"));
            return 0;
        }
        
        homeManager.teleportToDefaultHome(player);
        return 1;
    }
    
    /**
     * Execute /home <name>
     */
    private static int executeHome(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        
        homeManager.teleportToHome(player, homeName);
        return 1;
    }
    
    /**
     * Execute /sethome <name>
     */
    private static int executeSetHome(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        
        if (homeManager.setHome(player, homeName)) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Execute /delhome <name>
     */
    private static int executeDelHome(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        
        if (homeManager.deleteHome(player, homeName)) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Execute /homes
     */
    private static int executeHomes(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        HomeManager homeManager = HomeManager.getInstance();
        
        String homesList = homeManager.getFormattedHomesList(player);
        player.sendSystemMessage(MessageUtil.component(homesList));
        return 1;
    }
}