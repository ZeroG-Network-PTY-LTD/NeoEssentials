package com.zerog.neoessentials.webdashboard;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Launches and supervises a {@code cloudflared} connector process for the internal dashboard,
 * mirroring the external dashboard's own Docker-based cloudflared sidecar (see its
 * docker-compose.yml) but as a plain subprocess, since the mod runs inside the Minecraft server
 * process rather than its own container. Only active when {@code webDashboard.tunnel.enabled} is
 * true; this never downloads or installs cloudflared itself — the operator must already have the
 * binary installed (see {@link ConfigManager#getWebDashboardTunnelCloudflaredPath()}) and a
 * tunnel token configured on the Cloudflare Zero Trust dashboard (see
 * {@link ConfigManager#getWebDashboardTunnelToken()}), same prerequisites as the Docker setup.
 */
public final class CloudflaredTunnelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflaredTunnelManager.class);
    private static final CloudflaredTunnelManager INSTANCE = new CloudflaredTunnelManager();

    private volatile Process process;
    private volatile Thread logPumpThread;
    private volatile boolean stopping;

    private CloudflaredTunnelManager() {
    }

    public static CloudflaredTunnelManager getInstance() {
        return INSTANCE;
    }

    public boolean isRunning() {
        Process p = process;
        return p != null && p.isAlive();
    }

    /**
     * Starts the tunnel if {@code webDashboard.tunnel.enabled} and a token are configured. No-op
     * (returns false, logs nothing beyond a warning) if the feature is simply off, which is the
     * default — this is called unconditionally from the dashboard's own start path.
     */
    public synchronized boolean startIfConfigured() {
        if (!ConfigManager.isWebDashboardTunnelEnabled()) {
            return false;
        }
        if (isRunning()) {
            return true;
        }

        String token = ConfigManager.getWebDashboardTunnelToken();
        if (token == null || token.isBlank()) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD,
                "webDashboard.tunnel.enabled is true but no tunnel token is configured — not starting cloudflared");
            return false;
        }

        String cloudflaredPath = ConfigManager.getWebDashboardTunnelCloudflaredPath();
        stopping = false;

        try {
            ProcessBuilder builder = new ProcessBuilder(cloudflaredPath, "tunnel", "run");
            // Passed via the child's environment, not a CLI argument, so the token never shows
            // up in `ps`/task manager output the way an argument would.
            builder.environment().put("TUNNEL_TOKEN", token);
            builder.redirectErrorStream(true);

            process = builder.start();
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Started cloudflared tunnel connector (pid {})", process.pid());

            Thread pump = new Thread(this::pumpLogs, "neoessentials-cloudflared-log");
            pump.setDaemon(true);
            pump.start();
            logPumpThread = pump;

            return true;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD,
                "Failed to start cloudflared (is '{}' installed and on PATH?): {}", cloudflaredPath, e.getMessage());
            process = null;
            return false;
        }
    }

    private void pumpLogs() {
        Process p = process;
        if (p == null) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "[cloudflared] {}", line);
            }
        } catch (Exception ignored) {
            // Stream closes naturally when the process exits/is destroyed — nothing to report.
        }
        if (!stopping) {
            NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "cloudflared connector exited unexpectedly");
        }
    }

    public synchronized void stop() {
        Process p = process;
        if (p == null) return;

        stopping = true;
        try {
            p.destroy();
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
            NeoLog.info(LOGGER, LogCategory.WEB_DASHBOARD, "Stopped cloudflared tunnel connector");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        } finally {
            process = null;
            logPumpThread = null;
        }
    }
}
