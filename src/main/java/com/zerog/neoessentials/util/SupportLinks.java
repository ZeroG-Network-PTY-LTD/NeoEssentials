package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central home for the mod's support/help links (website, Discord, GitHub) — shared by the
 * always-on startup console banner and the "something actually went wrong" alert (console,
 * prominent form, plus a clickable in-game message shown once to the first admin who joins
 * after a real detected problem: a manager failing to initialize, or the permission system
 * falling back to emergency mode).
 */
public final class SupportLinks {
    private SupportLinks() {}

    public static final String SUPPORT_URL = "https://support.zerognetwork.co.za";
    public static final String DISCORD_URL = "https://discord.gg/dUGAQF2Mga";
    public static final String GITHUB_URL = "https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials";

    // Session-scoped (not persisted) — reset naturally on every server restart, matching
    // "once every server restart" for the problem alert.
    private static final AtomicBoolean PROBLEM_DETECTED = new AtomicBoolean(false);
    private static final AtomicBoolean SHOWN_INGAME = new AtomicBoolean(false);

    /** Marks that something actually went wrong this session — enables the join alert below. */
    public static void markProblemDetected() {
        PROBLEM_DETECTED.set(true);
    }

    /**
     * Whether the in-game join alert should fire for the CURRENT join being processed. Only
     * ever returns {@code true} once per server session (first qualifying admin to join),
     * even if multiple admins join afterward or the problem is flagged more than once.
     */
    public static boolean shouldAlertJoiningAdmin() {
        return PROBLEM_DETECTED.get() && SHOWN_INGAME.compareAndSet(false, true);
    }

    /**
     * Plain-text console line(s) — terminals don't support click events, so this is just the
     * bare URLs. {@code prominent} switches between a quiet one-liner (always printed once at
     * startup) and a bordered warning block (printed additionally when a real problem is
     * detected, right at the point of failure).
     */
    public static void logConsole(Logger logger, boolean prominent) {
        if (prominent) {
            logger.warn("╔══════════════════════════════════════════════════════════════╗");
            logger.warn("║  NeoEssentials ran into a problem — need help fixing it?      ║");
            logger.warn("╚══════════════════════════════════════════════════════════════╝");
            logger.warn("  Support: {}", SUPPORT_URL);
            logger.warn("  Discord: {}", DISCORD_URL);
            logger.warn("  GitHub:  {}", GITHUB_URL);
        } else {
            logger.info("Need help with NeoEssentials? Support: {} | Discord: {} | GitHub: {}",
                SUPPORT_URL, DISCORD_URL, GITHUB_URL);
        }
    }

    /** Clickable in-game chat message shown to the first admin joining after a detected problem. */
    public static Component chatMessage() {
        MutableComponent msg = Component.literal("[NE] ")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal("NeoEssentials ran into a problem on startup — need help? ")
                .withStyle(ChatFormatting.YELLOW));
        msg.append(link("[Support]", SUPPORT_URL));
        msg.append(Component.literal(" "));
        msg.append(link("[Discord]", DISCORD_URL));
        msg.append(Component.literal(" "));
        msg.append(link("[GitHub]", GITHUB_URL));
        return msg;
    }

    private static Component link(String label, String url) {
        return Component.literal(label).withStyle(style -> style
            .withColor(ChatFormatting.AQUA)
            .withUnderlined(true)
            .withClickEvent(ClickEventCompat.create(ClickEvent.Action.OPEN_URL, url)));
    }
}
