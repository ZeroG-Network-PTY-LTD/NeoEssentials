package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import com.zerog.neoessentials.economy.EconomyLocalization;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class PayToggleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("paytoggle")
                .requires(src -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer() != null ? src.getPlayer().getUUID() : null, "neoessentials.economy.paytoggle"))
                .executes(ctx -> execute(ctx))
        );
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.no_permission"));
            return 0;
        }
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.economy.paytoggle")) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.no_permission"));
            return 0;
        }
        java.util.UUID uuid = player.getUUID();
        boolean current = PayToggleManager.getInstance().getPayToggle(uuid);
        boolean newState = !current;
        PayToggleManager.getInstance().setPayToggle(uuid, newState);
        if (newState) {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.paytoggle.enabled"), false);
        } else {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.paytoggle.disabled"), false);
        }
        return 1;
    }
}
