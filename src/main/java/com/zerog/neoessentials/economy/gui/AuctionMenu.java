package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.auction.AuctionItem;
import com.zerog.neoessentials.economy.auction.AuctionManager;
                                                  // pageInfo.setHoverName(Component.literal("§ePage " + (currentPage + 1) + "/" + (maxPages + 1)));
        // Note: setHoverName may not be available in this MC version    // nextPage.setHoverName(Component.literal("§eNext Page"));
            // Note: setHoverName may not be available in this MC version// createAuction.setHoverName(Component.literal("§eCreate Auction"));
        // Note: setHoverName may not be available in this MC version  // refresh.setHoverName(Component.literal("§eRefresh Auctions"));
        // Note: setHoverName may not be available in this MC version  // viewModeItem.setHoverName(Component.literal("§eView: " + viewMode.name()));
        // Note: setHoverName may not be available in this MC version     // prevPage.setHoverName(Component.literal("§ePrevious Page"));
            // Note: setHoverName may not be available in this MC version/ Note: Setting NBT directly on displayed items for auction info
            // In newer MC versions, use hover text through lore insteadport net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.math.BigDecimal;
import java.util.List;

/**
 * GUI menu for browsing and interacting with auctions
 */
public class AuctionMenu extends BaseEconomyMenu {
    
    private final EconomyManager economyManager;
    private final AuctionManager auctionManager;
    private List<AuctionItem> currentAuctions;
    private int currentPage = 0;
    private AuctionViewMode viewMode = AuctionViewMode.ACTIVE;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows for items, 1 row for navigation
    
    public enum AuctionViewMode {
        ACTIVE, MY_AUCTIONS, MY_BIDS, COMPLETED
    }
    
