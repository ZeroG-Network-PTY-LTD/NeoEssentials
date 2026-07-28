package com.zerog.neoessentials.teams;

import java.util.UUID;

/**
 * A source of "which team is this player on" for the team chat channel. Implementations
 * detect their target mod via {@code ModList.isLoaded(...)} and talk to it purely through
 * reflection (see {@link FtbTeamsAdapter}) — no compile-time dependency, no risk of the
 * classloading crash NeoEssentials hit with direct JDA references in build.27 (see
 * DCIntegrationAdapter/Mc2DiscordAdapter history), since a class that never imports the
 * external mod's types can always be loaded even when that mod is absent.
 */
public interface TeamProviderAdapter {
    /** Whether the backing mod is installed and its API resolved successfully. */
    boolean isAvailable();

    /** Display name for logs (e.g. "FTB Teams"). */
    String getName();

    /**
     * The player's current team, as an opaque stable identifier (team UUID/id string) —
     * only ever compared for equality against other calls to this same method, never shown
     * to players directly. Returns {@code null} if the player isn't on a team, the lookup
     * failed, or {@link #isAvailable()} is false.
     */
    String getTeamId(UUID playerUUID);
}
