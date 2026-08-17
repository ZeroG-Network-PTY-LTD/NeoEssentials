package com.zerog.neoessentials.tablist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NeoEssentials Animation System
 *
 * <p>Loads named text animations from {@code animations.json} and makes them
 * available as placeholders anywhere in the mod via {@code {animation:NAME}}.
 *
 * <h2>animations.json structure</h2>
 * <pre>{@code
 * {
 *   "animations": [
 *     {
 *       "name": "Rainbow",
 *       "frames": [ "&cR&6ainbow", "&6R&cainbow", ... ],
 *       "frameDuration": 500
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Tablist header/footer: {@code &7Welcome {animation:Rainbow} to the server!}</li>
 *   <li>MOTD, chat prefixes, or any text processed through this manager's
 *       {@link #resolveAnimations(String)} method.</li>
 * </ul>
 *
 * <p>Frame advancement is driven by wall-clock time (milliseconds), so
 * {@code frameDuration: 500} means each frame is shown for 500 ms regardless
 * of the tablist refresh rate.  {@link TablistManager} calls
 * {@link #tick(long)} on every server tick so animations remain accurate
 * even when the tablist refresh interval is longer than the frame duration.
 */
public class AnimationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationManager.class);

    /** Matches {@code {animation:NAME}} tokens in text. */
    private static final Pattern ANIMATION_PATTERN =
        Pattern.compile("\\{animation:([^}]+)\\}", Pattern.CASE_INSENSITIVE);

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static final AnimationManager INSTANCE = new AnimationManager();
    public static AnimationManager getInstance() { return INSTANCE; }

    // ── Internal state ────────────────────────────────────────────────────────
    private static final class AnimationState {
        final String name;
        final List<String> frames;
        final long frameDurationMs;
        int   currentFrame  = 0;
        long  lastFrameTime = 0L;   // wall-clock ms of last frame advance

        AnimationState(String name, List<String> frames, long frameDurationMs) {
            this.name           = name;
            this.frames         = Collections.unmodifiableList(new ArrayList<>(frames));
            this.frameDurationMs = Math.max(50, frameDurationMs); // minimum 50 ms
            this.lastFrameTime  = System.currentTimeMillis();
        }
    }

    /** name (lower-cased) → animation state */
    private final Map<String, AnimationState> animations = new ConcurrentHashMap<>();

    private AnimationManager() {}

    // ── Config loading ────────────────────────────────────────────────────────

    /**
     * Load (or reload) all animations from {@code animations.json}.
     * Safe to call at any time; existing state is replaced atomically.
     */
    public void loadConfig() {
        Map<String, AnimationState> loaded = new LinkedHashMap<>();
        try {
            JsonObject root = ConfigManager.getInstance()
                .getConfig(ConfigManager.ANIMATIONS_CONFIG);
            if (root == null || !root.has("animations")) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "AnimationManager: no 'animations' array in animations.json — using empty set.");
                animations.clear();
                return;
            }
            JsonArray arr = root.getAsJsonArray("animations");
            for (var el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject obj   = el.getAsJsonObject();
                String     name  = obj.has("name") ? obj.get("name").getAsString().trim() : null;
                if (name == null || name.isEmpty()) {
                    LOGGER.warn("AnimationManager: skipping animation entry with missing/empty 'name'.");
                    continue;
                }
                List<String> frames = new ArrayList<>();
                if (obj.has("frames") && obj.get("frames").isJsonArray()) {
                    for (var f : obj.getAsJsonArray("frames")) {
                        if (f.isJsonPrimitive()) frames.add(f.getAsString());
                    }
                }
                if (frames.isEmpty()) {
                    LOGGER.warn("AnimationManager: animation '{}' has no frames — skipping.", name);
                    continue;
                }
                long duration = obj.has("frameDuration") ? obj.get("frameDuration").getAsLong() : 500L;
                loaded.put(name.toLowerCase(), new AnimationState(name, frames, duration));
            }
            animations.clear();
            animations.putAll(loaded);
            NeoLog.info(LOGGER, LogCategory.GENERAL, "AnimationManager: loaded {} animation(s): {}.",
                animations.size(), String.join(", ", animations.keySet()));
        } catch (Exception e) {
            LOGGER.error("AnimationManager: failed to load animations.json: {}", e.getMessage());
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    /**
     * Advance all animation frames based on elapsed wall-clock time.
     * Should be called on every server tick for accurate timing.
     *
     * @param nowMs {@code System.currentTimeMillis()} at invocation
     */
    public void tick(long nowMs) {
        for (AnimationState anim : animations.values()) {
            if (nowMs - anim.lastFrameTime >= anim.frameDurationMs) {
                anim.currentFrame = (anim.currentFrame + 1) % anim.frames.size();
                // Align next deadline to avoid drift accumulation
                anim.lastFrameTime += anim.frameDurationMs;
                // Guard against large pauses (server freeze / reload)
                if (nowMs - anim.lastFrameTime > anim.frameDurationMs) {
                    anim.lastFrameTime = nowMs;
                }
            }
        }
    }

    // ── Placeholder resolution ────────────────────────────────────────────────

    /**
     * Returns the current frame string for the named animation, or an empty
     * string if the animation does not exist.
     *
     * @param name animation name (case-insensitive)
     */
    public String getCurrentFrame(String name) {
        if (name == null) return "";
        AnimationState anim = animations.get(name.toLowerCase());
        if (anim == null || anim.frames.isEmpty()) return "";
        return anim.frames.get(anim.currentFrame);
    }

    /**
     * Replace all {@code {animation:NAME}} tokens in {@code text} with the
     * current frame of the named animation.
     *
     * <p>Unknown animation names are replaced with an empty string.
     * Returns {@code text} unchanged (including if {@code null}) when no
     * tokens are present.
     *
     * @param text source string, may be {@code null}
     * @return text with animation tokens resolved
     */
    public String resolveAnimations(String text) {
        if (text == null || !text.contains("{animation:")) return text;
        Matcher m = ANIMATION_PATTERN.matcher(text);
        if (!m.find()) return text;
        // Reset and do full replacement
        m.reset();
        StringBuilder sb = new StringBuilder(text.length());
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(getCurrentFrame(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** @return number of animations currently loaded */
    public int getAnimationCount() { return animations.size(); }

    /** @return unmodifiable view of loaded animation names (lower-cased keys) */
    public Set<String> getAnimationNames() { return Collections.unmodifiableSet(animations.keySet()); }

    /**
     * Returns a summary string for each loaded animation suitable for in-game
     * display (used by {@code /tablist animations list}).
     */
    public List<String> getSummaryLines() {
        if (animations.isEmpty()) return List.of("&7No animations loaded.");
        List<String> lines = new ArrayList<>();
        for (AnimationState anim : animations.values()) {
            lines.add(String.format("&e%s &8— &7%d frame(s), &a%dms &7per frame",
                anim.name, anim.frames.size(), anim.frameDurationMs));
        }
        return lines;
    }
}

