package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.moderation.WarnEntry;
import com.zerog.neoessentials.moderation.WarnManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Warn system commands:
 *   /warn <player> [reason]             — Issue a warning
 *   /warnings <player>                  — View a player's warnings + count
 *   /clearwarnings <player>             — Clear all warnings for a player
 *   /removewarn <player> <warnId>       — Remove a single warn by its ID
 */
public class WarnCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarnCommand.class);
    private static final int WARNS_PER_PAGE = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if moderation commands are enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isModerationEnabled()) {
            LOGGER.debug("Moderation module is disabled, skipping warn command registration");
            return;
        }
        var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance();

        // /warn <player> [reason]
        if (cfg.isCommandEnabled("warn")) {
        dispatcher.register(Commands.literal("warn")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.warn").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeWarn(ctx, StringArgumentType.getString(ctx, "player"), "No reason given."))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeWarn(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
        }

        // /warnings <player>
        if (cfg.isCommandEnabled("warnings")) {
        dispatcher.register(Commands.literal("warnings")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.warnings").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeWarnings(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
        }

        // /clearwarnings <player>
        if (cfg.isCommandEnabled("clearwarnings")) {
        dispatcher.register(Commands.literal("clearwarnings")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.warn").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeClearWarnings(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
        }

        // /removewarn <player> <warnId>
        if (cfg.isCommandEnabled("removewarn")) {
        dispatcher.register(Commands.literal("removewarn")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.warn").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .then(Commands.argument("warnId", StringArgumentType.word())
                    .executes(ctx -> executeRemoveWarn(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "warnId")))
                )
            )
        );
        }
    }

    // ── /warn ────────────────────────────────────────────────────────────────

    private static int executeWarn(CommandContext<CommandSourceStack> ctx,
                                   String playerName, String reason) {
        CommandSourceStack source = ctx.getSource();
        String warnedBy = getCommandSender(source);
        UUID   warnedById = getCommandSenderUUID(source);

        // Resolve target (online or offline by stored UUID)
        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        // Issue the warn
        WarnEntry entry = WarnManager.getInstance().addWarn(
            targetId, playerName, warnedById, warnedBy, reason);

        int total = WarnManager.getInstance().getWarnCount(targetId);

        // Feedback to command sender
        final String fShortId = entry.getId().substring(0, 8);
        final int fTotal = total;
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.issued",
            playerName, reason, fTotal, fShortId), true);

        // Always log to console so warns are always visible in server logs
        LOGGER.info("[Warn] {} warned {} for: {} (warn #{}, ID: {})",
            warnedBy, playerName, reason, total, entry.getId().substring(0, 8));

        // Notify target if online
        ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        if (target != null) {
            target.sendSystemMessage(MessageUtil.component("commands.neoessentials.warn.notify_target",
                warnedBy, reason, fTotal));
        }

        return 1;
    }

    // ── /warnings ────────────────────────────────────────────────────────────

    private static int executeWarnings(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();

        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        List<WarnEntry> warns = WarnManager.getInstance().getWarnings(targetId);

        if (warns.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.none_found", playerName), false);
            return 1;
        }

        final int fWarnCount = warns.size();
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.list_header", playerName, fWarnCount), false);

        int display = Math.min(warns.size(), WARNS_PER_PAGE);
        for (int i = 0; i < display; i++) {
            WarnEntry w = warns.get(i);
            String shortId = w.getId().substring(0, 8);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.list_entry",
                shortId, w.getFormattedTime(), w.getWarnedBy(), w.getReason()), false);
        }
        if (warns.size() > WARNS_PER_PAGE) {
            final int more = warns.size() - WARNS_PER_PAGE;
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.list_more", more), false);
        }
        return 1;
    }

    // ── /clearwarnings ───────────────────────────────────────────────────────

    private static int executeClearWarnings(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();

        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        int count = WarnManager.getInstance().clearWarnings(targetId);
        if (count == 0) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.clear_none", playerName), false);
        } else {
            String sender = getCommandSender(source);
            LOGGER.info("[Warn] {} cleared all {} warn(s) for {}", sender, count, playerName);
            final int fCount = count;
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.cleared", fCount, playerName), true);
        }
        return 1;
    }

    // ── /removewarn ──────────────────────────────────────────────────────────

    private static int executeRemoveWarn(CommandContext<CommandSourceStack> ctx,
                                         String playerName, String warnId) {
        CommandSourceStack source = ctx.getSource();

        UUID targetId = resolvePlayerUUID(ctx, playerName);
        if (targetId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        // The user may provide only the 8-char shortId prefix; resolve to full ID
        List<WarnEntry> warns = WarnManager.getInstance().getWarnings(targetId);
        String fullId = warns.stream()
            .filter(w -> w.getId().startsWith(warnId) || w.getId().equals(warnId))
            .map(WarnEntry::getId)
            .findFirst()
            .orElse(null);

        if (fullId == null) {
            source.sendFailure(MessageUtil.component("commands.neoessentials.warn.remove_id_not_found", warnId, playerName));
            return 0;
        }

        boolean removed = WarnManager.getInstance().removeWarn(targetId, fullId);
        if (removed) {
            String sender = getCommandSender(source);
            LOGGER.info("[Warn] {} removed warn {} from {}", sender, warnId, playerName);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.warn.removed", warnId, playerName), false);
        } else {
            source.sendFailure(MessageUtil.component("commands.neoessentials.warn.remove_failed", playerName));
        }
        return removed ? 1 : 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getCommandSender(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer p) return p.getName().getString();
        return "Console";
    }

    private static UUID getCommandSenderUUID(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer p) return p.getUUID();
        return null;
    }

    /**
     * Resolves a player UUID by name.
     * Checks online players first, then existing warn records for offline players.
     */
    private static UUID resolvePlayerUUID(CommandContext<CommandSourceStack> ctx, String playerName) {
        // Try online player first
        ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        if (online != null) return online.getUUID();

        // Fall back to warn history (so mods can view/clear warns for offline players)
        return WarnManager.getInstance().findUUIDByName(playerName);
    }
}



