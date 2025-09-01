package com.zerog.neoessentials.tablist;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.util.ColorUtil;
import com.zerog.neoessentials.util.DebugUtil;
import java.util.*;

/**
 * Enhanced Tablist Header/Footer Manager with improved performance and color support
 * Manages per-player tablist header/footer state, diffing, and safe updates.
 */
public class HeaderFooterManager {
    private final Map<UUID, ViewerState> viewerStates = Collections.synchronizedMap(new HashMap<>());
    private String[] headerTemplate;
    private String[] footerTemplate;
    private long headerAnimationIntervalMs = 800;
    private long footerAnimationIntervalMs = 1000;
    private int headerFrame = 0;
    private int footerFrame = 0;
    private long lastHeaderAnimTs = 0;
    private long lastFooterAnimTs = 0;
    
    // Configuration
    private boolean enableAnimations = true;
    private boolean enableColorCodes = true;
    private int maxUpdateRate = 5; // Max updates per second per player

    public static class ViewerState {
        public Component lastHeader;
        public Component lastFooter;
        public long lastUpdateTs;
        public String lastHeaderText;
        public String lastFooterText;
        public boolean needsUpdate = false;
    }

    public void setHeaderTemplate(String[] lines, long intervalMs) {
        this.headerTemplate = lines;
        this.headerAnimationIntervalMs = Math.max(intervalMs, 100); // Min 100ms
        this.headerFrame = 0;
        DebugUtil.debugLog("[HeaderFooter] Set header template with " + lines.length + " frames, interval: " + intervalMs + "ms");
        markAllPlayersForUpdate();
    }
    
    public void setFooterTemplate(String[] lines, long intervalMs) {
        this.footerTemplate = lines;
        this.footerAnimationIntervalMs = Math.max(intervalMs, 100); // Min 100ms
        this.footerFrame = 0;
        DebugUtil.debugLog("[HeaderFooter] Set footer template with " + lines.length + " frames, interval: " + intervalMs + "ms");
        markAllPlayersForUpdate();
    }

    /**
     * Configure animation and performance settings
     */
    public void configure(boolean enableAnimations, boolean enableColorCodes, int maxUpdateRate) {
        this.enableAnimations = enableAnimations;
        this.enableColorCodes = enableColorCodes;
        this.maxUpdateRate = Math.max(1, Math.min(20, maxUpdateRate)); // 1-20 updates per second
        DebugUtil.debugLog("[HeaderFooter] Configured - animations: " + enableAnimations + 
                           ", colors: " + enableColorCodes + ", max rate: " + maxUpdateRate + "/s");
    }

    public void onPlayerJoin(ServerPlayer player) {
        ViewerState state = new ViewerState();
        state.needsUpdate = true;
        viewerStates.put(player.getUUID(), state);
        scheduleHeaderFooterUpdate(player);
        DebugUtil.debugLog("[HeaderFooter] Player joined: " + player.getName().getString());
    }
    
    public void onPlayerQuit(ServerPlayer player) {
        viewerStates.remove(player.getUUID());
        DebugUtil.debugLog("[HeaderFooter] Player quit: " + player.getName().getString());
    }
    
    public void scheduleHeaderFooterUpdate(ServerPlayer player) {
        ViewerState state = viewerStates.get(player.getUUID());
        if (state != null) {
            state.needsUpdate = true;
        }
    }
    
