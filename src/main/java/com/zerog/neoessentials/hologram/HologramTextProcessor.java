package com.zerog.neoessentials.hologram;
import com.zerog.neoessentials.api.PlaceholderManager;
import com.zerog.neoessentials.chat.RichTextFormatter;
import com.zerog.neoessentials.tablist.AnimationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;
/**
 * Converts a raw hologram template string into a Component.
 */
public final class HologramTextProcessor {
    private HologramTextProcessor() {}
    public static Component process(String rawText, @Nullable ServerPlayer player) {
        if (rawText == null || rawText.isEmpty()) return Component.empty();
        try {
            // Resolve {animation:NAME} tokens first (same order as TablistManager), then
            // regular {placeholder}/PlaceholderAPI tokens — so holograms support both the
            // tablist's animation-frame system and any registered placeholder expansion.
            String resolved = AnimationManager.getInstance().resolveAnimations(rawText);
            resolved = PlaceholderManager.getInstance().setPlaceholders(player, resolved);
            return RichTextFormatter.processTablistText(resolved);
        } catch (Exception e) {
            return Component.literal(rawText);
        }
    }
    public static Component processStatic(String rawText) {
        return process(rawText, null);
    }
}
