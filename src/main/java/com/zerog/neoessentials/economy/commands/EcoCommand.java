package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.economy.EconomyLocalization;
import com.zerog.neoessentials.economy.EconomyPlayerUtil;
import com.zerog.neoessentials.economy.EconomyTransactionLogger;
import com.zerog.neoessentials.economy.managers.TransactionHistoryManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class EcoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("eco")
                .requires(src -> src.hasPermission(2) || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer() != null ? src.getPlayer().getUUID() : null, "neoessentials.economy.eco"))
                .then(net.minecraft.commands.Commands.literal("give")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().getName()),
                            builder
                        ))
                        .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> ecoAdminAction(ctx, "give")))))
                .then(net.minecraft.commands.Commands.literal("take")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().getName()),
                            builder
                        ))
                        .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> ecoAdminAction(ctx, "take")))))
                .then(net.minecraft.commands.Commands.literal("set")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().getName()),
                            builder
                        ))
                        .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                            .executes(ctx -> ecoAdminAction(ctx, "set")))))
                .then(net.minecraft.commands.Commands.literal("history")
                    .executes(ctx -> showHistory(ctx, ctx.getSource().getPlayerOrException()))
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .requires(src -> src.hasPermission(2))
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().getName()),
                            builder
                        ))
                        .executes(ctx -> showOtherHistory(ctx))
                    )
                )
        );
    }

    private static int ecoAdminAction(CommandContext<CommandSourceStack> ctx, String action) {
        if (!EconomyManager.getInstance().isEnabled()) return 0;
        ServerPlayer sender = null;
        try {
            sender = ctx.getSource().getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.eco.no_permission"));
            return 0;
        }
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.economy.eco")) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.no_permission"));
            return 0;
        }
        String playerName = StringArgumentType.getString(ctx, "player");
        double amountRaw = DoubleArgumentType.getDouble(ctx, "amount");
        BigDecimal amount = BigDecimal.valueOf(amountRaw);
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.eco.player_not_found"));
            return 0;
        }
        UUID uuid = uuidOpt.get();
        EconomyManager manager = EconomyManager.getInstance();
        String adminName = ctx.getSource().getTextName();
        switch (action) {
            case "give":
                manager.addBalance(uuid, amount);
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.eco.give", amount, playerName), false);
                EconomyTransactionLogger.log("ADMIN_GIVE", adminName, playerName, amount.toPlainString(), "Admin give");
                TransactionHistoryManager.getInstance().addTransaction(uuid, "Admin gave you " + amount);
                break;
            case "take":
                manager.subtractBalance(uuid, amount);
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.eco.take", amount, playerName), false);
                EconomyTransactionLogger.log("ADMIN_TAKE", adminName, playerName, amount.toPlainString(), "Admin take");
                TransactionHistoryManager.getInstance().addTransaction(uuid, "Admin took " + amount);
                break;
            case "set":
                manager.setBalance(uuid, amount);
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.eco.set", playerName, amount), false);
                EconomyTransactionLogger.log("ADMIN_SET", adminName, playerName, amount.toPlainString(), "Admin set");
                TransactionHistoryManager.getInstance().addTransaction(uuid, "Admin set your balance to " + amount);
                break;
        }
        return 1;
    }

    private static int showHistory(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        UUID uuid = player.getUUID();
        java.util.List<String> history = TransactionHistoryManager.getInstance().getHistory(uuid);
        if (history.isEmpty()) {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.header"), false);
            for (String entry : history) {
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.entry", entry), false);
            }
        }
        return 1;
    }
    private static int showOtherHistory(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(EconomyLocalization.component("commands.neoessentials.eco.player_not_found"));
            return 0;
        }
        java.util.List<String> history = TransactionHistoryManager.getInstance().getHistory(uuidOpt.get());
        if (history.isEmpty()) {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.header"), false);
            for (String entry : history) {
                ctx.getSource().sendSuccess(() -> EconomyLocalization.component("commands.neoessentials.history.entry", entry), false);
            }
        }
        return 1;
    }
}
