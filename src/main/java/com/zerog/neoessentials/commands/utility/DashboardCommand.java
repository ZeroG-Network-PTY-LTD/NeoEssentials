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

        String runningStatus = status.running ? "§a§lONLINE" : "§c§lOFFLINE";
        source.sendSuccess(() -> Component.literal("§7Status: " + runningStatus), false);

        String configStatus = status.configEnabled ? "§aEnabled" : "§cDisabled";
        source.sendSuccess(() -> Component.literal("§7Config: " + configStatus), false);

        if (status.manuallyDisabled) {
            source.sendSuccess(() -> Component.literal("§7Override: §eManually disabled"), false);
        }

        if (status.running) {
            source.sendSuccess(() -> Component.literal("§7URL: §b§n" + status.url), false);
            source.sendSuccess(() -> Component.literal("§7API: §b§n" + status.url + "/api/"), false);
        }

        // Show installed file version
        String installedVer = DashboardFileManager.getInstalledDashboardVersion();
        String currentVer   = DashboardFileManager.getCurrentModVersion();
        String verColour    = installedVer.equals(currentVer) ? "§a" : "§e";
        source.sendSuccess(() -> Component.literal(
            "§7Files: " + verColour + "build." + installedVer
            + (installedVer.equals(currentVer) ? " §8(up-to-date)" : " §e→ build." + currentVer + " available")), false);

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        if (!status.running) {
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7to start the server"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard stop §7to stop the server"), false);
        }

        return 1;
    }

    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!ConfigManager.isWebDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cDashboard is disabled in configuration!"), false);
            source.sendSuccess(() -> Component.literal("§7Enable it in §econfig/neoessentials.toml"), false);
            return 0;
        }

        if (DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§e§lWARNING: §eDashboard is already running!"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6Starting dashboard server..."), false);

        boolean success = DashboardLifecycleManager.startDashboard(source.getServer());

        if (success) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard started successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7URL: §b§n" + status.url), false);
            source.sendSuccess(() -> Component.literal("§7API: §b§n" + status.url + "/api/"), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to start dashboard!"), false);
            source.sendSuccess(() -> Component.literal("§7Check server logs for details"), false);
            return 0;
        }
    }

    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§e§lWARNING: §eDashboard is not running!"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6Stopping dashboard server..."), false);

        boolean success = DashboardLifecycleManager.stopDashboard();

        if (success) {
            source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard stopped successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7to restart it"), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to stop dashboard!"), false);
            source.sendSuccess(() -> Component.literal("§7Check server logs for details"), false);
            return 0;
        }
    }

    private static int restartDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§e§lWARNING: §eDashboard is not running!"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7instead"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6Restarting dashboard server..."), false);

        boolean stopSuccess = DashboardLifecycleManager.stopDashboard();
        if (!stopSuccess) {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to stop dashboard!"), false);
            return 0;
        }

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        boolean startSuccess = DashboardLifecycleManager.startDashboard(source.getServer());

        if (startSuccess) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard restarted successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7URL: §b§n" + status.url), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cFailed to restart dashboard!"), false);
            source.sendSuccess(() -> Component.literal("§7Check server logs for details"), false);
            return 0;
        }
    }

    private static int showUrl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal("§c§lERROR: §cDashboard is not running!"), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/dashboard start §7to start it"), false);
            return 0;
        }

        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
        source.sendSuccess(() -> Component.literal("§6§lDashboard URLs:"), false);
        source.sendSuccess(() -> Component.literal("§7Frontend: §b§n" + status.url), false);
        source.sendSuccess(() -> Component.literal("§7API: §b§n" + status.url + "/api/"), false);
        return 1;
    }

    // ── Update commands ────────────────────────────────────────────────────────

    /**
     * /dashboard update — smart per-file update using MD5 comparison.
     * Only overwrites files whose content differs from the JAR copy.
     */
    private static int updateDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6Checking dashboard files for updates..."), false);

        try {
            DashboardFileManager.UpdateSummary summary = DashboardFileManager.smartUpdateDashboardFiles(false);
            return sendUpdateSummary(source, summary, false);
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cUpdate failed: " + e.getMessage()), false);
            return 0;
        }
    }

    /**
     * /dashboard update check — dry-run: shows what would change without writing any files.
     */
    private static int checkDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6Checking dashboard files (dry-run — no files will be changed)..."), false);

        try {
            DashboardFileManager.UpdateSummary summary = DashboardFileManager.smartUpdateDashboardFiles(true);
            return sendUpdateSummary(source, summary, true);
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cCheck failed: " + e.getMessage()), false);
            return 0;
        }
    }

    /**
     * /dashboard update force — bypass checksum comparison and overwrite every file.
     */
    private static int forceUpdateDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6Force-updating ALL dashboard files from JAR..."), false);

        try {
            DashboardFileManager.forceUpdateDashboardFiles();
            source.sendSuccess(() -> Component.literal("§a§l✓ §aAll dashboard files replaced from JAR."), false);
            source.sendSuccess(() -> Component.literal("§7Path: §eneoessentials/webdashboard/"), false);
            if (DashboardAPI.getInstance().isRunning()) {
                source.sendSuccess(() -> Component.literal("§e⚠ §eRestart to apply: §b/dashboard restart"), false);
            }
            return 1;
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal("§c§l✗ §cForce update failed: " + e.getMessage()), false);
            return 0;
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private static int sendUpdateSummary(CommandSourceStack source,
                                          DashboardFileManager.UpdateSummary summary,
                                          boolean dryRun) {
        String label = dryRun ? "§8[dry-run] " : "";

        source.sendSuccess(() -> Component.literal("§6§l─────────────────────────────"), false);
        source.sendSuccess(() -> Component.literal(
            "§7Checked §f" + summary.total() + " §7file(s) " + label), false);

        if (!summary.added.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                (dryRun ? "§b⊕ " : "§a✚ ") + (dryRun ? "Would add" : "Added") +
                " §f" + summary.added.size() + "§7 new file(s):"), false);
            for (String f : summary.added)
                source.sendSuccess(() -> Component.literal("  §8» §f" + f), false);
        }

        if (!summary.updated.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                (dryRun ? "§e⟳ " : "§a✔ ") + (dryRun ? "Would update" : "Updated") +
                " §f" + summary.updated.size() + "§7 changed file(s):"), false);
            for (String f : summary.updated)
                source.sendSuccess(() -> Component.literal("  §8» §e" + f), false);
        }

        if (!summary.unchanged.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                "§8= Unchanged: §f" + summary.unchanged.size() + "§8 file(s) already up-to-date"), false);
        }

        if (!summary.failed.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                "§c✗ Failed: §f" + summary.failed.size() + "§c file(s):"), false);
            for (String f : summary.failed)
                source.sendSuccess(() -> Component.literal("  §8» §c" + f), false);
        }

        source.sendSuccess(() -> Component.literal("§6§l─────────────────────────────"), false);

        if (dryRun) {
            if (summary.hasChanges()) {
                source.sendSuccess(() -> Component.literal(
                    "§7Run §e/dashboard update §7to apply these changes."), false);
            } else {
                source.sendSuccess(() -> Component.literal("§a✓ All files are already up-to-date."), false);
            }
        } else {
            if (summary.hasChanges()) {
                source.sendSuccess(() -> Component.literal("§a§l✓ §aDashboard files updated successfully!"), false);
                if (DashboardAPI.getInstance().isRunning()) {
                    source.sendSuccess(() -> Component.literal("§e⚠ §eRestart to apply: §b/dashboard restart"), false);
                }
            } else {
                source.sendSuccess(() -> Component.literal("§a✓ All files were already up-to-date. Nothing changed."), false);
            }
        }

        return summary.failed.isEmpty() ? 1 : 0;
    }
}
