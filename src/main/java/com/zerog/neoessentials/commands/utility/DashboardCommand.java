package com.zerog.neoessentials.commands.utility;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.DashboardAPI;
import com.zerog.neoessentials.webdashboard.DashboardFileManager;
import com.zerog.neoessentials.webdashboard.DashboardLifecycleManager;
import com.zerog.neoessentials.webdashboard.security.ApiKeyManager;
import com.zerog.neoessentials.webdashboard.security.User;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
 * - /dashboard pair "&lt;dashboardUrl&gt;" &lt;code&gt; — complete the mutual pairing handshake with an
 *   external dashboard (see {@link com.zerog.neoessentials.webdashboard.security.DashboardUserSyncWebhook}).
 *   The URL must be quoted — Brigadier's unquoted string parsing can't contain ':' or '/',
 *   which every URL does.
 * - /dashboard unpair    — clear the paired connection and revoke its API key
 */
public class DashboardCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardCommand.class);
    private static final Gson GSON = new Gson();
    private static final HttpClient PAIR_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();

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
            .then(Commands.literal("pair")
                .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.dashboard.pair").hasPermission())
                .then(Commands.argument("dashboardUrl", StringArgumentType.string())
                    .then(Commands.argument("code", StringArgumentType.word())
                        .executes(ctx -> pair(ctx,
                            StringArgumentType.getString(ctx, "dashboardUrl"),
                            StringArgumentType.getString(ctx, "code"))))))
            .then(Commands.literal("unpair")
                .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.dashboard.pair").hasPermission())
                .executes(DashboardCommand::unpair))
        );
    }

    // ── Dashboard pairing (mutual handshake with an external dashboard) ────────

    private static int pair(CommandContext<CommandSourceStack> ctx, String dashboardUrl, String code) {
        CommandSourceStack source = ctx.getSource();
        String normalizedUrl = dashboardUrl.replaceAll("/+$", "");

        String modToken = ApiKeyManager.getInstance().createKey("dashboard-pairing", User.Role.ADMIN);
        String keyId = ApiKeyManager.extractKeyId(modToken);

        String serverName = ServerLifecycleHooks.getCurrentServer() != null
            ? ServerLifecycleHooks.getCurrentServer().getMotd()
            : "Minecraft Server";

        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        body.addProperty("modToken", modToken);
        body.addProperty("serverName", serverName);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizedUrl + "/api/pair/complete"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = PAIR_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                if (keyId != null) ApiKeyManager.getInstance().revoke(keyId);
                source.sendFailure(Component.literal("§cPairing failed — dashboard responded with " + response.statusCode() + ". Is the code correct and not expired?"));
                return 0;
            }

            JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
            if (json == null || !json.has("success") || !json.get("success").getAsBoolean() || !json.has("dashboardToken")) {
                if (keyId != null) ApiKeyManager.getInstance().revoke(keyId);
                String message = json != null && json.has("message") ? json.get("message").getAsString() : "unexpected response";
                source.sendFailure(Component.literal("§cPairing failed — " + message));
                return 0;
            }

            String dashboardToken = json.get("dashboardToken").getAsString();

            ConfigManager.setExternalDashboardUrl(normalizedUrl);
            ConfigManager.setExternalDashboardToken(dashboardToken);
            ConfigManager.setExternalDashboardKeyId(keyId != null ? keyId : "");

            source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§aPaired with dashboard at " + normalizedUrl + "."), false);
            source.sendSuccess(() -> Component.literal("§7Both directions are now connected — the dashboard can control this server, and this server can push account-sync events to it."), false);
            return 1;
        } catch (Exception e) {
            if (keyId != null) ApiKeyManager.getInstance().revoke(keyId);
            LOGGER.warn("Dashboard pairing failed: {}", e.getMessage());
            source.sendFailure(Component.literal("§cCould not reach the dashboard at " + normalizedUrl + " — " + e.getMessage()));
            return 0;
        }
    }

    private static int unpair(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String keyId = ConfigManager.getExternalDashboardKeyId();
        String url = ConfigManager.getExternalDashboardUrl();

        if (url == null || url.isBlank()) {
            source.sendSuccess(() -> Component.literal("§7Not currently paired with any dashboard."), false);
            return 1;
        }

        if (keyId != null && !keyId.isBlank()) {
            ApiKeyManager.getInstance().revoke(keyId);
        }
        ConfigManager.clearExternalDashboard();

        source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§aUnpaired from " + url + " and revoked its API key."), false);
        return 1;
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