    public AuctionMenu(int containerId, Inventory playerInventory, EconomyManager economyManager) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, 6);
        this.economyManager = economyManager;
        this.auctionManager = economyManager.getAuctionManager();
        
        setupClickHandler();
        refreshAuctions();
        updateDisplay();
    }
    
    private void setupClickHandler() {
        this.clickHandler = (slot, clickType) -> {
            if (slot < 0 || slot >= container.getContainerSize()) return;
            
            ItemStack clickedItem = container.getItem(slot);
            if (clickedItem.isEmpty()) return;
            
            // Navigation items (bottom row)
            if (slot >= 45) {
                handleNavigationClick(slot, clickType);
                return;
            }
            
            // Auction items
            int auctionIndex = currentPage * ITEMS_PER_PAGE + slot;
            if (auctionIndex >= 0 && auctionIndex < currentAuctions.size()) {
                AuctionItem auction = currentAuctions.get(auctionIndex);
                handleAuctionClick(auction, clickType);
            }
        };
    }
    
    private void handleNavigationClick(int slot, ClickType clickType) {
        switch (slot) {
            case 45: // Previous page
                if (currentPage > 0) {
                    currentPage--;
                    updateDisplay();
                }
                break;
            case 46: // Toggle view mode
                toggleViewMode();
                refreshAuctions();
                updateDisplay();
                break;
            case 49: // Refresh
                refreshAuctions();
                updateDisplay();
                break;
            case 52: // Create auction (if holding item)
                createAuction();
                break;
            case 53: // Next page
                int maxPages = (currentAuctions.size() - 1) / ITEMS_PER_PAGE;
                if (currentPage < maxPages) {
                    currentPage++;
                    updateDisplay();
                }
                break;
        }
    }
    
    private void handleAuctionClick(AuctionItem auction, ClickType clickType) {
        if (clickType == ClickType.PICKUP) {
            // Left click - place bid or buy now
            if (auction.hasBuyNow()) {
                buyNowAuction(auction);
            } else {
                showBidPrompt(auction);
            }
        } else if (clickType == ClickType.PICKUP_ALL) {
            // Right click - place bid with minimum increment
            placeBid(auction, auction.getCurrentBid().add(BigDecimal.ONE));
        } else if (clickType == ClickType.QUICK_MOVE) {
            // Shift click - show auction info
            showAuctionInfo(auction);
        }
    }
    
    private void toggleViewMode() {
        switch (viewMode) {
            case ACTIVE:
                viewMode = AuctionViewMode.MY_AUCTIONS;
                break;
            case MY_AUCTIONS:
                viewMode = AuctionViewMode.MY_BIDS;
                break;
            case MY_BIDS:
                viewMode = AuctionViewMode.COMPLETED;
                break;
            case COMPLETED:
                viewMode = AuctionViewMode.ACTIVE;
                break;
        }
        currentPage = 0;
    }
    
    private void createAuction() {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold an item to create an auction"));
            return;
        }
        
        player.sendSystemMessage(Component.literal("§eUse /auction create <price> <duration> [buynow] [description] to create an auction"));
        player.closeContainer();
    }
    
    private void buyNowAuction(AuctionItem auction) {
        AuctionManager.BuyNowResult result = auctionManager.buyNow(player, auction.getId());
        
        if (result.isSuccess()) {
            player.sendSystemMessage(Component.literal("§aSuccessfully purchased auction for " + 
                economyManager.formatCurrency(auction.getBuyNowPrice())));
            refreshAuctions();
            updateDisplay();
        } else {
            player.sendSystemMessage(Component.literal("§cPurchase failed: " + result.getMessage()));
        }
    }
    
    private void showBidPrompt(AuctionItem auction) {
        player.sendSystemMessage(Component.literal("§eUse /auction bid " + 
            auction.getId().toString().substring(0, 8) + " <amount> to place a bid"));
        player.sendSystemMessage(Component.literal("§7Current bid: §a" + 
            economyManager.formatCurrency(auction.getCurrentBid())));
    }
    
    private void placeBid(AuctionItem auction, BigDecimal amount) {
        AuctionManager.BidResult result = auctionManager.placeBid(player, auction.getId(), amount);
        
        if (result.isSuccess()) {
            player.sendSystemMessage(Component.literal("§aBid placed successfully for " + 
                economyManager.formatCurrency(amount)));
            refreshAuctions();
            updateDisplay();
        } else {
            player.sendSystemMessage(Component.literal("§cBid failed: " + result.getMessage()));
        }
    }
    
    private void showAuctionInfo(AuctionItem auction) {
        player.sendSystemMessage(Component.literal("§6=== Auction Information ==="));
        player.sendSystemMessage(Component.literal("§7ID: §e" + auction.getId().toString().substring(0, 8)));
        player.sendSystemMessage(Component.literal("§7Item: §b" + auction.getItemStack().getHoverName().getString()));
        player.sendSystemMessage(Component.literal("§7Seller: §a" + auction.getSellerName()));
        player.sendSystemMessage(Component.literal("§7Current Bid: §a" + economyManager.formatCurrency(auction.getCurrentBid())));
        
        if (auction.getBuyNowPrice() != null) {
            player.sendSystemMessage(Component.literal("§7Buy Now: §a" + economyManager.formatCurrency(auction.getBuyNowPrice())));
        }
        
        if (auction.getHighestBidder() != null) {
            player.sendSystemMessage(Component.literal("§7Highest Bidder: §a" + auction.getHighestBidderName()));
        }
        
        long minutes = auction.getTimeRemainingMinutes();
        String timeLeft = minutes > 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
        player.sendSystemMessage(Component.literal("§7Time Left: §e" + timeLeft));
        
        if (auction.getDescription() != null && !auction.getDescription().isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Description: §f" + auction.getDescription()));
        }
    }
    
    private void refreshAuctions() {
        switch (viewMode) {
            case ACTIVE:
                currentAuctions = auctionManager.getActiveAuctions();
                break;
            case MY_AUCTIONS:
                currentAuctions = auctionManager.getPlayerAuctions(player.getUUID());
                break;
            case MY_BIDS:
                // TODO: Implement getBiddedAuctions method in AuctionManager
                currentAuctions = auctionManager.getActiveAuctions(); // Fallback for now
                break;
            case COMPLETED:
                currentAuctions = auctionManager.getCompletedAuctions();
                break;
        }
        currentPage = 0;
    }
    
    private void updateDisplay() {
        // Clear container
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
        
        // Add auction items
        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < currentAuctions.size(); i++) {
            AuctionItem auction = currentAuctions.get(startIndex + i);
            ItemStack displayItem = auction.getItemStack().copy();
            
            // Add lore with auction information
            displayItem.getOrCreateTag().putString("AuctionDisplay", 
                "§7Current: §a" + economyManager.formatCurrency(auction.getCurrentBid()) +
                (auction.getBuyNowPrice() != null ? "\n§7Buy Now: §a" + economyManager.formatCurrency(auction.getBuyNowPrice()) : "") +
                "\n§7Time: §e" + auction.getTimeRemainingMinutes() + "m");
            
            container.setItem(i, displayItem);
        }
        
        // Add navigation items
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Items.ARROW);
            prevPage.setHoverName(Component.literal("§ePrevious Page"));
            container.setItem(45, prevPage);
        }
        
        ItemStack viewModeItem = new ItemStack(Items.ENDER_EYE);
        viewModeItem.setHoverName(Component.literal("§eView: " + viewMode.name()));
        container.setItem(46, viewModeItem);
        
        ItemStack refresh = new ItemStack(Items.COMPASS);
        refresh.setHoverName(Component.literal("§eRefresh Auctions"));
        container.setItem(49, refresh);
        
        ItemStack createAuction = new ItemStack(Items.GOLDEN_SWORD);
        createAuction.setHoverName(Component.literal("§eCreate Auction"));
        container.setItem(52, createAuction);
        
        int maxPages = currentAuctions.isEmpty() ? 0 : (currentAuctions.size() - 1) / ITEMS_PER_PAGE;
        if (currentPage < maxPages) {
            ItemStack nextPage = new ItemStack(Items.ARROW);
            nextPage.setHoverName(Component.literal("§eNext Page"));
            container.setItem(53, nextPage);
        }
        
        // Add page info
        ItemStack pageInfo = new ItemStack(Items.BOOK);
        pageInfo.setHoverName(Component.literal("§ePage " + (currentPage + 1) + "/" + (maxPages + 1)));
        container.setItem(48, pageInfo);
    }
    
    @Override
    public boolean clickMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        if (clickHandler != null && player instanceof ServerPlayer) {
            clickHandler.accept(id, ClickType.PICKUP);
            return true;
        }
        return false;
    }
    
    @Override
    public void refresh() {
        refreshAuctions();
        updateDisplay();
    }
    
    @Override
    public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        // Don't allow items to be moved
        return ItemStack.EMPTY;
    }
}
