package com.zerog.neoessentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Advanced chat component utility for creating rich text with hover/click events,
 * color codes, and interactive elements.
 */
public class ChatComponentUtil {
    
    // Pre-compiled regex patterns for performance
    private static final Pattern AMPERSAND_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /**
     * Create a clickable text component that runs a command when clicked.
     * @param text The display text
     * @param command The command to run (without /)
     * @param hoverText Optional hover text
     * @return Component with click functionality
     */
    public static Component createClickableCommand(String text, String command, String hoverText) {
        MutableComponent component = Component.literal(text);
        
        // Add click event to run command
        component.setStyle(Style.EMPTY
            .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.RUN_COMMAND, "/" + command))
            .withHoverEvent(hoverText != null ? 
                com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)) : null)
            .withColor(ChatFormatting.YELLOW)
            .withUnderlined(true)
        );
        
        return component;
    }
    
    /**
     * Create a clickable text component that suggests a command in chat.
     * @param text The display text
     * @param command The command to suggest (without /)
     * @param hoverText Optional hover text
     * @return Component with suggestion functionality
     */
    public static Component createClickableSuggestion(String text, String command, String hoverText) {
        MutableComponent component = Component.literal(text);
        
        component.setStyle(Style.EMPTY
            .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.SUGGEST_COMMAND, "/" + command))
            .withHoverEvent(hoverText != null ? 
                com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)) : null)
            .withColor(ChatFormatting.AQUA)
            .withUnderlined(true)
        );
        
        return component;
    }
    
    /**
     * Create a clickable URL component.
     * @param text The display text
     * @param url The URL to open
     * @param hoverText Optional hover text
     * @return Component with URL functionality
     */
    public static Component createClickableUrl(String text, String url, String hoverText) {
        MutableComponent component = Component.literal(text);
        
        component.setStyle(Style.EMPTY
            .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.OPEN_URL, url))
            .withHoverEvent(hoverText != null ? 
                com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)) : null)
            .withColor(ChatFormatting.BLUE)
            .withUnderlined(true)
        );
        
        return component;
    }
    
    /**
     * Create a component with hover text only.
     * @param text The display text
     * @param hoverText The hover text
     * @param color Optional color
     * @return Component with hover functionality
     */
    public static Component createHoverText(String text, String hoverText, ChatFormatting color) {
        MutableComponent component = Component.literal(text);
        
        component.setStyle(Style.EMPTY
            .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
            .withColor(color != null ? color : ChatFormatting.WHITE)
        );
        
        return component;
    }
    
    /**
     * Create a formatted balance component with hover details.
     * @param playerName The player name
     * @param balance The balance amount
     * @param currency The currency symbol
     * @return Formatted balance component
     */
    public static Component createBalanceComponent(String playerName, double balance, String currency) {
        String balanceText = String.format("%s%,.2f", currency, balance);
        String hoverText = String.format("Player: %s\nBalance: %s\nClick to pay this player", 
            playerName, balanceText);
        
        return createClickableSuggestion(balanceText, "pay " + playerName + " ", hoverText);
    }
    
    /**
     * Create a formatted player name component with hover info and click actions.
     * @param playerName The player name
     * @return Formatted player component
     */
    public static Component createPlayerComponent(String playerName) {
        String hoverText = String.format("Player: %s\nClick to message\nShift+Click to view profile", 
            playerName);
        
        return createClickableSuggestion(playerName, "msg " + playerName + " ", hoverText);
    }
    
    /**
     * Create a permission component with click to copy functionality.
     * @param permission The permission node
     * @return Formatted permission component
     */
    public static Component createPermissionComponent(String permission) {
        String hoverText = String.format("Permission: %s\nClick to copy to clipboard", permission);
        
        MutableComponent component = Component.literal(permission);
        component.setStyle(Style.EMPTY
            .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.COPY_TO_CLIPBOARD, permission))
            .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
            .withColor(ChatFormatting.LIGHT_PURPLE)
        );
        
        return component;
    }

    /**
     * Create a click-to-copy component for an arbitrary secret (API key/token), styled and
     * labeled distinctly from {@link #createPermissionComponent(String)} so it's visually clear
     * this is sensitive, one-time-shown material rather than a permission node.
     * @param label What this value is, shown in the hover tooltip (e.g. "API key")
     * @param value The raw secret to copy
     * @return Component that copies {@code value} to clipboard when clicked
     */
    public static Component createCopyableSecret(String label, String value) {
        String hoverText = String.format("%s\nClick to copy to clipboard", label);

        MutableComponent component = Component.literal(value);
        component.setStyle(Style.EMPTY
            .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
            .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
            .withColor(ChatFormatting.GOLD)
            .withUnderlined(true)
        );

        return component;
    }

    /**
     * Parse color codes in text and return a colored component.
     * Supports: §/& color codes (0-9, a-f), format codes (k-o, r), and hex (&#RRGGBB)
     * This method uses the same parsing logic as ChatFormatter for consistency.
     * @param text Text with color codes
     * @return Colored component
     */
    public static Component parseColorCodes(String text) {
        return parseColorCodesTracked(text, Style.EMPTY).component();
    }

    /**
     * Same as {@link #parseColorCodes(String)}, but starts from {@code baseStyle} instead of no
     * style at all — any color/format code encountered in {@code text} still overrides it as
     * normal, but text with no code of its own keeps {@code baseStyle} (e.g. a color inherited
     * from whatever preceded it in the template) instead of falling back to default/white. Appended
     * sibling components in this codebase's component tree do NOT inherit a preceding sibling's
     * color on their own, which is what this exists to work around — see
     * {@code ChatFormatter#createClickablePlayerNameComponent}'s ambient-style usage.
     */
    public static Component parseColorCodes(String text, Style baseStyle) {
        return parseColorCodesTracked(text, baseStyle).component();
    }

    /**
     * The style still "active" at the very end of {@code text} once every color/format code in
     * it has been applied in order — e.g. for {@code "&cHello &lWorld"} this is red+bold, not
     * just bold. Used to seed an ambient style for content appended immediately after {@code
     * text} (e.g. a clickable player name built as its own sibling component) so a color code
     * placed before it in a template actually carries over, instead of that sibling defaulting
     * to no color the way a fresh unstyled component otherwise would.
     */
    public static Style getTrailingStyle(String text) {
        return parseColorCodesTracked(text, Style.EMPTY).trailingStyle();
    }

    private record ParsedText(Component component, Style trailingStyle) {}

    private static ParsedText parseColorCodesTracked(String text, Style baseStyle) {
        if (text == null || text.isEmpty()) {
            return new ParsedText(Component.empty(), baseStyle);
        }

        MutableComponent result = Component.empty();

        // First convert & to § for uniform processing (using pre-compiled pattern)
        text = AMPERSAND_CODE_PATTERN.matcher(text).replaceAll("§$1");
        
        // Handle hex colors: &#RRGGBB -> RGB color
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            try {
                String hex = hexMatcher.group(1);
                // Replace with placeholder that we'll process later
                hexMatcher.appendReplacement(sb, "§#" + hex + "§");
            } catch (Exception e) {
                hexMatcher.appendReplacement(sb, "");
            }
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();
        
        // Now parse the text character by character, building Components
        StringBuilder currentText = new StringBuilder();
        net.minecraft.network.chat.Style currentStyle = baseStyle;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                
                // Handle hex color: §#RRGGBB§
                if (code == '#' && i + 8 < text.length() && text.charAt(i + 8) == '§') {
                    // Flush current text
                    if (currentText.length() > 0) {
                        result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                        currentText = new StringBuilder();
                    }
                    
                    try {
                        String hex = text.substring(i + 2, i + 8);
                        int rgb = Integer.parseInt(hex, 16);
                        currentStyle = currentStyle.withColor(net.minecraft.network.chat.TextColor.fromRgb(rgb));
                    } catch (Exception e) {
                        // Ignore invalid hex
                    }
                    i += 8; // Skip the hex color code
                    continue;
                }
                
                // Handle standard color codes
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    // Flush current text
                    if (currentText.length() > 0) {
                        result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                        currentText = new StringBuilder();
                    }
                    
                    // Apply the formatting
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = net.minecraft.network.chat.Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = net.minecraft.network.chat.Style.EMPTY.applyFormat(formatting);
                    } else {
                        // Format codes (bold, italic, etc)
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                    
                    i++; // Skip the code character
                    continue;
                }
            }
            
            currentText.append(c);
        }
        
        // Append any remaining text
        if (currentText.length() > 0) {
            result.append(Component.literal(currentText.toString()).setStyle(currentStyle));
        }

        return new ParsedText(result, currentStyle);
    }
    
    /**
     * Create a separator line component.
     * @param length Length of the separator
     * @param character Character to use for separator
     * @param color Color of the separator
     * @return Separator component
     */
    public static Component createSeparator(int length, char character, ChatFormatting color) {
        String separator = String.valueOf(character).repeat(length);
        return Component.literal(separator).withStyle(color != null ? color : ChatFormatting.GRAY);
    }
    
    /**
     * Create a progress bar component.
     * @param current Current value
     * @param max Maximum value
     * @param width Width of the progress bar
     * @return Progress bar component
     */
    public static Component createProgressBar(double current, double max, int width) {
        double percentage = Math.max(0, Math.min(1, current / max));
        int filled = (int) (percentage * width);
        int empty = width - filled;
        
        MutableComponent bar = Component.empty();
        
        // Filled portion (green)
        if (filled > 0) {
            bar.append(Component.literal("█".repeat(filled)).withStyle(ChatFormatting.GREEN));
        }
        
        // Empty portion (gray)
        if (empty > 0) {
            bar.append(Component.literal("█".repeat(empty)).withStyle(ChatFormatting.GRAY));
        }
        
        // Add percentage text
        String percentText = String.format(" %.1f%%", percentage * 100);
        bar.append(Component.literal(percentText).withStyle(ChatFormatting.WHITE));
        
        return bar;
    }
}