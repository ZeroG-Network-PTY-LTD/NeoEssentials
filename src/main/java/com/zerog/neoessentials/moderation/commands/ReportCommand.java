package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.moderation.ReportEntry;
import com.zerog.neoessentials.moderation.ReportManager;
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
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

/**
 * Player report commands:
 *   /report <player> <reason>                          — Report a player (any player, even
 *                                                          while staff are offline)
 *   /reports                                            — Staff: view the pending review queue
 *   /reviewreport <id> <accept|dismiss> [notes]         — Staff: resolve a report
 */
public class ReportCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportCommand.class);
    private static final int REPORTS_PER_PAGE = 8;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if moderation commands are enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isModerationEnabled()) {
            NeoLog.debug(LOGGER, LogCategory.MODERATION, "Moderation module is disabled, skipping report command registration");
            return;
        }
        var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance();

        // /report <player> <reason>
        if (cfg.isCommandEnabled("report")) {
        dispatcher.register(Commands.literal("report")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.report").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeReport(ctx,
                        StringArgumentType.getString(ctx, "player"),
                        StringArgumentType.getString(ctx, "reason")))
                )
            )
        );
        }

        // /reports
        if (cfg.isCommandEnabled("reports")) {
        dispatcher.register(Commands.literal("reports")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.reports").hasPermission())
            .executes(ReportCommand::executeReports)
        );
        }

        // /reviewreport <id> <accept|dismiss> [notes]
        if (cfg.isCommandEnabled("reviewreport")) {
        dispatcher.register(Commands.literal("reviewreport")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.reports").hasPermission())
            .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.literal("accept")
                    .executes(ctx -> executeReview(ctx, StringArgumentType.getString(ctx, "id"), ReportEntry.Status.REVIEWED, ""))
                    .then(Commands.argument("notes", StringArgumentType.greedyString())
                        .executes(ctx -> executeReview(ctx, StringArgumentType.getString(ctx, "id"),
                            ReportEntry.Status.REVIEWED, StringArgumentType.getString(ctx, "notes"))))
                )
                .then(Commands.literal("dismiss")
                    .executes(ctx -> executeReview(ctx, StringArgumentType.getString(ctx, "id"), ReportEntry.Status.DISMISSED, ""))
                    .then(Commands.argument("notes", StringArgumentType.greedyString())
                        .executes(ctx -> executeReview(ctx, StringArgumentType.getString(ctx, "id"),
                            ReportEntry.Status.DISMISSED, StringArgumentType.getString(ctx, "notes"))))
                )
            )
        );
        }
    }

    // ── /report ──────────────────────────────────────────────────────────────

    private static int executeReport(CommandContext<CommandSourceStack> ctx, String playerName, String reason) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer reporter)) {
            source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
            return 0;
        }

        if (reporter.getName().getString().equalsIgnoreCase(playerName)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.report.self"));
            return 0;
        }

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        UUID targetId;
        String resolvedName;
        if (target != null) {
            targetId = target.getUUID();
            resolvedName = target.getName().getString();
        } else {
            var profile = source.getServer().getProfileCache() != null
                ? source.getServer().getProfileCache().get(playerName) : java.util.Optional.<com.mojang.authlib.GameProfile>empty();
            if (profile.isEmpty()) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }
            targetId = profile.get().getId();
            resolvedName = profile.get().getName();
        }

        ReportEntry entry = ReportManager.getInstance().addReport(
            reporter.getUUID(), reporter.getName().getString(), targetId, resolvedName, reason);

        final String fShortId = entry.getId().substring(0, 8);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.report.submitted", resolvedName, fShortId), false);

        // Notify online staff immediately, even though the report also persists for offline review
        String staffMsg = "[Report] " + reporter.getName().getString() + " reported " + resolvedName + ": " + reason;
        for (ServerPlayer staff : source.getServer().getPlayerList().getPlayers()) {
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(staff.getUUID(), "neoessentials.moderation.reports")) {
                staff.sendSystemMessage(net.minecraft.network.chat.Component.literal(staffMsg));
            }
        }

        NeoLog.info(LOGGER, LogCategory.MODERATION, "[Report] {} reported {} (ID: {}): {}", reporter.getName().getString(), resolvedName, fShortId, reason);
        return 1;
    }

    // ── /reports ─────────────────────────────────────────────────────────────

    private static int executeReports(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<ReportEntry> pending = ReportManager.getInstance().getPendingReports();

        if (pending.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.report.none_pending"), false);
            return 1;
        }

        final int fCount = pending.size();
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.report.list_header", fCount), false);

        int display = Math.min(pending.size(), REPORTS_PER_PAGE);
        for (int i = 0; i < display; i++) {
            ReportEntry r = pending.get(i);
            String shortId = r.getId().substring(0, 8);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.report.list_entry",
                shortId, r.getTargetName(), r.getReporterName(), r.getReason(), r.getFormattedTime()), false);
        }
        if (pending.size() > REPORTS_PER_PAGE) {
            final int more = pending.size() - REPORTS_PER_PAGE;
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.report.list_more", more), false);
        }
        return 1;
    }

    // ── /reviewreport ────────────────────────────────────────────────────────

    private static int executeReview(CommandContext<CommandSourceStack> ctx, String reportId, ReportEntry.Status status, String notes) {
        CommandSourceStack source = ctx.getSource();
        String reviewerName = source.getEntity() instanceof ServerPlayer p ? p.getName().getString() : "Console";
        UUID reviewerId = source.getEntity() instanceof ServerPlayer p ? p.getUUID() : null;

        ReportEntry report = ReportManager.getInstance().findById(reportId);
        if (report == null) {
            source.sendFailure(MessageUtil.component("commands.neoessentials.report.not_found", reportId));
            return 0;
        }

        boolean success = ReportManager.getInstance().reviewReport(report.getId(), status, reviewerId, reviewerName,
            notes.isEmpty() ? null : notes);
        if (success) {
            NeoLog.info(LOGGER, LogCategory.MODERATION, "[Report] {} marked report {} as {}", reviewerName, reportId, status);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.report.reviewed",
                reportId, status.name(), report.getTargetName()), false);
            return 1;
        }
        source.sendFailure(MessageUtil.component("commands.neoessentials.report.not_found", reportId));
        return 0;
    }
}
