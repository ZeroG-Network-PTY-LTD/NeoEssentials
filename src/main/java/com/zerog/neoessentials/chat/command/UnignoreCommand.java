
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Handles the /unignore command for removing a player from the ignore list.
 */
public class UnignoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unignore")
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    String targetName = StringArgumentType.getString(ctx, "target");
                    
                    // Validate sender
                    ServerPlayer sender = source.getPlayer();
                    if (sender == null) {
                        source.sendFailure(Component.translatable("neoessentials.error.no_server"));
                        return 0;
                    }
                    
                    // Check permissions
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isUnignoreEnabled()) {
                        source.sendFailure(Component.translatable("commands.neoessentials.unignore.disabled"));
                        return 0;
                    }
                    
                    // Proper permission validation using PermissionAPI
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.unignore")) {
                        source.sendFailure(Component.translatable("commands.neoessentials.unignore.no_permission"));
                        return 0;
                    }
                    
                    com.zerog.neoessentials.chat.IgnoreManager.unignore(sender, targetName);
                    source.sendSuccess(() -> Component.translatable("commands.neoessentials.unignore.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
