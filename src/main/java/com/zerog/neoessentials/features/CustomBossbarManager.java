package com.zerog.neoessentials.features;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.zerog.neoessentials.config.TablistConfig;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.ColorUtil;
import com.zerog.neoessentials.util.DebugUtil;

import java.util.*;
import java.util.concurrent.*;

/**
 * Enhanced Bossbar Manager with improved scheduling, animations, and templating
 * EssentialsX-style Bossbar manager for NeoEssentials
 */
public class CustomBossbarManager {
    private static final CustomBossbarManager INSTANCE = new CustomBossbarManager();
    
    // Active bossbars per player
    private final Map<UUID, ActiveBossbar> activeBossbars = new ConcurrentHashMap<>();
    
    // Template system
    private final Map<String, BossbarTemplate> templates = new ConcurrentHashMap<>();
    
    // Scheduling system
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<UUID, ScheduledFuture<?>> scheduledRemovals = new ConcurrentHashMap<>();
    
    // Animation system
    private final Map<String, AnimatedBossbar> animatedBossbars = new ConcurrentHashMap<>();
    private ScheduledFuture<?> animationTask;
    
    // Configuration
    private boolean enableAnimations = true;
    private boolean enableColorCodes = true;
    private long animationInterval = 1000; // 1 second default

    public static CustomBossbarManager getInstance() {
        return INSTANCE;
    }
    
    private CustomBossbarManager() {
        loadDefaultTemplates();
        startAnimationTask();
    }

    /**
     * Load default bossbar templates
     */
    private void loadDefaultTemplates() {
        // Default templates
        templates.put("welcome", new BossbarTemplate("welcome", 
            "&a&lWelcome to the server, {player}!", 
            BossBarColor.GREEN, BossBarOverlay.PROGRESS));
        
        templates.put("announcement", new BossbarTemplate("announcement", 
            "&e&lServer Announcement", 
            BossBarColor.YELLOW, BossBarOverlay.NOTCHED_10));
        
        templates.put("warning", new BossbarTemplate("warning", 
            "&c&lWarning: {message}", 
            BossBarColor.RED, BossBarOverlay.NOTCHED_6));
        
        DebugUtil.debugLog("[Bossbar] Loaded " + templates.size() + " default templates");
    }

    /**
     * Start the animation task for animated bossbars
     */
    private void startAnimationTask() {
        if (animationTask != null) {
            animationTask.cancel(false);
        }
        
        animationTask = scheduler.scheduleAtFixedRate(() -> {
            if (!enableAnimations) return;
            
            try {
                updateAnimatedBossbars();
            } catch (Exception e) {
                DebugUtil.warnLog("[Bossbar] Error in animation task: " + e.getMessage());
            }
        }, 0, animationInterval, TimeUnit.MILLISECONDS);
        
        DebugUtil.debugLog("[Bossbar] Started animation task with " + animationInterval + "ms interval");
    }

    /**
     * Show bossbar to player with enhanced features
     */
    public void showBossbar(ServerPlayer player, String template, int durationSeconds) {
        showBossbar(player, template, durationSeconds, null);
    }
    
    /**
     * Show bossbar to player with custom message placeholders
     */
    public void showBossbar(ServerPlayer player, String template, int durationSeconds, Map<String, String> placeholders) {
        TablistConfig config = ConfigManager.getInstance().getTablistConfig();
        if (!config.bossbar.enabled) {
            DebugUtil.debugLog("[Bossbar] Bossbar disabled in config");
            return;
        }
        
        BossbarTemplate tpl = templates.get(template);
        if (tpl == null) {
            DebugUtil.warnLog("[Bossbar] Template not found: " + template);
            return;
        }
        
        // Remove existing bossbar if present
        removeBossbar(player);
        
        // Process placeholders
        String text = tpl.text.replace("{player}", player.getName().getString());
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        // Apply formatting
        String formatted = getBossbarFormat().replace("{bossbar}", text);
        Component component = enableColorCodes ? ColorUtil.colorize(formatted) : Component.literal(formatted);
        
        // Create bossbar
        ServerBossEvent bossbar = new ServerBossEvent(component, tpl.color, tpl.overlay);
        bossbar.setVisible(true);
        bossbar.addPlayer(player);
        
        // Store active bossbar
        ActiveBossbar activeBossbar = new ActiveBossbar(bossbar, template, System.currentTimeMillis());
        activeBossbars.put(player.getUUID(), activeBossbar);
        
        // Schedule removal
        if (durationSeconds > 0) {
            ScheduledFuture<?> removal = scheduler.schedule(() -> {
                removeBossbar(player);
            }, durationSeconds, TimeUnit.SECONDS);
            scheduledRemovals.put(player.getUUID(), removal);
        }
        
        DebugUtil.debugLog("[Bossbar] Showed '" + template + "' to " + player.getName().getString() + 
                          " for " + durationSeconds + " seconds");
    }

