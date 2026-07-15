package com.zerog.neoessentials.integrations.impl;

import com.hypherionmc.craterlib.api.game.authlib.CraterGameProfile;
import com.hypherionmc.sdlink.api.accounts.DiscordAuthor;
import com.hypherionmc.sdlink.api.accounts.DiscordUser;
import com.hypherionmc.sdlink.api.accounts.MinecraftAccount;
import com.hypherionmc.sdlink.api.messaging.MessageType;
import com.hypherionmc.sdlink.api.messaging.discord.DiscordMessageBuilder;
import com.hypherionmc.sdlink.core.discord.BotController;
import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple Discord Link (SDLink) integration adapter.
 * Talks to SDLink's real public API (com.hypherionmc.sdlink.api.*), compiled against
 * a compileOnly CurseMaven dependency — only ever touched after ModList confirms SDLink
 * is actually loaded, so the mod remains fully optional at runtime.
 */
public class SDLinkAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SDLinkAdapter.class);

    private boolean loaded = false;

    @Override
    public String getName() {
        return "Simple Discord Link";
    }

    @Override
    public boolean initialize() {
        loaded = ModList.get().isLoaded("sdlink");
        if (loaded) {
            LOGGER.info("Simple Discord Link mod detected, integration enabled.");
        } else {
            LOGGER.debug("Simple Discord Link mod not found, integration disabled.");
        }
        return loaded;
    }

    @Override
    public boolean isEnabled() {
        return loaded;
    }

    @Override
    public boolean isReady() {
        return loaded && BotController.INSTANCE != null && BotController.INSTANCE.isBotReady();
    }

    @Override
    public void onPlayerChat(ServerPlayer player, String channel, String message, String formattedMessage, String discordChannelId) {
        if (!isReady()) return;
        try {
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            send(MessageType.CHAT, authorFor(player), cleanMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to relay chat message via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public void onPrivateMessage(ServerPlayer sender, ServerPlayer recipient, String message) {
        if (!isReady()) return;
        try {
            send(MessageType.CUSTOM, authorFor(sender),
                String.format("Private message to %s: %s", recipient.getName().getString(), message));
        } catch (Exception e) {
            LOGGER.error("Failed to relay private message via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerMute(ServerPlayer player, String reason, boolean isMuted) {
        if (!isReady()) return;
        try {
            String action = isMuted ? "muted" : "unmuted";
            send(MessageType.CUSTOM, DiscordAuthor.getServer(),
                String.format("%s has been %s%s", player.getName().getString(), action,
                    reason != null && !reason.isEmpty() ? " (Reason: " + reason + ")" : ""));
        } catch (Exception e) {
            LOGGER.error("Failed to relay mute event via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public void onAfkStatusChange(ServerPlayer player, boolean isAfk, String reason) {
        if (!isReady()) return;
        try {
            String status = isAfk ? "is now AFK" : "is no longer AFK";
            send(MessageType.CUSTOM, DiscordAuthor.getServer(),
                String.format("%s %s%s", player.getName().getString(), status,
                    (isAfk && reason != null && !reason.isEmpty()) ? " (" + reason + ")" : ""));
        } catch (Exception e) {
            LOGGER.error("Failed to relay AFK event via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isReady()) return;
        try {
            send(MessageType.JOIN, authorFor(player), player.getName().getString() + " joined the server");
        } catch (Exception e) {
            LOGGER.error("Failed to relay join event via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerQuit(ServerPlayer player) {
        if (!isReady()) return;
        try {
            send(MessageType.LEAVE, authorFor(player), player.getName().getString() + " left the server");
        } catch (Exception e) {
            LOGGER.error("Failed to relay quit event via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerAdvancement(ServerPlayer player, String advancementName) {
        if (!isReady()) return;
        try {
            send(MessageType.ADVANCEMENTS, authorFor(player),
                player.getName().getString() + " earned the advancement " + advancementName);
        } catch (Exception e) {
            LOGGER.error("Failed to relay advancement event via SDLink: {}", e.getMessage());
        }
    }

    @Override
    public Optional<String> getLinkedDiscordId(UUID minecraftUuid) {
        if (!isReady()) return Optional.empty();
        try {
            MinecraftAccount account = MinecraftAccount.of(CraterGameProfile.fromGame(minecraftUuid.toString(), minecraftUuid));
            if (account == null || !account.isAccountVerified()) return Optional.empty();
            DiscordUser user = account.getDiscordUser();
            return user != null ? Optional.of(Long.toUnsignedString(user.getUserId())) : Optional.empty();
        } catch (Exception e) {
            LOGGER.debug("SDLink linked-account lookup failed for {}: {}", minecraftUuid, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<String> getDiscordRoleIds(UUID minecraftUuid) {
        // SDLink's public API doesn't expose the linked member's role ID list, only
        // display identity (DiscordUser: name/avatar/mention/role color) — role-based
        // permission sync isn't achievable through this adapter without reaching into
        // SDLink's internal (non-API) classes, which is exactly what this rewrite avoids.
        return List.of();
    }

    private DiscordAuthor authorFor(ServerPlayer player) {
        return DiscordAuthor.of(player.getName().getString(), player.getUUID().toString(), player.getName().getString());
    }

    private void send(MessageType type, DiscordAuthor author, String message) {
        new DiscordMessageBuilder(type)
            .author(author)
            .message(message)
            .build()
            .sendMessage();
    }

    @Override
    public void shutdown() {
        LOGGER.info("Simple Discord Link integration shut down.");
    }
}
