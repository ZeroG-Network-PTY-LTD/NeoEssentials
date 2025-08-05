package com.zerog.neoessentials.features;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Bossbar Manager for NeoEssentials
 * Provides dynamic bossbar management with customizable messages and styles
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class CustomBossbarManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomBossbarManager.class);
    private static CustomBossbarManager instance;
    
    private final Map<UUID, List<CustomBossbar>> activeBossbars = new ConcurrentHashMap<>();
    private final Map<String, BossbarTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, BossbarTheme> themes = new ConcurrentHashMap<>();
    private final Timer updateTimer = new Timer("BossbarUpdater", true);
    private int animationFrame = 0;
    
    private CustomBossbarManager() {
        NeoForge.EVENT_BUS.register(this);
        initializeDefaultTemplates();
        initializeDefaultThemes();
    }
    
    public static CustomBossbarManager getInstance() {
        if (instance == null) {
            instance = new CustomBossbarManager();
        }
        return instance;
    }
    
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        startUpdateTask();
        LOGGER.info("Custom Bossbar Manager initialized");
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Show welcome bossbar
            showBossbar(player, "welcome", 10);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            removeBossbar(player);
        }
    }
    
    /**
     * Initialize default bossbar templates
     */
    private void initializeDefaultTemplates() {
        // Welcome bossbar
        templates.put("welcome", new BossbarTemplate(
            "§6§lWelcome to the Server!",
            "§7Enjoy your stay and have fun!",
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false
        ));
        
        // Server info bossbar
        templates.put("serverinfo", new BossbarTemplate(
            "§b§lServer Information",
            "§fOnline: §a{online}§f/§a{max} §7| §fTPS: §a{tps}",
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10,
            0.8f,
            false,
            false
        ));
        
        // Event announcement bossbar
        templates.put("event", new BossbarTemplate(
            "§d§lEvent Announcement",
            "§fCheck out the latest server events!",
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_6,
            1.0f,
            false,
            false
        ));
        
        // Warning bossbar
        templates.put("warning", new BossbarTemplate(
            "§c§lWarning",
            "§fPlease read the server rules!",
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            true,
            false
        ));
        
        // Progress bossbar
        templates.put("progress", new BossbarTemplate(
            "§e§lProgress",
            "§fTask in progress...",
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_20,
            0.5f,
            false,
            true
        ));
        
        // Health bossbar
        templates.put("health", new BossbarTemplate(
            "§c§lHealth Status",
            "§fHealth: §a{health}§f/§a{maxhealth}",
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false
        ));
        
        // Animated welcome bossbar
        templates.put("animated_welcome", new BossbarTemplate(
            "§6§l{animated_title}",
            "§7{animated_subtitle}",
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS,
            1.0f,
            false,
            false
        ));
        
        LOGGER.info("Initialized {} default bossbar templates", templates.size());
    }
    
    /**
     * Initialize default bossbar themes
     */
    private void initializeDefaultThemes() {
        // Default theme
        themes.put("default", new BossbarTheme(
            "§f§l{title}",
            "§7{subtitle}",
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
        ));
        
        // Modern theme
        themes.put("modern", new BossbarTheme(
            "§b§l► {title} §b§l◄",
            "§f{subtitle}",
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10
        ));
        
        // Classic theme
        themes.put("classic", new BossbarTheme(
            "§6§l═══ {title} ═══",
            "§e{subtitle}",
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_6
        ));
        
        // Minimalist theme
        themes.put("minimalist", new BossbarTheme(
            "§f{title}",
            "§8{subtitle}",
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
        ));
        
        LOGGER.info("Initialized {} default bossbar themes", themes.size());
    }
    
    /**
     * Show a bossbar to a player
     */
    public void showBossbar(ServerPlayer player, String templateName, int durationSeconds) {
        BossbarTemplate template = templates.get(templateName);
        if (template == null) {
            LOGGER.warn("Unknown bossbar template: {}", templateName);
            return;
        }
        
        showBossbar(player, template, durationSeconds);
    }
    
    /**
     * Show a custom bossbar to a player
     */
    public void showBossbar(ServerPlayer player, BossbarTemplate template, int durationSeconds) {
        try {
            UUID bossbarId = UUID.randomUUID();
            String processedText = processPlaceholders(template.getText(), player);
            
            CustomBossbar bossbar = new CustomBossbar(
                bossbarId,
                Component.literal(processedText),
                template.getColor(),
                template.getOverlay(),
                template.getProgress(),
                template.isDarkenScreen(),
                template.isPlayBossMusic(),
                System.currentTimeMillis() + (durationSeconds * 1000L)
            );
            
            // Remove existing bossbar if any
            removeBossbar(player);
            
            // Store and send new bossbar
            activeBossbars.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(bossbar);
            sendBossbarPacket(player, bossbar, "ADD");
            
            LOGGER.debug("Showing bossbar '{}' to player {} for {} seconds", 
                template.getTitle(), player.getDisplayName().getString(), durationSeconds);
            
        } catch (Exception e) {
            LOGGER.error("Failed to show bossbar to player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Update bossbar for a player
     */
    public void updateBossbar(ServerPlayer player, String newText, float newProgress) {
        List<CustomBossbar> bossbars = activeBossbars.get(player.getUUID());
        if (bossbars == null || bossbars.isEmpty()) return;
        
        try {
            // Update the most recent bossbar
            CustomBossbar bossbar = bossbars.get(bossbars.size() - 1);
            bossbar.setName(Component.literal(processPlaceholders(newText, player)));
            bossbar.setProgress(Math.max(0.0f, Math.min(1.0f, newProgress)));
            
            sendBossbarPacket(player, bossbar, "UPDATE_NAME");
            sendBossbarPacket(player, bossbar, "UPDATE_PROGRESS");
            
        } catch (Exception e) {
            LOGGER.error("Failed to update bossbar for player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Remove bossbar from a player
     */
    public void removeBossbar(ServerPlayer player) {
        List<CustomBossbar> bossbars = activeBossbars.remove(player.getUUID());
        if (bossbars == null || bossbars.isEmpty()) return;
        
        try {
            for (CustomBossbar bossbar : bossbars) {
                sendBossbarPacket(player, bossbar, "REMOVE");
            }
            LOGGER.debug("Removed {} bossbars from player: {}", bossbars.size(), player.getDisplayName().getString());
        } catch (Exception e) {
            LOGGER.error("Failed to remove bossbar from player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Broadcast bossbar to all players
     */
    public void broadcastBossbar(String templateName, int durationSeconds) {
        BossbarTemplate template = templates.get(templateName);
        if (template == null) {
            LOGGER.warn("Unknown bossbar template: {}", templateName);
            return;
        }
        
        // This would need server reference to get all players
        // For now, we'll log the intent
        LOGGER.info("Broadcasting bossbar '{}' for {} seconds", templateName, durationSeconds);
    }
    
    /**
     * Add or update a bossbar template
     */
    public void addTemplate(String name, BossbarTemplate template) {
        templates.put(name, template);
        LOGGER.info("Added/updated bossbar template: {}", name);
    }
    
    /**
     * Remove a bossbar template
     */
    public void removeTemplate(String name) {
        if (templates.remove(name) != null) {
            LOGGER.info("Removed bossbar template: {}", name);
        }
    }
    
    /**
     * Get all template names
     */
    public Set<String> getTemplateNames() {
        return new HashSet<>(templates.keySet());
    }
    
    /**
     * Show bossbar with theme
     */
    public void showBossbarWithTheme(ServerPlayer player, String templateName, String themeName, int durationSeconds) {
        BossbarTemplate template = templates.get(templateName);
        BossbarTheme theme = themes.get(themeName);
        
        if (template == null) {
            LOGGER.warn("Unknown bossbar template: {}", templateName);
            return;
        }
        
        if (theme == null) {
            LOGGER.warn("Unknown bossbar theme: {}, using default", themeName);
            theme = themes.get("default");
        }
        
        showBossbarWithTheme(player, template, theme, durationSeconds);
    }
    
    /**
     * Show custom bossbar with theme
     */
    public void showBossbarWithTheme(ServerPlayer player, BossbarTemplate template, BossbarTheme theme, int durationSeconds) {
        try {
            UUID bossbarId = UUID.randomUUID();
            
            // Apply theme formatting to template text
            String themedTitle = theme.formatTitle(template.getTitle());
            String themedSubtitle = theme.formatSubtitle(template.getText());
            String processedText = processPlaceholders(themedTitle + " " + themedSubtitle, player);
            
            CustomBossbar bossbar = new CustomBossbar(
                bossbarId,
                Component.literal(processedText),
                theme.getColor(),
                theme.getOverlay(),
                template.getProgress(),
                template.isDarkenScreen(),
                template.isPlayBossMusic(),
                System.currentTimeMillis() + (durationSeconds * 1000L)
            );
            
            // Add to player's bossbar list
            activeBossbars.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(bossbar);
            sendBossbarPacket(player, bossbar, "ADD");
            
            LOGGER.debug("Showing themed bossbar '{}' with theme '{}' to player {} for {} seconds", 
                template.getTitle(), theme.getName(), player.getDisplayName().getString(), durationSeconds);
            
        } catch (Exception e) {
            LOGGER.error("Failed to show themed bossbar to player: " + player.getDisplayName().getString(), e);
        }
    }
    
    /**
     * Add or update a bossbar theme
     */
    public void addTheme(String name, BossbarTheme theme) {
        themes.put(name, theme);
        LOGGER.info("Added/updated bossbar theme: {}", name);
    }
    
    /**
     * Get all theme names
     */
    public Set<String> getThemeNames() {
        return new HashSet<>(themes.keySet());
    }
    
    /**
     * Process placeholders in text
     */
    private String processPlaceholders(String text, ServerPlayer player) {
        if (text == null) return "";
        
        // Enhanced placeholder processing with animations
        String processed = text
            .replace("{player}", player.getDisplayName().getString())
            .replace("{online}", "1") // Placeholder - would need server reference
            .replace("{max}", "20")   // Placeholder - would need server reference
            .replace("{tps}", "20.0") // Placeholder - would need TPS calculation
            .replace("{world}", player.level().dimension().location().getPath())
            .replace("{health}", String.valueOf((int)player.getHealth()))
            .replace("{maxhealth}", String.valueOf((int)player.getMaxHealth()))
            .replace("{animated_title}", getAnimatedTitle())
            .replace("{animated_subtitle}", getAnimatedSubtitle());
        
        return processed;
    }
    
    /**
     * Get animated title based on current frame
     */
    private String getAnimatedTitle() {
        String[] titles = {
            "Welcome to the Server!",
            "§6Welcome to the Server!",
            "§e§lWelcome to the Server!",
            "§6§lWelcome to the Server!",
            "Welcome to the Server!"
        };
        return titles[animationFrame % titles.length];
    }
    
    /**
     * Get animated subtitle based on current frame
     */
    private String getAnimatedSubtitle() {
        String[] subtitles = {
            "Enjoy your stay!",
            "Have fun and follow the rules!",
            "Welcome aboard, adventurer!",
            "Ready for an epic journey?",
            "Let the adventure begin!"
        };
        return subtitles[animationFrame % subtitles.length];
    }
    
    /**
     * Send bossbar packet to player
     */
    private void sendBossbarPacket(ServerPlayer player, CustomBossbar bossbar, String operation) {
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
     * Start the update task for managing bossbar durations
     */
    private void startUpdateTask() {
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                animationFrame = (animationFrame + 1) % 20; // 20-frame animation cycle
                
                activeBossbars.entrySet().removeIf(entry -> {
                    List<CustomBossbar> bossbars = entry.getValue();
                    bossbars.removeIf(bossbar -> currentTime >= bossbar.getExpireTime());
                    return bossbars.isEmpty();
                });
            }
        }, 1000, 1000); // Check every second
    }
    
    /**
     * Shutdown the bossbar manager
     */
    public void shutdown() {
        updateTimer.cancel();
        activeBossbars.clear();
        LOGGER.info("Custom Bossbar Manager shutdown");
    }
    
    /**
     * Custom bossbar implementation
     */
    public static class CustomBossbar extends BossEvent {
        private final long expireTime;
        
        public CustomBossbar(UUID id, Component name, BossBarColor color, BossBarOverlay overlay, 
                           float progress, boolean darkenScreen, boolean playBossMusic, long expireTime) {
            super(id, name, color, overlay);
            this.setProgress(progress);
            this.setDarkenScreen(darkenScreen);
            this.setPlayBossMusic(playBossMusic);
            this.expireTime = expireTime;
        }
        
        public long getExpireTime() {
            return expireTime;
        }
    }
    
    /**
     * Bossbar template for reusable configurations
     */
    public static class BossbarTemplate {
        private final String title;
        private final String text;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        private final float progress;
        private final boolean darkenScreen;
        private final boolean playBossMusic;
        
        public BossbarTemplate(String title, String text, BossEvent.BossBarColor color, 
                             BossEvent.BossBarOverlay overlay, float progress, 
                             boolean darkenScreen, boolean playBossMusic) {
            this.title = title;
            this.text = text;
            this.color = color;
            this.overlay = overlay;
            this.progress = progress;
            this.darkenScreen = darkenScreen;
            this.playBossMusic = playBossMusic;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getText() { return text; }
        public BossEvent.BossBarColor getColor() { return color; }
        public BossEvent.BossBarOverlay getOverlay() { return overlay; }
        public float getProgress() { return progress; }
        public boolean isDarkenScreen() { return darkenScreen; }
        public boolean isPlayBossMusic() { return playBossMusic; }
    }
    
    /**
     * Bossbar theme for consistent styling across different bossbars
     */
    public static class BossbarTheme {
        private final String name;
        private final String titleFormat;
        private final String subtitleFormat;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        
        public BossbarTheme(String titleFormat, String subtitleFormat, 
                           BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
            this("default", titleFormat, subtitleFormat, color, overlay);
        }
        
        public BossbarTheme(String name, String titleFormat, String subtitleFormat, 
                           BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
            this.name = name;
            this.titleFormat = titleFormat;
            this.subtitleFormat = subtitleFormat;
            this.color = color;
            this.overlay = overlay;
        }
        
        public String formatTitle(String title) {
            return titleFormat.replace("{title}", title);
        }
        
        public String formatSubtitle(String subtitle) {
            return subtitleFormat.replace("{subtitle}", subtitle);
        }
        
        // Getters
        public String getName() { return name; }
        public String getTitleFormat() { return titleFormat; }
        public String getSubtitleFormat() { return subtitleFormat; }
        public BossEvent.BossBarColor getColor() { return color; }
        public BossEvent.BossBarOverlay getOverlay() { return overlay; }
    }
}
