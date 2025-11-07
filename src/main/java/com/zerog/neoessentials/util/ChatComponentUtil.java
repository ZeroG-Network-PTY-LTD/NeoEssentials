package com.zerog.neoessentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

/**
 * Advanced chat component utility for creating rich text with hover/click events,
 * color codes, and interactive elements.
 */
public class ChatComponentUtil {
    
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
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command))
            .withHoverEvent(hoverText != null ? 
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)) : null)
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
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + command))
            .withHoverEvent(hoverText != null ? 
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)) : null)
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
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .withHoverEvent(hoverText != null ? 
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)) : null)
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
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
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
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, permission))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
            .withColor(ChatFormatting.LIGHT_PURPLE)
        );
        
        return component;
    }
    
    /**
     * Parse color codes in text and return a colored component.
     * Supports both § and & color codes.
     * @param text Text with color codes
     * @return Colored component
     */
    public static Component parseColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Replace & with § for consistency
        text = text.replace('&', '§');

        MutableComponent result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        ChatFormatting currentColor = ChatFormatting.WHITE;
        net.minecraft.network.chat.TextColor hexColor = null;
        boolean bold = false, italic = false, underlined = false, strikethrough = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Hex color: #RRGGBB (must be at start of segment)
            if (c == '#' && i + 6 < text.length()) {
                String hex = text.substring(i, i + 7);
                if (hex.matches("#[0-9a-fA-F]{6}")) {
                    // Add current text segment if not empty
                    if (currentText.length() > 0) {
                        MutableComponent segment = Component.literal(currentText.toString());
                        Style style = Style.EMPTY;
                        if (hexColor != null) style = style.withColor(hexColor);
                        else style = style.withColor(currentColor);
                        if (bold) style = style.withBold(true);
                        if (italic) style = style.withItalic(true);
                        if (underlined) style = style.withUnderlined(true);
                        if (strikethrough) style = style.withStrikethrough(true);
                        segment.setStyle(style);
                        result.append(segment);
                        currentText.setLength(0);
                    }
                    hexColor = net.minecraft.network.chat.TextColor.parseColor(hex).result().orElse(null);
                    // Reset formatting when color changes
                    bold = italic = underlined = strikethrough = false;
                    i += 6;
                    continue;
                }
            }

            if (c == '§' && i + 1 < text.length()) {
                // Add current text segment if not empty
                if (currentText.length() > 0) {
                    MutableComponent segment = Component.literal(currentText.toString());
                    Style style = Style.EMPTY;
                    if (hexColor != null) style = style.withColor(hexColor);
                    else style = style.withColor(currentColor);
                    if (bold) style = style.withBold(true);
                    if (italic) style = style.withItalic(true);
                    if (underlined) style = style.withUnderlined(true);
                    if (strikethrough) style = style.withStrikethrough(true);
                    segment.setStyle(style);
                    result.append(segment);
                    currentText.setLength(0);
                }

                // Parse formatting code
                char formatCode = text.charAt(i + 1);
                ChatFormatting formatting = ChatFormatting.getByCode(formatCode);

                if (formatting != null) {
                    if (formatting.isColor()) {
                        currentColor = formatting;
                        hexColor = null; // Reset hex color if vanilla color code is used
                        // Reset formatting when color changes
                        bold = italic = underlined = strikethrough = false;
                    } else {
                        // Apply formatting
                        switch (formatting) {
                            case BOLD -> bold = true;
                            case ITALIC -> italic = true;
                            case UNDERLINE -> underlined = true;
                            case STRIKETHROUGH -> strikethrough = true;
                            case OBFUSCATED -> {
                                // Obfuscated formatting - could be handled if needed
                            }
                            case RESET -> {
                                currentColor = ChatFormatting.WHITE;
                                hexColor = null;
                                bold = italic = underlined = strikethrough = false;
                            }
                            // Color cases (handled above in isColor() check, but needed for completeness)
                            case BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, 
                                 GOLD, GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, 
                                 YELLOW, WHITE -> {
                                // Colors are handled in the isColor() block above
                            }
                        }
                    }
                }

                i++; // Skip the format code character
            } else {
                currentText.append(c);
            }
        }

        // Add final text segment
        if (currentText.length() > 0) {
            MutableComponent segment = Component.literal(currentText.toString());
            Style style = Style.EMPTY;
            if (hexColor != null) style = style.withColor(hexColor);
            else style = style.withColor(currentColor);
            if (bold) style = style.withBold(true);
            if (italic) style = style.withItalic(true);
            if (underlined) style = style.withUnderlined(true);
            if (strikethrough) style = style.withStrikethrough(true);
            segment.setStyle(style);
            result.append(segment);
        }

        return result;
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