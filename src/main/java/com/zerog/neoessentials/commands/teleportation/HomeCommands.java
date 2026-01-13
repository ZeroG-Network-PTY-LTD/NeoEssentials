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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeCommands {
    // Track pending delete confirmations: player UUID -> home name
    private static final Map<UUID, String> pendingDeleteConfirmations = new ConcurrentHashMap<>();
    
    // Track pending sethome overwrite confirmations: player UUID -> home name
    private static final Map<UUID, String> pendingSetHomeConfirmations = new ConcurrentHashMap<>();

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
                .then(Commands.literal("confirm")
                    .executes(HomeCommands::executeSetHomeConfirm)
                )
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
                .then(Commands.literal("confirm")
                    .executes(HomeCommands::executeDelHomeConfirm)
                )
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
        // Jail escape prevention
        com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();
        com.zerog.neoessentials.moderation.JailManager jailManager = com.zerog.neoessentials.moderation.JailManager.getInstance();
        if (config.isPreventJailEscapeEnabled() && jailManager.isPlayerJailed(player.getUUID())) {
            context.getSource().sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.jail.prevent_escape"));
            return 0;
        }
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
        // Jail escape prevention
        com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();
        com.zerog.neoessentials.moderation.JailManager jailManager = com.zerog.neoessentials.moderation.JailManager.getInstance();
        if (config.isPreventJailEscapeEnabled() && jailManager.isPlayerJailed(player.getUUID())) {
            context.getSource().sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.jail.prevent_escape"));
            return 0;
        }
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
        
        // If home exists, require confirmation
        if (homeManager.getHome(player, homeName) != null) {
            // Check if already pending confirmation for this home
            String pending = pendingSetHomeConfirmations.get(player.getUUID());
            if (pending != null && pending.equals(homeName)) {
                player.sendSystemMessage(MessageUtil.warning("You have already requested to overwrite home '" + homeName + "'. Use /sethome " + homeName + " confirm to overwrite."));
                return 0;
            }
            // Set pending confirmation
            pendingSetHomeConfirmations.put(player.getUUID(), homeName);
            player.sendSystemMessage(MessageUtil.warning("Home '" + homeName + "' already exists. Use /sethome " + homeName + " confirm to overwrite."));
            return 0;
        }

        if (homeManager.setHome(player, homeName)) {
            return 1;
        }
        return 0;
    }

    /**
     * Execute /sethome <name> confirm
     */
    private static int executeSetHomeConfirm(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        
        // Check if there's a pending confirmation for this home
        String pending = pendingSetHomeConfirmations.get(player.getUUID());
        if (pending == null || !pending.equals(homeName)) {
            player.sendSystemMessage(MessageUtil.error("No pending overwrite confirmation for home '" + homeName + "'. Use /sethome " + homeName + " first."));
            return 0;
        }

        // Remove pending confirmation
        pendingSetHomeConfirmations.remove(player.getUUID());

        // Set the home (this will overwrite the existing one)
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
        ConfigManager config = ConfigManager.getInstance();

        if (config.isRequireConfirmationForDeleteEnabled()) {
            // If already pending confirmation for this home, prompt to use confirm
            String pending = pendingDeleteConfirmations.get(player.getUUID());
            if (pending != null && pending.equals(homeName)) {
                player.sendSystemMessage(MessageUtil.warning("You have already requested to delete home '" + homeName + "'. Use /delhome " + homeName + " confirm to confirm deletion."));
                return 0;
            }
            // Set pending confirmation
            pendingDeleteConfirmations.put(player.getUUID(), homeName);
            player.sendSystemMessage(MessageUtil.warning("Are you sure you want to delete home '" + homeName + "'? Use /delhome " + homeName + " confirm to confirm deletion."));
            return 0;
        }

        // No confirmation required, delete immediately
        if (homeManager.deleteHome(player, homeName)) {
            return 1;
        }
        return 0;
    }

    /**
     * Execute /delhome <name> confirm
     */
    private static int executeDelHomeConfirm(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        ConfigManager config = ConfigManager.getInstance();

        if (!config.isRequireConfirmationForDeleteEnabled()) {
            player.sendSystemMessage(MessageUtil.error("Confirmation is not required for home deletion."));
            return 0;
        }

        String pending = pendingDeleteConfirmations.get(player.getUUID());
        if (pending == null || !pending.equals(homeName)) {
            player.sendSystemMessage(MessageUtil.error("No pending delete confirmation for home '" + homeName + "'. Use /delhome " + homeName + " first."));
            return 0;
        }

        // Remove pending confirmation
        pendingDeleteConfirmations.remove(player.getUUID());
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