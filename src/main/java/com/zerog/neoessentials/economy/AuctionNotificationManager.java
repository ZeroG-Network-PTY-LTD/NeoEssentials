package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages notifications for auction events such as bids, wins, and auction endings.
 * Provides real-time updates to players who are watching auctions.
 */
public class AuctionNotificationManager {
    private static AuctionNotificationManager instance;
    
    // Map of auction ID to set of player UUIDs watching that auction
    private final Map<UUID, Set<UUID>> watchList = new ConcurrentHashMap<>();
    
    // Map of player UUID to set of auction IDs they're watching
    private final Map<UUID, Set<UUID>> playerWatchMap = new ConcurrentHashMap<>();
    
    private AuctionNotificationManager() {}
    
    public static AuctionNotificationManager getInstance() {
        if (instance == null) {
            instance = new AuctionNotificationManager();
        }
        return instance;
    }
    
    /**
     * Add a player to watch an auction
     * 
     * @param playerId The player's UUID
     * @param auctionId The auction's UUID
     */
    public void addWatcher(UUID playerId, UUID auctionId) {
        // Add to auction watch list
        watchList.computeIfAbsent(auctionId, k -> new HashSet<>()).add(playerId);
        
        // Add to player watch map
        playerWatchMap.computeIfAbsent(playerId, k -> new HashSet<>()).add(auctionId);
    }
    
    /**
     * Remove a player from watching an auction
     * 
     * @param playerId The player's UUID
     * @param auctionId The auction's UUID
     */
    public void removeWatcher(UUID playerId, UUID auctionId) {
        // Remove from auction watch list
        Set<UUID> watchers = watchList.get(auctionId);
        if (watchers != null) {
            watchers.remove(playerId);
            if (watchers.isEmpty()) {
                watchList.remove(auctionId);
            }
        }
        
        // Remove from player watch map
        Set<UUID> playerWatches = playerWatchMap.get(playerId);
        if (playerWatches != null) {
            playerWatches.remove(auctionId);
            if (playerWatches.isEmpty()) {
                playerWatchMap.remove(playerId);
            }
        }
    }
    
    /**
     * Check if a player is watching an auction
     * 
     * @param playerId The player's UUID
     * @param auctionId The auction's UUID
     * @return True if the player is watching the auction
     */
    public boolean isWatching(UUID playerId, UUID auctionId) {
        Set<UUID> playerWatches = playerWatchMap.get(playerId);
        return playerWatches != null && playerWatches.contains(auctionId);
    }
    
    /**
     * Get all auctions a player is watching
     * 
     * @param playerId The player's UUID
     * @return Set of auction IDs the player is watching
     */
    public Set<UUID> getPlayerWatchList(UUID playerId) {
        return new HashSet<>(playerWatchMap.getOrDefault(playerId, new HashSet<>()));
    }
    
    /**
     * Get all players watching an auction
     * 
     * @param auctionId The auction's UUID
     * @return Set of player UUIDs watching the auction
     */
    public Set<UUID> getAuctionWatchers(UUID auctionId) {
        return new HashSet<>(watchList.getOrDefault(auctionId, new HashSet<>()));
    }
    
    /**
     * Notify all watchers when a new bid is placed on an auction
     * 
     * @param auction The auction with the new bid
     * @param bidder The player who placed the bid
     * @param bidAmount The bid amount
     * @param server The Minecraft server instance
     */
    public void notifyNewBid(Auction auction, ServerPlayer bidder, double bidAmount, MinecraftServer server) {
        Set<UUID> watchers = getAuctionWatchers(auction.getAuctionId());
        if (watchers.isEmpty()) return;
        
        String message = String.format("§e[Auction] §a%s §7bid §e%s §7on §a%s §7(ID: %s)",
            bidder.getName().getString(),
            auction.getCurrency().format(bidAmount),
            auction.getItemName(),
            auction.getAuctionId().toString().substring(0, 8));
        
        for (UUID watcherId : watchers) {
            // Don't notify the bidder themselves
            if (watcherId.equals(bidder.getUUID())) continue;
            
            ServerPlayer watcher = server.getPlayerList().getPlayer(watcherId);
            if (watcher != null) {
                LanguageUtil.sendMessage(watcher, message);
                
                // Send additional info if they were the previous highest bidder
                if (watcherId.equals(auction.getCurrentBidder())) {
                    LanguageUtil.sendMessage(watcher, "§c⚠ You have been outbid! Consider placing a higher bid.");
                }
            }
        }
    }
    
    /**
     * Notify all watchers when an auction ends
     * 
     * @param auction The auction that ended
     * @param server The Minecraft server instance
     */
    public void notifyAuctionEnded(Auction auction, MinecraftServer server) {
        Set<UUID> watchers = getAuctionWatchers(auction.getAuctionId());
        if (watchers.isEmpty()) return;
        
        String baseMessage = String.format("§e[Auction] §7Auction for §a%s §7has ended (ID: %s)",
            auction.getItemName(),
            auction.getAuctionId().toString().substring(0, 8));
        
        UUID winner = auction.getCurrentBidder();
        String winnerMessage;
        
        if (winner != null) {
            ServerPlayer winnerPlayer = server.getPlayerList().getPlayer(winner);
            String winnerName = winnerPlayer != null ? winnerPlayer.getName().getString() : "Unknown Player";
            winnerMessage = String.format("§a%s §7won with a bid of §e%s",
                winnerName, auction.getCurrency().format(auction.getCurrentBid()));
        } else {
            winnerMessage = "§7No bids were placed";
        }
        
        for (UUID watcherId : watchers) {
            ServerPlayer watcher = server.getPlayerList().getPlayer(watcherId);
            if (watcher != null) {
                LanguageUtil.sendMessage(watcher, baseMessage);
                LanguageUtil.sendMessage(watcher, winnerMessage);
                
                // Special message for winner
                if (watcherId.equals(winner)) {
                    LanguageUtil.sendMessage(watcher, "§a🎉 Congratulations! You won the auction!");
                    LanguageUtil.sendMessage(watcher, "§7Use §e/auction collect §7to claim your item.");
                }
            }
        }
        
        // Clean up watchers for this auction
        clearAuctionWatchers(auction.getAuctionId());
    }
    
