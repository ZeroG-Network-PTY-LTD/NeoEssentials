package com.zerog.neoessentials.chat;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rich Text Formatter - Phase 4
 * Provides advanced text effects:
 * - Gradient text (<gradient:start-end>text</gradient>)
 * - Rainbow text (<rainbow>text</rainbow>)
 * - Animated text (future)
 */
public class RichTextFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RichTextFormatter.class);

    // Patterns for rich text tags
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([0-9a-fA-F]{6})-([0-9a-fA-F]{6})>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>(.*?)</rainbow>", Pattern.CASE_INSENSITIVE);

    // Rainbow color spectrum (HSV based)
    private static final int[] RAINBOW_COLORS = {
        0xFF0000, // Red
        0xFF7F00, // Orange
        0xFFFF00, // Yellow
        0x00FF00, // Green
        0x0000FF, // Blue
        0x4B0082, // Indigo
        0x9400D3  // Violet
    };

    /**
     * Pre-process rich text tags (gradient / rainbow) into &#RRGGBB hex codes and return
     * the result as a plain String, leaving all {@code &} / {@code §} color codes untouched.
     * <p>
     * Use this instead of {@link #processRichText(String)} when the string must remain a
     * String for further processing (e.g. URL / mention enhancement), so that {@code &}
     * color codes are not prematurely converted to Component objects and then stripped by
     * {@link Component#getString()}.
     *
     * @param text input string, may contain gradient/rainbow tags and {@code &} color codes
     * @return string with gradient/rainbow tags replaced by {@code &#RRGGBB} sequences;
     *         all other {@code &} codes are left intact
     */
    @SuppressWarnings("unused") // Called from ChatFormatter.formatMessage
    public static String preprocessTags(String text) {
        try {
            if (isRichTextEnabled()) {
                text = processGradients(text);
                text = processRainbow(text);
            }
            return text;
        } catch (Exception e) {
            LOGGER.error("Error pre-processing rich text tags: {}", e.getMessage(), e);
            return text;
        }
    }

    /**
     * Process rich text formatting tags and convert to colored components.
     */
    public static Component processRichText(String text) {
        try {
            if (isRichTextEnabled()) {
                // Process gradients first
                text = processGradients(text);

                // Process rainbow
                text = processRainbow(text);
            }

            // Always parse color codes (even if rich text effects are disabled)
            return com.zerog.neoessentials.util.ChatComponentUtil.parseColorCodes(text);

        } catch (Exception e) {
            LOGGER.error("Error processing rich text: {}", e.getMessage(), e);
            // Still try to parse color codes on error
            try {
                return com.zerog.neoessentials.util.ChatComponentUtil.parseColorCodes(text);
            } catch (Exception e2) {
                return Component.literal(text);
            }
        }
    }

    /**
     * Process gradient tags and convert to hex color codes.
     */
    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String content = matcher.group(3);

            String gradientText = createGradient(content, startHex, endHex);
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Create gradient text with color interpolation.
     */
    private static String createGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return text;

        int startColor = Integer.parseInt(startHex, 16);
        int endColor = Integer.parseInt(endHex, 16);

        // Extract RGB components
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;

        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;

        StringBuilder gradientText = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            // Skip spaces (don't color them)
            if (c == ' ') {
                gradientText.append(c);
                continue;
            }

            // Calculate color for this position
            float progress = length > 1 ? (float) i / (length - 1) : 0;

            int r = (int) (startR + (endR - startR) * progress);
            int g = (int) (startG + (endG - startG) * progress);
            int b = (int) (startB + (endB - startB) * progress);

            // Convert to hex
            String hexColor = String.format("%02X%02X%02X", r, g, b);

            gradientText.append("&#").append(hexColor).append(c);
        }

        return gradientText.toString();
    }

    /**
     * Process rainbow tags and convert to rainbow colored text.
     */
    private static String processRainbow(String text) {
        Matcher matcher = RAINBOW_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String content = matcher.group(1);
            String rainbowText = createRainbow(content);
            matcher.appendReplacement(result, Matcher.quoteReplacement(rainbowText));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Create rainbow colored text.
     */
    private static String createRainbow(String text) {
        if (text.isEmpty()) return text;

        StringBuilder rainbowText = new StringBuilder();
        int colorIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Skip spaces
            if (c == ' ') {
                rainbowText.append(c);
                continue;
            }

            // Get color from rainbow spectrum
            int color = RAINBOW_COLORS[colorIndex % RAINBOW_COLORS.length];
            String hexColor = String.format("%06X", color);

            rainbowText.append("&#").append(hexColor).append(c);
            colorIndex++;
        }

        return rainbowText.toString();
    }

    /**
     * Check if rich text is enabled in config.
     */
    private static boolean isRichTextEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("richText")) {
                return chatConfig.getAsJsonObject("richText").get("enabled").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
}

