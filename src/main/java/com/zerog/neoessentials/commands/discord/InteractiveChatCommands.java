package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
// import com.zerog.neoessentials.discord.DiscordInteractiveChat; // TEMPORARILY DISABLED
import com.zerog.neoessentials.error.ErrorHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * InteractiveChat-style commands for NeoEssentials
 * Provides manual commands to trigger interactive features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class InteractiveChatCommands {
    
    /**
     * Register all InteractiveChat-style commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        try {
            // /ic reload - Reload InteractiveChat (like the original plugin)
            dispatcher.register(Commands.literal("ic")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        try {
                            var source = context.getSource();
                            source.sendSuccess(() -> Component.literal("InteractiveChat system reloaded!")
                                .withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        } catch (Exception e) {
                            ErrorHandler.handleSystemError("InteractiveChat Commands", "reload", e);
                            return 0;
                        }
                    })
                )
                .then(Commands.literal("list")
                    .executes(context -> {
                        try {
                            var source = context.getSource();
                            var player = source.getPlayerOrException();
                            
                            // Show available placeholders (like InteractiveChat)
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("▬▬▬▬▬▬ ").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY))
                                .append(Component.literal("INTERACTIVE PLACEHOLDERS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                .append(Component.literal(" ▬▬▬▬▬▬").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("[item]").withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" - Show your held item").withStyle(ChatFormatting.GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("[inv]").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("[inventory]").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" - Show your inventory").withStyle(ChatFormatting.GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("[ender]").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("[enderchest]").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal("[echest]").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" - Show your ender chest").withStyle(ChatFormatting.GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("[pos]").withStyle(ChatFormatting.BLUE, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" - Show your coordinates").withStyle(ChatFormatting.GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("[health]").withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" - Show your health status").withStyle(ChatFormatting.GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("")
                                .append(Component.literal("[time]").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE))
                                .append(Component.literal(" - Show the current time").withStyle(ChatFormatting.GRAY))
                            );
                            
                            player.sendSystemMessage(Component.literal("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                                .withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.DARK_GRAY));
                            
                            player.sendSystemMessage(Component.literal("Use these placeholders in chat to create interactive messages!")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                            
                            return 1;
                        } catch (Exception e) {
                            ErrorHandler.handleSystemError("InteractiveChat Commands", "list", e);
                            return 0;
                        }
                    })
                )
                .then(Commands.literal("chat")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            try {
                                var player = context.getSource().getPlayerOrException();
                                String message = StringArgumentType.getString(context, "message");
                                
                                // Simulate a chat message with interactive content
                                // This triggers our InteractiveChat system
                                var server = player.getServer();
                                if (server != null) {
                                    // Send the message through chat normally, our event handler will catch it
                                    server.getPlayerList().broadcastSystemMessage(
                                        Component.literal("<" + player.getName().getString() + "> " + message), false
                                    );
                                    
                                    player.sendSystemMessage(Component.literal("Sent interactive chat message!")
                                        .withStyle(ChatFormatting.GREEN));
                                }
                                
                                return 1;
                            } catch (Exception e) {
                                ErrorHandler.handleSystemError("InteractiveChat Commands", "chat", e);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("viewinv")
                    .then(Commands.argument("target", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            try {
                                var viewer = context.getSource().getPlayerOrException();
                                var target = EntityArgument.getPlayer(context, "target");
                                
                                // DiscordInteractiveChat.showPlayerInventory(viewer, target);
                                return 1;
                            } catch (Exception e) {
                                ErrorHandler.handleSystemError("InteractiveChat Commands", "viewinv", e);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("viewender")
                    .then(Commands.argument("target", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            try {
                                var viewer = context.getSource().getPlayerOrException();
                                var target = EntityArgument.getPlayer(context, "target");
                                
                                // DiscordInteractiveChat.showPlayerEnderChest(viewer, target);
                                return 1;
                            } catch (Exception e) {
                                ErrorHandler.handleSystemError("InteractiveChat Commands", "viewender", e);
                                return 0;
                            }
                        })
                    )
                )
                .then(Commands.literal("viewitem")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            try {
                                var viewer = context.getSource().getPlayerOrException();
                                var target = EntityArgument.getPlayer(context, "target");
                                
                                // DiscordInteractiveChat.showPlayerItem(viewer, target);
                                return 1;
                            } catch (Exception e) {
                                ErrorHandler.handleSystemError("InteractiveChat Commands", "viewitem", e);
                                return 0;
                            }
                        })
                    )
                )
            );
            
            // Alternative command aliases (like InteractiveChat)
            dispatcher.register(Commands.literal("interactivechat")
                .redirect(dispatcher.getRoot().getChild("ic"))
            );
            
        } catch (Exception e) {
            ErrorHandler.handleSystemError("InteractiveChat Commands", "register commands", e);
        }
    }
}
