package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.DashboardAPI;
import com.zerog.neoessentials.webdashboard.DashboardFileManager;
import com.zerog.neoessentials.webdashboard.DashboardLifecycleManager;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to manage the Dashboard API server
 * Usage:
 * - /dashboard           — Show dashboard status
 * - /dashboard start     — Start the dashboard
 * - /dashboard stop      — Stop the dashboard
 * - /dashboard restart   — Restart the dashboard
 * - /dashboard url       — Show dashboard URL
 * - /dashboard update    — Smart-update changed dashboard files from JAR
 * - /dashboard update check — Preview which files would change (dry-run)
 */
public class DashboardCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("dashboard")) {
            return;
        }
        dispatcher.register(Commands.literal("dashboard")
            .requires(source -> PermissionValidator.validateAdminPermission(source, "neoessentials.admin.dashboard").hasPermission())
            .executes(DashboardCommand::showStatus)
            .then(Commands.literal("start")
                .executes(DashboardCommand::startDashboard))
            .then(Commands.literal("stop")
                .executes(DashboardCommand::stopDashboard))
            .then(Commands.literal("restart")
                .executes(DashboardCommand::restartDashboard))
            .then(Commands.literal("status")
                .executes(DashboardCommand::showStatus))
            .then(Commands.literal("url")
                .executes(DashboardCommand::showUrl))
            .then(Commands.literal("update")
                .executes(DashboardCommand::updateDashboardFiles)
                // /dashboard update check — dry-run preview
                .then(Commands.literal("check")
                    .executes(DashboardCommand::checkDashboardFiles))
                // /dashboard update force — bypass checksum, overwrite everything
                .then(Commands.literal("force")
                    .executes(DashboardCommand::forceUpdateDashboardFiles)))
        );
    }

    // ── Existing commands ──────────────────────────────────────────────────────

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        String runningStatus = MessageUtil.localize(status.running ? "commands.neoessentials.dashboard.status_running" : "commands.neoessentials.dashboard.status_offline");
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.status_line", runningStatus), false);

        String configStatus = MessageUtil.localize(status.configEnabled ? "commands.neoessentials.dashboard.config_enabled" : "commands.neoessentials.dashboard.config_disabled");
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.config_line", configStatus), false);

        if (status.manuallyDisabled) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.override_manually_disabled"), false);
        }

        if (status.running) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.url_line", status.url), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.api_line", status.url), false);
        }

        // Show installed file version
        String installedVer = DashboardFileManager.getInstalledDashboardVersion();
        String currentVer   = DashboardFileManager.getCurrentModVersion();
        String verColour    = installedVer.equals(currentVer) ? "§a" : "§e";
        String filesSuffix  = installedVer.equals(currentVer)
            ? MessageUtil.localize("commands.neoessentials.dashboard.files_uptodate_suffix")
            : MessageUtil.localize("commands.neoessentials.dashboard.files_update_available_suffix", currentVer);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.files_line", verColour, installedVer, filesSuffix), false);

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.footer_separator"), false);

        if (!status.running) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.hint_start"), false);
        } else {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.hint_stop"), false);
        }

        return 1;
    }

    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!ConfigManager.isWebDashboardEnabled()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.disabled_in_config"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.enable_in_config_hint"), false);
            return 0;
        }

        if (DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.already_running"), false);
            return 0;
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.starting"), false);

        boolean success = DashboardLifecycleManager.startDashboard(source.getServer());

        if (success) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.started_success"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.url_line", status.url), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.api_line", status.url), false);
            return 1;
        } else {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.failed_start"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.check_logs"), false);
            return 0;
        }
    }

    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.not_running"), false);
            return 0;
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.stopping"), false);

        boolean success = DashboardLifecycleManager.stopDashboard();

        if (success) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.stopped_success"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.hint_restart"), false);
            return 1;
        } else {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.failed_stop"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.check_logs"), false);
            return 0;
        }
    }

    private static int restartDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.not_running"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.hint_start_instead"), false);
            return 0;
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.restarting"), false);

        boolean stopSuccess = DashboardLifecycleManager.stopDashboard();
        if (!stopSuccess) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.failed_stop"), false);
            return 0;
        }

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        boolean startSuccess = DashboardLifecycleManager.startDashboard(source.getServer());

        if (startSuccess) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.restarted_success"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.url_line", status.url), false);
            return 1;
        } else {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.failed_restart"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.check_logs"), false);
            return 0;
        }
    }

    private static int showUrl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.error_not_running"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.hint_start_it"), false);
            return 0;
        }

        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.urls_header"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.frontend_line", status.url), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.api_line", status.url), false);
        return 1;
    }

    // ── Update commands ────────────────────────────────────────────────────────

    /**
     * /dashboard update — smart per-file update using MD5 comparison.
     * Only overwrites files whose content differs from the JAR copy.
     */
    private static int updateDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.checking_updates"), false);

        try {
            DashboardFileManager.UpdateSummary summary = DashboardFileManager.smartUpdateDashboardFiles(false);
            return sendUpdateSummary(source, summary, false);
        } catch (Exception e) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.update_failed", e.getMessage()), false);
            return 0;
        }
    }

    /**
     * /dashboard update check — dry-run: shows what would change without writing any files.
     */
    private static int checkDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.checking_dry_run"), false);

        try {
            DashboardFileManager.UpdateSummary summary = DashboardFileManager.smartUpdateDashboardFiles(true);
            return sendUpdateSummary(source, summary, true);
        } catch (Exception e) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.check_failed", e.getMessage()), false);
            return 0;
        }
    }

    /**
     * /dashboard update force — bypass checksum comparison and overwrite every file.
     */
    private static int forceUpdateDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.force_updating"), false);

        try {
            DashboardFileManager.forceUpdateDashboardFiles();
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.force_update_done"), false);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.force_update_path"), false);
            if (DashboardAPI.getInstance().isRunning()) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.restart_to_apply"), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.force_update_failed", e.getMessage()), false);
            return 0;
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private static int sendUpdateSummary(CommandSourceStack source,
                                          DashboardFileManager.UpdateSummary summary,
                                          boolean dryRun) {
        String label = dryRun ? MessageUtil.localize("commands.neoessentials.dashboard.summary_dryrun_label") : "";

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_checked", summary.total(), label), false);

        if (!summary.added.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component(dryRun
                ? "commands.neoessentials.dashboard.summary_added_dryrun"
                : "commands.neoessentials.dashboard.summary_added_normal", summary.added.size()), false);
            for (String f : summary.added)
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_file_item", f), false);
        }

        if (!summary.updated.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component(dryRun
                ? "commands.neoessentials.dashboard.summary_updated_dryrun"
                : "commands.neoessentials.dashboard.summary_updated_normal", summary.updated.size()), false);
            for (String f : summary.updated)
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_file_item_updated", f), false);
        }

        if (!summary.unchanged.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_unchanged", summary.unchanged.size()), false);
        }

        if (!summary.failed.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_failed", summary.failed.size()), false);
            for (String f : summary.failed)
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_file_item_failed", f), false);
        }

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_separator"), false);

        if (dryRun) {
            if (summary.hasChanges()) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_dryrun_hint"), false);
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_dryrun_uptodate"), false);
            }
        } else {
            if (summary.hasChanges()) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_update_success"), false);
                if (DashboardAPI.getInstance().isRunning()) {
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.restart_to_apply"), false);
                }
            } else {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.summary_update_nochange"), false);
            }
        }

        return summary.failed.isEmpty() ? 1 : 0;
    }
}
