package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class BaltopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("baltop")
                .requires(src -> com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer() != null ? src.getPlayer().getUUID() : null, "neoessentials.economy.baltop"))
                .executes(ctx -> execute(ctx))
        );
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!EconomyManager.getInstance().isEnabled()) return 0;
        ServerPlayer sender = null;
        try {
            sender = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.baltop.no_permission"));
            return 0;
        }
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.economy.baltop")) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        // Get all balances and sort by value descending
        java.util.Map<UUID, java.math.BigDecimal> all = EconomyManager.getInstance().getAllBalances();
        java.util.List<java.util.Map.Entry<UUID, java.math.BigDecimal>> top = all.entrySet().stream()
            .sorted(java.util.Map.Entry.<UUID, java.math.BigDecimal>comparingByValue(java.util.Comparator.reverseOrder()))
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.baltop.header"), false);
        final int[] rank = {1};
        for (java.util.Map.Entry<UUID, java.math.BigDecimal> entry : top) {
            String name = ctx.getSource().getServer().getProfileCache().get(entry.getKey()).map(p -> p.getName()).orElse(entry.getKey().toString());
            java.math.BigDecimal balance = entry.getValue();
            final int currentRank = rank[0];
            ctx.getSource().sendSuccess(() -> MessageUtil.info(
                "commands.neoessentials.baltop.entry", currentRank, name, balance, currency
            ), false);
            rank[0]++;
        }
        return 1;
    }
}
