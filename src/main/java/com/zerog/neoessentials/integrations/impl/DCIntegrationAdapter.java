package com.zerog.neoessentials.integrations.impl;

import com.zerog.neoessentials.integrations.ChatIntegrationAdapter;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.PlayerLink;
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
 * and relays those events to Discord entirely on its own. This adapter deliberately does NOT
 * override onPlayerChat/onPlayerJoin/onPlayerQuit — calling DiscordIntegration.sendMessage()
 * for those would double-post alongside DCIntegration's own mixin-driven relay. Its role here
 * is readiness reporting and account-link lookups only.
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
