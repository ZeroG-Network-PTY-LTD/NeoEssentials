package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.WarpManager;
import com.zerog.neoessentials.utils.MessageUtil;
<<<<<<< HEAD
<<<<<<< HEAD
import com.zerog.neoessentials.utils.PermissionUtil;
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
import com.zerog.neoessentials.utils.PermissionUtil;
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
import com.zerog.neoessentials.utils.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Handles all warp-related commands
 */
public class WarpCommands {
    
    /**
     * Register all warp-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
<<<<<<< HEAD
<<<<<<< HEAD
        NeoEssentials.LOGGER.info("Registering warp commands");
        
        // /warp <n> - Teleport to a warp
        dispatcher.register(
            Commands.literal("warp")
                .requires(source -> {
                    boolean hasPermission = PermissionUtil.hasPermission(source, "neoessentials.command.warp");
                    NeoEssentials.LOGGER.debug("Permission check for 'neoessentials.command.warp': {}", hasPermission);
                    return hasPermission;
                })
=======
        // /warp <name> - Teleport to a warp
        dispatcher.register(
            Commands.literal("warp")
<<<<<<< HEAD
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp"))
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
=======
        NeoEssentials.LOGGER.info("Registering warp commands");
        
        // /warp <n> - Teleport to a warp
        dispatcher.register(
            Commands.literal("warp")
                .requires(source -> {
                    boolean hasPermission = PermissionUtil.hasPermission(source, "neoessentials.command.warp");
                    NeoEssentials.LOGGER.debug("Permission check for 'neoessentials.command.warp': {}", hasPermission);
                    return hasPermission;
                })
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeWarp)
                )
                .executes(this::executeWarpList)
        );
        
        // /warps - List all available warps
        dispatcher.register(
            Commands.literal("warps")
<<<<<<< HEAD
<<<<<<< HEAD
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.list"))
                .executes(this::executeWarpList)
        );
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        // /warphelp - Show help for warp commands
        dispatcher.register(
            Commands.literal("warphelp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.help"))
                .executes(this::executeWarpHelp)
        );
        
        // /setwarp <n> - Set a warp at the player's location
        dispatcher.register(
            Commands.literal("setwarp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.set"))
=======
                .requires(source -> source.hasPermission(0)) // Available to all players
=======
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.list"))
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
                .executes(this::executeWarpList)
        );
        
        // /setwarp <name> - Set a warp at the player's location
=======
        // /setwarp <n> - Set a warp at the player's location
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        dispatcher.register(
            Commands.literal("setwarp")
<<<<<<< HEAD
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.set"))
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeSetWarp)
                )
        );
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        // /delwarp <n> - Delete a warp
        dispatcher.register(
            Commands.literal("delwarp")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.delete"))
=======
        // /delwarp <name> - Delete a warp
        dispatcher.register(
            Commands.literal("delwarp")
<<<<<<< HEAD
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.delete"))
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(this::executeDeleteWarp)
                )
        );
        
        // /warpplayer <player> <warp> - Teleport another player to a warp
        dispatcher.register(
            Commands.literal("warpplayer")
<<<<<<< HEAD
<<<<<<< HEAD
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.player"))
=======
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.warp.player"))
>>>>>>> 796dc37 (refactor: Update warp command permissions and storage handling)
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("warp", StringArgumentType.word())
                        .executes(this::executeWarpPlayer)
                    )
                )
        );
<<<<<<< HEAD
<<<<<<< HEAD
        
        NeoEssentials.LOGGER.info("Warp commands registered successfully");
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        
        NeoEssentials.LOGGER.info("Warp commands registered successfully");
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
    }
    
    /**
     * Execute the /warp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
<<<<<<< HEAD
<<<<<<< HEAD
        NeoEssentials.LOGGER.debug("Player {} is attempting to teleport to warp '{}'", player.getScoreboardName(), warpName);
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
            NeoEssentials.LOGGER.debug("Warp '{}' not found for player {}", warpName, player.getScoreboardName());
=======
=======
        NeoEssentials.LOGGER.debug("Player {} is attempting to teleport to warp '{}'", player.getScoreboardName(), warpName);
        
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.debug("Warp '{}' not found for player {}", warpName, player.getScoreboardName());
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        NeoEssentials.LOGGER.debug("Attempting to teleport player {} to warp '{}' at dimension {}, [{}, {}, {}]",
            player.getScoreboardName(), warpName, warpLocation.getDimension(), 
            warpLocation.getX(), warpLocation.getY(), warpLocation.getZ());
            
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        // Teleport the player to the warp
        boolean success = teleportPlayerToWarp(player, warpLocation);
        
        if (success) {
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.debug("Successfully teleported player {} to warp '{}'", player.getScoreboardName(), warpName);
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.debug("Successfully teleported player {} to warp '{}'", player.getScoreboardName(), warpName);
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            MutableComponent message = Component.literal("Teleported to warp '" + warpName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.error("Failed to teleport player {} to warp '{}'", player.getScoreboardName(), warpName);
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.error("Failed to teleport player {} to warp '{}'", player.getScoreboardName(), warpName);
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            context.getSource().sendFailure(Component.literal("Failed to teleport to warp '" + warpName + "'"));
            return 0;
        }
    }
    
    /**
     * Execute the /warps command to list all available warps
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarpList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
<<<<<<< HEAD
<<<<<<< HEAD
        NeoEssentials.LOGGER.debug("Player {} is requesting the warp list", player.getScoreboardName());
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warps command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        Map<String, WarpManager.WarpLocation> warps = warpManager.getWarps();
        
        if (warps.isEmpty()) {
            NeoEssentials.LOGGER.debug("No warps found for player {}", player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("No warps have been set"));
            return 0;
        }
          MutableComponent message = Component.literal("§2Available warps: ");
<<<<<<< HEAD
=======
=======
        NeoEssentials.LOGGER.debug("Player {} is requesting the warp list", player.getScoreboardName());
        
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warps command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        Map<String, WarpManager.WarpLocation> warps = warpManager.getWarps();
        
        if (warps.isEmpty()) {
            NeoEssentials.LOGGER.debug("No warps found for player {}", player.getScoreboardName());
            context.getSource().sendFailure(Component.literal("No warps have been set"));
            return 0;
        }
        
        MutableComponent message = Component.literal("Available warps: ");
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        
        boolean first = true;
        for (String warpName : warps.keySet()) {
            if (!first) {
<<<<<<< HEAD
<<<<<<< HEAD
                message.append(Component.literal("§7, "));
            }
            
            // Create clickable warp name with hover info
            MutableComponent warpComponent = Component.literal("§b" + warpName);
            
            // Add hover text
            MutableComponent hoverText = Component.literal("§eClick to teleport to §b" + warpName);
            WarpManager.WarpLocation warpLocation = warps.get(warpName);
            
            if (warpLocation != null) {
                hoverText.append(Component.literal("\n§7Dimension: §f" + warpLocation.getDimension()));
                hoverText.append(Component.literal("\n§7Location: §f" + 
                    (int)warpLocation.getX() + ", " + 
                    (int)warpLocation.getY() + ", " + 
                    (int)warpLocation.getZ()));
            }
            
            warpComponent = MessageUtil.addHoverText(warpComponent, hoverText);
            
            // Add click event to teleport to the warp
            warpComponent = MessageUtil.makeClickableCommand(warpComponent, "/warp " + warpName);
            
            message.append(warpComponent);
            first = false;
        }
        
        NeoEssentials.LOGGER.debug("Sending interactive warp list ({} warps) to player {}", warps.size(), player.getScoreboardName());
        
        // Add a heading
        MessageUtil.sendInfo(player, Component.literal("§2§l====== §r§6Warp List §2§l======"));
        
        // Send the warp list
        MessageUtil.sendInfo(player, message);
        
        // Add clickable help button
        MutableComponent helpMessage = Component.literal("§7Type ");
        MutableComponent helpButton = Component.literal("§e/warphelp");
        helpButton = MessageUtil.makeClickableCommand(helpButton, "/warphelp");
        helpButton = MessageUtil.addHoverText(helpButton, Component.literal("§7Click to view warp command help"));
        helpMessage.append(helpButton);
        helpMessage.append(Component.literal(" §7for more information."));
        
        MessageUtil.sendInfo(player, helpMessage);
=======
                message.append(Component.literal(", "));
=======
                message.append(Component.literal("§7, "));
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
            }
            
            // Create clickable warp name with hover info
            MutableComponent warpComponent = Component.literal("§b" + warpName);
            
            // Add hover text
            MutableComponent hoverText = Component.literal("§eClick to teleport to §b" + warpName);
            WarpManager.WarpLocation warpLocation = warps.get(warpName);
            
            if (warpLocation != null) {
                hoverText.append(Component.literal("\n§7Dimension: §f" + warpLocation.getDimension()));
                hoverText.append(Component.literal("\n§7Location: §f" + 
                    (int)warpLocation.getX() + ", " + 
                    (int)warpLocation.getY() + ", " + 
                    (int)warpLocation.getZ()));
            }
            
            warpComponent = MessageUtil.addHoverText(warpComponent, hoverText);
            
            // Add click event to teleport to the warp
            warpComponent = MessageUtil.makeClickableCommand(warpComponent, "/warp " + warpName);
            
            message.append(warpComponent);
            first = false;
        }
        
        NeoEssentials.LOGGER.debug("Sending interactive warp list ({} warps) to player {}", warps.size(), player.getScoreboardName());
        
        // Add a heading
        MessageUtil.sendInfo(player, Component.literal("§2§l====== §r§6Warp List §2§l======"));
        
        // Send the warp list
        MessageUtil.sendInfo(player, message);
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
        
        // Add clickable help button
        MutableComponent helpMessage = Component.literal("§7Type ");
        MutableComponent helpButton = Component.literal("§e/warphelp");
        helpButton = MessageUtil.makeClickableCommand(helpButton, "/warphelp");
        helpButton = MessageUtil.addHoverText(helpButton, Component.literal("§7Click to view warp command help"));
        helpMessage.append(helpButton);
        helpMessage.append(Component.literal(" §7for more information."));
        
        MessageUtil.sendInfo(player, helpMessage);
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
        return 1;
    }
    
    /**
     * Execute the /setwarp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeSetWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        NeoEssentials.LOGGER.debug("Player {} is attempting to set warp '{}' at [{}, {}, {}] in dimension {}",
            player.getScoreboardName(), warpName, 
            player.getX(), player.getY(), player.getZ(),
            player.level().dimension().location());
        
<<<<<<< HEAD
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /setwarp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        boolean success = warpManager.setWarp(player, warpName);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} set warp '{}' at [{}, {}, {}] in dimension {}",
                player.getScoreboardName(), warpName, 
                player.getX(), player.getY(), player.getZ(),
                player.level().dimension().location());
=======
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /setwarp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        boolean success = warpManager.setWarp(player, warpName);
        
        if (success) {
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.info("Player {} set warp '{}' at [{}, {}, {}] in dimension {}",
                player.getScoreboardName(), warpName, 
                player.getX(), player.getY(), player.getZ(),
                player.level().dimension().location());
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            MutableComponent message = Component.literal("Set warp '" + warpName + "' at your current location");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.error("Failed to set warp '{}' for player {}", warpName, player.getScoreboardName());
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.error("Failed to set warp '{}' for player {}", warpName, player.getScoreboardName());
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            context.getSource().sendFailure(Component.literal("Failed to set warp '" + warpName + "'"));
            return 0;
        }
    }
    
    /**
     * Execute the /delwarp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeDeleteWarp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String warpName = StringArgumentType.getString(context, "name");
        
<<<<<<< HEAD
<<<<<<< HEAD
        NeoEssentials.LOGGER.debug("Player {} is attempting to delete warp '{}'", player.getScoreboardName(), warpName);
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /delwarp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        boolean success = warpManager.deleteWarp(warpName);
        
        if (success) {
            NeoEssentials.LOGGER.info("Player {} deleted warp '{}'", player.getScoreboardName(), warpName);
=======
=======
        NeoEssentials.LOGGER.debug("Player {} is attempting to delete warp '{}'", player.getScoreboardName(), warpName);
        
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /delwarp command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        boolean success = warpManager.deleteWarp(warpName);
        
        if (success) {
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.info("Player {} deleted warp '{}'", player.getScoreboardName(), warpName);
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            MutableComponent message = Component.literal("Deleted warp '" + warpName + "'");
            MessageUtil.sendSuccess(player, message);
            return 1;
        } else {
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.debug("Warp '{}' not found for deletion by player {}", warpName, player.getScoreboardName());
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.debug("Warp '{}' not found for deletion by player {}", warpName, player.getScoreboardName());
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
    }
    
    /**
     * Execute the /warpplayer command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarpPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        String warpName = StringArgumentType.getString(context, "warp");
        
<<<<<<< HEAD
<<<<<<< HEAD
        NeoEssentials.LOGGER.debug("Player {} is attempting to warp player {} to warp '{}'", 
            source.getScoreboardName(), targetPlayer.getScoreboardName(), warpName);
        
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warpplayer command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
            NeoEssentials.LOGGER.debug("Warp '{}' not found for warpplayer command by {}", warpName, source.getScoreboardName());
=======
=======
        NeoEssentials.LOGGER.debug("Player {} is attempting to warp player {} to warp '{}'", 
            source.getScoreboardName(), targetPlayer.getScoreboardName(), warpName);
        
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
        if (warpManager == null) {
            NeoEssentials.LOGGER.error("WarpManager is null when executing /warpplayer command");
            context.getSource().sendFailure(Component.literal("Warp system is not available"));
            return 0;
        }
        
        WarpManager.WarpLocation warpLocation = warpManager.getWarp(warpName);
        
        if (warpLocation == null) {
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.debug("Warp '{}' not found for warpplayer command by {}", warpName, source.getScoreboardName());
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            context.getSource().sendFailure(Component.literal("Warp '" + warpName + "' not found"));
            return 0;
        }
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        NeoEssentials.LOGGER.debug("Attempting to teleport player {} to warp '{}' at dimension {}, [{}, {}, {}]",
            targetPlayer.getScoreboardName(), warpName, warpLocation.getDimension(), 
            warpLocation.getX(), warpLocation.getY(), warpLocation.getZ());
        
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        // Teleport the target player to the warp
        boolean success = teleportPlayerToWarp(targetPlayer, warpLocation);
        
        if (success) {
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.info("Player {} teleported {} to warp '{}'", 
                source.getScoreboardName(), targetPlayer.getScoreboardName(), warpName);
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.info("Player {} teleported {} to warp '{}'", 
                source.getScoreboardName(), targetPlayer.getScoreboardName(), warpName);
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            MutableComponent messageToAdmin = Component.literal("Teleported " + targetPlayer.getScoreboardName() + " to warp '" + warpName + "'");
            MessageUtil.sendSuccess(source, messageToAdmin);
            
            MutableComponent messageToTarget = Component.literal("You have been teleported to warp '" + warpName + "'");
            MessageUtil.sendInfo(targetPlayer, messageToTarget);
            return 1;
        } else {
<<<<<<< HEAD
<<<<<<< HEAD
            NeoEssentials.LOGGER.error("Failed to teleport player {} to warp '{}'", targetPlayer.getScoreboardName(), warpName);
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
            NeoEssentials.LOGGER.error("Failed to teleport player {} to warp '{}'", targetPlayer.getScoreboardName(), warpName);
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
            context.getSource().sendFailure(Component.literal("Failed to teleport " + targetPlayer.getScoreboardName() + " to warp '" + warpName + "'"));
            return 0;
        }
    }
<<<<<<< HEAD
<<<<<<< HEAD
      /**
=======
    
    /**
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
      /**
>>>>>>> 734727c (feat: Enhance command registration and execution with improved error handling and user feedback)
     * Teleports a player to a warp location
     * 
     * @param player The player to teleport
     * @param warpLocation The warp location
     * @return True if teleportation was successful, false otherwise
<<<<<<< HEAD
<<<<<<< HEAD
     */
    private boolean teleportPlayerToWarp(ServerPlayer player, WarpManager.WarpLocation warpLocation) {
<<<<<<< HEAD
=======
     */
    private boolean teleportPlayerToWarp(ServerPlayer player, WarpManager.WarpLocation warpLocation) {
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        if (player == null || warpLocation == null) {
            NeoEssentials.LOGGER.error("Cannot teleport with null player or warp location");
            return false;
        }
        
<<<<<<< HEAD
=======
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
     */    private boolean teleportPlayerToWarp(ServerPlayer player, WarpManager.WarpLocation warpLocation) {
>>>>>>> 0e64616 (chore: Update build number to 12 and timestamp in buildnumber.properties; enhance logging in command registration and warp management)
=======
>>>>>>> 1713a18 (Refactor warp command handling and improve MySQL storage)
        String dimensionKey = warpLocation.getDimension();
        double x = warpLocation.getX();
        double y = warpLocation.getY();
        double z = warpLocation.getZ();
        float yaw = warpLocation.getYaw();
        float pitch = warpLocation.getPitch();
        
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 0e64616 (chore: Update build number to 12 and timestamp in buildnumber.properties; enhance logging in command registration and warp management)
        // Check if player's server is available
        if (player.getServer() == null) {
            NeoEssentials.LOGGER.error("Cannot teleport player, server instance is null");
            return false;
        }
        
<<<<<<< HEAD
        // Get the server from the player
        ServerLevel targetLevel = null;
        
        // Try direct match first
        for (ServerLevel level : player.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) {
                targetLevel = level;
                NeoEssentials.LOGGER.debug("Found exact dimension match: {}", dimensionKey);
=======
=======
>>>>>>> 0e64616 (chore: Update build number to 12 and timestamp in buildnumber.properties; enhance logging in command registration and warp management)
        // Get the server from the player
        ServerLevel targetLevel = null;
        
        // Try direct match first
        for (ServerLevel level : player.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionKey)) {
                targetLevel = level;
<<<<<<< HEAD
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
                NeoEssentials.LOGGER.debug("Found exact dimension match: {}", dimensionKey);
>>>>>>> 734727c (feat: Enhance command registration and execution with improved error handling and user feedback)
                break;
            }
        }
        
<<<<<<< HEAD
<<<<<<< HEAD
        // If direct match failed, try checking just the path part
        if (targetLevel == null) {
            for (ServerLevel level : player.getServer().getAllLevels()) {
                String levelPath = level.dimension().location().getPath();
                if (dimensionKey.contains(levelPath) || dimensionKey.endsWith(":" + levelPath)) {
                    NeoEssentials.LOGGER.debug("Found path-based dimension match: {} matches {}", 
                        levelPath, dimensionKey);
                    targetLevel = level;
                    break;
                }
                
                // Also try the reverse - maybe the stored key is just the path but we need the full key
                String fullLevelKey = level.dimension().location().toString();
                if (fullLevelKey.endsWith(":" + dimensionKey)) {
                    NeoEssentials.LOGGER.debug("Found path-based dimension match: {} matches {}", 
                        fullLevelKey, dimensionKey);
                    targetLevel = level;
                    break;
                }
            }
        }
        
        // Last resort - try matching common dimension names
        if (targetLevel == null) {
            String lowerDimKey = dimensionKey.toLowerCase();
            
            for (ServerLevel level : player.getServer().getAllLevels()) {
                String levelPath = level.dimension().location().getPath().toLowerCase();
                String fullLevelKey = level.dimension().location().toString().toLowerCase();
                
                // Match common dimension patterns
                if ((levelPath.contains("overworld") || fullLevelKey.contains("overworld")) && 
                    lowerDimKey.contains("overworld")) {
                    targetLevel = level;
                    NeoEssentials.LOGGER.debug("Found overworld dimension using common name matching");
                    break;
                } else if ((levelPath.contains("nether") || fullLevelKey.contains("nether")) && 
                           lowerDimKey.contains("nether")) {
                    targetLevel = level;
                    NeoEssentials.LOGGER.debug("Found nether dimension using common name matching");
                    break;
                } else if ((levelPath.contains("end") || fullLevelKey.contains("end")) && 
                           lowerDimKey.contains("end")) {
                    targetLevel = level;
                    NeoEssentials.LOGGER.debug("Found end dimension using common name matching");
                    break;
                }
            }
        }
        
        // If all attempts failed, fall back to the overworld
        if (targetLevel == null) {
            NeoEssentials.LOGGER.warn("Could not find dimension '{}', defaulting to overworld", dimensionKey);
            targetLevel = player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            
            // If even this failed, we can't teleport
            if (targetLevel == null) {
                NeoEssentials.LOGGER.error("Failed to find any valid dimension, teleport canceled");
                return false;
            }
        }
        
        // Log successful dimension resolution
        NeoEssentials.LOGGER.info("Teleporting player {} to warp at [{}, {}, {}] in dimension {}", 
            player.getScoreboardName(), x, y, z, targetLevel.dimension().location());
        
        // Teleport the player
        return TeleportUtil.teleport(player, targetLevel, x, y, z, yaw, pitch);
    }

    /**
     * Execute the /warphelp command
     * 
     * @param context The command context
     * @return Command result
     */
    private int executeWarpHelp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        NeoEssentials.LOGGER.debug("Player {} is requesting warp help", player.getScoreboardName());
        
        // Header
        MessageUtil.sendInfo(player, Component.literal("§2§l====== §r§6Warp Commands §2§l======"));
        
        // Commands list with clickable examples
        MutableComponent warpCmd = Component.literal("§b/warp <name>");
        warpCmd = MessageUtil.addHoverText(warpCmd, Component.literal("§7Teleport to a warp"));
        MessageUtil.sendInfo(player, warpCmd.append(Component.literal(" §7- Teleport to a warp location")));
        
        MutableComponent warpsCmd = Component.literal("§b/warps");
        warpsCmd = MessageUtil.makeClickableCommand(warpsCmd, "/warps");
        warpsCmd = MessageUtil.addHoverText(warpsCmd, Component.literal("§7List all available warps\n§eClick to execute"));
        MessageUtil.sendInfo(player, warpsCmd.append(Component.literal(" §7- List all available warps")));
        
        // Only show admin commands to players with appropriate permissions
<<<<<<< HEAD
        if (PermissionUtil.hasPermission((ServerPlayer)player, "neoessentials.command.warp.set")) {
=======
        if (PermissionUtil.hasPermission(player, "neoessentials.command.warp.set")) {
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
            MutableComponent setWarpCmd = Component.literal("§b/setwarp <name>");
            setWarpCmd = MessageUtil.addHoverText(setWarpCmd, Component.literal("§7Create a new warp at your location"));
            MessageUtil.sendInfo(player, setWarpCmd.append(Component.literal(" §7- Create a new warp at your location")));
        }
        
<<<<<<< HEAD
        if (PermissionUtil.hasPermission((ServerPlayer)player, "neoessentials.command.warp.delete")) {
=======
        if (PermissionUtil.hasPermission(player, "neoessentials.command.warp.delete")) {
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
            MutableComponent delWarpCmd = Component.literal("§b/delwarp <name>");
            delWarpCmd = MessageUtil.addHoverText(delWarpCmd, Component.literal("§7Delete an existing warp"));
            MessageUtil.sendInfo(player, delWarpCmd.append(Component.literal(" §7- Delete an existing warp")));
        }
        
<<<<<<< HEAD
        if (PermissionUtil.hasPermission((ServerPlayer)player, "neoessentials.command.warp.player")) {
=======
        if (PermissionUtil.hasPermission(player, "neoessentials.command.warp.player")) {
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
            MutableComponent warpPlayerCmd = Component.literal("§b/warpplayer <player> <warp>");
            warpPlayerCmd = MessageUtil.addHoverText(warpPlayerCmd, Component.literal("§7Teleport another player to a warp"));
            MessageUtil.sendInfo(player, warpPlayerCmd.append(Component.literal(" §7- Teleport another player to a warp")));
        }
        
        return 1;
    }
<<<<<<< HEAD
=======
=======
        // If direct match failed, try checking just the path part
>>>>>>> 734727c (feat: Enhance command registration and execution with improved error handling and user feedback)
        if (targetLevel == null) {
            for (ServerLevel level : player.getServer().getAllLevels()) {
                String levelPath = level.dimension().location().getPath();
                if (dimensionKey.contains(levelPath) || dimensionKey.endsWith(":" + levelPath)) {
                    NeoEssentials.LOGGER.debug("Found path-based dimension match: {} matches {}", 
                        levelPath, dimensionKey);
                    targetLevel = level;
                    break;
                }
                
                // Also try the reverse - maybe the stored key is just the path but we need the full key
                String fullLevelKey = level.dimension().location().toString();
                if (fullLevelKey.endsWith(":" + dimensionKey)) {
                    NeoEssentials.LOGGER.debug("Found path-based dimension match: {} matches {}", 
                        fullLevelKey, dimensionKey);
                    targetLevel = level;
                    break;
                }
            }
        }
        
        // Last resort - try matching common dimension names
        if (targetLevel == null) {
            String lowerDimKey = dimensionKey.toLowerCase();
            
            for (ServerLevel level : player.getServer().getAllLevels()) {
                String levelPath = level.dimension().location().getPath().toLowerCase();
                String fullLevelKey = level.dimension().location().toString().toLowerCase();
                
                // Match common dimension patterns
                if ((levelPath.contains("overworld") || fullLevelKey.contains("overworld")) && 
                    lowerDimKey.contains("overworld")) {
                    targetLevel = level;
                    NeoEssentials.LOGGER.debug("Found overworld dimension using common name matching");
                    break;
                } else if ((levelPath.contains("nether") || fullLevelKey.contains("nether")) && 
                           lowerDimKey.contains("nether")) {
                    targetLevel = level;
                    NeoEssentials.LOGGER.debug("Found nether dimension using common name matching");
                    break;
                } else if ((levelPath.contains("end") || fullLevelKey.contains("end")) && 
                           lowerDimKey.contains("end")) {
                    targetLevel = level;
                    NeoEssentials.LOGGER.debug("Found end dimension using common name matching");
                    break;
                }
            }
        }
        
        // If all attempts failed, fall back to the overworld
        if (targetLevel == null) {
            NeoEssentials.LOGGER.warn("Could not find dimension '{}', defaulting to overworld", dimensionKey);
            targetLevel = player.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            
            // If even this failed, we can't teleport
            if (targetLevel == null) {
                NeoEssentials.LOGGER.error("Failed to find any valid dimension, teleport canceled");
                return false;
            }
        }
        
        // Log successful dimension resolution
        NeoEssentials.LOGGER.info("Teleporting player {} to warp at [{}, {}, {}] in dimension {}", 
            player.getScoreboardName(), x, y, z, targetLevel.dimension().location());
        
        // Teleport the player
        return TeleportUtil.teleport(player, targetLevel, x, y, z, yaw, pitch);
    }
>>>>>>> eab9ffa (feat: Implement core event handling for NeoEssentials mod)
=======
>>>>>>> ca88c1e (Refactor code structure for improved readability and maintainability)
}
