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

    /**
     * Resolves {@code {animation:NAME}}/placeholder tokens WITHOUT converting to a
     * {@link Component} — for change detection only, never for display.
     *
     * <p>{@link Component#getString()} returns the plain-text content with all styling
     * stripped, so two frames of a color-only animation (e.g. a gradient cycling through hex
     * stops over the same literal words, like {@code <gradient:#FF0000-#0000FF>Text</gradient>}
     * vs {@code <gradient:#0000FF-#FF0000>Text</gradient>}) resolve to the IDENTICAL plain
     * string. Comparing {@code Component.getString()} output across frames therefore never sees
     * a change for that (extremely common) case — the animation clock keeps advancing, but the
     * hologram never re-renders past its first frame. This returns the resolved string before
     * that stripping happens (gradient/rainbow tags and color codes still present), so a
     * color-only frame change is still detected as a change.
     */
    public static String resolveRaw(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";
        try {
            String resolved = AnimationManager.getInstance().resolveAnimations(rawText);
            return PlaceholderManager.getInstance().setPlaceholders(null, resolved);
        } catch (Exception e) {
            return rawText;
        }
    }
}
