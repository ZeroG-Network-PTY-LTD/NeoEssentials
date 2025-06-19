package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.PermissionUtil;
import com.zerog.neoessentials.utils.TextUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.*;

/**
 * Implements messaging commands like /msg, /r, /broadcast, /mail, etc
 */
public class MessagingCommands {
    
    // Last message tracking for reply command
    private final Map<UUID, UUID> lastMessageSender = new HashMap<>();
    private final Map<UUID, UUID> lastMessageRecipient = new HashMap<>();
    
    // Players who have disabled messages
    private final Set<UUID> msgToggledOff = new HashSet<>();
    
    // Players who have toggled recipient/sender reply mode
    private final Set<UUID> replyToRecipient = new HashSet<>();
    
    // Players with social spy enabled
    private final Set<UUID> socialSpyEnabled = new HashSet<>();
    
    /**
     * Registers all message-related commands
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /msg <player> <message>
        dispatcher.register(Commands.literal("msg")
            .requires(source -> CommandManager.hasPermission(source, "essentials.msg"))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        ServerPlayer recipient = EntityArgument.getPlayer(context, "player");
                        String message = StringArgumentType.getString(context, "message");
                        return sendPrivateMessage(context, recipient, message);
                    })
                )
            )
        );
        
        // Aliases for /msg
        registerMessageAliases(dispatcher, "w");
        registerMessageAliases(dispatcher, "tell");
        registerMessageAliases(dispatcher, "whisper");
        registerMessageAliases(dispatcher, "t");
        registerMessageAliases(dispatcher, "pm");
        
        // /r <message>
        dispatcher.register(Commands.literal("r")
            .requires(source -> CommandManager.hasPermission(source, "essentials.msg"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String message = StringArgumentType.getString(context, "message");
                    return replyCommand(context, message);
                })
            )
        );
        
        // Alias for /r
        registerReplyAliases(dispatcher, "reply");
        
        // /msgtoggle [on|off] [player]
        dispatcher.register(Commands.literal("msgtoggle")
            .requires(source -> CommandManager.hasPermission(source, "essentials.msgtoggle"))
            .executes(context -> toggleMsgCommand(context, null, context.getSource().getPlayerOrException()))
            .then(Commands.literal("on")
                .executes(context -> toggleMsgCommand(
                    context, 
                    true,
                    context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.msgtoggle.others"))
                    .executes(context -> toggleMsgCommand(
                        context,
                        true,
                        EntityArgument.getPlayer(context, "player")
                    ))
                )
            )
            .then(Commands.literal("off")
                .executes(context -> toggleMsgCommand(
                    context, 
                    false,
                    context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.msgtoggle.others"))
                    .executes(context -> toggleMsgCommand(
                        context,
                        false,
                        EntityArgument.getPlayer(context, "player")
                    ))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> CommandManager.hasPermission(source, "essentials.msgtoggle.others"))
                .executes(context -> toggleMsgCommand(
                    context,
                    null,
                    EntityArgument.getPlayer(context, "player")
                ))
            )
        );
        
        // /rtoggle [on|off] [player]
        dispatcher.register(Commands.literal("rtoggle")
            .requires(source -> CommandManager.hasPermission(source, "essentials.rtoggle"))
            .executes(context -> toggleReplyModeCommand(context, null, context.getSource().getPlayerOrException()))
            .then(Commands.literal("on")
                .executes(context -> toggleReplyModeCommand(
                    context, 
                    true,
                    context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.rtoggle.others"))
                    .executes(context -> toggleReplyModeCommand(
                        context,
                        true,
                        EntityArgument.getPlayer(context, "player")
                    ))
                )
            )
            .then(Commands.literal("off")
                .executes(context -> toggleReplyModeCommand(
                    context, 
                    false,
                    context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.rtoggle.others"))
                    .executes(context -> toggleReplyModeCommand(
                        context,
                        false,
                        EntityArgument.getPlayer(context, "player")
                    ))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> CommandManager.hasPermission(source, "essentials.rtoggle.others"))
                .executes(context -> toggleReplyModeCommand(
                    context,
                    null,
                    EntityArgument.getPlayer(context, "player")
                ))
            )
        );
        
        // /socialspy [on|off] [player]
        dispatcher.register(Commands.literal("socialspy")
            .requires(source -> CommandManager.hasPermission(source, "essentials.socialspy"))
            .executes(context -> toggleSocialSpyCommand(context, null, context.getSource().getPlayerOrException()))
            .then(Commands.literal("on")
                .executes(context -> toggleSocialSpyCommand(
                    context, 
                    true,
                    context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.socialspy.others"))
                    .executes(context -> toggleSocialSpyCommand(
                        context,
                        true,
                        EntityArgument.getPlayer(context, "player")
                    ))
                )
            )
            .then(Commands.literal("off")
                .executes(context -> toggleSocialSpyCommand(
                    context, 
                    false,
                    context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> CommandManager.hasPermission(source, "essentials.socialspy.others"))
                    .executes(context -> toggleSocialSpyCommand(
                        context,
                        false,
                        EntityArgument.getPlayer(context, "player")
                    ))
                )
            )
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> CommandManager.hasPermission(source, "essentials.socialspy.others"))
                .executes(context -> toggleSocialSpyCommand(
                    context,
                    null,
                    EntityArgument.getPlayer(context, "player")
                ))
            )
        );
        
        // /broadcast <message>
        dispatcher.register(Commands.literal("broadcast")
            .requires(source -> CommandManager.hasPermission(source, "essentials.broadcast"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> broadcastCommand(context, StringArgumentType.getString(context, "message")))
            )
        );
        
        // Aliases for broadcast
        registerBroadcastAliases(dispatcher, "bc");
        registerBroadcastAliases(dispatcher, "bcast");
    }
    
    /**
     * Register aliases for the msg command with the same structure
     */
    private void registerMessageAliases(CommandDispatcher<CommandSourceStack> dispatcher, String alias) {
        dispatcher.register(Commands.literal(alias)
            .requires(source -> CommandManager.hasPermission(source, "essentials.msg"))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        ServerPlayer recipient = EntityArgument.getPlayer(context, "player");
                        String message = StringArgumentType.getString(context, "message");
                        return sendPrivateMessage(context, recipient, message);
                    })
                )
            )
        );
    }
    
    /**
     * Register aliases for the reply command with the same structure
     */
    private void registerReplyAliases(CommandDispatcher<CommandSourceStack> dispatcher, String alias) {
        dispatcher.register(Commands.literal(alias)
            .requires(source -> CommandManager.hasPermission(source, "essentials.msg"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> {
                    String message = StringArgumentType.getString(context, "message");
                    return replyCommand(context, message);
                })
            )
        );
    }
    
    /**
     * Register aliases for the broadcast command with the same structure
     */
    private void registerBroadcastAliases(CommandDispatcher<CommandSourceStack> dispatcher, String alias) {
        dispatcher.register(Commands.literal(alias)
            .requires(source -> CommandManager.hasPermission(source, "essentials.broadcast"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(context -> broadcastCommand(context, StringArgumentType.getString(context, "message")))
            )
        );
    }
    
    /**
     * Sends a private message from one player to another
     */
    private int sendPrivateMessage(CommandContext<CommandSourceStack> context, ServerPlayer recipient, String message) 
        throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        
        // Check if sender is messaging themselves
        if (sender.getUUID().equals(recipient.getUUID())) {
            context.getSource().sendFailure(Component.translatable("commands.message.sameTarget"));
            return 0;
        }
        
        // Check if recipient has messages toggled off
        if (msgToggledOff.contains(recipient.getUUID()) && 
            !CommandManager.hasPermission(context.getSource(), "essentials.msgtoggle.bypass")) {
            context.getSource().sendFailure(Component.translatable("commands.message.ignored", recipient.getDisplayName()));
            return 0;
        }
        
        // Format and colorize the message if allowed
        boolean canUseColor = CommandManager.hasPermission(context.getSource(), "essentials.msg.color");
        String formattedMessage = canUseColor ? TextUtil.translateColors(message) : message;
        
        // Build the message components
        Component senderMsg = Component.translatable("commands.message.display.outgoing", 
                                                   recipient.getDisplayName(), 
                                                   formattedMessage)
                                     .withStyle(style -> style.withColor(TextColor.fromRgb(0xA0A0A0)));
        
        Component recipientMsg = Component.translatable("commands.message.display.incoming", 
                                                      sender.getDisplayName(), 
                                                      formattedMessage)
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(0xA0A0A0)));
        
        // Send the messages
        context.getSource().sendSuccess(() -> senderMsg, false);
        recipient.sendSystemMessage(recipientMsg);
        
        // Send to social spies
        sendToSocialSpies(sender, recipient, formattedMessage);
        
        // Update last message tracking
        lastMessageSender.put(recipient.getUUID(), sender.getUUID());
        lastMessageRecipient.put(sender.getUUID(), recipient.getUUID());
        
        return 1;
    }
    
    /**
     * Implements the reply command to message the last person who messaged you
     */
    private int replyCommand(CommandContext<CommandSourceStack> context, String message) 
        throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        UUID senderUuid = sender.getUUID();
        
        // Determine who to reply to based on sender's reply mode
        UUID targetUuid = isReplyingToRecipient(senderUuid) ? 
                          lastMessageRecipient.get(senderUuid) : 
                          lastMessageSender.get(senderUuid);
        
        if (targetUuid == null) {
            context.getSource().sendFailure(Component.translatable("commands.message.noReply"));
            return 0;
        }
        
        // Find the player in the server
        ServerPlayer recipient = context.getSource().getServer().getPlayerList().getPlayer(targetUuid);
        if (recipient == null) {
            context.getSource().sendFailure(Component.translatable("commands.message.playerNotFound"));
            return 0;
        }
        
        // Forward to the regular message handler
        return sendPrivateMessage(context, recipient, message);
    }
    
    /**
     * Toggle whether a player receives private messages
     */
    private int toggleMsgCommand(CommandContext<CommandSourceStack> context, Boolean enabled, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        boolean isIgnoring = msgToggledOff.contains(playerUuid);
        
        // If enabled is specified, set to that value, otherwise toggle
        boolean shouldIgnore = (enabled != null) ? !enabled : !isIgnoring;
        
        if (shouldIgnore) {
            msgToggledOff.add(playerUuid);
        } else {
            msgToggledOff.remove(playerUuid);
        }
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            if (shouldIgnore) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.msgtoggle.disabled.self"), true);
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.msgtoggle.enabled.self"), true);
            }
        } else {
            if (shouldIgnore) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.msgtoggle.disabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.msgtoggle.disabled.by", context.getSource().getDisplayName()));
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.msgtoggle.enabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(Component.translatable("commands.msgtoggle.enabled.by", context.getSource().getDisplayName()));
            }
        }
        
        return 1;
    }
    
    /**
     * Toggle whether the reply command targets the last recipient or last sender
     */
    private int toggleReplyModeCommand(CommandContext<CommandSourceStack> context, Boolean enabled, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        boolean isReplyingToRecipient = replyToRecipient.contains(playerUuid);
        
        // If enabled is specified, set to that value, otherwise toggle
        boolean shouldReplyToRecipient = (enabled != null) ? enabled : !isReplyingToRecipient;
        
        if (shouldReplyToRecipient) {
            replyToRecipient.add(playerUuid);
        } else {
            replyToRecipient.remove(playerUuid);
        }
        
        String modeDescription = shouldReplyToRecipient ? 
                "commands.rtoggle.recipient" : "commands.rtoggle.sender";
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            context.getSource().sendSuccess(() -> 
                Component.translatable("commands.rtoggle.success.self", Component.translatable(modeDescription)), true);
        } else {
            context.getSource().sendSuccess(() -> 
                Component.translatable("commands.rtoggle.success.other", 
                    player.getDisplayName(), 
                    Component.translatable(modeDescription)), true);
            player.sendSystemMessage(
                Component.translatable("commands.rtoggle.updated", Component.translatable(modeDescription))
            );
        }
        
        return 1;
    }
    
    /**
     * Toggle social spy mode for a player
     */
    private int toggleSocialSpyCommand(CommandContext<CommandSourceStack> context, Boolean enabled, ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        boolean hasSpyEnabled = socialSpyEnabled.contains(playerUuid);
        
        // If enabled is specified, set to that value, otherwise toggle
        boolean shouldSpyBeEnabled = (enabled != null) ? enabled : !hasSpyEnabled;
        
        if (shouldSpyBeEnabled) {
            socialSpyEnabled.add(playerUuid);
        } else {
            socialSpyEnabled.remove(playerUuid);
        }
        
        // Send confirmation message
        if (player == context.getSource().getEntity()) {
            if (shouldSpyBeEnabled) {
                context.getSource().sendSuccess(() -> Component.translatable("commands.socialspy.enabled.self"), true);
            } else {
                context.getSource().sendSuccess(() -> Component.translatable("commands.socialspy.disabled.self"), true);
            }
        } else {
            if (shouldSpyBeEnabled) {
                context.getSource().sendSuccess(() -> 
                    Component.translatable("commands.socialspy.enabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(
                    Component.translatable("commands.socialspy.enabled.by", context.getSource().getDisplayName())
                );
            } else {
                context.getSource().sendSuccess(() -> 
                    Component.translatable("commands.socialspy.disabled.other", player.getDisplayName()), true);
                player.sendSystemMessage(
                    Component.translatable("commands.socialspy.disabled.by", context.getSource().getDisplayName())
                );
            }
        }
        
        return 1;
    }
    
    /**
     * Broadcast a message to all online players
     */
    private int broadcastCommand(CommandContext<CommandSourceStack> context, String message) {
        // Format and colorize the message if allowed
        boolean canUseColor = CommandManager.hasPermission(context.getSource(), "essentials.broadcast.color");
        String formattedMessage = canUseColor ? TextUtil.translateColors(message) : message;
        
        // Create the broadcast component
        Component broadcastMsg = Component.translatable("chat.type.announcement", 
                "Server", formattedMessage)
                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFA500)));
        
        // Send to all players
        context.getSource().getServer().getPlayerList().broadcastSystemMessage(broadcastMsg, false);
        
        return 1;
    }
    
    /**
     * Checks if a player has their message receiving toggled off
     */
    public boolean isIgnoringMessages(UUID playerUuid) {
        return msgToggledOff.contains(playerUuid);
    }
    
    /**
     * Checks if a player is replying to the last recipient or sender
     * True = replying to recipient, False = replying to sender
     */
    public boolean isReplyingToRecipient(UUID playerUuid) {
        return replyToRecipient.contains(playerUuid);
    }
    
    /**
     * Checks if a player has social spy enabled
     */
    public boolean hasSocialSpyEnabled(UUID playerUuid) {
        return socialSpyEnabled.contains(playerUuid);
    }
    
    /**
     * Send a private message to all players with social spy enabled
     */
