package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.gui.CustomGuiManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI Command - Opens custom GUIs for players
 * 
 * Commands:
 * - /gui <type> - Opens specified GUI
 * - /shop - Opens shop GUI
 * - /menu - Opens main menu GUI
 * - /stats - Opens player stats GUI
 * - /servergui - Opens server info GUI
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GuiCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main GUI command
        dispatcher.register(Commands.literal("gui")
            .requires(source -> source.isPlayer())
            .then(Commands.argument("type", StringArgumentType.string())
                .suggests((context, builder) -> {
                    builder.suggest("shop");
                    builder.suggest("stats");
                    builder.suggest("server");
                    builder.suggest("economy");
                    builder.suggest("kits");
                    builder.suggest("warps");
                    builder.suggest("teleport");
                    return builder.buildFuture();
                })
                .executes(GuiCommand::executeGuiCommand)
                .then(Commands.argument("category", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        String type = StringArgumentType.getString(context, "type");
                        if ("shop".equals(type)) {
                            builder.suggest("weapons");
                            builder.suggest("armor");
                            builder.suggest("food");
                            builder.suggest("blocks");
                            builder.suggest("redstone");
                        }
                        return builder.buildFuture();
                    })
                    .executes(GuiCommand::executeGuiCategoryCommand)))
        );
        
        // Shop command shortcut
        dispatcher.register(Commands.literal("shop")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.SHOP_MAIN);
                return 1;
            })
            .then(Commands.argument("category", StringArgumentType.string())
                .suggests((context, builder) -> {
                    builder.suggest("weapons");
                    builder.suggest("armor");
                    builder.suggest("food");
                    builder.suggest("blocks");
                    builder.suggest("redstone");
                    return builder.buildFuture();
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String category = StringArgumentType.getString(context, "category");
                    CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.SHOP_CATEGORY, category);
                    return 1;
                }))
        );
        
        // Menu command shortcut
        dispatcher.register(Commands.literal("menu")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.SERVER_INFO);
                return 1;
            })
        );
        
        // Stats command shortcut
        dispatcher.register(Commands.literal("stats")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.PLAYER_STATS);
                return 1;
            })
            .then(Commands.argument("target", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                    CustomGuiManager.getInstance().openGui(target, CustomGuiManager.GuiType.PLAYER_STATS);
                    
                    ServerPlayer sender = context.getSource().getPlayerOrException();
                    sender.sendSystemMessage(Component.literal("§aOpened stats GUI for " + target.getDisplayName().getString()));
                    return 1;
                }))
        );
        
        // Server GUI command shortcut
        dispatcher.register(Commands.literal("servergui")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.SERVER_INFO);
                return 1;
            })
        );
        
        // Economy GUI command shortcut
        dispatcher.register(Commands.literal("economy")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.ECONOMY_MANAGEMENT);
                return 1;
            })
        );
        
        // Kits GUI command shortcut
        dispatcher.register(Commands.literal("kits")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.KIT_SELECTOR);
                return 1;
            })
        );
        
        // Warps GUI command shortcut
        dispatcher.register(Commands.literal("warps")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.WARP_SELECTOR);
                return 1;
            })
        );
        
        // Teleport menu GUI command shortcut
        dispatcher.register(Commands.literal("tpmenu")
            .requires(source -> source.isPlayer())
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.TELEPORT_MENU);
                return 1;
            })
        );
    }
    
    private static int executeGuiCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String type = StringArgumentType.getString(context, "type").toLowerCase();
            
            CustomGuiManager.GuiType guiType = switch (type) {
                case "shop" -> CustomGuiManager.GuiType.SHOP_MAIN;
                case "stats" -> CustomGuiManager.GuiType.PLAYER_STATS;
                case "server" -> CustomGuiManager.GuiType.SERVER_INFO;
                case "economy" -> CustomGuiManager.GuiType.ECONOMY_MANAGEMENT;
                case "kits" -> CustomGuiManager.GuiType.KIT_SELECTOR;
                case "warps" -> CustomGuiManager.GuiType.WARP_SELECTOR;
                case "teleport" -> CustomGuiManager.GuiType.TELEPORT_MENU;
                default -> null;
            };
            
            if (guiType == null) {
                player.sendSystemMessage(Component.literal("§cUnknown GUI type: " + type));
                player.sendSystemMessage(Component.literal("§7Available types: shop, stats, server, economy, kits, warps, teleport"));
                return 0;
            }
            
            CustomGuiManager.getInstance().openGui(player, guiType);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing GUI command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to open GUI: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeGuiCategoryCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String type = StringArgumentType.getString(context, "type").toLowerCase();
            String category = StringArgumentType.getString(context, "category").toLowerCase();
            
            if ("shop".equals(type)) {
                CustomGuiManager.getInstance().openGui(player, CustomGuiManager.GuiType.SHOP_CATEGORY, category);
                return 1;
            } else {
                player.sendSystemMessage(Component.literal("§cCategory parameter only supported for shop GUI"));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing GUI category command", e);
            context.getSource().sendFailure(Component.literal("§cFailed to open GUI: " + e.getMessage()));
            return 0;
        }
    }
}
