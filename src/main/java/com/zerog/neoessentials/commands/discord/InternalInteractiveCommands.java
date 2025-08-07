package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.discord.DiscordInteractiveChat;
import com.zerog.neoessentials.error.ErrorHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Internal commands for handling Discord Interactive Chat clickables
 * These commands are not meant to be used directly by players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class InternalInteractiveCommands {
    
    /**
     * Register all internal interactive commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            // Internal command to show item details
            dispatcher.register(Commands.literal("neoessentials_internal_showitem")
                .then(Commands.argument("target_uuid", StringArgumentType.string())
                    .executes(context -> {
                        try {
                            if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                return 0;
                            }
                            
                            String targetUuidStr = StringArgumentType.getString(context, "target_uuid");
                            UUID targetUuid = UUID.fromString(targetUuidStr);
                            
                            var server = viewer.getServer();
                            if (server == null) {
                                viewer.sendSystemMessage(Component.literal("Server not available!")
                                    .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            
                            ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
                            if (target == null) {
                                viewer.sendSystemMessage(Component.literal("Target player not found!")
                                    .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            
                            DiscordInteractiveChat.showPlayerItem(viewer, target);
                            return 1;
                            
                        } catch (Exception e) {
                            ErrorHandler.handleSystemError("Internal Interactive Commands", "show item", e);
                            return 0;
                        }
                    })
                )
            );
            
            // Internal command to show inventory
            dispatcher.register(Commands.literal("neoessentials_internal_showinv")
                .then(Commands.argument("target_uuid", StringArgumentType.string())
                    .executes(context -> {
                        try {
                            if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                return 0;
                            }
                            
                            String targetUuidStr = StringArgumentType.getString(context, "target_uuid");
                            UUID targetUuid = UUID.fromString(targetUuidStr);
                            
                            var server = viewer.getServer();
                            if (server == null) {
                                viewer.sendSystemMessage(Component.literal("Server not available!")
                                    .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            
                            ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
                            if (target == null) {
                                viewer.sendSystemMessage(Component.literal("Target player not found!")
                                    .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            
                            DiscordInteractiveChat.showPlayerInventory(viewer, target);
                            return 1;
                            
                        } catch (Exception e) {
                            ErrorHandler.handleSystemError("Internal Interactive Commands", "show inventory", e);
                            return 0;
                        }
                    })
                )
            );
            
            // Internal command to show ender chest
            dispatcher.register(Commands.literal("neoessentials_internal_showechest")
                .then(Commands.argument("target_uuid", StringArgumentType.string())
                    .executes(context -> {
                        try {
                            if (!(context.getSource().getEntity() instanceof ServerPlayer viewer)) {
                                return 0;
                            }
                            
                            String targetUuidStr = StringArgumentType.getString(context, "target_uuid");
                            UUID targetUuid = UUID.fromString(targetUuidStr);
                            
                            var server = viewer.getServer();
                            if (server == null) {
                                viewer.sendSystemMessage(Component.literal("Server not available!")
                                    .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            
                            ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
                            if (target == null) {
                                viewer.sendSystemMessage(Component.literal("Target player not found!")
                                    .withStyle(ChatFormatting.RED));
                                return 0;
                            }
                            
                            DiscordInteractiveChat.showPlayerEnderChest(viewer, target);
                            return 1;
                            
                        } catch (Exception e) {
                            ErrorHandler.handleSystemError("Internal Interactive Commands", "show ender chest", e);
                            return 0;
                        }
                    })
                )
            );
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("Internal Interactive Commands", "register commands", e);
        }
    }
}
