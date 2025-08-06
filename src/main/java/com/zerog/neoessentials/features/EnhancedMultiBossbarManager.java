package com.zerog.neoessentials.features;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Multi-Bossbar Manager for NeoEssentials
 * Supports multiple simultaneous bossbars per player with advanced animations and triggers
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class EnhancedMultiBossbarManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedMultiBossbarManager.class);
    private static EnhancedMultiBossbarManager instance;
    
    // Player bossbar management - each player can have multiple active bossbars
    private final Map<UUID, Map<String, EnhancedBossbar>> playerBossbars = new ConcurrentHashMap<>();
    private final Map<String, BossbarTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, BossbarAnimation> animations = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    
    // Update management
    private final Timer updateTimer = new Timer("EnhancedBossbarUpdater", true);
    private boolean updateTaskStarted = false;
    private int animationTick = 0;
    
    private EnhancedMultiBossbarManager() {
        initializeDefaultTemplates();
        initializeDefaultAnimations();
    }
    
    public static EnhancedMultiBossbarManager getInstance() {
        if (instance == null) {
            instance = new EnhancedMultiBossbarManager();
        }
        return instance;
    }
    
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        startUpdateTask();
        LOGGER.info("Enhanced Multi-Bossbar Manager initialized with {} templates and {} animations", 
            templates.size(), animations.size());
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerBossbars.put(player.getUUID(), new ConcurrentHashMap<>());
            loadPlayerPermissions(player);
            
            // Show welcome bossbar if enabled
            showBossbar(player, "welcome", "auto", 10);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            hideAllBossbars(player);
            playerBossbars.remove(player.getUUID());
            playerPermissions.remove(player.getUUID());
        }
    }
    
    /**
     * Initialize default bossbar templates
     */
    private void initializeDefaultTemplates() {
        // Welcome bossbar
        templates.put("welcome", new BossbarTemplate(
            "welcome",
            "§6§lWelcome to the Server, {player}!",
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false,
            BossbarTrigger.MANUAL,
            Arrays.asList("neoessentials.bossbar.welcome")
        ));
        
        // Health monitor
        templates.put("health", new BossbarTemplate(
            "health",
            "§c❤ Health: {health}/{max_health}",
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false,
            BossbarTrigger.HEALTH_LOW,
            Arrays.asList("neoessentials.bossbar.health")
        ));
        
        // XP Progress
        templates.put("xp_progress", new BossbarTemplate(
            "xp_progress",
            "§a⚡ Level {level} - {exp}% to next level",
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.NOTCHED_10,
            0.0f,
            false,
            false,
            BossbarTrigger.XP_GAIN,
            Arrays.asList("neoessentials.bossbar.xp")
        ));
        
        // Server announcements
        templates.put("announcement", new BossbarTemplate(
            "announcement",
            "§d📢 {message}",
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            true,
            BossbarTrigger.MANUAL,
            Arrays.asList("neoessentials.bossbar.announcements")
        ));
        
        // Event countdown
        templates.put("countdown", new BossbarTemplate(
            "countdown",
            "§e⏰ {event_name} starts in {countdown}",
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_20,
            1.0f,
            false,
            false,
            BossbarTrigger.SCHEDULED,
            Arrays.asList("neoessentials.bossbar.events")
        ));
        
        // Economy notifications
        templates.put("money_gained", new BossbarTemplate(
            "money_gained",
            "§a💰 +${amount} | Balance: ${balance}",
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false,
            BossbarTrigger.MONEY_CHANGE,
            Arrays.asList("neoessentials.bossbar.economy")
        ));
        
        // PvP status
        templates.put("pvp_combat", new BossbarTemplate(
            "pvp_combat",
            "§c⚔ Combat Mode - {combat_time} seconds",
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_6,
            1.0f,
            true,
            false,
            BossbarTrigger.COMBAT_ENTER,
            Arrays.asList("neoessentials.bossbar.pvp")
        ));
        
        // Admin tools
        templates.put("admin_tools", new BossbarTemplate(
            "admin_tools",
            "§4🔧 Admin Mode | TPS: {tps} | Players: {online}",
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false,
            BossbarTrigger.PERMISSION,
            Arrays.asList("neoessentials.admin", "neoessentials.bossbar.admin")
        ));
    }
    
    /**
     * Initialize default animations
     */
    private void initializeDefaultAnimations() {
        // Rainbow color animation
        animations.put("rainbow", new BossbarAnimation(
            "rainbow",
            AnimationType.COLOR_CYCLE,
            Arrays.asList(
                BossEvent.BossBarColor.RED,
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarColor.PINK
            ),
            20 // 1 second per color
        ));
        
        // Progress pulse animation
        animations.put("pulse", new BossbarAnimation(
            "pulse",
            AnimationType.PROGRESS_PULSE,
            Arrays.asList(0.2f, 0.4f, 0.6f, 0.8f, 1.0f, 0.8f, 0.6f, 0.4f),
            10 // 0.5 seconds per step
        ));
        
        // Text typewriter effect
        animations.put("typewriter", new BossbarAnimation(
            "typewriter",
            AnimationType.TEXT_TYPEWRITER,
            Arrays.asList("Loading", "Loading.", "Loading..", "Loading..."),
            15 // 0.75 seconds per step
        ));
        
        // Emergency flash
        animations.put("emergency", new BossbarAnimation(
            "emergency",
            AnimationType.COLOR_FLASH,
            Arrays.asList(BossEvent.BossBarColor.RED, BossEvent.BossBarColor.WHITE),
            5 // 0.25 seconds per flash
        ));
    }
    
    /**
     * Load player permissions
     */
    private void loadPlayerPermissions(ServerPlayer player) {
        Set<String> permissions = new HashSet<>();
        
        // Add permissions based on player's permission level
        if (player.hasPermissions(4)) { // OP level 4
            permissions.add("neoessentials.admin");
            permissions.add("neoessentials.bossbar.admin");
        }
        if (player.hasPermissions(2)) { // OP level 2+
            permissions.add("neoessentials.bossbar.welcome");
            permissions.add("neoessentials.bossbar.health");
            permissions.add("neoessentials.bossbar.xp");
            permissions.add("neoessentials.bossbar.announcements");
            permissions.add("neoessentials.bossbar.events");
            permissions.add("neoessentials.bossbar.economy");
            permissions.add("neoessentials.bossbar.pvp");
        }
        
        playerPermissions.put(player.getUUID(), permissions);
    }
    
    /**
     * Show bossbar to player
     */
    public void showBossbar(ServerPlayer player, String templateName, String animationName, int durationSeconds) {
        BossbarTemplate template = templates.get(templateName);
        if (template == null) {
            LOGGER.warn("Unknown bossbar template: {}", templateName);
            return;
        }
        
        // Check permissions
        if (!hasPermissionForTemplate(player, template)) {
            LOGGER.debug("Player {} lacks permission for bossbar template {}", 
                player.getDisplayName().getString(), templateName);
            return;
        }
        
        try {
            UUID bossbarId = UUID.randomUUID();
            String processedText = processPlaceholders(template.text, player);
            
            EnhancedBossbar bossbar = new EnhancedBossbar(
                bossbarId,
                Component.literal(processedText),
                template.color,
                template.overlay,
                template.progress,
                template.darkenScreen,
                template.playBossMusic,
                System.currentTimeMillis() + (durationSeconds * 1000L),
                template,
                animationName,
                0
            );
            
            // Store bossbar
            Map<String, EnhancedBossbar> playerBars = playerBossbars.get(player.getUUID());
            if (playerBars != null) {
                playerBars.put(templateName, bossbar);
                sendBossbarPacket(player, bossbar, "ADD");
                
                LOGGER.debug("Showing bossbar '{}' to player {} for {} seconds with animation '{}'", 
                    templateName, player.getDisplayName().getString(), durationSeconds, animationName);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to show bossbar to player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Update specific bossbar
     */
    public void updateBossbar(ServerPlayer player, String templateName, String newText, float newProgress) {
        Map<String, EnhancedBossbar> playerBars = playerBossbars.get(player.getUUID());
        if (playerBars == null) return;
        
        EnhancedBossbar bossbar = playerBars.get(templateName);
        if (bossbar == null) return;
        
        try {
            bossbar.setName(Component.literal(processPlaceholders(newText, player)));
            bossbar.setProgress(Math.max(0.0f, Math.min(1.0f, newProgress)));
            
            sendBossbarPacket(player, bossbar, "UPDATE_NAME");
            sendBossbarPacket(player, bossbar, "UPDATE_PROGRESS");
            
        } catch (Exception e) {
            LOGGER.error("Failed to update bossbar for player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Hide specific bossbar
     */
    public void hideBossbar(ServerPlayer player, String templateName) {
        Map<String, EnhancedBossbar> playerBars = playerBossbars.get(player.getUUID());
        if (playerBars == null) return;
        
        EnhancedBossbar bossbar = playerBars.remove(templateName);
        if (bossbar == null) return;
        
        try {
            sendBossbarPacket(player, bossbar, "REMOVE");
            LOGGER.debug("Removed bossbar '{}' from player: {}", templateName, player.getDisplayName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to remove bossbar from player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Hide all bossbars for player
     */
    public void hideAllBossbars(ServerPlayer player) {
        Map<String, EnhancedBossbar> playerBars = playerBossbars.get(player.getUUID());
        if (playerBars == null) return;
        
        for (String templateName : new HashSet<>(playerBars.keySet())) {
            hideBossbar(player, templateName);
        }
    }
    
    /**
     * Broadcast bossbar to all players
     */
    public void broadcastBossbar(String templateName, String animationName, int durationSeconds) {
        BossbarTemplate template = templates.get(templateName);
        if (template == null) {
            LOGGER.warn("Unknown bossbar template for broadcast: {}", templateName);
            return;
        }
        
        // Would need server reference - placeholder for now
        LOGGER.info("Broadcasting bossbar '{}' with animation '{}' for {} seconds", 
            templateName, animationName, durationSeconds);
    }
    
    /**
     * Trigger automatic bossbars based on conditions
     */
    public void triggerAutomaticBossbars(ServerPlayer player, BossbarTrigger trigger, Map<String, Object> context) {
        for (BossbarTemplate template : templates.values()) {
            if (template.trigger == trigger && hasPermissionForTemplate(player, template)) {
                
                // Apply trigger-specific logic
                switch (trigger) {
                    case HEALTH_LOW:
                        Float health = (Float) context.get("health");
                        Float maxHealth = (Float) context.get("maxHealth");
                        if (health != null && maxHealth != null && health < (maxHealth * 0.3f)) {
                            showBossbar(player, template.name, "emergency", 5);
                        }
                        break;
                        
                    case XP_GAIN:
                        showBossbar(player, template.name, "pulse", 3);
                        break;
                        
                    case MONEY_CHANGE:
                        showBossbar(player, template.name, "auto", 4);
                        break;
                        
                    case COMBAT_ENTER:
                        showBossbar(player, template.name, "emergency", 15);
                        break;
                        
                    default:
                        break;
                }
            }
        }
    }
    
    /**
     * Check if player has permission for template
     */
    private boolean hasPermissionForTemplate(ServerPlayer player, BossbarTemplate template) {
        Set<String> playerPerms = playerPermissions.get(player.getUUID());
        if (playerPerms == null) return false;
        
        for (String requiredPerm : template.requiredPermissions) {
            if (!playerPerms.contains(requiredPerm)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Process placeholders in text
     */
    private String processPlaceholders(String text, ServerPlayer player) {
        if (text == null) return "";
        
        return text
            .replace("{player}", player.getDisplayName().getString())
            .replace("{health}", String.valueOf((int)player.getHealth()))
            .replace("{max_health}", String.valueOf((int)player.getMaxHealth()))
            .replace("{level}", String.valueOf(player.experienceLevel))
            .replace("{exp}", String.valueOf((int)(player.experienceProgress * 100)))
            .replace("{balance}", "$1000") // Placeholder
            .replace("{amount}", "$50") // Placeholder
            .replace("{tps}", "20.0") // Placeholder
            .replace("{online}", "5") // Placeholder
            .replace("{message}", "Server announcement!") // Placeholder
            .replace("{event_name}", "Build Contest") // Placeholder
            .replace("{countdown}", "5:00") // Placeholder
            .replace("{combat_time}", "15") // Placeholder
            .replace("&", "§");
    }
    
    /**
     * Send bossbar packet to player
     */
    private void sendBossbarPacket(ServerPlayer player, EnhancedBossbar bossbar, String operation) {
        try {
            ClientboundBossEventPacket packet = switch (operation) {
                case "ADD" -> ClientboundBossEventPacket.createAddPacket(bossbar);
                case "REMOVE" -> ClientboundBossEventPacket.createRemovePacket(bossbar.getId());
                case "UPDATE_NAME" -> ClientboundBossEventPacket.createUpdateNamePacket(bossbar);
                case "UPDATE_PROGRESS" -> ClientboundBossEventPacket.createUpdateProgressPacket(bossbar);
                case "UPDATE_STYLE" -> ClientboundBossEventPacket.createUpdateStylePacket(bossbar);
                case "UPDATE_PROPERTIES" -> ClientboundBossEventPacket.createUpdatePropertiesPacket(bossbar);
                default -> ClientboundBossEventPacket.createAddPacket(bossbar);
            };
            
            player.connection.send(packet);
        } catch (Exception e) {
            LOGGER.error("Failed to send bossbar packet", e);
        }
    }
    
    /**
     * Start the update task for animations and expiration
     */
    private void startUpdateTask() {
        if (updateTaskStarted) return;
        
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                animationTick++;
                updateAllBossbars();
            }
        }, 50, 50); // Update every 50ms (20 TPS)
        
        updateTaskStarted = true;
        LOGGER.debug("Enhanced bossbar update task started");
    }
    
    /**
     * Update all active bossbars
     */
    private void updateAllBossbars() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Map<String, EnhancedBossbar>> playerEntry : playerBossbars.entrySet()) {
            Map<String, EnhancedBossbar> playerBars = playerEntry.getValue();
            
            // Remove expired bossbars
            playerBars.entrySet().removeIf(entry -> {
                EnhancedBossbar bossbar = entry.getValue();
                return currentTime >= bossbar.getExpireTime();
            });
            
            // Update animations for remaining bossbars
            for (EnhancedBossbar bossbar : playerBars.values()) {
                updateBossbarAnimation(bossbar);
            }
        }
    }
    
    /**
     * Update bossbar animation
     */
    private void updateBossbarAnimation(EnhancedBossbar bossbar) {
        if (bossbar.animationName == null || "auto".equals(bossbar.animationName)) return;
        
        BossbarAnimation animation = animations.get(bossbar.animationName);
        if (animation == null) return;
        
        if ((animationTick - bossbar.animationStartTick) % animation.frameDelay == 0) {
            int frameIndex = ((animationTick - bossbar.animationStartTick) / animation.frameDelay) % animation.frames.size();
            
            switch (animation.type) {
                case COLOR_CYCLE:
                case COLOR_FLASH:
                    if (animation.frames.get(frameIndex) instanceof BossEvent.BossBarColor color) {
                        bossbar.setColor(color);
                    }
                    break;
                    
                case PROGRESS_PULSE:
                    if (animation.frames.get(frameIndex) instanceof Float progress) {
                        bossbar.setProgress(progress);
                    }
                    break;
                    
                case TEXT_TYPEWRITER:
                    if (animation.frames.get(frameIndex) instanceof String text) {
                        bossbar.setName(Component.literal(text));
                    }
                    break;
                    
                case OVERLAY_CHANGE:
                    if (animation.frames.get(frameIndex) instanceof BossEvent.BossBarOverlay overlay) {
                        bossbar.setOverlay(overlay);
                    }
                    break;
            }
        }
    }
    
    /**
     * Get available templates
     */
    public Set<String> getAvailableTemplates() {
        return templates.keySet();
    }
    
    /**
     * Get available animations
     */
    public Set<String> getAvailableAnimations() {
        return animations.keySet();
    }
    
    /**
     * Shutdown
     */
    public void shutdown() {
        updateTimer.cancel();
        playerBossbars.clear();
        playerPermissions.clear();
        LOGGER.info("Enhanced Multi-Bossbar Manager shutdown");
    }
    
    /**
     * Enhanced bossbar implementation
     */
    public static class EnhancedBossbar extends BossEvent {
        private final long expireTime;
        private final BossbarTemplate template;
        private final String animationName;
        private final int animationStartTick;
        
        public EnhancedBossbar(UUID id, Component name, BossBarColor color, BossBarOverlay overlay, 
                              float progress, boolean darkenScreen, boolean playBossMusic, long expireTime,
                              BossbarTemplate template, String animationName, int animationStartTick) {
            super(id, name, color, overlay);
            this.setProgress(progress);
            this.setDarkenScreen(darkenScreen);
            this.setPlayBossMusic(playBossMusic);
            this.expireTime = expireTime;
            this.template = template;
            this.animationName = animationName;
            this.animationStartTick = animationStartTick;
        }
        
        public long getExpireTime() { return expireTime; }
        public BossbarTemplate getTemplate() { return template; }
        public String getAnimationName() { return animationName; }
        public int getAnimationStartTick() { return animationStartTick; }
    }
    
    /**
     * Bossbar template
     */
    public static class BossbarTemplate {
        public final String name;
        public final String text;
        public final BossEvent.BossBarColor color;
        public final BossEvent.BossBarOverlay overlay;
        public final float progress;
        public final boolean darkenScreen;
        public final boolean playBossMusic;
        public final BossbarTrigger trigger;
        public final List<String> requiredPermissions;
        
        public BossbarTemplate(String name, String text, BossEvent.BossBarColor color, 
                              BossEvent.BossBarOverlay overlay, float progress, 
                              boolean darkenScreen, boolean playBossMusic, BossbarTrigger trigger,
                              List<String> requiredPermissions) {
            this.name = name;
            this.text = text;
            this.color = color;
            this.overlay = overlay;
            this.progress = progress;
            this.darkenScreen = darkenScreen;
            this.playBossMusic = playBossMusic;
            this.trigger = trigger;
            this.requiredPermissions = new ArrayList<>(requiredPermissions);
        }
    }
    
    /**
     * Bossbar animation
     */
    public static class BossbarAnimation {
        public final String name;
        public final AnimationType type;
        public final List<Object> frames;
        public final int frameDelay; // ticks between frames
        
        public BossbarAnimation(String name, AnimationType type, List<Object> frames, int frameDelay) {
            this.name = name;
            this.type = type;
            this.frames = new ArrayList<>(frames);
            this.frameDelay = frameDelay;
        }
    }
    
    /**
     * Bossbar trigger types
     */
    public enum BossbarTrigger {
        MANUAL,
        HEALTH_LOW,
        XP_GAIN,
        MONEY_CHANGE,
        COMBAT_ENTER,
        COMBAT_EXIT,
        PERMISSION,
        SCHEDULED,
        WORLD_CHANGE,
        ACHIEVEMENT
    }
    
    /**
     * Animation types
     */
    public enum AnimationType {
        COLOR_CYCLE,
        COLOR_FLASH,
        PROGRESS_PULSE,
        TEXT_TYPEWRITER,
        OVERLAY_CHANGE
    }
}
