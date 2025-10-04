package com.zerog.neoessentials.utils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.chat.AfkManager;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.MessageUtil;

/**
 * Handles the /afk command for toggling AFK (away from keyboard) status.
 * Supports optional custom AFK messages and admin functionality.
 */
public class AfkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
            .requires(cs -> cs.getEntity() instanceof ServerPlayer)
            // /afk [message] - Toggle AFK with optional message
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.afk");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    // Check if AFK command is enabled
                    ChatManager chatManager = ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isAfkEnabled()) {
                        ctx.getSource().sendFailure(Component.translatable("commands.neoessentials.afk.disabled"));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    String message = StringArgumentType.getString(ctx, "message");
                    
                    // Toggle AFK with custom message
                    AfkManager.getInstance().toggleAfk(player, message);
                    
                    boolean isNowAfk = AfkManager.getInstance().isAfk(player);
                    if (isNowAfk) {
                        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.afk.enabled"), false);
                    } else {
                        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.afk.disabled_self"), false);
                    }
                    return 1;
                })
            )
            // /afk - Toggle AFK without message
            .executes(ctx -> {
                PermissionValidator.PermissionResult permResult = 
                    PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.afk");
                if (!permResult.hasPermission()) {
                    ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                    return 0;
                }
                
                // Check if AFK command is enabled
                ChatManager chatManager = ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isAfkEnabled()) {
                    ctx.getSource().sendFailure(Component.translatable("commands.neoessentials.afk.disabled"));
                    return 0;
                }
                
                ServerPlayer player = permResult.getPlayer();
                
                // Toggle AFK without message
                AfkManager.getInstance().toggleAfk(player, null);
                
                boolean isNowAfk = AfkManager.getInstance().isAfk(player);
                if (isNowAfk) {
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.afk.enabled"), false);
                } else {
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.afk.disabled_self"), false);
                }
                return 1;
            })
        );
    }
}
