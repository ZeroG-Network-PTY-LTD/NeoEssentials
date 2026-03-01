package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.chat.AfkManager;
import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.MessageUtil;

/**
 * Handles the /afk command for toggling AFK (away from keyboard) status.
 * Supports optional custom AFK messages and admin functionality.
 */
public class AfkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register main command
        registerAfkCommand(dispatcher, "afk");
        // Register alias
        registerAfkCommand(dispatcher, "away");
    }
    
    private static void registerAfkCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            // /afk [message] - Toggle AFK with optional message
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.afk.player_only");
                    if (player == null) return 0;

                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.afk");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    // Check if chat module is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.afk.disabled"));
                        return 0;
                    }
                    
                    // Check if individual afk command is enabled
                    if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("afk")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.afk.disabled"));
                        return 0;
                    }
                    
                    // Legacy check for backwards compatibility
                    ChatManager chatManager = ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isAfkEnabled()) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.afk.disabled"));
                        return 0;
                    }
                    
                    String message = StringArgumentType.getString(ctx, "message");
                    // Determine current state to give correct feedback
                    boolean wasAfk = AfkManager.getInstance().isAfk(player);
                    AfkManager.getInstance().toggleAfk(player, message);
                    // Personal feedback (broadcast is handled inside AfkManager)
                    if (!wasAfk) {
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eYou are now AFK."), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eYou are no longer AFK."), false);
                    }
                    return 1;
                })
            )
            // /afk - Toggle AFK without message
            .executes(ctx -> {
                ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.afk.player_only");
                if (player == null) return 0;

                PermissionValidator.PermissionResult permResult =
                    PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.afk");
                if (!permResult.hasPermission()) {
                    ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                    return 0;
                }
                
                // Check if chat module is enabled
                if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.afk.disabled"));
                    return 0;
                }
                
                // Check if individual afk command is enabled
                if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("afk")) {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.afk.disabled"));
                    return 0;
                }
                
                // Legacy check for backwards compatibility
                ChatManager chatManager = ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isAfkEnabled()) {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.afk.disabled"));
                    return 0;
                }
                
                // Toggle AFK without message (player already retrieved above)
                boolean wasAfk = AfkManager.getInstance().isAfk(player);
                AfkManager.getInstance().toggleAfk(player, null);
                // Personal feedback
                if (!wasAfk) {
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eYou are now AFK."), false);
                } else {
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§eYou are no longer AFK."), false);
                }
                return 1;
            })
        );
    }
}
