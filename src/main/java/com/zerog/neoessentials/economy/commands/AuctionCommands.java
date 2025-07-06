package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.auction.AuctionItem;
import com.zerog.neoessentials.economy.auction.AuctionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Commands for auction system
 */
public class AuctionCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("auction")
            .requires(source -> source.hasPermission(2) || !source.isPlayer())
            
            .then(Commands.literal("list")
                .executes(AuctionCommands::listAuctions)
                .then(Commands.argument("status", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("active");
                        builder.suggest("completed");
                        builder.suggest("my");
                        return builder.buildFuture();
                    })
                    .executes(AuctionCommands::listAuctionsByStatus)))
            
            .then(Commands.literal("create")
                .then(Commands.argument("startPrice", StringArgumentType.word())
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 168)) // 1 hour to 1 week
                        .executes(ctx -> createAuction(ctx, null, null))
                        .then(Commands.argument("buyNowPrice", StringArgumentType.word())
                            .executes(ctx -> createAuction(ctx, StringArgumentType.getString(ctx, "buyNowPrice"), null))
                            .then(Commands.argument("description", StringArgumentType.greedyString())
                                .executes(ctx -> createAuction(ctx, StringArgumentType.getString(ctx, "buyNowPrice"), 
                                    StringArgumentType.getString(ctx, "description"))))))))
            
            .then(Commands.literal("bid")
                .then(Commands.argument("auctionId", StringArgumentType.string())
                    .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(AuctionCommands::bidOnAuction))))
            
            .then(Commands.literal("buynow")
                .then(Commands.argument("auctionId", StringArgumentType.string())
                    .executes(AuctionCommands::buyNowAuction)))
            
            .then(Commands.literal("cancel")
                .then(Commands.argument("auctionId", StringArgumentType.string())
                    .executes(AuctionCommands::cancelAuction)))
            
            .then(Commands.literal("info")
                .then(Commands.argument("auctionId", StringArgumentType.string())
                    .executes(AuctionCommands::showAuctionInfo)))
            
            .then(Commands.literal("stats")
                .executes(AuctionCommands::showStats)
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(AuctionCommands::showPlayerStats)))
            
            // Admin commands
            .then(Commands.literal("admin")
                .requires(source -> source.hasPermission(3))
                
                .then(Commands.literal("force-end")
                    .then(Commands.argument("auctionId", StringArgumentType.string())
                        .executes(AuctionCommands::forceEndAuction)))
                
                .then(Commands.literal("cleanup")
                    .executes(AuctionCommands::cleanupExpiredAuctions))
                
                .then(Commands.literal("stats")
                    .executes(AuctionCommands::showAdminStats))));
        
        // Aliases
        dispatcher.register(Commands.literal("auc").redirect(dispatcher.getRoot().getChild("auction")));
        dispatcher.register(Commands.literal("auctions").redirect(dispatcher.getRoot().getChild("auction")));
    }
    
    private static int listAuctions(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            AuctionManager auctionManager = economyManager.getAuctionManager();
            List<AuctionItem> auctions = auctionManager.getActiveAuctions();
            
            if (auctions.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§eNo active auctions found"), false);
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§6=== Active Auctions ==="), false);
            
            for (AuctionItem auction : auctions.subList(0, Math.min(10, auctions.size()))) {
                String timeLeft = formatTimeLeft(auction.getTimeRemainingMinutes() * 60);
                Component message = Component.literal(String.format(
                    "§f[§e%s§f] §b%s §7x%d - §aCurrent: %s §7| Buy Now: %s §7| %s",
                    auction.getId().toString().substring(0, 8),
                    auction.getItemStack().getHoverName().getString(),
                    auction.getItemStack().getCount(),
                    economyManager.formatCurrency(auction.getCurrentBid()),
                    auction.getBuyNowPrice() != null ? economyManager.formatCurrency(auction.getBuyNowPrice()) : "N/A",
                    timeLeft
                ));
                source.sendSuccess(() -> message, false);
            }
            
            if (auctions.size() > 10) {
                source.sendSuccess(() -> Component.literal("§7... and " + (auctions.size() - 10) + " more"), false);
            }
            
            return auctions.size();
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction list command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while listing auctions"));
            return 0;
        }
    }
    
    private static int listAuctionsByStatus(CommandContext<CommandSourceStack> context) {
        try {
            String status = StringArgumentType.getString(context, "status");
            CommandSourceStack source = context.getSource();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            AuctionManager auctionManager = economyManager.getAuctionManager();
            List<AuctionItem> auctions;
            
            switch (status.toLowerCase()) {
                case "active":
                    auctions = auctionManager.getActiveAuctions();
                    break;
                case "completed":
                    auctions = auctionManager.getCompletedAuctions();
                    break;
                case "my":
                    if (!source.isPlayer()) {
                        source.sendFailure(Component.literal("Only players can view their own auctions"));
                        return 0;
                    }
                    ServerPlayer player = source.getPlayerOrException();
                    auctions = auctionManager.getPlayerAuctions(player.getUUID());
                    break;
                default:
                    source.sendFailure(Component.literal("Invalid status. Use: active, completed, or my"));
                    return 0;
            }
            
            if (auctions.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§eNo " + status + " auctions found"), false);
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§6=== " + status.substring(0, 1).toUpperCase() + status.substring(1) + " Auctions ==="), false);
            
            for (AuctionItem auction : auctions.subList(0, Math.min(10, auctions.size()))) {
                String timeInfo = status.equals("active") 
                    ? formatTimeLeft(auction.getTimeRemainingMinutes() * 60)
                    : "Ended " + auction.getEndTime().toString();
                    
                Component message = Component.literal(String.format(
                    "§f[§e%s§f] §b%s §7x%d - §aFinal: %s §7| %s",
                    auction.getId().toString().substring(0, 8),
                    auction.getItemStack().getHoverName().getString(),
                    auction.getItemStack().getCount(),
                    economyManager.formatCurrency(auction.getCurrentBid()),
                    timeInfo
                ));
                source.sendSuccess(() -> message, false);
            }
            
            return auctions.size();
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction list by status command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while listing auctions"));
            return 0;
        }
    }
    
    private static int createAuction(CommandContext<CommandSourceStack> context, String buyNowPriceStr, String description) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can create auctions"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            String startPriceStr = StringArgumentType.getString(context, "startPrice");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            
            // Parse starting price
            BigDecimal startPrice;
            try {
                startPrice = new BigDecimal(startPriceStr);
                if (startPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    source.sendFailure(Component.literal("Starting price must be positive"));
                    return 0;
                }
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("Invalid starting price format"));
                return 0;
            }
            
            // Parse buy now price (optional)
            BigDecimal buyNowPrice = null;
            if (buyNowPriceStr != null) {
                try {
                    buyNowPrice = new BigDecimal(buyNowPriceStr);
                    if (buyNowPrice.compareTo(startPrice) <= 0) {
                        source.sendFailure(Component.literal("Buy now price must be higher than starting price"));
                        return 0;
                    }
                } catch (NumberFormatException e) {
                    source.sendFailure(Component.literal("Invalid buy now price format"));
                    return 0;
                }
            }
            
            // Get item from player's hand
            ItemStack itemStack = player.getMainHandItem();
            if (itemStack.isEmpty()) {
                source.sendFailure(Component.literal("You must hold an item to auction"));
                return 0;
            }
            
            // Create auction
            AuctionManager auctionManager = economyManager.getAuctionManager();
            AuctionManager.AuctionResult result = auctionManager.createAuction(
                player, itemStack.copy(), startPrice, buyNowPrice, duration, description
            );
            
            if (result.isSuccess()) {
                // Remove item from player's inventory
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                
                source.sendSuccess(() -> Component.literal("§aAuction created successfully!"), false);
                source.sendSuccess(() -> Component.literal("§7Auction ID: §e" + result.getAuction().getId()), false);
                source.sendSuccess(() -> Component.literal("§7Starting price: §a" + economyManager.formatCurrency(startPrice)), false);
                if (buyNowPrice != null) {
                    final BigDecimal finalBuyNowPrice = buyNowPrice;
                    source.sendSuccess(() -> Component.literal("§7Buy now price: §a" + economyManager.formatCurrency(finalBuyNowPrice)), false);
                }
                source.sendSuccess(() -> Component.literal("§7Duration: §e" + duration + " hours"), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to create auction: " + result.getMessage()));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction create command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while creating auction"));
            return 0;
        }
    }
    
    private static int bidOnAuction(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can bid on auctions"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            String auctionIdStr = StringArgumentType.getString(context, "auctionId");
            String amountStr = StringArgumentType.getString(context, "amount");
            
            // Parse auction ID
            UUID auctionId;
            try {
                auctionId = UUID.fromString(auctionIdStr);
            } catch (IllegalArgumentException e) {
                // Try to find by partial ID
                List<AuctionItem> auctions = economyManager.getAuctionManager().getActiveAuctions();
                AuctionItem found = null;
                for (AuctionItem auction : auctions) {
                    if (auction.getId().toString().startsWith(auctionIdStr)) {
                        if (found != null) {
                            source.sendFailure(Component.literal("Multiple auctions match that ID. Please be more specific."));
                            return 0;
                        }
                        found = auction;
                    }
                }
                if (found == null) {
                    source.sendFailure(Component.literal("No auction found with that ID"));
                    return 0;
                }
                auctionId = found.getId();
            }
            
            // Parse bid amount
            BigDecimal bidAmount;
            try {
                bidAmount = new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("Invalid bid amount format"));
                return 0;
            }
            
            // Place bid
            AuctionManager auctionManager = economyManager.getAuctionManager();
            AuctionManager.BidResult result = auctionManager.placeBid(player, auctionId, bidAmount);
            
            if (result.isSuccess()) {
                source.sendSuccess(() -> Component.literal("§aBid placed successfully!"), false);
                source.sendSuccess(() -> Component.literal("§7Your bid: §a" + economyManager.formatCurrency(bidAmount)), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to place bid: " + result.getMessage()));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction bid command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while placing bid"));
            return 0;
        }
    }
    
    private static int buyNowAuction(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can buy auctions"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            String auctionIdStr = StringArgumentType.getString(context, "auctionId");
            
            // Parse auction ID
            UUID auctionId;
            try {
                auctionId = UUID.fromString(auctionIdStr);
            } catch (IllegalArgumentException e) {
                // Try to find by partial ID
                List<AuctionItem> auctions = economyManager.getAuctionManager().getActiveAuctions();
                AuctionItem found = null;
                for (AuctionItem auction : auctions) {
                    if (auction.getId().toString().startsWith(auctionIdStr)) {
                        if (found != null) {
                            source.sendFailure(Component.literal("Multiple auctions match that ID. Please be more specific."));
                            return 0;
                        }
                        found = auction;
                    }
                }
                if (found == null) {
                    source.sendFailure(Component.literal("No auction found with that ID"));
                    return 0;
                }
                auctionId = found.getId();
            }
            
            // Buy now
            AuctionManager auctionManager = economyManager.getAuctionManager();
            AuctionManager.BuyNowResult result = auctionManager.buyNow(player, auctionId);
            
            if (result.isSuccess()) {
                source.sendSuccess(() -> Component.literal("§aAuction purchased successfully!"), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to purchase auction: " + result.getMessage()));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction buy now command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while purchasing auction"));
            return 0;
        }
    }
    
    private static int cancelAuction(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can cancel auctions"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            String auctionIdStr = StringArgumentType.getString(context, "auctionId");
            
            // Parse auction ID
            UUID auctionId;
            try {
                auctionId = UUID.fromString(auctionIdStr);
            } catch (IllegalArgumentException e) {
                // Try to find by partial ID
                List<AuctionItem> auctions = economyManager.getAuctionManager().getPlayerAuctions(player.getUUID());
                AuctionItem found = null;
                for (AuctionItem auction : auctions) {
                    if (auction.getId().toString().startsWith(auctionIdStr)) {
                        if (found != null) {
                            source.sendFailure(Component.literal("Multiple auctions match that ID. Please be more specific."));
                            return 0;
                        }
                        found = auction;
                    }
                }
                if (found == null) {
                    source.sendFailure(Component.literal("No auction found with that ID"));
                    return 0;
                }
                auctionId = found.getId();
            }
            
            // Cancel auction
            AuctionManager auctionManager = economyManager.getAuctionManager();
            AuctionManager.AuctionResult result = auctionManager.cancelAuction(player, auctionId, false);
            
            if (result.isSuccess()) {
                source.sendSuccess(() -> Component.literal("§aAuction cancelled successfully!"), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cFailed to cancel auction: " + result.getMessage()));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction cancel command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while cancelling auction"));
            return 0;
        }
    }
    
    private static int showAuctionInfo(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            String auctionIdStr = StringArgumentType.getString(context, "auctionId");
            
            // Parse auction ID
            UUID auctionId;
            try {
                auctionId = UUID.fromString(auctionIdStr);
            } catch (IllegalArgumentException e) {
                // Try to find by partial ID
                List<AuctionItem> auctions = economyManager.getAuctionManager().getActiveAuctions();
                AuctionItem found = null;
                for (AuctionItem auction : auctions) {
                    if (auction.getId().toString().startsWith(auctionIdStr)) {
                        if (found != null) {
                            source.sendFailure(Component.literal("Multiple auctions match that ID. Please be more specific."));
                            return 0;
                        }
                        found = auction;
                    }
                }
                if (found == null) {
                    source.sendFailure(Component.literal("No auction found with that ID"));
                    return 0;
                }
                auctionId = found.getId();
            }
            
            // Get auction info
            AuctionItem auction = economyManager.getAuctionManager().getAuction(auctionId);
            if (auction == null) {
                source.sendFailure(Component.literal("Auction not found"));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§6=== Auction Information ==="), false);
            source.sendSuccess(() -> Component.literal("§7ID: §e" + auction.getId()), false);
            source.sendSuccess(() -> Component.literal("§7Item: §b" + auction.getItemStack().getHoverName().getString() + " §7x" + auction.getItemStack().getCount()), false);
            source.sendSuccess(() -> Component.literal("§7Seller: §a" + auction.getSellerName()), false);
            source.sendSuccess(() -> Component.literal("§7Starting Price: §a" + economyManager.formatCurrency(auction.getStartingPrice())), false);
            source.sendSuccess(() -> Component.literal("§7Current Price: §a" + economyManager.formatCurrency(auction.getCurrentBid())), false);
            
            if (auction.getBuyNowPrice() != null) {
                source.sendSuccess(() -> Component.literal("§7Buy Now Price: §a" + economyManager.formatCurrency(auction.getBuyNowPrice())), false);
            }
            
            if (auction.getHighestBidder() != null) {
                source.sendSuccess(() -> Component.literal("§7Highest Bidder: §a" + auction.getHighestBidderName()), false);
            }
            
            String timeLeft = formatTimeLeft(auction.getTimeRemainingMinutes() * 60);
            source.sendSuccess(() -> Component.literal("§7Time Left: §e" + timeLeft), false);
            
            if (auction.getDescription() != null && !auction.getDescription().isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7Description: §f" + auction.getDescription()), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction info command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while showing auction info"));
            return 0;
        }
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            AuctionManager auctionManager = economyManager.getAuctionManager();
            AuctionManager.AuctionStatistics stats = auctionManager.getGlobalStatistics();
            
            source.sendSuccess(() -> Component.literal("§6=== Auction Statistics ==="), false);
            source.sendSuccess(() -> Component.literal("§7Total Auctions: §e" + stats.getTotalAuctions()), false);
            source.sendSuccess(() -> Component.literal("§7Active Auctions: §e" + stats.getActiveAuctions()), false);
            source.sendSuccess(() -> Component.literal("§7Completed Auctions: §e" + stats.getCompletedAuctions()), false);
            source.sendSuccess(() -> Component.literal("§7Total Value Traded: §a" + economyManager.formatCurrency(stats.getTotalValue())), false);
            source.sendSuccess(() -> Component.literal("§7Average Auction Value: §a" + economyManager.formatCurrency(stats.getAverageValue())), false);
            
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction stats command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while showing stats"));
            return 0;
        }
    }
    
    private static int showPlayerStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String playerName = StringArgumentType.getString(context, "player");
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            // TODO: Get player UUID from name
            // For now, just show a placeholder message
            source.sendFailure(Component.literal("Player statistics not implemented yet"));
            return 0;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction player stats command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while showing player stats"));
            return 0;
        }
    }
    
    // Admin commands
    private static int forceEndAuction(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String auctionIdStr = StringArgumentType.getString(context, "auctionId");
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            UUID auctionId;
            try {
                auctionId = UUID.fromString(auctionIdStr);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Invalid auction ID format"));
                return 0;
            }
            
            AuctionManager auctionManager = economyManager.getAuctionManager();
            boolean success = auctionManager.forceEndAuction(auctionId);
            
            if (success) {
                source.sendSuccess(() -> Component.literal("§aAuction force-ended successfully"), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("Failed to force-end auction"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction force-end command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while force-ending auction"));
            return 0;
        }
    }
    
    private static int cleanupExpiredAuctions(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            AuctionManager auctionManager = economyManager.getAuctionManager();
            int cleaned = auctionManager.cleanupExpiredAuctions();
            
            source.sendSuccess(() -> Component.literal("§aCleaned up " + cleaned + " expired auctions"), false);
            return cleaned;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction cleanup command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while cleaning up auctions"));
            return 0;
        }
    }
    
    private static int showAdminStats(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            AuctionManager auctionManager = economyManager.getAuctionManager();
            AuctionManager.AuctionStatistics stats = auctionManager.getGlobalStatistics();
            
            source.sendSuccess(() -> Component.literal("§6=== Admin Auction Statistics ==="), false);
            source.sendSuccess(() -> Component.literal("§7Total Auctions: §e" + stats.getTotalAuctions()), false);
            source.sendSuccess(() -> Component.literal("§7Active Auctions: §e" + stats.getActiveAuctions()), false);
            source.sendSuccess(() -> Component.literal("§7Completed Auctions: §e" + stats.getCompletedAuctions()), false);
            source.sendSuccess(() -> Component.literal("§7Total Value Traded: §a" + economyManager.formatCurrency(stats.getTotalValue())), false);
            source.sendSuccess(() -> Component.literal("§7Average Auction Value: §a" + economyManager.formatCurrency(stats.getAverageValue())), false);
            
            // Additional admin-specific stats
            List<AuctionItem> activeAuctions = auctionManager.getActiveAuctions();
            List<AuctionItem> completedAuctions = auctionManager.getCompletedAuctions();
            
            source.sendSuccess(() -> Component.literal("§7Memory Usage:"), false);
            source.sendSuccess(() -> Component.literal("§7  Active in Memory: §e" + activeAuctions.size()), false);
            source.sendSuccess(() -> Component.literal("§7  Completed in Memory: §e" + completedAuctions.size()), false);
            
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in auction admin stats command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while showing admin stats"));
            return 0;
        }
    }
    
    private static String formatTimeLeft(long seconds) {
        if (seconds <= 0) {
            return "Expired";
        }
        
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
