package com.zerog.neoessentials.chat;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Approximates "live" chat animation for a message containing {@code {animation:NAME}}.
 *
 * <p>Vanilla Minecraft has no packet that repaints an already-delivered chat line — unlike
 * tablist/scoreboard/hologram packets, which get continuously re-sent, a chat message is a
 * permanent, immutable entry in the client's chat log the instant it's delivered. So instead of
 * trying (and failing) to animate the actual chat line, this briefly flashes the SAME message
 * through a few more animation frames via the action bar (the text above the hotbar) right after
 * it's sent — giving a genuine "this is animating" impression with zero impact on chat history:
 * the action bar isn't logged anywhere and leaves no trace once it clears, while the one real
 * chat line (already sent by the caller before this is scheduled) remains the single, permanent
 * record of the message.
 */
public final class ChatAnimationPreview {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatAnimationPreview.class);

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Chat-Animation-Preview");
        t.setDaemon(true);
        return t;
    });

    private ChatAnimationPreview() {}

    /** True if {@code message} (the player's raw/pre-format text) actually references an animation. */
    public static boolean referencesAnimation(String message) {
        return message != null && message.toLowerCase(java.util.Locale.ROOT).contains("{animation:");
    }

    /**
     * Schedules {@code frameCount - 1} additional action-bar flashes (frame 1 is whatever the
     * caller already sent as the real chat line) to every recipient in {@code recipients}, one
     * every {@code intervalMs}. Each flash re-runs {@code chatFormat}/{@code message} through
     * {@link ChatFormatter#formatMessage} fresh — {@link com.zerog.neoessentials.tablist.AnimationManager}'s
     * own tick clock naturally advances the frame between calls, so no separate frame-tracking is
     * needed here.
     */
    public static void schedule(ServerPlayer sender, List<ServerPlayer> recipients,
                                 String chatFormat, String message, String channel) {
        if (!ConfigManager.isChatAnimationPreviewEnabled()) return;
        MinecraftServer server = sender.getServer();
        if (server == null || recipients == null || recipients.isEmpty()) return;

        int frameCount = ConfigManager.getChatAnimationPreviewFrameCount();
        long intervalMs = ConfigManager.getChatAnimationPreviewIntervalMs();
        java.util.List<java.util.UUID> recipientIds = recipients.stream().map(ServerPlayer::getUUID).toList();

        for (int i = 1; i < frameCount; i++) {
            long delay = intervalMs * i;
            SCHEDULER.schedule(() -> server.execute(() -> flashFrame(server, recipientIds, chatFormat, sender, message, channel)),
                delay, TimeUnit.MILLISECONDS);
        }
    }

    private static void flashFrame(MinecraftServer server, List<java.util.UUID> recipientIds,
                                    String chatFormat, ServerPlayer sender, String message, String channel) {
        try {
            ServerPlayer liveSender = server.getPlayerList().getPlayer(sender.getUUID());
            if (liveSender == null) return; // sender left mid-preview
            Component frame = ChatFormatter.formatMessage(chatFormat, liveSender, message, channel);
            for (java.util.UUID id : recipientIds) {
                ServerPlayer target = server.getPlayerList().getPlayer(id);
                if (target != null) target.sendSystemMessage(frame, true); // true = action bar overlay
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error flashing chat animation preview frame", e);
        }
    }
}
