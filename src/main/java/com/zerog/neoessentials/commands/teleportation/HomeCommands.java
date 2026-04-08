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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Commands for the home teleportation system:
 * - /home [name] - Teleport to home
 * - /sethome <name> - Set a home
 * - /delhome <name> - Delete a home  
 * - /homes - List all homes
 */

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
    private static final String PERMISSION_RENAMEHOME = "neoessentials.renamehome";
    private static final String PERMISSION_RENAMEHOME_OTHERS = "neoessentials.renamehome.others";

    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            HomeManager homeManager = HomeManager.getInstance();
            java.util.List<String> homeNames = homeManager.getHomeNames(player);
            // Debug logging for home suggestions
            if (com.zerog.neoessentials.config.ConfigManager.isDebugModeEnabled()) {
                System.out.println("[DEBUG] Home suggestions for " + player.getName().getString() + ": " + homeNames);
            }
            return SharedSuggestionProvider.suggest(homeNames, builder);
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
            if (config.isCommandEnabled("renamehome")) {
                registerRenameHomeCommand(dispatcher);
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
                    boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_HOME);
                    if (!hasPerm) {
                        return false;
                    }
                    return hasPerm;
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
                    boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETHOME);
                    if (!hasPerm) {
                        return false;
                    }
                    return hasPerm;
                }
                return false; // Console can't use homes
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(HomeCommands::executeSetHome)
                .then(Commands.literal("confirm")
                    .executes(HomeCommands::executeSetHomeConfirm)
                )
                .then(Commands.literal("deny")
                    .executes(HomeCommands::executeSetHomeDeny)
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
                    boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_DELHOME);
                    if (!hasPerm) {
                        return false;
                    }
                    return hasPerm;
                }
                return false; // Console can't use homes
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(HOME_SUGGESTIONS)
                .executes(HomeCommands::executeDelHome)
                .then(Commands.literal("confirm")
                    .executes(HomeCommands::executeDelHomeConfirm)
                )
                .then(Commands.literal("deny")
                    .executes(HomeCommands::executeDelHomeDeny)
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
                    boolean hasPerm = PermissionAPI.hasPermission(player.getUUID(), PERMISSION_HOMES);
                    if (!hasPerm) {
                        return false;
                    }
                    return hasPerm;
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
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("This command can only be used by players."));
            return 0;
        }
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
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("This command can only be used by players."));
            return 0;
        }
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
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.command.player_only"));
            return 0;
        }
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        // Enforce dynamic home limit
        int maxHomes = homeManager.getMaxHomesForPlayer(player);
        int currentHomes = homeManager.getHomeNames(player).size();
        if (homeManager.getHome(player, homeName) == null && currentHomes >= maxHomes) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.limit_exceeded", maxHomes));
            return 0;
        }
        // If home exists, require confirmation
        if (homeManager.getHome(player, homeName) != null) {
            String pending = pendingSetHomeConfirmations.get(player.getUUID());
            if (pending != null && pending.equals(homeName)) {
                player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.home.overwrite_already_pending", homeName));
                return 0;
            }
            pendingSetHomeConfirmations.put(player.getUUID(), homeName);
            player.sendSystemMessage(MessageUtil.homeConfirmComponent(
                homeName,
                "overwrite",
                "/sethome " + homeName + " confirm",
                "/sethome " + homeName + " deny"
            ));
            return 0;
        }
        pendingSetHomeConfirmations.remove(player.getUUID());
        if (!pendingSetHomeConfirmations.containsKey(player.getUUID())) {
            if (homeManager.setHome(player, homeName)) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.set", homeName, player.blockPosition().toShortString()));
                return 1;
            }
        }
        return 0;
    }

    /**
     * Execute /sethome <name> confirm
     */
    private static int executeSetHomeConfirm(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.command.player_only"));
            return 0;
        }
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        String pending = pendingSetHomeConfirmations.get(player.getUUID());
        if (pending == null || !pending.equals(homeName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.no_pending_overwrite", homeName));
            return 0;
        }
        pendingSetHomeConfirmations.remove(player.getUUID());
        boolean success = homeManager.setHome(player, homeName);
        if (success) {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.overwrite_success", homeName));
            return 1;
        } else {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.overwrite_failed", homeName));
            return 0;
        }
    }

    /**
     * Execute /sethome <name> deny
     */
    private static int executeSetHomeDeny(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.command.player_only"));
            return 0;
        }
        String homeName = StringArgumentType.getString(context, "name");
        String pending = pendingSetHomeConfirmations.get(player.getUUID());
        if (pending != null && pending.equals(homeName)) {
            pendingSetHomeConfirmations.remove(player.getUUID());
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.home.overwrite_cancelled", homeName));
            return 1;
        }
        player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.home.no_pending_overwrite", homeName));
        return 0;
    }

    /**
     * Execute /delhome <name>
     */
    private static int executeDelHome(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.command.player_only"));
            return 0;
        }
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        // Just delete directly - confirmation system is broken
        if (homeManager.deleteHome(player, homeName)) {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.delete_success", homeName));
            return 1;
        }
        player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.delete_failed", homeName));
        return 0;
    }

    /**
     * Execute /delhome <name> confirm
     */
    private static int executeDelHomeConfirm(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.command.player_only"));
            return 0;
        }
        String homeName = StringArgumentType.getString(context, "name");
        HomeManager homeManager = HomeManager.getInstance();
        ConfigManager config = ConfigManager.getInstance();
        // Guard: Only allow a single confirm, do not allow repeated confirm arguments
        String pending = pendingDeleteConfirmations.get(player.getUUID());
        if (!config.isRequireConfirmationForDeleteEnabled()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.delete_no_confirm_required"));
            return 0;
        }
        if (pending == null || !pending.equals(homeName)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.no_pending_delete", homeName));
            // Always clear any accidental stacking
            pendingDeleteConfirmations.remove(player.getUUID());
            return 0;
        }
        // Remove pending confirmation before attempting deletion to prevent stacking
        pendingDeleteConfirmations.remove(player.getUUID());
        boolean success = homeManager.deleteHome(player, homeName);
        if (success) {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.home.delete_success", homeName));
            return 1;
        } else {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.home.delete_failed", homeName));
            return 0;
        }
    }

    /**
     * Execute /homes
     */
    private static int executeHomes(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("This command can only be used by players."));
            return 0;
        }
        HomeManager homeManager = HomeManager.getInstance();
        
        String homesList = homeManager.getFormattedHomesList(player);
        player.sendSystemMessage(MessageUtil.component(homesList));
        return 1;
    }

    // Add handler for /delhome <name> deny
    private static int executeDelHomeDeny(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        if (player == null) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.command.player_only"));
            return 0;
        }
        String homeName = StringArgumentType.getString(context, "name");
        String pending = pendingDeleteConfirmations.get(player.getUUID());
        if (pending != null && pending.equals(homeName)) {
            pendingDeleteConfirmations.remove(player.getUUID());
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.home.delete_cancelled", homeName));
            return 1;
        }
        player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.teleport.home.no_pending_delete", homeName));
        return 0;
    }

    // ── /renamehome <old> <new> ───────────────────────────────────────────────
    // Essentials: Commandrenamehome — rename an existing home.
    // Supports "player:home new" admin format.
    private static void registerRenameHomeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("renamehome")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), PERMISSION_RENAMEHOME))
            // /renamehome <old> <new>
            .then(Commands.argument("oldname", StringArgumentType.word())
                .then(Commands.argument("newname", StringArgumentType.word())
                    .executes(ctx -> executeRenameHome(ctx,
                        StringArgumentType.getString(ctx, "oldname"),
                        StringArgumentType.getString(ctx, "newname"),
                        null))
                )
            )
            // /renamehome <player:old> <new>  — admin format
            .then(Commands.argument("playercolon", StringArgumentType.word())
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), PERMISSION_RENAMEHOME_OTHERS))
                .then(Commands.argument("newname2", StringArgumentType.word())
                    .executes(ctx -> {
                        String arg = StringArgumentType.getString(ctx, "playercolon");
                        String newName = StringArgumentType.getString(ctx, "newname2");
                        if (arg.contains(":")) {
                            String[] parts = arg.split(":", 2);
                            return executeRenameHome(ctx, parts[1], newName, parts[0]);
                        }
                        return executeRenameHome(ctx, arg, newName, null);
                    })
                )
            )
        );
    }

    private static int executeRenameHome(CommandContext<CommandSourceStack> ctx,
            String oldName, String newName, String targetPlayerName) {
        var src = ctx.getSource();
        ServerPlayer target;
        if (targetPlayerName != null) {
            target = src.getServer().getPlayerList().getPlayerByName(targetPlayerName);
            if (target == null) {
                src.sendFailure(com.zerog.neoessentials.util.MessageUtil.error(
                    "commands.neoessentials.general.player_not_found", targetPlayerName));
                return 0;
            }
        } else {
            target = src.getPlayer();
            if (target == null) {
                src.sendFailure(com.zerog.neoessentials.util.MessageUtil.error(
                    "commands.neoessentials.general.player_only"));
                return 0;
            }
        }
        return HomeManager.getInstance().renameHome(target, oldName.toLowerCase(), newName.toLowerCase()) ? 1 : 0;
    }
}
