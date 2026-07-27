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
            warnIfNativeChatRelayConflicts();
        } else {
            LOGGER.debug("Simple Discord Link mod not found, integration disabled.");
        }
        return loaded;
    }

    /**
     * SDLink has its own independent, config-driven "relay every chat message to Discord"
     * feature ({@code chat.playerMessages} in its own config file) — completely separate from,
     * and unaware of, NeoEssentials' per-channel {@code chat.channels.<name>.discord} relay.
     * Running both at once means every message NeoEssentials also explicitly relays gets
     * posted to Discord TWICE, and — since SDLink's native relay has no concept of
     * NeoEssentials' chat channels — a channel NeoEssentials treats as private (e.g. a staff
     * channel) still gets relayed in full by SDLink's own blanket relay regardless. NeoEssentials
     * has no way to suppress that from its side, so the only real fix is disabling SDLink's own
     * relay and letting NeoEssentials be the single source of truth. This is a read-only,
     * best-effort text scan (not a full TOML parse) purely to surface an actionable warning —
     * never mind it and never write to SDLink's config file if the scan fails for any reason.
     */
    private void warnIfNativeChatRelayConflicts() {
        try {
            java.nio.file.Path cfg = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get()
                .resolve("config").resolve("simple-discord-link").resolve("simple-discord-link.toml");
            if (!java.nio.file.Files.exists(cfg)) return;

            boolean inChatSection = false;
            for (String line : java.nio.file.Files.readAllLines(cfg)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inChatSection = trimmed.equalsIgnoreCase("[chat]");
                    continue;
                }
                if (inChatSection && trimmed.matches("(?i)playerMessages\\s*=\\s*true.*")) {
                    LOGGER.warn("Simple Discord Link's own 'chat.playerMessages' is enabled in " +
                        "config/simple-discord-link/simple-discord-link.toml. That makes SDLink relay " +
                        "EVERY Minecraft chat message to its own configured Discord channel on its own, " +
                        "entirely independent of NeoEssentials' chat.channels.*.discord relay. With both " +
                        "active, every relayed message posts to Discord twice, and a NeoEssentials channel " +
                        "you intend to keep private (e.g. a staff channel) will still be relayed in full by " +
                        "SDLink's own blanket relay regardless of NeoEssentials' settings. Set " +
                        "'playerMessages = false' under [chat] in that file and restart to let " +
                        "NeoEssentials' own per-channel Discord relay be the only one active.");
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not check Simple Discord Link's config for a conflicting native chat relay: {}", e.getMessage());
        }
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
            if (discordChannelId != null && !discordChannelId.isBlank()) {
                // A specific Discord channel was configured for this NeoEssentials chat channel
                // (e.g. a private staff channel). SDLink's DiscordMessageBuilder has no API to
                // target an arbitrary channel — MessageType.CHAT always posts wherever SDLink's
                // OWN messageDestinations.chat config says, ignoring this parameter entirely.
                // Route directly via sendToChannel() instead, or the configured channel would be
                // silently discarded and the message would always land in SDLink's single default
                // chat channel regardless of how private it was meant to be. This loses the rich
                // author/avatar embed styling send() below gets from SDLink itself, since that
                // styling is only available through the type-routed builder — a plain but
                // correctly-targeted message is the higher priority for a channel meant to be
                // private in the first place.
                sendToChannel(discordChannelId, player.getName().getString() + ": " + cleanMessage);
            } else {
                send(MessageType.CHAT, authorFor(player), cleanMessage);
            }
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
    public Optional<UUID> getLinkedMinecraftUuid(String discordId) {
        if (!isReady()) return Optional.empty();
        try {
            MinecraftAccount account = MinecraftAccount.fromDiscordId(discordId);
            return account != null && account.isAccountVerified() ? Optional.of(account.getUuid()) : Optional.empty();
        } catch (Exception e) {
            LOGGER.debug("SDLink reverse-account lookup failed for {}: {}", discordId, e.getMessage());
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

    @Override
    public boolean sendToChannel(String channelId, String message) {
        if (!isReady()) return false;
        try {
            var jda = BotController.INSTANCE.getJDA();
            var channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                LOGGER.warn("SDLink: no text channel found with ID '{}' (bot may not be in that server, or the ID is wrong)", channelId);
                return false;
            }
            channel.sendMessage(message).queue();
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send message to Discord channel {} via SDLink: {}", channelId, e.getMessage());
            return false;
        }
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
