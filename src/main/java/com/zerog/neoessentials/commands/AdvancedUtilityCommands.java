package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Advanced utility commands for server management and administration.
 * Provides commands for server information, player management, and utilities.
 */
public class AdvancedUtilityCommands {

    /**
     * Registers all advanced utility commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /serverinfo - Display server information
        dispatcher.register(
            Commands.literal("serverinfo")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.serverinfo"))
                .executes(this::showServerInfo)
        );

        // /ping - Show connection ping
        dispatcher.register(
            Commands.literal("ping")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.ping"))
                .executes(this::showPing)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasModeratorPermission(source, "neoessentials.command.ping.others"))
                        .executes(this::showPlayerPing)
                )
        );

        // /playerlist - Enhanced player list
        dispatcher.register(
            Commands.literal("playerlist")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.playerlist"))
                .executes(this::showPlayerList)
        );

        // /motd - Message of the Day
        dispatcher.register(
            Commands.literal("motd")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.motd"))
                .executes(this::showMotd)
                .then(
                    Commands.literal("set")
                        .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.motd.set"))
                        .then(
                            Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::setMotd)
                        )
                )
        );

        // /broadcast - Server-wide broadcast
        dispatcher.register(
            Commands.literal("broadcast")
                .requires(source -> PermissionUtil.hasModeratorPermission(source, "neoessentials.command.broadcast"))
                .then(
                    Commands.argument("message", StringArgumentType.greedyString())
                        .executes(this::broadcastMessage)
                )
        );

        // /sudo - Execute command as another player
        dispatcher.register(
            Commands.literal("sudo")
                .requires(source -> PermissionUtil.hasAdminPermission(source, "neoessentials.command.sudo"))
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .then(
                            Commands.argument("command", StringArgumentType.greedyString())
                                .executes(this::executeAsPlayer)
                        )
                )
        );

        // /workbench - Open crafting table
        dispatcher.register(
            Commands.literal("workbench")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.workbench"))
                .executes(this::openWorkbench)
        );

        // /enderchest - Open ender chest
        dispatcher.register(
            Commands.literal("enderchest")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.enderchest"))
                .executes(this::openEnderChest)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasModeratorPermission(source, "neoessentials.command.enderchest.others"))
                        .executes(this::openPlayerEnderChest)
                )
        );
    }

    /**
     * Shows server information.
     */
    private int showServerInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();

            LanguageUtil.sendMessage(player, "commands.serverinfo.header");
            
            // Server version and mod info
            String serverVersion = server.getServerVersion();
            LanguageUtil.sendMessage(player, "§7Server Version: §e" + serverVersion);
            LanguageUtil.sendMessage(player, "§7NeoEssentials Version: §e1.0.2");
            
            // Player count
            int onlinePlayers = server.getPlayerCount();
            int maxPlayers = server.getMaxPlayers();
            LanguageUtil.sendMessage(player, "§7Players: §e" + onlinePlayers + "§7/§e" + maxPlayers);
            
            // World count
            int worldCount = 0;
            for (@SuppressWarnings("unused") net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                worldCount++;
            }
            LanguageUtil.sendMessage(player, "§7Worlds: §e" + worldCount);
            
            // Server uptime (simplified)
            LanguageUtil.sendMessage(player, "§7Server running smoothly!");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing server info: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Shows the player's ping.
     */
    private int showPing(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int ping = player.connection.latency();
            
            LanguageUtil.sendMessage(player, "commands.ping.self", String.valueOf(ping));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing ping: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Shows another player's ping.
     */
    private int showPlayerPing(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            int ping = target.connection.latency();
            
            LanguageUtil.sendMessage(executor, "commands.ping.other", 
                target.getScoreboardName(), String.valueOf(ping));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing player ping: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Shows an enhanced player list.
     */
    private int showPlayerList(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MinecraftServer server = context.getSource().getServer();
            
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
            
            LanguageUtil.sendMessage(player, "commands.playerlist.header");
            LanguageUtil.sendMessage(player, "§7Online players (" + players.size() + "):");
            
            for (ServerPlayer onlinePlayer : players) {
                String worldName = onlinePlayer.serverLevel().dimension().location().getPath();
                int ping = onlinePlayer.connection.latency();
                
                LanguageUtil.sendMessage(player, "§e%s §7- World: §a%s §7- Ping: §6%dms",
                    onlinePlayer.getScoreboardName(), worldName, ping);
            }
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing player list: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Shows the Message of the Day.
     */
    private int showMotd(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // In a full implementation, you'd store the MOTD in a config file or database
            LanguageUtil.sendMessage(player, "commands.motd.header");
            LanguageUtil.sendMessage(player, "§7Welcome to the server!");
            LanguageUtil.sendMessage(player, "§7Enjoy your stay and follow the rules.");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error showing MOTD: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Sets the Message of the Day.
     */
    private int setMotd(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String message = StringArgumentType.getString(context, "message");
            
            // In a full implementation, you'd save this to a config file or database
            LanguageUtil.sendMessage(player, "commands.motd.set", message);
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error setting MOTD: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Broadcasts a message to all players.
     */
    private int broadcastMessage(CommandContext<CommandSourceStack> context) {
        try {
            String message = StringArgumentType.getString(context, "message");
            MinecraftServer server = context.getSource().getServer();
            
            String formattedMessage = LanguageUtil.formatText("§6[Broadcast] §f" + message);
            
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                LanguageUtil.sendMessage(player, formattedMessage);
            }
            
            NeoEssentials.LOGGER.info("Broadcast: {}", message);
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error broadcasting message: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Executes a command as another player.
     */
    private int executeAsPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String command = StringArgumentType.getString(context, "command");
            
            // Execute the command as the target player
            MinecraftServer server = context.getSource().getServer();
            CommandSourceStack targetSource = target.createCommandSourceStack();
            
            server.getCommands().performPrefixedCommand(targetSource, command);
            
            LanguageUtil.sendMessage(context.getSource().getPlayerOrException(), 
                "commands.sudo.executed", target.getScoreboardName(), command);
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error executing sudo command: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Opens a crafting table for the player.
     */
    private int openWorkbench(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Open crafting table
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, p) -> new net.minecraft.world.inventory.CraftingMenu(id, inventory),
                net.minecraft.network.chat.Component.translatable("container.crafting")
            ));
            
            LanguageUtil.sendMessage(player, "commands.workbench.opened");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening workbench: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Opens the player's ender chest.
     */
    private int openEnderChest(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, p) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()),
                net.minecraft.network.chat.Component.translatable("container.enderchest")
            ));
            
            LanguageUtil.sendMessage(player, "commands.enderchest.opened");
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening ender chest: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Opens another player's ender chest.
     */
    private int openPlayerEnderChest(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            executor.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, p) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory, target.getEnderChestInventory()),
                net.minecraft.network.chat.Component.literal(target.getScoreboardName() + "'s Ender Chest")
            ));
            
            LanguageUtil.sendMessage(executor, "commands.enderchest.opened.other", target.getScoreboardName());
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening player ender chest: {}", e.getMessage());
            return 0;
        }
    }
}