<<<<<<< HEAD
<<<<<<< HEAD
    private void sendToSocialSpies(ServerPlayer sender, ServerPlayer recipient, String message) {        // Skip if sender or recipient has exemption
        if (PermissionUtil.hasPermission((ServerPlayer)sender, "essentials.chat.spy.exempt") || 
            PermissionUtil.hasPermission((ServerPlayer)recipient, "essentials.chat.spy.exempt")) {
=======
    private void sendToSocialSpies(ServerPlayer sender, ServerPlayer recipient, String message) {
<<<<<<< HEAD
        // Skip if sender or recipient has exemption
        if (PermissionUtil.hasPermission(sender, "essentials.chat.spy.exempt") || 
            PermissionUtil.hasPermission(recipient, "essentials.chat.spy.exempt")) {
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
        // Skip if sender or recipient has exemption        if (PermissionUtil.hasPermission((ServerPlayer)sender, "essentials.chat.spy.exempt") || 
=======
    private void sendToSocialSpies(ServerPlayer sender, ServerPlayer recipient, String message) {        // Skip if sender or recipient has exemption
        if (PermissionUtil.hasPermission((ServerPlayer)sender, "essentials.chat.spy.exempt") || 
>>>>>>> 18240f3 (fix: Update permission checks in JailCommands, MessagingCommands, and KitManager to ensure proper player type handling)
            PermissionUtil.hasPermission((ServerPlayer)recipient, "essentials.chat.spy.exempt")) {
>>>>>>> 30e3241 (Refactor code structure for improved readability and maintainability)
            return;
        }
        
        PlayerList playerList = sender.getServer().getPlayerList();
        Component spyMessage = Component.translatable("commands.socialspy.format", 
                                                    sender.getDisplayName(), 
                                                    recipient.getDisplayName(), 
                                                    message)
                                      .withStyle(style -> style.withColor(TextColor.fromRgb(0x7289DA)));
        
        // Send to all players with social spy enabled
        for (ServerPlayer player : playerList.getPlayers()) {
            if (socialSpyEnabled.contains(player.getUUID()) && 
                !player.getUUID().equals(sender.getUUID()) && 
                !player.getUUID().equals(recipient.getUUID()) &&
<<<<<<< HEAD
<<<<<<< HEAD
                PermissionUtil.hasPermission((ServerPlayer)player, "essentials.socialspy")) {
=======
                PermissionUtil.hasPermission(player, "essentials.socialspy")) {
>>>>>>> 1fb47d4 (Implement messaging and moderation commands, add time utility for duration parsing)
=======
                PermissionUtil.hasPermission((ServerPlayer)player, "essentials.socialspy")) {
>>>>>>> 30e3241 (Refactor code structure for improved readability and maintainability)
                player.sendSystemMessage(spyMessage);
            }
        }
    }
}
