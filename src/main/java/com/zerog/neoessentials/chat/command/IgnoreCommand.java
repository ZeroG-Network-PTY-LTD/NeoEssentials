
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles the /ignore command for ignoring messages from a player.
 */
public class IgnoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ignore")
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
                    
                    // Check if trying to ignore self
                    if (sender.getName().getString().equalsIgnoreCase(targetName)) {
                        source.sendFailure(Component.translatable("commands.neoessentials.ignore.self"));
                        return 0;
                    }
                    
                    // Check permissions
                    ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                    if (chatManager != null && !chatManager.isIgnoreEnabled()) {
                        source.sendFailure(Component.translatable("commands.neoessentials.ignore.disabled"));
                        return 0;
                    }
                    
                    // Proper permission validation using PermissionAPI
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.ignore")) {
                        source.sendFailure(Component.translatable("commands.neoessentials.ignore.no_permission"));
                        return 0;
                    }
                    
                    com.zerog.neoessentials.chat.IgnoreManager.ignore(sender, targetName);
                    source.sendSuccess(() -> Component.translatable("commands.neoessentials.ignore.success", targetName), false);
                    return 1;
                })
            )
        );
    }
}
