package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
        String url = ConfigManager.getUserSyncWebhookUrl();
        if (url == null || url.isBlank()) return; // disabled — the common case

        JsonObject payload = new JsonObject();
        payload.addProperty("event", event);
        payload.addProperty("id", user.getId());
        payload.addProperty("username", user.getUsername());
        payload.addProperty("email", user.getEmail());
        payload.addProperty("role", user.getRole().name());
        payload.addProperty("enabled", user.isEnabled());
        payload.addProperty("timestamp", System.currentTimeMillis());
        String body = payload.toString();

        String secret = ConfigManager.getUserSyncWebhookSecret();

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

                if (secret != null && !secret.isBlank()) {
                    request.header("X-NeoEssentials-Signature", sign(body, secret));
                }

                HttpResponse<Void> response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 400) {
                    LOGGER.warn("User-sync webhook returned {} for event '{}' (user: {})", response.statusCode(), event, user.getUsername());
                }
            } catch (Exception e) {
                LOGGER.warn("User-sync webhook unreachable for event '{}' (user: {}): {}", event, user.getUsername(), e.getMessage());
            }
        });
    }

    private static String sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            LOGGER.warn("Failed to sign user-sync webhook payload: {}", e.getMessage());
            return "";
        }
    }
}
