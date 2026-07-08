package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.util.MessageUtil;
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
        // Register main command
        registerEcoCommand(dispatcher, "eco");
        // Register alias
        registerEcoCommand(dispatcher, "economy");
    }
    
    private static void registerEcoCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal(commandName)
                .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2) || com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer() != null ? src.getPlayer().getUUID() : null, "neoessentials.economy.eco"))
                .then(net.minecraft.commands.Commands.literal("give")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
                            builder
                        ))
                        .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> ecoAdminAction(ctx, "give")))))
                .then(net.minecraft.commands.Commands.literal("take")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
                            builder
                        ))
                        .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(ctx -> ecoAdminAction(ctx, "take")))))
                .then(net.minecraft.commands.Commands.literal("set")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
                            builder
                        ))
                        .then(net.minecraft.commands.Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                            .executes(ctx -> ecoAdminAction(ctx, "set")))))
                // /eco reset <player>  — Essentials: EcoCommands.RESET sets to startingBalance
                .then(net.minecraft.commands.Commands.literal("reset")
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
                            builder
                        ))
                        .executes(ctx -> ecoAdminAction(ctx, "reset"))))
                .then(net.minecraft.commands.Commands.literal("history")
                    .executes(ctx -> showHistory(ctx, ctx.getSource().getPlayerOrException()))
                    .then(net.minecraft.commands.Commands.argument("player", StringArgumentType.word())
                        .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2))
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(p -> p.getGameProfile().name()),
                            builder
                        ))
                        .executes(ctx -> showOtherHistory(ctx))
                    )
                )
        );
    }

    private static int ecoAdminAction(CommandContext<CommandSourceStack> ctx, String action) {
        // Check if economy is enabled
        if (!EconomyManager.getInstance().isEnabled()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.economy.disabled"));
            return 0;
        }

        // Validate admin permission
        com.zerog.neoessentials.util.PermissionValidator.PermissionResult permResult =
            com.zerog.neoessentials.util.PermissionValidator.validateAdminPermission(ctx.getSource(), "neoessentials.economy.eco");
        if (!permResult.hasPermission()) {
            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
            return 0;
        }

        String playerName = StringArgumentType.getString(ctx, "player");
        com.zerog.neoessentials.util.InputValidator.ValidationResult nameValidation =
            com.zerog.neoessentials.util.InputValidator.validatePlayerName(playerName);
        if (!nameValidation.isValid()) {
            ctx.getSource().sendFailure(MessageUtil.error(nameValidation.getErrorMessage()));
            return 0;
        }
        String validPlayerName = nameValidation.getValue(String.class);

        // Resolve UUID — supports offline players via profile cache (Essentials: loopOfflinePlayersConsumer)
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, validPlayerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.eco.player_not_found"));
            return 0;
        }
        UUID uuid = uuidOpt.get();

        // "reset" uses no amount arg — sets to starting balance
        if ("reset".equals(action)) {
            BigDecimal startBal = BigDecimal.valueOf(
                com.zerog.neoessentials.config.ConfigManager.getEconomyStartingBalance());
            EconomyManager.getInstance().setBalance(uuid, startBal);
            String adminName = ctx.getSource().getTextName();
            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.eco.reset", validPlayerName, startBal,
                EconomyManager.getInstance().getCurrencySymbol()), false);
            EconomyTransactionLogger.log("ADMIN_RESET", adminName, validPlayerName,
                startBal.toPlainString(), "Reset to starting balance");
            TransactionHistoryManager.getInstance().addTransaction(uuid,
                MessageUtil.localize("commands.neoessentials.transaction.admin_reset", startBal));
            BaltopCommand.invalidateCache();
            // Notify target if online
            var onlineTarget = server.getPlayerList().getPlayer(uuid);
            if (onlineTarget != null) {
                onlineTarget.sendSystemMessage(MessageUtil.info(
                    "commands.neoessentials.eco.reset_notify", startBal,
                    EconomyManager.getInstance().getCurrencySymbol()));
            }
            return 1;
        }

        // All other actions need an amount
        double amountRaw = DoubleArgumentType.getDouble(ctx, "amount");

        BigDecimal amount;
        if ("set".equals(action)) {
            if (amountRaw < 0 || Double.isNaN(amountRaw) || Double.isInfinite(amountRaw)) {
                ctx.getSource().sendFailure(MessageUtil.error("Invalid amount: must be non-negative"));
                return 0;
            }
            amount = BigDecimal.valueOf(amountRaw);
        } else {
            com.zerog.neoessentials.util.InputValidator.ValidationResult amountValidation =
                com.zerog.neoessentials.util.InputValidator.validateEconomyAmount(amountRaw);
            if (!amountValidation.isValid()) {
                ctx.getSource().sendFailure(MessageUtil.error(amountValidation.getErrorMessage()));
                return 0;
            }
            amount = amountValidation.getValue(BigDecimal.class);
        }


        EconomyManager manager = EconomyManager.getInstance();
        String adminName = ctx.getSource().getTextName();
        final BigDecimal finalAmount = amount;
        switch (action) {
            case "give" -> {
                boolean ok = manager.addBalance(uuid, finalAmount);
                if (!ok) {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.eco.give_failed", finalAmount, validPlayerName));
                    return 0;
                }
                ctx.getSource().sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.eco.give", finalAmount, validPlayerName,
                    manager.getCurrencySymbol()), false);
                EconomyTransactionLogger.log("ADMIN_GIVE", adminName, validPlayerName,
                    finalAmount.toPlainString(), "Admin give");
                TransactionHistoryManager.getInstance().addTransaction(uuid,
                    MessageUtil.localize("commands.neoessentials.transaction.admin_gave", finalAmount));
                var t1 = server.getPlayerList().getPlayer(uuid);
                if (t1 != null) t1.sendSystemMessage(MessageUtil.info(
                    "commands.neoessentials.eco.received_give", finalAmount,
                    manager.getCurrencySymbol()));
            }
            case "take" -> {
                boolean ok = manager.subtractBalance(uuid, finalAmount);
                if (!ok) {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.eco.take_failed", finalAmount, validPlayerName));
                    return 0;
                }
                ctx.getSource().sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.eco.take", finalAmount, validPlayerName,
                    manager.getCurrencySymbol()), false);
                EconomyTransactionLogger.log("ADMIN_TAKE", adminName, validPlayerName,
                    finalAmount.toPlainString(), "Admin take");
                TransactionHistoryManager.getInstance().addTransaction(uuid,
                    MessageUtil.localize("commands.neoessentials.transaction.admin_took", finalAmount));
                var t3 = server.getPlayerList().getPlayer(uuid);
                if (t3 != null) t3.sendSystemMessage(MessageUtil.info(
                    "commands.neoessentials.eco.take_notify", finalAmount,
                    manager.getCurrencySymbol()));
            }
            case "set" -> {
                manager.setBalance(uuid, finalAmount);
                ctx.getSource().sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.eco.set", validPlayerName, finalAmount,
                    manager.getCurrencySymbol()), false);
                EconomyTransactionLogger.log("ADMIN_SET", adminName, validPlayerName,
                    finalAmount.toPlainString(), "Admin set");
                TransactionHistoryManager.getInstance().addTransaction(uuid,
                    MessageUtil.localize("commands.neoessentials.transaction.admin_set", finalAmount));
                var t2 = server.getPlayerList().getPlayer(uuid);
                if (t2 != null) t2.sendSystemMessage(MessageUtil.info(
                    "commands.neoessentials.eco.set_notify", finalAmount,
                    manager.getCurrencySymbol()));
            }
        }
        // Invalidate baltop cache so next /baltop shows fresh data
        BaltopCommand.invalidateCache();
        return 1;
    }

    private static int showHistory(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        UUID uuid = player.getUUID();
        java.util.List<String> history = TransactionHistoryManager.getInstance().getHistory(uuid);
        if (history.isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.history.empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.history.header"), false);
            for (String entry : history) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.history.entry", entry), false);
            }
        }
        return 1;
    }
    private static int showOtherHistory(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        MinecraftServer server = ctx.getSource().getServer();
        Optional<UUID> uuidOpt = EconomyPlayerUtil.getUUIDByName(server, playerName);
        if (uuidOpt.isEmpty()) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.eco.player_not_found"));
            return 0;
        }
        java.util.List<String> history = TransactionHistoryManager.getInstance().getHistory(uuidOpt.get());
        if (history.isEmpty()) {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.history.empty"), false);
        } else {
            ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.history.header"), false);
            for (String entry : history) {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.history.entry", entry), false);
            }
        }
        return 1;
    }
}
