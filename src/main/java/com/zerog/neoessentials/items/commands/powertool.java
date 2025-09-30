package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.zerog.neoessentials.data.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.CommandConfig;

public class powertool {
    /**
     * Register the /powertool and /pt commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CommandConfig.isCommandEnabled("powertool")) return;
        dispatcher.register(
            Commands.literal("powertool")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool")) {
                            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                            return 0;
                        }
                        String cmd = StringArgumentType.getString(ctx, "command");
                        assign(player, cmd);
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.powertool.assign.success"), false);
                        return 1;
                    })
                )
        );
        dispatcher.register(
            Commands.literal("pt")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool")) {
                            ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                            return 0;
                        }
                        String cmd = StringArgumentType.getString(ctx, "command");
                        assign(player, cmd);
                        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.powertool.assign.success"), false);
                        return 1;
                    })
                )
        );
    }

    /**
     * Assigns a command to the item in the player's main hand using Data Components API.
     */
    public static void assign(ServerPlayer player, String command) {
        var stack = player.getMainHandItem();
        if (!stack.isEmpty()) {
            stack.set(ModDataComponents.POWERTOOL_COMMAND, command);
        }
    }
}
