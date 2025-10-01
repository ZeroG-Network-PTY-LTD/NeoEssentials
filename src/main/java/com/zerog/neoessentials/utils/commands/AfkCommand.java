package com.zerog.neoessentials.utils.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.chat.AfkManager;

/**
 * Handles the /afk command for toggling AFK (away from keyboard) status.
 */
public class AfkCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer sender = source.getPlayer();
                ChatManager chatManager = ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isAfkEnabled()) {
                    source.sendFailure(Component.translatable("neoessentials.afk.disabled"));
                    return 0;
                }
                if (chatManager != null && !chatManager.hasChatPermission("neoessentials.command.afk")) {
                    source.sendFailure(Component.translatable("neoessentials.no_permission"));
                    return 0;
                }
                // Implement actual AFK toggle logic
                AfkManager.toggleAfk(sender);
                source.sendSuccess(() -> Component.translatable("neoessentials.afk.toggled"), false);
                return 1;
            })
        );
    }
}
