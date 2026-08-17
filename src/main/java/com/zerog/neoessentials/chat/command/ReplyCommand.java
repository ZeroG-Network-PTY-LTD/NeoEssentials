package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Handles the /reply command for replying to the last private message sender.
 */
public class ReplyCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplyCommand.class);
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NeoLog.debug(LOGGER, LogCategory.CHAT, "ReplyCommand - Registering /reply command");
        // Register with vanilla aliases to override vanilla behavior
        registerCommand(dispatcher, "reply");
        registerCommand(dispatcher, "r");
    }
    
    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    NeoLog.debug(LOGGER, LogCategory.CHAT, "ReplyCommand - Command executed!");
                    CommandSourceStack source = ctx.getSource();
                    String message = StringArgumentType.getString(ctx, "message");
                    
                    // Validate sender
                    ServerPlayer sender = source.getPlayer();
                    if (sender == null) {
                        source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                        return 0;
                    }
                    
                    // Find target from last message history
                    NeoLog.debug(LOGGER, LogCategory.CHAT, "ReplyCommand - Looking for last messager for {}", sender.getName().getString());
                    ServerPlayer target = com.zerog.neoessentials.chat.LastMessageManager.getLastMessager(sender);
                    NeoLog.debug(LOGGER, LogCategory.CHAT, "ReplyCommand - Found target: {}", (target != null ? target.getName().getString() : "null"));
                    if (target == null) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.no_target"));
                        return 0;
                    }
                    
                    // Show who we're replying to for confirmation
                    NeoLog.debug(LOGGER, LogCategory.CHAT, "Player {} replying to {}", sender.getName().getString(), target.getName().getString());
                    
                    // Check if target is still online
                    if (!target.getServer().getPlayerList().getPlayers().contains(target)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.target_offline"));
                        return 0;
                    }
                    
                    // Check if chat module is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.disabled"));
                        return 0;
                    }
                    
                    // Check if individual reply command is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("reply")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.disabled"));
                        return 0;
                    }
                    
                    // Legacy check for backwards compatibility
                    com.zerog.neoessentials.chat.ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isReplyEnabled()) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.disabled"));
                        return 0;
                    }
                    
                    // Proper permission validation using PermissionAPI
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.reply")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.no_permission"));
                        return 0;
                    }
                    
                    // --- Mute/ignore/msgtoggle check ---
                    if (com.zerog.neoessentials.chat.MuteManager.isMuted(sender)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.sender_muted"));
                        return 0;
                    }
                    
                    if (com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(target, sender)) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.reply.target_ignoring"));
                        return 0;
                    }
                    
                    if (com.zerog.neoessentials.chat.MsgToggleManager.isMsgToggled(target)) {
                        // Check if sender has bypass permission
                        if (!sender.hasPermissions(4) && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.msgtoggle.bypass")) {
                            source.sendFailure(MessageUtil.error("commands.neoessentials.reply.target_toggled_off", target.getName().getString()));
                            return 0;
                        }
                    }
                    
                    // Send reply messages using resolveTemplate for uniform placeholder support
                    String toTemplate  = MsgCommand.getMsgFormat("replyFormatTo",   "commands.neoessentials.reply.format.to");
                    String fromTemplate = MsgCommand.getMsgFormat("replyFormatFrom", "commands.neoessentials.reply.format.from");

                    // Pass target context for "To" (target's display name), sender for "From"
                    Map<String, String> vars = Map.of("message", message, "MESSAGE", message);
                    String resolvedToMessage   = MessageUtil.resolveTemplate(target, toTemplate,   vars);
                    String resolvedFromMessage = MessageUtil.resolveTemplate(sender, fromTemplate, vars);
                    
                    target.sendSystemMessage(MessageUtil.coloredText(resolvedFromMessage));
                    sender.sendSystemMessage(MessageUtil.coloredText(resolvedToMessage));
                    
                    // When replying, the target should now be able to reply back to the sender
                    NeoLog.debug(LOGGER, LogCategory.CHAT, "ReplyCommand - Setting last messager: {} can reply to {}", target.getName().getString(), sender.getName().getString());
                    com.zerog.neoessentials.chat.LastMessageManager.setLastMessager(target, sender);
                    
                    com.zerog.neoessentials.api.ChatAPI.broadcastSocialSpy(sender, target, message);
                    
                    // --- External plugin integration ---
                    com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastPrivateMessage(sender, target, message);
                    
                    return 1;
                })
            )
        );
    }
}