    /**
     * Remove bossbar from player
     */
    public void removeBossbar(ServerPlayer player) {
        ActiveBossbar activeBossbar = activeBossbars.remove(player.getUUID());
        if (activeBossbar != null) {
            activeBossbar.bossbar.removePlayer(player);
            activeBossbar.bossbar.setVisible(false);
            
            // Cancel scheduled removal
            ScheduledFuture<?> removal = scheduledRemovals.remove(player.getUUID());
            if (removal != null) {
                removal.cancel(false);
            }
            
            DebugUtil.debugLog("[Bossbar] Removed bossbar from " + player.getName().getString());
        }
    }

    /**
     * Broadcast bossbar to all online players
     */
    public void broadcastBossbar(String template, int durationSeconds) {
        broadcastBossbar(template, durationSeconds, null);
    }
    
    /**
     * Broadcast bossbar to all online players with placeholders
     */
    public void broadcastBossbar(String template, int durationSeconds, Map<String, String> placeholders) {
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            showBossbar(player, template, durationSeconds, placeholders);
        }
        
        DebugUtil.debugLog("[Bossbar] Broadcasted '" + template + "' to " + 
                          server.getPlayerCount() + " players for " + durationSeconds + " seconds");
    }

    /**
     * Update bossbar text and progress for a player
     */
    public void updateBossbar(ServerPlayer player, String text, float progress) {
        ActiveBossbar activeBossbar = activeBossbars.get(player.getUUID());
        if (activeBossbar != null) {
            String formatted = getBossbarFormat().replace("{bossbar}", text);
            Component component = enableColorCodes ? ColorUtil.colorize(formatted) : Component.literal(formatted);
            
            activeBossbar.bossbar.setName(component);
            activeBossbar.bossbar.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
            
            DebugUtil.debugLog("[Bossbar] Updated bossbar for " + player.getName().getString() + 
                              " - progress: " + progress);
        }
    }
    
    /**
     * Create animated bossbar
     */
    public void createAnimatedBossbar(String id, List<String> frames, long intervalMs, BossBarColor color, BossBarOverlay overlay) {
        AnimatedBossbar animated = new AnimatedBossbar(frames, intervalMs, color, overlay);
        animatedBossbars.put(id, animated);
        DebugUtil.debugLog("[Bossbar] Created animated bossbar '" + id + "' with " + frames.size() + " frames");
    }
    
    /**
     * Show animated bossbar to player
     */
    public void showAnimatedBossbar(ServerPlayer player, String animatedId, int durationSeconds) {
        AnimatedBossbar animated = animatedBossbars.get(animatedId);
        if (animated == null) {
            DebugUtil.warnLog("[Bossbar] Animated bossbar not found: " + animatedId);
            return;
        }
        
        // Remove existing bossbar
        removeBossbar(player);
        
        // Create bossbar with first frame
        String currentFrame = animated.getCurrentFrame();
        Component component = enableColorCodes ? ColorUtil.colorize(currentFrame) : Component.literal(currentFrame);
        
        ServerBossEvent bossbar = new ServerBossEvent(component, animated.color, animated.overlay);
        bossbar.setVisible(true);
        bossbar.addPlayer(player);
        
        // Store as active
        ActiveBossbar activeBossbar = new ActiveBossbar(bossbar, animatedId, System.currentTimeMillis());
        activeBossbar.isAnimated = true;
        activeBossbars.put(player.getUUID(), activeBossbar);
        
        // Schedule removal
        if (durationSeconds > 0) {
            ScheduledFuture<?> removal = scheduler.schedule(() -> {
                removeBossbar(player);
            }, durationSeconds, TimeUnit.SECONDS);
            scheduledRemovals.put(player.getUUID(), removal);
        }
        
        DebugUtil.debugLog("[Bossbar] Showed animated bossbar '" + animatedId + "' to " + 
                          player.getName().getString() + " for " + durationSeconds + " seconds");
    }
    
    /**
     * Update all animated bossbars
     */
    private void updateAnimatedBossbars() {
        long now = System.currentTimeMillis();
        
        // Update animation frames
        for (AnimatedBossbar animated : animatedBossbars.values()) {
            animated.tick(now);
        }
        
        // Update active animated bossbars
        for (Map.Entry<UUID, ActiveBossbar> entry : activeBossbars.entrySet()) {
            ActiveBossbar activeBossbar = entry.getValue();
            if (activeBossbar.isAnimated) {
                AnimatedBossbar animated = animatedBossbars.get(activeBossbar.templateId);
                if (animated != null) {
                    String currentFrame = animated.getCurrentFrame();
                    Component component = enableColorCodes ? ColorUtil.colorize(currentFrame) : Component.literal(currentFrame);
                    activeBossbar.bossbar.setName(component);
                }
            }
        }
    }

    /**
     * Configuration methods
     */
    public void setAnimationInterval(long intervalMs) {
        this.animationInterval = Math.max(100, intervalMs); // Min 100ms
        startAnimationTask(); // Restart with new interval
        DebugUtil.debugLog("[Bossbar] Set animation interval to " + intervalMs + "ms");
    }
    
    public void setEnableAnimations(boolean enable) {
        this.enableAnimations = enable;
        DebugUtil.debugLog("[Bossbar] Animations " + (enable ? "enabled" : "disabled"));
    }
    
    public void setEnableColorCodes(boolean enable) {
        this.enableColorCodes = enable;
        DebugUtil.debugLog("[Bossbar] Color codes " + (enable ? "enabled" : "disabled"));
    }

    public void reloadAnimations() {
        // Reload bossbar animations/templates from config
        // Clear existing templates and reload from files
        templates.clear();
        animatedBossbars.clear();
        
        // Load from configuration files here
        DebugUtil.debugLog("[Bossbar] Reloaded animations and templates");
    }

    public String getAnimationStats() {
        return String.format("Templates: %d, Active: %d, Animated: %d, Scheduled removals: %d", 
                            templates.size(), activeBossbars.size(), animatedBossbars.size(), scheduledRemovals.size());
    }

    public List<String> getAvailableAnimations() {
        return new ArrayList<>(animatedBossbars.keySet());
    }
    
    /**
     * Get available template names for command completions
     */
    public List<String> getTemplateNames() {
        return new ArrayList<>(templates.keySet());
    }

    public void shutdown() {
        // Cleanup all bossbars on shutdown
        for (ActiveBossbar activeBossbar : activeBossbars.values()) {
            activeBossbar.bossbar.setVisible(false);
            activeBossbar.bossbar.getPlayers().forEach(activeBossbar.bossbar::removePlayer);
        }
        activeBossbars.clear();
        
        // Cancel all scheduled removals
        for (ScheduledFuture<?> removal : scheduledRemovals.values()) {
            removal.cancel(false);
        }
        scheduledRemovals.clear();
        
        // Shutdown scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        DebugUtil.debugLog("[Bossbar] Shutdown complete");
    }
    
    /**
     * Get bossbar format from the new config structure
     * Falls back to default format if configuration is missing or invalid
     */
    private String getBossbarFormat() {
        TablistConfig config = ConfigManager.getInstance().getTablistConfig();
        
        // Try to get format from new bossbar structure
        if (config != null && config.bossbar != null && 
            config.bossbar.layouts != null && !config.bossbar.layouts.isEmpty()) {
            
            TablistConfig.BossbarLayout layout = config.bossbar.layouts.get(0);
            if (layout.bars != null && !layout.bars.isEmpty()) {
                TablistConfig.BossbarInfo bar = layout.bars.get(0);
                if (bar.text != null && !bar.text.isEmpty()) {
                    return bar.text;
                }
            }
        }
        
        // Fallback to default format
        return "Boss: {bossbar} | {message} [{progress}%]";
    }
    
    /**
     * Inner class for bossbar template
     */
    public static class BossbarTemplate {
        public String name;
        public String text;
        public BossBarColor color;
        public BossBarOverlay overlay;
        public float progress;

        public BossbarTemplate(String name, String text, BossBarColor color, BossBarOverlay overlay) {
            this.name = name;
            this.text = text;
            this.color = color;
            this.overlay = overlay;
            this.progress = 1.0f;
        }
    }
    
    /**
     * Inner class to hold active bossbar data
     */
    public static class ActiveBossbar {
        public final ServerBossEvent bossbar;
        public final String templateId;
        public final long startTime;
        public boolean isAnimated = false;
        
        public ActiveBossbar(ServerBossEvent bossbar, String templateId, long startTime) {
            this.bossbar = bossbar;
            this.templateId = templateId;
            this.startTime = startTime;
        }
    }
    
    /**
     * Inner class for animated bossbars
     */
    public static class AnimatedBossbar {
        public final List<String> frames;
        public final long intervalMs;
        public final BossBarColor color;
        public final BossBarOverlay overlay;
        private int currentFrame = 0;
        private long lastUpdate = 0;
        
        public AnimatedBossbar(List<String> frames, long intervalMs, BossBarColor color, BossBarOverlay overlay) {
            this.frames = new ArrayList<>(frames);
            this.intervalMs = intervalMs;
            this.color = color;
            this.overlay = overlay;
        }
        
        public String getCurrentFrame() {
            if (frames.isEmpty()) return "";
            return frames.get(currentFrame);
        }
        
        public void tick(long now) {
            if (now - lastUpdate >= intervalMs) {
                currentFrame = (currentFrame + 1) % frames.size();
                lastUpdate = now;
            }
        }
    }
}
