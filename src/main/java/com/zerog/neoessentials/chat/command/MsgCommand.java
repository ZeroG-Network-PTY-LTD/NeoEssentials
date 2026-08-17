package com.zerog.neoessentials.chat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Handles the /msg command for private messaging between players.
 */
public class MsgCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MsgCommand.class);

    /**
     * Registers the /msg command with the dispatcher.
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Registering /msg command");
        // Register with vanilla aliases to override vanilla behavior
        registerCommand(dispatcher, "msg");
        registerCommand(dispatcher, "tell");
        registerCommand(dispatcher, "w");
        
        // Also register with test names to see if custom commands work at all
        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Also registering test commands: /message, /pm");
        registerCommand(dispatcher, "message");
        registerCommand(dispatcher, "pm");
    }
    
    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Command executed!");
                        CommandSourceStack source = ctx.getSource();
                        ServerPlayer target;
                        try {
                            target = EntityArgument.getPlayer(ctx, "target");
                        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.not_found", "Unknown"));
                            return 0;
                        }
                        String message = StringArgumentType.getString(ctx, "message");
                        
                        // Validate sender
                        ServerPlayer sender = source.getPlayer();
                        if (sender == null) {
                            source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                            return 0;
                        }
                        
                        MinecraftServer server = sender.getServer();
                        if (server == null) {
                            source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
                            return 0;
                        }
                        
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Processing message from {} to {}", sender.getName().getString(), target.getName().getString());
                        
                        // Check if messaging self
                        if (sender.equals(target)) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Player trying to message self");
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.self"));
                            return 0;
                        }
                        
                        // Check if chat module is enabled
                        if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Chat module is disabled");
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.disabled"));
                            return 0;
                        }
                        
                        // Check if individual msg command is enabled
                        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("msg")) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Msg command is disabled");
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.disabled"));
                            return 0;
                        }
                        
                        // Legacy check for backwards compatibility
                        ChatManager chatManager = ChatAPI.getChatManager();
                        if (chatManager != null && !chatManager.isMsgEnabled()) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Messaging is disabled (legacy check)");
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.disabled"));
                            return 0;
                        }
                        
                        // Proper permission validation using PermissionAPI
                        boolean hasPermission = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.msg");
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Permission check for {}: {}", sender.getName().getString(), hasPermission);
                        if (!hasPermission) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: No permission for neoessentials.chat.msg");
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.no_permission"));
                            return 0;
                        }
                        
                        // --- Mute/ignore/msgtoggle check ---
                        String senderName = sender.getName().getString();
                        boolean isMuted = com.zerog.neoessentials.chat.MuteManager.isMuted(sender);
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Checking mute for {}, result: {}", senderName, isMuted);
                        if (isMuted) {
                            // Log for debugging
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Player is muted");
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "Blocked /msg from muted player: {}", senderName);
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.sender_muted"));
                            return 0;
                        }
                        
                        if (com.zerog.neoessentials.chat.IgnoreManager.isIgnoring(target, sender)) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Target is ignoring sender");
                            source.sendFailure(MessageUtil.error("commands.neoessentials.msg.target_ignoring"));
                            return 0;
                        }
                        
                        if (com.zerog.neoessentials.chat.MsgToggleManager.isMsgToggled(target)) {
                            // Check if sender has bypass permission
                            if (!sender.hasPermissions(4) && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.msgtoggle.bypass")) {
                                NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - FAILED: Target has messaging toggled off and sender lacks bypass");
                                source.sendFailure(MessageUtil.error("commands.neoessentials.msg.target_toggled_off", target.getName().getString()));
                                return 0;
                            }
                        }
                        
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - SUCCESS: All checks passed, sending message");
                        
                        // Get templates (config override > lang key)
                        String toTemplate  = getMsgFormat("msgFormatTo",  "commands.neoessentials.msg.format.to");
                        String fromTemplate = getMsgFormat("msgFormatFrom", "commands.neoessentials.msg.format.from");

                        // Resolve templates: {message}/{MESSAGE} + PlaceholderAPI for remaining tokens.
                        // Pass target context for "To" message ({neoessentials_displayname} = target name)
                        // Pass sender context for "From" message ({neoessentials_displayname} = sender name)
                        Map<String, String> vars = Map.of("message", message, "MESSAGE", message);
                        String resolvedToMessage   = MessageUtil.resolveTemplate(target, toTemplate,   vars);
                        String resolvedFromMessage = MessageUtil.resolveTemplate(sender, fromTemplate, vars);
                        
                        target.sendSystemMessage(MessageUtil.coloredText(resolvedFromMessage));
                        sender.sendSystemMessage(MessageUtil.coloredText(resolvedToMessage));
                        
                        // Update last message tracking for reply functionality
                        // Only the target should be able to reply to the sender
                        NeoLog.debug(LOGGER, LogCategory.CHAT, "MsgCommand - Setting last messager: {} can reply to {}", target.getName().getString(), sender.getName().getString());
                        com.zerog.neoessentials.chat.LastMessageManager.setLastMessager(target, sender);
                        
                        // --- SocialSpy integration ---
                        ChatAPI.broadcastSocialSpy(sender, target, message);
                        
                        // --- External plugin integration ---
                        com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastPrivateMessage(sender, target, message);
                        
                        return 1;
                    })
                )
            )
        );
    }

    /**
     * Retrieves a private-message format template.
     * Priority: {@code chat.messaging.<configKey>} in config.json → lang key fallback.
     *
     * @param configKey  Key name inside {@code chat.messaging} section of config.json
     * @param langKey    Translation key fallback
     */
    static String getMsgFormat(String configKey, String langKey) {
        try {
            com.google.gson.JsonObject chatConfig =
                com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig != null && chatConfig.has("messaging")) {
                com.google.gson.JsonObject messaging = chatConfig.getAsJsonObject("messaging");
                if (messaging != null && messaging.has(configKey)) {
                    String fmt = messaging.get(configKey).getAsString();
                    if (fmt != null && !fmt.isBlank()) return fmt;
                }
            }
        } catch (Exception ignored) {}
        return MessageUtil.localize(langKey);
    }
}
