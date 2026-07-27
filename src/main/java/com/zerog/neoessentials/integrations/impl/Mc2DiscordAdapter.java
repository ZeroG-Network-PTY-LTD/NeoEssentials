package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import fr.denisd3d.mc2discord.core.Mc2Discord;
import fr.denisd3d.mc2discord.core.MessageManager;
import fr.denisd3d.mc2discord.core.storage.LinkedPlayerEntry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Mc2Discord integration adapter. Talks to Mc2Discord's real public API
 * (fr.denisd3d.mc2discord.core.*), compiled against a compileOnly CurseMaven dependency —
 * only ever touched after ModList confirms Mc2Discord is actually loaded, so the mod
 * remains fully optional at runtime.
 *
 * MessageManager's methods return a lazy Reactor Mono — nothing is sent until
 * .subscribe() is called, so every call here subscribes to actually fire the message.
 */
public class Mc2DiscordAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Mc2DiscordAdapter.class);

    private boolean loaded = false;

    @Override
    public String getName() {
        return "Mc2Discord";
    }

    @Override
    public boolean initialize() {
        loaded = ModList.get().isLoaded("mc2discord");
        if (loaded) {
            LOGGER.info("Mc2Discord mod detected, integration enabled.");
        } else {
            LOGGER.debug("Mc2Discord mod not found, integration disabled.");
        }
        return loaded;
    }

    @Override
    public boolean isEnabled() {
        return loaded;
    }

    @Override
    public boolean isReady() {
        return loaded && Mc2Discord.INSTANCE != null
            && Mc2Discord.INSTANCE.client != null
            && Mc2Discord.INSTANCE.errors.isEmpty();
    }

    @Override
    public void onPlayerChat(ServerPlayer player, String channel, String message, String formattedMessage, String discordChannelId) {
        if (!isReady()) return;
        try {
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            if (discordChannelId != null && !discordChannelId.isBlank()) {
                // Same rationale as SDLinkAdapter's equivalent fix: sendChatMessage() always
                // posts wherever Mc2Discord's OWN config routes chat, ignoring this parameter
                // entirely. Route directly to the configured channel instead, or a channel
                // NeoEssentials intends to be distinct (e.g. a private staff channel) would
                // silently end up wherever Mc2Discord's default chat channel is instead.
                sendToChannel(discordChannelId, player.getName().getString() + ": " + cleanMessage);
            } else {
                MessageManager.sendChatMessage(cleanMessage, player.getName().getString(), avatarFor(player)).subscribe();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to relay chat message via Mc2Discord: {}", e.getMessage());
        }
    }

    @Override
    public boolean sendToChannel(String channelId, String message) {
        if (!isReady()) return false;
        try {
            // createPlainTextMessage's 3rd parameter (Possible<String>) is NOT a channel/target
            // override — verified by reading the compiled method body — passing it non-absent
            // instead re-runs the message through Mc2Discord's OWN discord_chat_format template
            // (meant for console/system-style broadcasts with a synthetic zero-UUID "player").
            // Possible.absent() sends the message text completely unmodified to the given
            // channel via the underlying Discord4J client, which is what we want here.
            fr.denisd3d.mc2discord.shadow.discord4j.common.util.Snowflake snowflake =
                fr.denisd3d.mc2discord.shadow.discord4j.common.util.Snowflake.of(channelId);
            MessageManager.createPlainTextMessage(snowflake, message,
                fr.denisd3d.mc2discord.shadow.discord4j.discordjson.possible.Possible.absent(), false).subscribe();
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send message to Discord channel {} via Mc2Discord: {}", channelId, e.getMessage());
            return false;
        }
    }

    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isReady()) return;
        try {
            MessageManager.sendInfoMessage("join", player.getName().getString() + " joined the server").subscribe();
        } catch (Exception e) {
            LOGGER.error("Failed to relay join event via Mc2Discord: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerQuit(ServerPlayer player) {
        if (!isReady()) return;
        try {
            MessageManager.sendInfoMessage("leave", player.getName().getString() + " left the server").subscribe();
        } catch (Exception e) {
            LOGGER.error("Failed to relay quit event via Mc2Discord: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerAdvancement(ServerPlayer player, String advancementName) {
        if (!isReady()) return;
        try {
            MessageManager.sendInfoMessage("advancement",
                player.getName().getString() + " earned the advancement " + advancementName).subscribe();
        } catch (Exception e) {
            LOGGER.error("Failed to relay advancement event via Mc2Discord: {}", e.getMessage());
        }
    }

    @Override
    public void onPlayerMute(ServerPlayer player, String reason, boolean isMuted) {
        if (!isReady()) return;
        try {
            String action = isMuted ? "muted" : "unmuted";
            MessageManager.sendInfoMessage("moderation",
                String.format("%s has been %s%s", player.getName().getString(), action,
                    reason != null && !reason.isEmpty() ? " (Reason: " + reason + ")" : "")).subscribe();
        } catch (Exception e) {
            LOGGER.error("Failed to relay mute event via Mc2Discord: {}", e.getMessage());
        }
    }

    @Override
    public Optional<String> getLinkedDiscordId(UUID minecraftUuid) {
        if (!isReady()) return Optional.empty();
        try {
            LinkedPlayerEntry entry = Mc2Discord.INSTANCE.linkedPlayerList.get(minecraftUuid);
            return entry != null ? Optional.of(entry.getDiscordId().asString()) : Optional.empty();
        } catch (Exception e) {
            LOGGER.debug("Mc2Discord linked-account lookup failed for {}: {}", minecraftUuid, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<UUID> getLinkedMinecraftUuid(String discordId) {
        if (!isReady()) return Optional.empty();
        try {
            for (LinkedPlayerEntry entry : Mc2Discord.INSTANCE.linkedPlayerList.getEntries()) {
                if (entry.getDiscordId().asString().equals(discordId)) {
                    return Optional.of(entry.getPlayerUuid());
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.debug("Mc2Discord reverse-account lookup failed for {}: {}", discordId, e.getMessage());
            return Optional.empty();
        }
    }

    private String avatarFor(ServerPlayer player) {
        return "https://mc-heads.net/avatar/" + player.getUUID();
    }

    @Override
    public void shutdown() {
        LOGGER.info("Mc2Discord integration shut down.");
    }
}
