package com.zerog.neoessentials.hologram;
import com.zerog.neoessentials.api.PlaceholderManager;
import com.zerog.neoessentials.chat.RichTextFormatter;
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
            String resolved = PlaceholderManager.getInstance().setPlaceholders(player, rawText);
            return RichTextFormatter.processTablistText(resolved);
        } catch (Exception e) {
            return Component.literal(rawText);
        }
    }
    public static Component processStatic(String rawText) {
        return process(rawText, null);
    }
}
