package com.zerog.neoessentials.ui.tablist;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.TablistTomlConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles animations for tablist headers and footers.
 * This class provides various animation types and manages animation state for each player.
 */
public class TablistAnimationManager {    /**
     * Enum defining the different types of animations available
     */
    public enum AnimationType {
        NONE("none"),
        ROTATION("rotation"),
        SCROLL("scroll"),
        FADE("fade"),
        RAINBOW("rainbow"),
        TYPEWRITER("typewriter"),
        BLINK("blink"),
        WAVE("wave");
        
        private final String configValue;
        
        AnimationType(String configValue) {
            this.configValue = configValue;
        }
        
        public String getConfigValue() {
            return configValue;
        }
        
        public static AnimationType fromConfigValue(String value) {
            for (AnimationType type : values()) {
                if (type.configValue.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return ROTATION; // Default to rotation
        }
    }
    
    // Animation state tracking per player
    private final Map<UUID, PlayerAnimationState> playerStates = new ConcurrentHashMap<>();
    
    // Animation processors for each animation type
    private final Map<AnimationType, AnimationProcessor> animationProcessors = new HashMap<>();
    
    /**
     * Initializes the animation manager with all animation processors
     */
    public TablistAnimationManager() {        // Register animation processors
        animationProcessors.put(AnimationType.NONE, new NoAnimationProcessor());
        animationProcessors.put(AnimationType.ROTATION, new RotationAnimationProcessor());
        animationProcessors.put(AnimationType.SCROLL, new ScrollAnimationProcessor());
        animationProcessors.put(AnimationType.FADE, new FadeAnimationProcessor());
        animationProcessors.put(AnimationType.RAINBOW, new RainbowAnimationProcessor());
        animationProcessors.put(AnimationType.TYPEWRITER, new TypewriterAnimationProcessor());
        animationProcessors.put(AnimationType.BLINK, new BlinkAnimationProcessor());
        animationProcessors.put(AnimationType.WAVE, new WaveAnimationProcessor());
        
        NeoEssentials.LOGGER.info("TablistAnimationManager initialized with {} animation types", animationProcessors.size());
    }
    
    /**
     * Gets the animation state for a player, creating it if it doesn't exist
     * 
     * @param player The player
     * @return The player's animation state
     */
    public PlayerAnimationState getPlayerState(ServerPlayer player) {
        return playerStates.computeIfAbsent(player.getUUID(), uuid -> new PlayerAnimationState());
    }
    
    /**
     * Removes a player's animation state when they disconnect
     * 
     * @param playerId The UUID of the disconnected player
     */
    public void removePlayer(UUID playerId) {
        playerStates.remove(playerId);
    }
    
    /**
     * Updates the animation frame for all players
     */
    public void updateAnimationFrames() {
        playerStates.forEach((uuid, state) -> {
            state.headerFrame++;
            state.footerFrame++;
        });
    }
    
    /**
     * Gets the animated header for a player
     * 
     * @param player The player
     * @param headerTemplates The list of header templates
     * @param placeholderManager The placeholder manager for processing placeholders
     * @return The animated header component
     */
    public Component getAnimatedHeader(ServerPlayer player, List<String> headerTemplates, TablistPlaceholderManager placeholderManager) {
        PlayerAnimationState state = getPlayerState(player);
        AnimationType animationType = AnimationType.fromConfigValue(TablistTomlConfig.HEADER_ANIMATION_TYPE.get());
        AnimationProcessor processor = animationProcessors.getOrDefault(animationType, animationProcessors.get(AnimationType.ROTATION));
        
        // Process the animation
        String animatedText = processor.processFrame(headerTemplates, player, state.headerFrame);
        
        // Process placeholders
        animatedText = placeholderManager.processPlaceholders(animatedText, player);
        
        // Convert to Component
        return TablistPlaceholderManager.colorize(animatedText);
    }
    
    /**
     * Gets the animated footer for a player
     * 
     * @param player The player
     * @param footerTemplates The list of footer templates
     * @param placeholderManager The placeholder manager for processing placeholders
     * @return The animated footer component
     */
    public Component getAnimatedFooter(ServerPlayer player, List<String> footerTemplates, TablistPlaceholderManager placeholderManager) {
        PlayerAnimationState state = getPlayerState(player);
        AnimationType animationType = AnimationType.fromConfigValue(TablistTomlConfig.FOOTER_ANIMATION_TYPE.get());
        AnimationProcessor processor = animationProcessors.getOrDefault(animationType, animationProcessors.get(AnimationType.ROTATION));
        
        // Process the animation
        String animatedText = processor.processFrame(footerTemplates, player, state.footerFrame);
        
        // Process placeholders
        animatedText = placeholderManager.processPlaceholders(animatedText, player);
        
        // Convert to Component
        return TablistPlaceholderManager.colorize(animatedText);
    }
    
    /**
     * Class to track animation state for a player
     */
    public static class PlayerAnimationState {
        public int headerFrame = 0;
        public int footerFrame = 0;
    }
    
    /**
     * Interface for animation processors
     */
    public interface AnimationProcessor {
        /**
         * Process a frame of animation
         * 
         * @param templates The list of templates to animate
         * @param player The player to animate for
         * @param frame The current frame number
         * @return The processed text for this frame
         */
        String processFrame(List<String> templates, ServerPlayer player, int frame);
    }
    
    /**
     * No animation - just returns the first template
     */
    private static class NoAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            return templates.get(0);
        }
    }
    
