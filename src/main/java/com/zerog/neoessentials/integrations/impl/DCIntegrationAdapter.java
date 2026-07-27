package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Discord Integration (DCIntegration) adapter. Talks to its real public API
 * (de.erdbeerbaerlp.dcintegration.common.*), compiled against a compileOnly CurseMaven
 * dependency — only ever touched after ModList confirms DCIntegration is actually loaded,
 * so the mod remains fully optional at runtime.
 *
 * Unlike SDLink/Mc2Discord, DCIntegration mixins directly into vanilla chat/join/leave/
 * command handling (ChatMixin, PlayerManagerMixin, NetworkHandlerMixin, CommandManagerMixin)
 * and relays those events to Discord entirely on its own, to whatever channel its own config
 * (general.botChannel / advanced.chatOutputChannelID) points at — this adapter has no
 * visibility into or control over that path. onPlayerJoin/onPlayerQuit are deliberately left
 * unimplemented, since calling DiscordIntegration.sendMessage() for those would double-post
 * alongside DCIntegration's own mixin-driven relay.
 *
 * onPlayerChat IS implemented, but ONLY acts when a NeoEssentials chat channel has a specific
 * discord.channelId configured (e.g. a private staff channel) — that is additive, not
 * duplicative, since DCIntegration's own native relay has no concept of NeoEssentials channels
 * and only ever posts to its own single configured channel. When discordChannelId is null (the
 * channel is configured to just use "the default" — most channels), this deliberately does
 * nothing at all, exactly as before, to avoid double-posting against DCIntegration's own
 * mixin-driven relay of the SAME message.
 */
public class DCIntegrationAdapter implements ChatIntegrationAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DCIntegrationAdapter.class);

    private boolean loaded = false;

    @Override
    public String getName() {
        return "DCIntegration";
    }

    @Override
    public boolean initialize() {
        loaded = ModList.get().isLoaded("dcintegration");
        if (loaded) {
            LOGGER.info("DCIntegration mod detected, integration enabled.");
        } else {
            LOGGER.debug("DCIntegration mod not found, integration disabled.");
        }
        return loaded;
    }

    @Override
    public boolean isEnabled() {
        return loaded;
    }

    @Override
    public boolean isReady() {
        return loaded && DiscordIntegration.INSTANCE != null && DiscordIntegration.INSTANCE.getJDA() != null;
    }

    @Override
    public void onPlayerChat(ServerPlayer player, String channel, String message, String formattedMessage, String discordChannelId) {
        if (!isReady() || discordChannelId == null || discordChannelId.isBlank()) return;
        try {
            String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
            sendToChannel(discordChannelId, player.getName().getString() + ": " + cleanMessage);
        } catch (Exception e) {
            LOGGER.error("Failed to relay chat message via DCIntegration: {}", e.getMessage());
        }
    }

    @Override
    public boolean sendToChannel(String channelId, String message) {
        if (!isReady()) return false;
        try {
            var channel = DiscordIntegration.INSTANCE.getJDA().getTextChannelById(channelId);
            if (channel == null) {
                LOGGER.warn("DCIntegration: no text channel found with ID '{}' (bot may not be in that server, or the ID is wrong)", channelId);
                return false;
            }
            DiscordIntegration.INSTANCE.sendMessage(message, channel);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send message to Discord channel {} via DCIntegration: {}", channelId, e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<String> getLinkedDiscordId(UUID minecraftUuid) {
        if (!isReady()) return Optional.empty();
        try {
            PlayerLink link = LinkManager.getLink(null, minecraftUuid);
            return link != null ? Optional.of(link.discordID) : Optional.empty();
        } catch (Exception e) {
            LOGGER.debug("DCIntegration linked-account lookup failed for {}: {}", minecraftUuid, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<UUID> getLinkedMinecraftUuid(String discordId) {
        if (!isReady()) return Optional.empty();
        try {
            PlayerLink link = LinkManager.getLink(discordId, null);
            return link != null ? Optional.of(UUID.fromString(link.mcPlayerUUID)) : Optional.empty();
        } catch (Exception e) {
            LOGGER.debug("DCIntegration reverse-account lookup failed for {}: {}", discordId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("DCIntegration integration shut down.");
    }
}
