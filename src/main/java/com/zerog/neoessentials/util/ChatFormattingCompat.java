package com.zerog.neoessentials.util;

import net.minecraft.ChatFormatting;

import java.util.EnumSet;
import java.util.Set;

/**
 * {@code ChatFormatting#isColor()} was removed in 26.2 — the enum no longer distinguishes
 * color codes from formatting codes ({@link ChatFormatting} became a bare code table). The
 * 16 color entries are still declared first, in the same fixed order as the §0-§f code table,
 * so they're enumerated explicitly here rather than relied on positionally.
 */
public final class ChatFormattingCompat {
    private ChatFormattingCompat() {}

    private static final Set<ChatFormatting> COLORS = EnumSet.of(
        ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_AQUA,
        ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.GOLD, ChatFormatting.GRAY,
        ChatFormatting.DARK_GRAY, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
        ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, ChatFormatting.WHITE
    );

    public static boolean isColor(ChatFormatting formatting) {
        return COLORS.contains(formatting);
    }
}
