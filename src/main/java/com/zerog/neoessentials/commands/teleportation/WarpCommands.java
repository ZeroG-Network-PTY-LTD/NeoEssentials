package com.zerog.neoessentials.commands.teleportation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.teleportation.Warp.WarpManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for the warp teleportation system:
 * - /warp <name> - Teleport to warp
 * - /setwarp <name> [coordinates] - Create a warp (admin)
 * - /delwarp <name> - Delete a warp (admin)
 * - /warps - List all warps
 */
public class WarpCommands {
    
    // Permission nodes for warp commands (matching PermissionRegistry)
    private static final String PERMISSION_WARP = "neoessentials.teleport.warp";
    private static final String PERMISSION_SETWARP = "neoessentials.teleport.warp.create";
    private static final String PERMISSION_DELWARP = "neoessentials.teleport.warp.delete";
    private static final String PERMISSION_WARPS = "neoessentials.teleport.warp.list";
    
    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGESTIONS = (context, builder) -> {
        WarpManager warpManager = WarpManager.getInstance();
        return SharedSuggestionProvider.suggest(warpManager.getWarpNames(), builder);
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        // Only register if teleportation module is enabled
        if (config.isTeleportationEnabled()) {
            // Register individual commands based on their command settings
            if (config.isCommandEnabled("warp")) {
                registerWarpCommand(dispatcher);
            }
            if (config.isCommandEnabled("setwarp")) {
                registerSetWarpCommand(dispatcher);
            }
            if (config.isCommandEnabled("delwarp")) {
                registerDelWarpCommand(dispatcher);
            }
            if (config.isCommandEnabled("listwarps")) {
                registerWarpsCommand(dispatcher);
            }
        }
    }
    
    /**
     * Register /warp <name> command
     */
    private static void registerWarpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_WARP);
                }
                return source.hasPermission(2); // Console fallback
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(WARP_SUGGESTIONS)
                .executes(WarpCommands::executeWarp)
            )
        );
    }
    
    /**
     * Register /setwarp <name> [coordinates] command with aliases
     */
    private static void registerSetWarpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerSetWarpCommandWithName(dispatcher, "setwarp");
        registerSetWarpCommandWithName(dispatcher, "createwarp");
        registerSetWarpCommandWithName(dispatcher, "addwarp");
    }
    
    private static void registerSetWarpCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETWARP);
                }
                return source.hasPermission(3); // Console fallback
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(WarpCommands::executeSetWarpHere)
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(WarpCommands::executeSetWarpAt)
                )
            )
        );
    }
    
    /**
     * Register /delwarp <name> command with aliases
     */
    private static void registerDelWarpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerDelWarpCommandWithName(dispatcher, "delwarp");
        registerDelWarpCommandWithName(dispatcher, "deletewarp");
        registerDelWarpCommandWithName(dispatcher, "removewarp");
        registerDelWarpCommandWithName(dispatcher, "rwarp");
    }
    
    private static void registerDelWarpCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_DELWARP);
                }
                return source.hasPermission(3); // Console fallback
            })
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(WARP_SUGGESTIONS)
                .executes(WarpCommands::executeDelWarp)
            )
        );
    }
    
    /**
     * Register /warps command with aliases
     */
    private static void registerWarpsCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerWarpsCommandWithName(dispatcher, "warps");
        registerWarpsCommandWithName(dispatcher, "warplist");
        registerWarpsCommandWithName(dispatcher, "listwarps");
    }
    
    private static void registerWarpsCommandWithName(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_WARPS);
                }
                return source.hasPermission(2); // Console fallback
            })
            .executes(WarpCommands::executeWarps)
        );
    }
    
    /**
     * Execute /warp <name>
     */
    private static int executeWarp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        // Jail escape prevention
        com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();
        com.zerog.neoessentials.moderation.JailManager jailManager = com.zerog.neoessentials.moderation.JailManager.getInstance();
        if (config.isPreventJailEscapeEnabled() && jailManager.isPlayerJailed(player.getUUID())) {
            context.getSource().sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.jail.prevent_escape"));
            return 0;
        }
        // Check permission
        if (!hasWarpPermission(player)) {
            context.getSource().sendFailure(MessageUtil.error("teleport.warp.no_permission"));
            return 0;
        }
        warpManager.teleportToWarp(player, warpName);
        return 1;
    }
    
    /**
     * Execute /setwarp <name> (at current location)
     */
    private static int executeSetWarpHere(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        
        // Check permission
        if (!hasSetWarpPermission(player)) {
            context.getSource().sendFailure(MessageUtil.error("teleport.warp.no_set_permission"));
            return 0;
        }
        
        if (warpManager.createWarp(player, warpName)) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Execute /setwarp <name> <coordinates>
     */
    private static int executeSetWarpAt(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        
        // Check permission
        if (!hasSetWarpPermission(player)) {
            context.getSource().sendFailure(MessageUtil.error("teleport.warp.no_set_permission"));
            return 0;
        }
        
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
            ServerLevel level = player.serverLevel();
            
            if (warpManager.createWarp(player, warpName, level, pos)) {
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("teleport.warp.invalid_coordinates"));
        }
        return 0;
    }
    
    /**
     * Execute /delwarp <name>
     */
    private static int executeDelWarp(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String warpName = StringArgumentType.getString(context, "name");
        WarpManager warpManager = WarpManager.getInstance();
        
        // Check permission
        if (!hasSetWarpPermission(player)) {
            context.getSource().sendFailure(MessageUtil.error("teleport.warp.no_delete_permission"));
            return 0;
        }
        
        if (warpManager.deleteWarp(player, warpName)) {
            return 1;
        }
        return 0;
    }
    
    /**
     * Execute /warps
     */
    private static int executeWarps(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        WarpManager warpManager = WarpManager.getInstance();
        
        String warpsList = warpManager.getFormattedWarpsList();
        player.sendSystemMessage(MessageUtil.component(warpsList));
        
        // Show statistics if player has admin permission
        if (hasSetWarpPermission(player)) {
            String stats = warpManager.getStatistics();
            player.sendSystemMessage(MessageUtil.component(stats));
        }
        
        return 1;
    }
    
    /**
     * Check if player has permission to use warps
     */
    private static boolean hasWarpPermission(ServerPlayer player) {
        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_WARP);
    }
    
    /**
     * Check if player has permission to create warps
     */
    private static boolean hasSetWarpPermission(ServerPlayer player) {
        return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_SETWARP);
    }
}