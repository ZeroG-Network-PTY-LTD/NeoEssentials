package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.util.UUID;

public class BalanceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("balance")
                .executes(ctx -> execute(ctx))
                .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                    .requires(src -> src.hasPermission(2) || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer() != null ? src.getPlayer().getUUID() : null, "neoessentials.economy.balance.others"))
                    .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                            .map(p -> p.getGameProfile().getName()),
                        builder
                    ))
                    .executes(ctx -> executeOther(ctx))
                )
        );
        dispatcher.register(
            net.minecraft.commands.Commands.literal("bal")
                .executes(ctx -> execute(ctx))
        );
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!EconomyManager.getInstance().isEnabled()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.eco.disabled"));
            return 0;
        }
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.balance.player_not_found"));
            return 0;
        }
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.economy.balance")) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        UUID uuid = player.getUUID();
        BigDecimal balance = EconomyManager.getInstance().getBalance(uuid);
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.balance", balance, currency), false);
        return 1;
    }

    private static int executeOther(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        if (!EconomyManager.getInstance().isEnabled()) return 0;
        ServerPlayer sender = null;
        try {
            sender = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.balance.player_not_found"));
            return 0;
        }
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.economy.balance.others")) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        String playerName = StringArgumentType.getString(ctx, "player");
        java.util.Optional<UUID> uuidOpt = com.zerog.neoessentials.economy.EconomyPlayerUtil.getUUIDByName(ctx.getSource().getServer(), playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.balance.player_not_found"));
            return 0;
        }
        BigDecimal balance = EconomyManager.getInstance().getBalance(uuidOpt.get());
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.balance", balance, currency), false);
        return 1;
    }
}