    /**
     * Notify all watchers when an auction is bought out instantly
     * 
     * @param auction The auction that was bought out
     * @param buyer The player who bought the item
     * @param server The Minecraft server instance
     */
    public void notifyAuctionBuyout(Auction auction, ServerPlayer buyer, MinecraftServer server) {
        Set<UUID> watchers = getAuctionWatchers(auction.getAuctionId());
        if (watchers.isEmpty()) return;
        
        String message = String.format("§e[Auction] §a%s §7bought §a%s §7instantly for §e%s §7(ID: %s)",
            buyer.getName().getString(),
            auction.getItemName(),
            auction.getCurrency().format(auction.getBuyoutPrice()),
            auction.getAuctionId().toString().substring(0, 8));
        
        for (UUID watcherId : watchers) {
            // Don't notify the buyer themselves
            if (watcherId.equals(buyer.getUUID())) continue;
            
            ServerPlayer watcher = server.getPlayerList().getPlayer(watcherId);
            if (watcher != null) {
                LanguageUtil.sendMessage(watcher, message);
                LanguageUtil.sendMessage(watcher, "§7This auction is no longer available.");
            }
        }
        
        // Clean up watchers for this auction
        clearAuctionWatchers(auction.getAuctionId());
    }
    
    /**
     * Notify all watchers when an auction is cancelled
     * 
     * @param auction The auction that was cancelled
     * @param server The Minecraft server instance
     */
    public void notifyAuctionCancelled(Auction auction, MinecraftServer server) {
        Set<UUID> watchers = getAuctionWatchers(auction.getAuctionId());
        if (watchers.isEmpty()) return;
        
        String message = String.format("§e[Auction] §7Auction for §a%s §7was cancelled by the seller (ID: %s)",
            auction.getItemName(),
            auction.getAuctionId().toString().substring(0, 8));
        
        for (UUID watcherId : watchers) {
            ServerPlayer watcher = server.getPlayerList().getPlayer(watcherId);
            if (watcher != null) {
                LanguageUtil.sendMessage(watcher, message);
                
                if (watcherId.equals(auction.getCurrentBidder())) {
                    LanguageUtil.sendMessage(watcher, "§7Your bid has been refunded.");
                }
            }
        }
        
        // Clean up watchers for this auction
        clearAuctionWatchers(auction.getAuctionId());
    }
    
    /**
     * Send a reminder notification to watchers when an auction is about to end
     * 
     * @param auction The auction ending soon
     * @param minutesRemaining Minutes remaining until auction ends
     * @param server The Minecraft server instance
     */
    public void notifyAuctionEndingSoon(Auction auction, int minutesRemaining, MinecraftServer server) {
        Set<UUID> watchers = getAuctionWatchers(auction.getAuctionId());
        if (watchers.isEmpty()) return;
        
        String message = String.format("§e[Auction] §c⏰ §a%s §7ends in §e%d minute%s §7(ID: %s)",
            auction.getItemName(),
            minutesRemaining,
            minutesRemaining == 1 ? "" : "s",
            auction.getAuctionId().toString().substring(0, 8));
        
        String currentBidMessage = String.format("§7Current bid: §e%s", 
            auction.getCurrency().format(auction.getCurrentBid()));
        
        for (UUID watcherId : watchers) {
            ServerPlayer watcher = server.getPlayerList().getPlayer(watcherId);
            if (watcher != null) {
                LanguageUtil.sendMessage(watcher, message);
                LanguageUtil.sendMessage(watcher, currentBidMessage);
            }
        }
    }
    
    /**
     * Clear all watchers for an auction (called when auction ends or is removed)
     * 
     * @param auctionId The auction ID to clear watchers for
     */
    public void clearAuctionWatchers(UUID auctionId) {
        Set<UUID> watchers = watchList.remove(auctionId);
        if (watchers != null) {
            for (UUID watcherId : watchers) {
                Set<UUID> playerWatches = playerWatchMap.get(watcherId);
                if (playerWatches != null) {
                    playerWatches.remove(auctionId);
                    if (playerWatches.isEmpty()) {
                        playerWatchMap.remove(watcherId);
                    }
                }
            }
        }
    }
    
    /**
     * Get total watch count across all auctions
     * 
     * @return Total number of watch relationships
     */
    public int getTotalWatchCount() {
        return watchList.values().stream().mapToInt(Set::size).sum();
    }
    
    /**
     * Get statistics about the watch system
     * 
     * @return Map containing watch statistics
     */
    public Map<String, Object> getWatchStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWatchers", playerWatchMap.size());
        stats.put("totalWatchedAuctions", watchList.size());
        stats.put("totalWatchRelationships", getTotalWatchCount());
        
        // Find most watched auction
        UUID mostWatchedAuction = null;
        int maxWatchers = 0;
        for (Map.Entry<UUID, Set<UUID>> entry : watchList.entrySet()) {
            if (entry.getValue().size() > maxWatchers) {
                maxWatchers = entry.getValue().size();
                mostWatchedAuction = entry.getKey();
            }
        }
        
        stats.put("mostWatchedAuction", mostWatchedAuction);
        stats.put("mostWatchedAuctionWatchers", maxWatchers);
        
        return stats;
    }
}
