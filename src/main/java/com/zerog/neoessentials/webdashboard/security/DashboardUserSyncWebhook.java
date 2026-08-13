package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Best-effort outbound notification whenever a dashboard_user is created, updated, or deleted
 * (see call sites in {@link AuthenticationManager}) — lets an external dashboard mirror the
 * mod's own account lifecycle into its own user table automatically, rather than requiring an
 * operator to maintain both sides by hand. This is a push on top of the existing
 * {@code /api/users/*} REST endpoints, not a replacement for them: if the webhook URL isn't
 * configured, or the receiving end is briefly unreachable, nothing here blocks or fails the
 * actual account change — it already happened in the mod's own storage regardless.
 */
public class DashboardUserSyncWebhook {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardUserSyncWebhook.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();

    private DashboardUserSyncWebhook() {}

    /** Fire-and-forget notification for a user lifecycle event ("user_created"/"user_updated"/"user_deleted"). */
    public static void notify(String event, User user) {
        String url = ConfigManager.getExternalDashboardUrl();
        String token = ConfigManager.getExternalDashboardToken();
        if (url == null || url.isBlank() || token == null || token.isBlank()) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Skipping user-sync webhook for event '{}': dashboard not paired", event);
            return; // not paired — the common case
        }
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Dispatching user-sync webhook '{}' for user {}", event, user.getUsername());

        JsonObject payload = new JsonObject();
        payload.addProperty("event", event);
        payload.addProperty("id", user.getId());
        payload.addProperty("username", user.getUsername());
        payload.addProperty("email", user.getEmail());
        payload.addProperty("role", user.getRole().name());
        payload.addProperty("enabled", user.isEnabled());
        payload.addProperty("timestamp", System.currentTimeMillis());
        String body = payload.toString();

        String endpoint = url.replaceAll("/+$", "") + "/webhooks/mod/user-sync";

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

                HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 400) {
                    NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "User-sync webhook returned {} for event '{}' (user: {})", response.statusCode(), event, user.getUsername());
                }
            } catch (Exception e) {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "User-sync webhook unreachable for event '{}' (user: {}): {}", event, user.getUsername(), e.getMessage());
            }
        });
    }
}
