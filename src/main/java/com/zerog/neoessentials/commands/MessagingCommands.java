package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.MessagingManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * Messaging command implementation
 * Handles /msg, /r, /mail, /broadcast commands
 */
public class MessagingCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /msg <player> <message> - Send private message
        dispatcher.register(Commands.literal("msg")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> sendPrivateMessage(context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "message")))
                )
            )
        );
        
        // /tell <player> <message> - Alias for /msg
        dispatcher.register(Commands.literal("tell")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> sendPrivateMessage(context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "message")))
                )
            )
        );
        
        // /r <message> - Reply to last message
        dispatcher.register(Commands.literal("r")
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> replyToMessage(context, StringArgumentType.getString(context, "message")))
            )
        );
        
        // /mail send <player> <message> - Send mail
        dispatcher.register(Commands.literal("mail")
            .then(Commands.literal("send")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendMail(context,
                            StringArgumentType.getString(context, "player"),
                            StringArgumentType.getString(context, "message")))
                    )
                )
            )
            .then(Commands.literal("read")
                .executes(MessagingCommands::readMail)
            )
            .then(Commands.literal("clear")
                .executes(MessagingCommands::clearMail)
            )
        );
        
        // /broadcast <message> - Broadcast message (admin only)
        dispatcher.register(Commands.literal("broadcast")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> broadcastMessage(context, StringArgumentType.getString(context, "message")))
            )
        );
        
        // /ignore <player> - Ignore player
        dispatcher.register(Commands.literal("ignore")
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> ignorePlayer(context, StringArgumentType.getString(context, "player")))
            )
        );
        
        // /unignore <player> - Unignore player
        dispatcher.register(Commands.literal("unignore")
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> unignorePlayer(context, StringArgumentType.getString(context, "player")))
            )
        );
    }
    
    private static int sendPrivateMessage(CommandContext<CommandSourceStack> context, ServerPlayer target, String message) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        boolean success = messagingManager.sendPrivateMessage(sender, target.getName().getString(), message);
        return success ? 1 : 0;
    }
    
    private static int replyToMessage(CommandContext<CommandSourceStack> context, String message) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        boolean success = messagingManager.replyToMessage(sender, message);
        return success ? 1 : 0;
    }
    
    private static int sendMail(CommandContext<CommandSourceStack> context, String targetName, String message) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        boolean success = messagingManager.sendMail(sender, targetName, message);
        return success ? 1 : 0;
    }
    
    private static int readMail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        messagingManager.readMail(player);
        return 1;
    }
    
    private static int clearMail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        boolean success = messagingManager.clearMail(player);
        return success ? 1 : 0;
    }
    
    private static int broadcastMessage(CommandContext<CommandSourceStack> context, String message) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        messagingManager.broadcast(sender, message);
        return 1;
    }
    
    private static int ignorePlayer(CommandContext<CommandSourceStack> context, String targetName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        boolean success = messagingManager.ignorePlayer(player, targetName);
        return success ? 1 : 0;
    }
    
    private static int unignorePlayer(CommandContext<CommandSourceStack> context, String targetName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessagingManager messagingManager = MessagingManager.getInstance();
        
        boolean success = messagingManager.unignorePlayer(player, targetName);
        return success ? 1 : 0;
    }
}
