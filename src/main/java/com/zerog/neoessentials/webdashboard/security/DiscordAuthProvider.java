package com.zerog.neoessentials.webdashboard.security;

import com.zerog.neoessentials.integrations.ChatIntegrationManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provider for Discord account-link lookups, backed by whichever Discord companion mod
 * (SDLink, Mc2Discord) is installed and ready — via {@link ChatIntegrationManager}'s
 * generic adapter contract, not mod-specific reflection.
 */
public class DiscordAuthProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordAuthProvider.class);
    private static DiscordAuthProvider INSTANCE;

    private DiscordAuthProvider() {
    }

    public static DiscordAuthProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscordAuthProvider();
        }
        return INSTANCE;
    }

    /**
     * Whether a Discord companion mod (SDLink, Mc2Discord, or DCIntegration) is installed and its
     * bot connection is currently ready.
     */
    public boolean isAvailable() {
        return ChatIntegrationManager.getAdapters().stream()
            .anyMatch(com.zerog.neoessentials.integrations.ChatIntegrationAdapter::isReady);
    }

    /**
     * Get linked Discord account for a Minecraft username.
     */
    public DiscordUser getLinkedAccount(String minecraftUsername) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Server not available, cannot retrieve linked account");
                return null;
            }

            ServerPlayer player = server.getPlayerList().getPlayerByName(minecraftUsername);
            UUID playerUuid = player != null ? player.getUUID()
                : server.services().nameToIdCache().get(minecraftUsername).map(profile -> profile.id()).orElse(null);

            if (playerUuid == null) {
                NeoLog.warn(LOGGER, LogCategory.WEB_DASHBOARD, "Cannot find UUID for player: {}", minecraftUsername);
                return null;
            }

            return getLinkedAccountByUuid(playerUuid);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error getting linked account for " + minecraftUsername, e);
            return null;
        }
    }

    /**
     * Get linked Discord account by Minecraft UUID, checking whichever adapter is ready.
     *
     * <p>{@code discordUsername} is filled with the raw Discord snowflake ID — none of the
     * companion-mod adapters (SDLink/Mc2Discord/DCIntegration) expose a resolved display name
     * through {@link com.zerog.neoessentials.integrations.ChatIntegrationAdapter}, only
     * {@code getLinkedDiscordId}, so there's nothing else to put there without new API surface
     * on all three adapters. {@code minecraftUsername}, unlike that, IS resolvable locally (the
     * server already knows every online/cached player's name for a UUID), so it's looked up
     * properly instead of also falling back to the UUID string.
     */
    public DiscordUser getLinkedAccountByUuid(UUID minecraftUuid) {
        Optional<String> discordId = ChatIntegrationManager.findLinkedDiscordId(minecraftUuid);
        if (discordId.isEmpty()) {
            NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "No linked Discord account found for UUID: {}", minecraftUuid);
            return null;
        }

        List<String> discordRoles = ChatIntegrationManager.findDiscordRoleIds(minecraftUuid);
        String minecraftUsername = resolveMinecraftUsername(minecraftUuid);
        return new DiscordUser(discordId.get(), discordId.get(), minecraftUsername, minecraftUuid.toString(), discordRoles);
    }

    /** Resolves a UUID to its player name via the server's own player list/profile cache,
     *  falling back to the UUID string only if neither has it (e.g. the player has never
     *  joined this server). */
    private static String resolveMinecraftUsername(UUID minecraftUuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return minecraftUuid.toString();

        ServerPlayer player = server.getPlayerList().getPlayer(minecraftUuid);
        if (player != null) return player.getName().getString();

        return server.services().nameToIdCache().get(minecraftUuid)
            .map(net.minecraft.server.players.NameAndId::name)
            .orElse(minecraftUuid.toString());
    }

    /**
     * Check if a Minecraft UUID is linked to Discord.
     */
    public boolean isAccountLinkedByUuid(UUID minecraftUuid) {
        DiscordUser user = getLinkedAccountByUuid(minecraftUuid);
        return user != null && user.isLinked();
    }
}
