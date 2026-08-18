package com.zerog.neoessentials.integrations;

/**
 * Shared text sanitization for player-supplied content relayed to Discord by any
 * {@link ChatIntegrationAdapter} implementation (SDLink, Mc2Discord, DCIntegration).
 */
public final class DiscordTextSanitizer {
    private DiscordTextSanitizer() {}

    /**
     * Neutralizes Discord mention syntax before player-supplied text is relayed — none of the
     * chat-bridge mods' own send APIs (or this mod's own direct JDA calls) restrict allowed
     * mention types on outgoing messages, so an unsanitized "@everyone"/"@here" or a pasted
     * role/user mention ({@code <@123>}/{@code <@&123>}) in a player's chat message or /msg
     * would actually ping the whole server/role if the bridge bot has that permission — a
     * griefing vector reachable by any player who can type in a bridged channel, not just
     * staff. Inserting a zero-width space breaks Discord's mention parser while leaving the
     * text visually identical to a human reader.
     */
    public static String sanitizeMentions(String text) {
        if (text == null || text.isEmpty()) return text;
        return text
            .replace("@everyone", "@​everyone")
            .replace("@here", "@​here")
            .replaceAll("<(@[!&]?\\d+)>", "<​$1>");
    }
}
