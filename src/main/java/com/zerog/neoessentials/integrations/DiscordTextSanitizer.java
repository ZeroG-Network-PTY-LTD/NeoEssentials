package com.zerog.neoessentials.integrations;

/**
 * Shared text sanitization for player-supplied content relayed to Discord by any
 * {@link ChatIntegrationAdapter} implementation (SDLink, Mc2Discord, DCIntegration, Webhook).
 */
public final class DiscordTextSanitizer {
    private DiscordTextSanitizer() {}

    /**
     * Conservative cap for a chunk of player-supplied text (chat message, mute/AFK reason,
     * advancement name, private message body) going into a Discord message. Discord's actual
     * plain-message-content cap is 2000 characters, but this mod's own adapters often prepend a
     * player name or other fixed text to whatever gets truncated here (e.g.
     * {@code player.getName() + ": " + message}) — capping well under 2000 leaves headroom for
     * that prefix so the final composed string still can't exceed Discord's real limit.
     */
    public static final int DISCORD_TEXT_LIMIT = 1900;

    /**
     * Truncates {@code text} to at most {@code maxLength} characters, replacing the last
     * character with an ellipsis when it was cut short, so the API doesn't silently reject (or
     * throw on) a message a player made too long — e.g. a very long chat line, or a custom
     * advancement/mute-reason string. No-op if already within the limit.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 1) + "…";
    }

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

    /**
     * "{@code <name> has been muted/unmuted (Reason: <reason>)}" — shared by every adapter
     * (SDLink/Mc2Discord/DCIntegration/Webhook) so the sanitize+truncate step for a player-typed
     * mute reason can't be forgotten on one of them again (see git history — it was, four times).
     */
    public static String formatMuteEvent(String displayName, boolean isMuted, String reason) {
        String action = isMuted ? "muted" : "unmuted";
        String safeReason = (reason != null && !reason.isEmpty()) ? truncate(sanitizeMentions(reason), DISCORD_TEXT_LIMIT) : null;
        return String.format("%s has been %s%s", displayName, action, safeReason != null ? " (Reason: " + safeReason + ")" : "");
    }

    /**
     * "{@code <name> is now/no longer AFK (<reason>)}" — same sharing rationale as
     * {@link #formatMuteEvent}; the reason only ever shows when going AFK, never on return.
     */
    public static String formatAfkEvent(String displayName, boolean isAfk, String reason) {
        String status = isAfk ? "is now AFK" : "is no longer AFK";
        String safeReason = (isAfk && reason != null && !reason.isEmpty()) ? truncate(sanitizeMentions(reason), DISCORD_TEXT_LIMIT) : null;
        return String.format("%s %s%s", displayName, status, safeReason != null ? " (" + safeReason + ")" : "");
    }

    /**
     * "{@code <name> earned the advancement <advancementName>}" — same sharing rationale as
     * {@link #formatMuteEvent}. An advancement's title is normally server/datapack-defined, not
     * player-typed, but some setups (e.g. FTB Quests custom rewards, a datapack reading a
     * scoreboard/NBT string into a title) let it carry player-influenced text — sanitize it the
     * same as every other relayed string on general principle, since none of the chat-bridge
     * mods' own send APIs restrict mention types either way.
     */
    public static String formatAdvancementEvent(String displayName, String advancementName) {
        String safeName = truncate(sanitizeMentions(advancementName), DISCORD_TEXT_LIMIT);
        return String.format("%s earned the advancement %s", displayName, safeName);
    }
}