    /**
     * Mark all players for update (useful when templates change)
     */
    private void markAllPlayersForUpdate() {
        synchronized (viewerStates) {
            for (ViewerState state : viewerStates.values()) {
                state.needsUpdate = true;
            }
        }
    }
    /**
     * Enhanced tick method with better performance and animation handling
     */
    public void tick(long now, com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager) {
        // Animation frame advance (only if animations enabled)
        if (enableAnimations) {
            if (headerTemplate != null && headerTemplate.length > 1 && 
                now - lastHeaderAnimTs >= headerAnimationIntervalMs) {
                headerFrame = (headerFrame + 1) % headerTemplate.length;
                lastHeaderAnimTs = now;
                markAllPlayersForUpdate();
            }
            if (footerTemplate != null && footerTemplate.length > 1 && 
                now - lastFooterAnimTs >= footerAnimationIntervalMs) {
                footerFrame = (footerFrame + 1) % footerTemplate.length;
                lastFooterAnimTs = now;
                markAllPlayersForUpdate();
            }
        }
        
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        long minUpdateInterval = 1000 / maxUpdateRate; // Convert rate to interval
        
        for (ServerPlayer player : players) {
            ViewerState state = viewerStates.get(player.getUUID());
            if (state == null) continue;
            
            // Rate limiting check
            if (!state.needsUpdate && now - state.lastUpdateTs < minUpdateInterval) {
                continue;
            }
            
            // Get header/footer text for this player
            String headerRaw = getHeaderForPlayer(player);
            String footerRaw = getFooterForPlayer(player);
            
            // Process placeholders
            String headerStr = placeholderManager.processPlaceholders(headerRaw, player);
            String footerStr = placeholderManager.processPlaceholders(footerRaw, player);
            
            // Check if content actually changed (performance optimization)
            if (!state.needsUpdate && 
                Objects.equals(headerStr, state.lastHeaderText) && 
                Objects.equals(footerStr, state.lastFooterText)) {
                continue;
            }
            
            // Apply color formatting if enabled
            Component header = enableColorCodes ? ColorUtil.colorize(headerStr) : Component.literal(headerStr);
            Component footer = enableColorCodes ? ColorUtil.colorize(footerStr) : Component.literal(footerStr);
            
            // Send update
            sendHeaderFooter(player, header, footer);
            
            // Update state
            state.lastHeader = header;
            state.lastFooter = footer;
            state.lastHeaderText = headerStr;
            state.lastFooterText = footerStr;
            state.lastUpdateTs = now;
            state.needsUpdate = false;
        }
    }

    /**
     * Get header text for specific player (supports per-player customization)
     */
    private String getHeaderForPlayer(ServerPlayer player) {
        if (headerTemplate == null || headerTemplate.length == 0) {
            return "";
        }
        
        // Support group-based or permission-based headers in the future
        String baseHeader = headerTemplate[headerFrame % headerTemplate.length];
        
        // Add server-specific placeholders
        return baseHeader
            .replace("{server}", getServerName())
            .replace("{player}", player.getName().getString())
            .replace("{player_count}", String.valueOf(getOnlinePlayerCount()));
    }
    
    /**
     * Get footer text for specific player (supports per-player customization)
     */
    private String getFooterForPlayer(ServerPlayer player) {
        if (footerTemplate == null || footerTemplate.length == 0) {
            return "";
        }
        
        // Support group-based or permission-based footers in the future
        String baseFooter = footerTemplate[footerFrame % footerTemplate.length];
        
        // Add server-specific placeholders
        return baseFooter
            .replace("{server}", getServerName())
            .replace("{player}", player.getName().getString())
            .replace("{player_count}", String.valueOf(getOnlinePlayerCount()));
    }
    
    /**
     * Helper methods for built-in placeholders
     */
    private String getServerName() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getMotd() : "NeoEssentials Server";
    }
    
    private int getOnlinePlayerCount() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerCount() : 0;
    }

    /**
     * Enhanced method to send header/footer with better error handling and logging
     */
    public void sendHeaderFooter(ServerPlayer player, Component header, Component footer) {
        if (player == null || player.connection == null) {
            DebugUtil.debugLog("[HeaderFooter] Cannot send to null player or connection");
            return;
        }
        
        var server = player.getServer();
        if (server != null) {
            server.execute(() -> {
                try {
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(header, footer));
                    DebugUtil.debugLog("[HeaderFooter] Sent header/footer to " + player.getName().getString());
                } catch (Exception e) {
                    DebugUtil.warnLog("[HeaderFooter] Error sending header/footer to " + 
                                     player.getName().getString() + ": " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Get current animation frame info for debugging
     */
    public String getAnimationStatus() {
        return String.format("Header: frame %d/%d (interval: %dms), Footer: frame %d/%d (interval: %dms)", 
                           headerFrame + 1, 
                           headerTemplate != null ? headerTemplate.length : 0, 
                           headerAnimationIntervalMs,
                           footerFrame + 1, 
                           footerTemplate != null ? footerTemplate.length : 0, 
                           footerAnimationIntervalMs);
    }
    
    /**
     * Get viewer count for debugging
     */
    public int getViewerCount() {
        return viewerStates.size();
    }
    
    /**
     * Force update all viewers (useful for debugging or config reloads)
     */
    public void forceUpdateAll() {
        markAllPlayersForUpdate();
        DebugUtil.debugLog("[HeaderFooter] Forced update for all " + viewerStates.size() + " viewers");
    }
}