    /**
     * Rotation animation - cycles through templates
     */
    private static class RotationAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            int index = frame % templates.size();
            return templates.get(index);
        }
    }
    
    /**
     * Scroll animation - scrolls text horizontally
     */
    private static class ScrollAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get the template to scroll
            String template = templates.get(frame % templates.size());
            
            // Remove color codes for scrolling calculation
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // If text is shorter than scroll width, no need to scroll
            int scrollWidth = TablistTomlConfig.SCROLL_WIDTH.get();
            if (plainText.length() <= scrollWidth) {
                return template;
            }
            
            // Calculate scroll position
            int effectiveFrame = frame % (plainText.length() + scrollWidth);
            
            // Create scrolled text with padding
            String scrolledText = plainText + " " + plainText;
            String visiblePortion = TablistPlaceholderManager.substring(scrolledText, effectiveFrame, effectiveFrame + scrollWidth);
            
            // Preserve color codes from the original template
            return TablistPlaceholderManager.transferColors(template, visiblePortion);
        }
    }
    
    /**
     * Fade animation - fades between colors
     */
    private static class FadeAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] FADE_COLORS = {
            ChatFormatting.WHITE,
            ChatFormatting.YELLOW,
            ChatFormatting.GOLD,
            ChatFormatting.RED,
            ChatFormatting.DARK_RED,
            ChatFormatting.DARK_PURPLE,
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.AQUA,
            ChatFormatting.GREEN
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame % templates.size());
            
            // Get the color for this frame
            ChatFormatting color = FADE_COLORS[frame % FADE_COLORS.length];
            
            // Apply the color to the template
            return color + TablistPlaceholderManager.stripColor(template);
        }
    }
    
    /**
     * Rainbow animation - cycles through colors for each character
     */
    private static class RainbowAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] RAINBOW_COLORS = {
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.LIGHT_PURPLE
        };
        
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame % templates.size());
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Build rainbow text
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                int colorIndex = (i + frame) % RAINBOW_COLORS.length;
                result.append(RAINBOW_COLORS[colorIndex]).append(plainText.charAt(i));
            }
            
            return result.toString();
        }
    }
    
    /**
     * Typewriter animation - types out the text
     */
    private static class TypewriterAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame / 20 % templates.size()); // Slower rotation
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Calculate how many characters to show
            int charactersToShow = frame % (plainText.length() + 15);
            if (charactersToShow > plainText.length()) {
                // Hold on complete text for a while
                return template;
            }
            
            // Show only the first N characters
            String visiblePortion = plainText.substring(0, charactersToShow);
            
            // Preserve color codes from the original template
            return TablistPlaceholderManager.transferColors(template, visiblePortion) + "§r§e_";
        }
    }
      /**
     * Blink animation - makes text blink
     */
    private static class BlinkAnimationProcessor implements AnimationProcessor {
        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame / 10 % templates.size());
            
            // Blink every 10 frames
            if ((frame / 5) % 2 == 0) {
                return template;
            } else {
                // This effectively hides the text using a color that matches the background
                return "§8" + TablistPlaceholderManager.stripColor(template);
            }
        }
    }
    
    /**
     * Wave animation - creates a wave effect where characters rise and fall
     */
    private static class WaveAnimationProcessor implements AnimationProcessor {
        private static final ChatFormatting[] WAVE_COLORS = {
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.DARK_AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.AQUA,
            ChatFormatting.WHITE
        };

        @Override
        public String processFrame(List<String> templates, ServerPlayer player, int frame) {
            if (templates.isEmpty()) {
                return "";
            }
            
            // Get template to use
            String template = templates.get(frame / 15 % templates.size());
            String plainText = TablistPlaceholderManager.stripColor(template);
            
            // Build wave text
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < plainText.length(); i++) {
                // Calculate wave position (shifted by character position for wave effect)
                int wavePos = (frame + i * 2) % WAVE_COLORS.length;
                result.append(WAVE_COLORS[wavePos]).append(plainText.charAt(i));
            }
            
            return result.toString();
        }
    }
}
