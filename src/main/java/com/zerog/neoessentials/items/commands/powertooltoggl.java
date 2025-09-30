package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.data.ModDataComponents;
import com.zerog.neoessentials.config.CommandConfig;

public class powertooltoggl {
    /**
     * Register the /powertooltoggle and /pttoggle commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CommandConfig.isCommandEnabled("powertooltoggle")) return;
        dispatcher.register(
            Commands.literal("powertooltoggle")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool.toggle")) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    toggle(player);
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.powertool.toggle.success"), false);
                    return 1;
                })
        );
        dispatcher.register(
            Commands.literal("pttoggle")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool.toggle")) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    toggle(player);
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.powertool.toggle.success"), false);
                    return 1;
                })
        );
    }

    /**
     * Toggles powertool activation state for the item in the player's main hand using Data Components API.
     */
    public static void toggle(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!stack.isEmpty()) {
            boolean enabled = Boolean.TRUE.equals(stack.get(ModDataComponents.POWERTOOL_TOGGLE));
            stack.set(ModDataComponents.POWERTOOL_TOGGLE, !enabled);
        }
    }
}
