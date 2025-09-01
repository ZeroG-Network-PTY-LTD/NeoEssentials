package com.zerog.neoessentials.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event handler and custom events for NeoEssentials
 * Provides integration points for other mods
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class NeoEssentialsEventHandler {
    
    /**
     * Base class for all NeoEssentials events
     */
    public static abstract class NeoEssentialsEvent extends Event {
        private final ServerPlayer player;
        
        protected NeoEssentialsEvent(ServerPlayer player) {
            this.player = player;
        }
        
        public ServerPlayer getPlayer() {
            return player;
        }
        
        public UUID getPlayerUUID() {
            return player != null ? player.getUUID() : null;
        }
    }
    
    /**
     * Base class for cancellable NeoEssentials events
     */
    public static abstract class CancellableNeoEssentialsEvent extends NeoEssentialsEvent implements ICancellableEvent {
        protected CancellableNeoEssentialsEvent(ServerPlayer player) {
            super(player);
        }
    }
    
    // ========================= ECONOMY EVENTS =========================
    
    /**
     * Fired when a player's balance changes
     */
    public static class EconomyBalanceChangeEvent extends NeoEssentialsEvent {
        private final BigDecimal oldBalance;
        private final BigDecimal newBalance;
        private final String reason;
        private final TransactionType type;
        
        public EconomyBalanceChangeEvent(ServerPlayer player, BigDecimal oldBalance, BigDecimal newBalance, String reason, TransactionType type) {
            super(player);
            this.oldBalance = oldBalance;
            this.newBalance = newBalance;
            this.reason = reason;
            this.type = type;
        }
        
        public BigDecimal getOldBalance() { return oldBalance; }
        public BigDecimal getNewBalance() { return newBalance; }
        public BigDecimal getChange() { return newBalance.subtract(oldBalance); }
        public String getReason() { return reason; }
        public TransactionType getType() { return type; }
        
        public enum TransactionType {
            DEPOSIT, WITHDRAW, SET, TRANSFER_SEND, TRANSFER_RECEIVE
        }
    }
    
    /**
     * Fired before a transaction occurs (cancellable)
     */
    public static class EconomyTransactionEvent extends CancellableNeoEssentialsEvent {
        private final BigDecimal amount;
        private final String reason;
        private final EconomyBalanceChangeEvent.TransactionType type;
        private final ServerPlayer otherPlayer; // For transfers
        
        public EconomyTransactionEvent(ServerPlayer player, BigDecimal amount, String reason, EconomyBalanceChangeEvent.TransactionType type) {
            this(player, amount, reason, type, null);
        }
        
        public EconomyTransactionEvent(ServerPlayer player, BigDecimal amount, String reason, EconomyBalanceChangeEvent.TransactionType type, ServerPlayer otherPlayer) {
            super(player);
            this.amount = amount;
            this.reason = reason;
            this.type = type;
            this.otherPlayer = otherPlayer;
        }
        
        public BigDecimal getAmount() { return amount; }
        public String getReason() { return reason; }
        public EconomyBalanceChangeEvent.TransactionType getType() { return type; }
        public ServerPlayer getOtherPlayer() { return otherPlayer; }
    }
    
    // ========================= HOME EVENTS =========================
    
    /**
     * Fired when a player sets a home
     */
    public static class HomeSetEvent extends CancellableNeoEssentialsEvent {
        private final String homeName;
        private final double x, y, z;
        private final String world;
        
        public HomeSetEvent(ServerPlayer player, String homeName, double x, double y, double z, String world) {
            super(player);
            this.homeName = homeName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
        
        public String getHomeName() { return homeName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public String getWorld() { return world; }
    }
    
    /**
     * Fired when a player teleports to a home
     */
    public static class HomeTeleportEvent extends CancellableNeoEssentialsEvent {
        private final String homeName;
        private final double x, y, z;
        private final String world;
        
        public HomeTeleportEvent(ServerPlayer player, String homeName, double x, double y, double z, String world) {
            super(player);
            this.homeName = homeName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
        
        public String getHomeName() { return homeName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public String getWorld() { return world; }
    }
    
    /**
     * Fired when a player deletes a home
     */
    public static class HomeDeleteEvent extends CancellableNeoEssentialsEvent {
        private final String homeName;
        
        public HomeDeleteEvent(ServerPlayer player, String homeName) {
            super(player);
            this.homeName = homeName;
        }
        
        public String getHomeName() { return homeName; }
    }
    
    // ========================= WARP EVENTS =========================
    
    /**
     * Fired when a player teleports to a warp
     */
    public static class WarpTeleportEvent extends CancellableNeoEssentialsEvent {
        private final String warpName;
        private final double x, y, z;
        private final String world;
        
        public WarpTeleportEvent(ServerPlayer player, String warpName, double x, double y, double z, String world) {
            super(player);
            this.warpName = warpName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
        
        public String getWarpName() { return warpName; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public String getWorld() { return world; }
    }
    
    // ========================= KIT EVENTS =========================
    
    /**
     * Fired when a player receives a kit
     */
    public static class KitGiveEvent extends CancellableNeoEssentialsEvent {
        private final String kitName;
        private final boolean hasCooldown;
        private final long cooldownTime;
        
        public KitGiveEvent(ServerPlayer player, String kitName, boolean hasCooldown, long cooldownTime) {
            super(player);
            this.kitName = kitName;
            this.hasCooldown = hasCooldown;
            this.cooldownTime = cooldownTime;
        }
        
        public String getKitName() { return kitName; }
        public boolean hasCooldown() { return hasCooldown; }
        public long getCooldownTime() { return cooldownTime; }
    }
    
    // ========================= MESSAGING EVENTS =========================
    
    /**
     * Fired when a player sends a private message
     */
    public static class PrivateMessageEvent extends CancellableNeoEssentialsEvent {
        private final ServerPlayer recipient;
        private String message;
        
        public PrivateMessageEvent(ServerPlayer sender, ServerPlayer recipient, String message) {
            super(sender);
            this.recipient = recipient;
            this.message = message;
        }
        
        public ServerPlayer getSender() { return getPlayer(); }
        public ServerPlayer getRecipient() { return recipient; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * Fired when a player sends mail
     */
    public static class MailSendEvent extends CancellableNeoEssentialsEvent {
        private final String recipientName;
        private String message;
        
        public MailSendEvent(ServerPlayer sender, String recipientName, String message) {
            super(sender);
            this.recipientName = recipientName;
            this.message = message;
        }
        
        public ServerPlayer getSender() { return getPlayer(); }
        public String getRecipientName() { return recipientName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    // ========================= TELEPORT EVENTS =========================
    
    /**
     * Fired when a player teleports to spawn
     */
    public static class SpawnTeleportEvent extends CancellableNeoEssentialsEvent {
        private final double x, y, z;
        private final String world;
        
        public SpawnTeleportEvent(ServerPlayer player, double x, double y, double z, String world) {
            super(player);
            this.x = x;
            this.y = y;
            this.z = z;
            this.world = world;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public String getWorld() { return world; }
    }
    
    /**
     * Fired when a teleport request is sent
     */
    public static class TeleportRequestEvent extends CancellableNeoEssentialsEvent {
        private final ServerPlayer target;
        private final RequestType type;
        
        public TeleportRequestEvent(ServerPlayer requester, ServerPlayer target, RequestType type) {
            super(requester);
            this.target = target;
            this.type = type;
        }
        
        public ServerPlayer getRequester() { return getPlayer(); }
        public ServerPlayer getTarget() { return target; }
        public RequestType getType() { return type; }
        
        public enum RequestType {
            TPA, // Teleport to target
            TPAHERE // Target teleports to requester
        }
    }
    
    // ========================= MODERATION EVENTS =========================
    
    /**
     * Fired when a player is muted
     */
    public static class PlayerMuteEvent extends CancellableNeoEssentialsEvent {
        private final ServerPlayer moderator;
        private final String reason;
        private final long duration; // 0 for permanent
        
        public PlayerMuteEvent(ServerPlayer player, ServerPlayer moderator, String reason, long duration) {
            super(player);
            this.moderator = moderator;
            this.reason = reason;
            this.duration = duration;
        }
        
        public ServerPlayer getModerator() { return moderator; }
        public String getReason() { return reason; }
        public long getDuration() { return duration; }
        public boolean isPermanent() { return duration == 0; }
    }
    
    /**
     * Fired when a player is unmuted
     */
    public static class PlayerUnmuteEvent extends NeoEssentialsEvent {
        private final ServerPlayer moderator;
        private final String reason;
        
        public PlayerUnmuteEvent(ServerPlayer player, ServerPlayer moderator, String reason) {
            super(player);
            this.moderator = moderator;
            this.reason = reason;
        }
        
        public ServerPlayer getModerator() { return moderator; }
        public String getReason() { return reason; }
    }
    
    /**
     * Fired when a player is kicked
     */
    public static class PlayerKickEvent extends CancellableNeoEssentialsEvent {
        private final ServerPlayer moderator;
        private String reason;
        
        public PlayerKickEvent(ServerPlayer player, ServerPlayer moderator, String reason) {
            super(player);
            this.moderator = moderator;
            this.reason = reason;
        }
        
        public ServerPlayer getModerator() { return moderator; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    // ========================= PLAYER EVENTS =========================
    
    /**
     * Fired when a player's AFK status changes
     */
    public static class PlayerAFKEvent extends NeoEssentialsEvent {
        private final boolean afk;
        private final long timestamp;
        
        public PlayerAFKEvent(ServerPlayer player, boolean afk, long timestamp) {
            super(player);
            this.afk = afk;
            this.timestamp = timestamp;
        }
        
        public boolean isAFK() { return afk; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Fired when a player's nickname changes
     */
    public static class PlayerNicknameChangeEvent extends CancellableNeoEssentialsEvent {
        private final String oldNickname;
        private String newNickname;
        
        public PlayerNicknameChangeEvent(ServerPlayer player, String oldNickname, String newNickname) {
            super(player);
            this.oldNickname = oldNickname;
            this.newNickname = newNickname;
        }
        
        public String getOldNickname() { return oldNickname; }
        public String getNewNickname() { return newNickname; }
        public void setNewNickname(String newNickname) { this.newNickname = newNickname; }
    }
    
    // ========================= PLACEHOLDER EVENTS =========================
    
    /**
     * Fired when a placeholder is registered
     */
    public static class PlaceholderRegisterEvent extends Event {
        private final String identifier;
        private final String providerName;
        
        public PlaceholderRegisterEvent(String identifier, String providerName) {
            this.identifier = identifier;
            this.providerName = providerName;
        }
        
        public String getIdentifier() { return identifier; }
        public String getProviderName() { return providerName; }
    }
    
    /**
     * Fired when placeholders are processed
     */
    public static class PlaceholderProcessEvent extends Event {
        private final String originalText;
        private String processedText;
        private final ServerPlayer player;
        
        public PlaceholderProcessEvent(String originalText, String processedText, ServerPlayer player) {
            this.originalText = originalText;
            this.processedText = processedText;
            this.player = player;
        }
        
        public String getOriginalText() { return originalText; }
        public String getProcessedText() { return processedText; }
        public void setProcessedText(String processedText) { this.processedText = processedText; }
        public ServerPlayer getPlayer() { return player; }
    }
}