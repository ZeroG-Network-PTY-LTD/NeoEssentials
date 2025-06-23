package com.zerog.neoessentials.ui.tablist.placeholders;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry for custom placeholders in the tablist
 */
public class CustomPlaceholderRegistry {
    // Pattern for detecting placeholders with arguments: %placeholder:arg1,arg2%
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)(?::(.*?))?%");
    
    // Map of registered placeholders
    private final Map<String, CustomPlaceholder> placeholders = new HashMap<>();
    
    /**
     * Registers a custom placeholder
     * @param placeholder The placeholder to register
     * @return This registry for chaining
     */
    public CustomPlaceholderRegistry register(CustomPlaceholder placeholder) {
        placeholders.put(placeholder.getName().toLowerCase(), placeholder);
        NeoEssentials.LOGGER.debug("Registered custom placeholder: {}", placeholder.getName());
        return this;
    }
    
    /**
     * Gets a registered placeholder by name
     * @param name The name of the placeholder
     * @return The placeholder, if registered
     */
    public Optional<CustomPlaceholder> getPlaceholder(String name) {
        return Optional.ofNullable(placeholders.get(name.toLowerCase()));
    }
    
    /**
     * Processes text and replaces any custom placeholders
     * @param text The text to process
     * @param player The player to evaluate placeholders for
     * @return The processed text
     */
    public String processPlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Find and replace all placeholders
        StringBuilder result = new StringBuilder(text);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        
        // Track offset from replacements changing the string length
        int offset = 0;
        
        while (matcher.find()) {
            String placeholderName = matcher.group(1);
            String argsText = matcher.group(2);
            String[] args = argsText != null ? argsText.split(",") : new String[0];
            
            // Try to get the placeholder
            Optional<CustomPlaceholder> placeholder = getPlaceholder(placeholderName);
            if (placeholder.isPresent()) {
                // Get the replacement value
                String replacement = placeholder.get().getValue(player, args);
                
                // Replace in the result
                int startPos = matcher.start() + offset;
                int endPos = matcher.end() + offset;
                result.replace(startPos, endPos, replacement);
                
                // Update offset for next replacement
                offset += replacement.length() - (endPos - startPos);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Gets all registered placeholders
     * @return Map of placeholder names to placeholders
     */
    public Map<String, CustomPlaceholder> getAllPlaceholders() {
        return new HashMap<>(placeholders);
    }
}
