package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.kits.Kit;
import com.zerog.neoessentials.kits.KitManager;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * /kit [name] [player]
 *
 * Improvements ported from EssentialsX Commandkit:
 *  - /kit              → shows available kits list (no args)
 *  - /kit <name>       → give kit to self
 *  - /kit <name> <player> → give kit to another player (neoessentials.kit.others)
 *  - Console support: /kit <name> <player>
 *  - Recipient receives "kitReceive" notification
 *  - Clean permission flow — no redundant double-deny
 */
public class KitCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isKitSystemEnabled()) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("kit")) return;

        dispatcher.register(Commands.literal("kit")
            .requires(src -> {
                var p = src.getPlayer();
                // console always allowed (will need player arg); players need base use perm
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.kits.use");
            })
            // /kit — list kits
            .executes(KitCommand::listAvailableKits)
            // /kit <name>
            .then(Commands.argument("kitname", StringArgumentType.word())
                .suggests(KitCommand::suggestKits)
                // /kit <name>  (self)
                .executes(ctx -> executeGiveKit(ctx,
                    StringArgumentType.getString(ctx, "kitname"), null))
                // /kit <name> <player>  (others — Essentials: essentials.kit.others)
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerNames(), builder))
                    .requires(src -> {
                        var p = src.getPlayer();
                        return p == null
                            || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.kit.others");
                    })
                    .executes(ctx -> executeGiveKit(ctx,
                        StringArgumentType.getString(ctx, "kitname"),
                        StringArgumentType.getString(ctx, "target")))
                )
            )
        );
    }

    // ── Suggestions ───────────────────────────────────────────────────────────
    private static CompletableFuture<Suggestions> suggestKits(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        var p = ctx.getSource().getPlayer();
        for (Kit kit : KitManager.getInstance().getAllKits()) {
            if (!kit.isEnabled()) continue;
            if (p != null) {
                String perm = kit.getPermission() != null && !kit.getPermission().isEmpty()
                    ? kit.getPermission() : "neoessentials.kits." + kit.getName().toLowerCase();
                if (!PermissionAPI.hasPermission(p.getUUID(), perm)) continue;
            }
            builder.suggest(kit.getName());
        }
        return builder.buildFuture();
    }

    // ── /kit (no args) ────────────────────────────────────────────────────────
    private static int listAvailableKits(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();

        var available = player != null
            ? KitManager.getInstance().getAvailableKits(player)
            : new java.util.ArrayList<>(KitManager.getInstance().getAllKits());

        if (available.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.kits.list_empty"), false);
            return 1;
        }

        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.kits.list_header",
            available.size()), false);

        for (Kit kit : available) {
            long remaining = player != null
                ? KitManager.getInstance().getRemainingCooldownPublic(player.getUUID(), kit.getName())
                : 0L;
            String cooldownStr = remaining > 0
                ? MessageUtil.localize("commands.neoessentials.kits.list_cooldown", formatTime(remaining))
                : MessageUtil.localize("commands.neoessentials.kits.list_ready");
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.kits.list_entry",
                kit.getName(), kit.getItems().size(), cooldownStr), false);
        }
        return 1;
    }

    // ── /kit <name> [player] ─────────────────────────────────────────────────
    private static int executeGiveKit(CommandContext<CommandSourceStack> ctx,
                                      String kitName, String targetName) {
        var source = ctx.getSource();
        var sender = source.getPlayer(); // null if console

        // Console must provide a target
        if (sender == null && targetName == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.kits.console_needs_target"));
            return 0;
        }

        // Resolve recipient
        ServerPlayer recipient;
        if (targetName != null) {
            recipient = source.getServer().getPlayerList().getPlayerByName(targetName);
            if (recipient == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
                return 0;
            }
        } else {
            recipient = sender;
        }

        // Per-kit permission check on the SENDER (Essentials: kit.checkPerms(userFrom))
        if (sender != null) {
            Kit kit = KitManager.getInstance().getKit(kitName);
            if (kit == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kits.not_found", kitName));
                return 0;
            }
            String perm = kit.getPermission() != null && !kit.getPermission().isEmpty()
                ? kit.getPermission() : "neoessentials.kits." + kitName.toLowerCase();
            if (!PermissionAPI.hasPermission(sender.getUUID(), perm)) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kits.no_permission_kit", kitName));
                return 0;
            }
        }

        // Economy cost check
        int cost = (int) com.zerog.neoessentials.config.ConfigManager.getKitCommandCost("kit");
        if (cost > 0 && sender != null
                && com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().isEnabled()) {
            var eco = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance();
            if (eco.getBalance(sender.getUUID()).doubleValue() < cost) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kits.not_enough_money", cost));
                return 0;
            }
            if (!eco.subtractBalance(sender.getUUID(), java.math.BigDecimal.valueOf(cost))) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kits.charge_failed"));
                return 0;
            }
        }

        // canUseKit checks cooldown, max uses, enabled flag on RECIPIENT
        var canUse = KitManager.getInstance().canUseKit(recipient, kitName);
        if (!canUse.isAllowed()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.kits.cannot_use",
                canUse.getMessage()));
            return 0;
        }

        var giveResult = KitManager.getInstance().giveKit(recipient, kitName);
        if (!giveResult.isAllowed()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.kits.give_failed",
                giveResult.getMessage()));
            return 0;
        }

        // Notify sender (Essentials: kitGiveTo)
        if (targetName != null) {
            final String rName = recipient.getName().getString();
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.kits.gave_to", kitName, rName), true);
            // Notify recipient (Essentials: kitReceive)
            recipient.sendSystemMessage(MessageUtil.info(
                "commands.neoessentials.kits.received_from",
                kitName, sender != null ? sender.getName().getString() : "Console"));
        } else {
            Kit kit = KitManager.getInstance().getKit(kitName);
            String display = kit != null ? kit.getDisplayName() : kitName;
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.kits.given", display), false);
        }

        LOGGER.info("{} gave kit '{}' to {}",
            sender != null ? sender.getName().getString() : "Console",
            kitName, recipient.getName().getString());
        return 1;
    }

    private static String formatTime(long millis) {
        long s = millis / 1000, m = s / 60, h = m / 60;
        if (h > 0) return h + "h " + (m % 60) + "m";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }
}