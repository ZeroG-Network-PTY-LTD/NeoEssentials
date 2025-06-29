package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles all auction-related commands for the NeoEssentials economy system.
 * 
 * Commands:
 * - /auction create <type> <item> <starting-price> [duration] - Create an auction
 * - /auction list [type] - List active auctions
 * - /auction info <auction-id> - Get auction information
 * - /auction bid <auction-id> <amount> - Place a bid on an auction
 * - /auction buyout <auction-id> - Buy auction immediately (if buy-it-now)
 * - /auction cancel <auction-id> - Cancel your auction
 * - /auction history [player] - View auction history
 * - /auction search <item> - Search for specific item auctions
 * - /auction watch <auction-id> - Watch an auction for updates
 * - /auction unwatch <auction-id> - Stop watching an auction
 */
public class AuctionCommands {
    
    /**
     * Registers all auction commands with the command dispatcher.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Main /auction command with subcommands
        dispatcher.register(
            Commands.literal("auction")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction"))
                .executes(context -> {
                    // Show auction help when no subcommand is provided
                    return showAuctionHelp(context.getSource());
                })
                
                // /auction create <type> <item> <starting-price> [duration]
                .then(Commands.literal("create")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.create"))
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("standard");
                            builder.suggest("buyitnow");
                            builder.suggest("reserve");
                            builder.suggest("dutch");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("item", StringArgumentType.string())
                            .then(Commands.argument("starting_price", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String type = StringArgumentType.getString(context, "type");
                                    String item = StringArgumentType.getString(context, "item");
                                    double startingPrice = DoubleArgumentType.getDouble(context, "starting_price");
                                    return createAuction(player, type, item, startingPrice, 86400); // 24 hours default
                                })
                                .then(Commands.argument("duration", IntegerArgumentType.integer(300, 604800)) // 5 min to 1 week
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        String type = StringArgumentType.getString(context, "type");
                                        String item = StringArgumentType.getString(context, "item");
                                        double startingPrice = DoubleArgumentType.getDouble(context, "starting_price");
                                        int duration = IntegerArgumentType.getInteger(context, "duration");
                                        return createAuction(player, type, item, startingPrice, duration);
                                    })
                                )
                            )
                        )
                    )
                )
                
                // /auction list [type]
                .then(Commands.literal("list")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.list"))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return listAuctions(player, null);
                    })
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("all");
                            builder.suggest("standard");
                            builder.suggest("buyitnow");
                            builder.suggest("reserve");
                            builder.suggest("dutch");
                            builder.suggest("ending");
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String type = StringArgumentType.getString(context, "type");
                            return listAuctions(player, type);
                        })
                    )
                )
                
                // /auction info <auction-id>
                .then(Commands.literal("info")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.info"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String auctionId = StringArgumentType.getString(context, "auction_id");
                            return showAuctionInfo(player, auctionId);
                        })
                    )
                )
                
                // /auction bid <auction-id> <amount>
                .then(Commands.literal("bid")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.bid"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                String auctionId = StringArgumentType.getString(context, "auction_id");
                                double amount = DoubleArgumentType.getDouble(context, "amount");
                                return placeBid(player, auctionId, amount);
                            })
                        )
                    )
                )
                
                // /auction buyout <auction-id>
                .then(Commands.literal("buyout")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.buyout"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String auctionId = StringArgumentType.getString(context, "auction_id");
                            return buyoutAuction(player, auctionId);
                        })
                    )
                )
                
                // /auction cancel <auction-id>
                .then(Commands.literal("cancel")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.cancel"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String auctionId = StringArgumentType.getString(context, "auction_id");
                            return cancelAuction(player, auctionId);
                        })
                    )
                )
                
                // /auction history [player]
                .then(Commands.literal("history")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.history"))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return showAuctionHistory(player, null);
                    })
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.history.others"))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            return showAuctionHistory(player, target);
                        })
                    )
                )
                
                // /auction search <item>
                .then(Commands.literal("search")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.search"))
                    .then(Commands.argument("item", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String item = StringArgumentType.getString(context, "item");
                            return searchAuctions(player, item);
                        })
                    )
                )
                
                // /auction watch <auction-id>
                .then(Commands.literal("watch")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.watch"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String auctionId = StringArgumentType.getString(context, "auction_id");
                            return watchAuction(player, auctionId);
                        })
                    )
                )
                
                // /auction unwatch <auction-id>
                .then(Commands.literal("unwatch")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.watch"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String auctionId = StringArgumentType.getString(context, "auction_id");
                            return unwatchAuction(player, auctionId);
                        })
                    )
                )
        );
        
        // Add alias commands
        dispatcher.register(
            Commands.literal("auc")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction"))
                .redirect(dispatcher.getRoot().getChild("auction"))
        );
    }
    
    // Auction command implementations
    
    private int showAuctionHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== NeoEssentials Auction House ===");
            MessageUtil.sendMessage(player, "§e/auction create <type> <item> <price> [duration] §7- Create auction");
            MessageUtil.sendMessage(player, "§e/auction list [type] §7- List active auctions");
            MessageUtil.sendMessage(player, "§e/auction info <id> §7- Get auction information");
            MessageUtil.sendMessage(player, "§e/auction bid <id> <amount> §7- Place a bid");
            MessageUtil.sendMessage(player, "§e/auction buyout <id> §7- Buy immediately");
            MessageUtil.sendMessage(player, "§e/auction cancel <id> §7- Cancel your auction");
            MessageUtil.sendMessage(player, "§e/auction history [player] §7- View auction history");
            MessageUtil.sendMessage(player, "§e/auction search <item> §7- Search for items");
            MessageUtil.sendMessage(player, "§e/auction watch <id> §7- Watch auction for updates");
            MessageUtil.sendMessage(player, "§e/auction unwatch <id> §7- Stop watching auction");
            MessageUtil.sendMessage(player, "§7Auction Types: standard, buyitnow, reserve, dutch");
            MessageUtil.sendMessage(player, "§7Alias: §e/auc§7 can be used instead of §e/auction");
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Command can only be used by players."));
            return 0;
        }
    }
    
    private int createAuction(ServerPlayer player, String typeStr, String itemName, double startingPrice, int duration) {
        try {
            Auction.AuctionType type = Auction.AuctionType.valueOf(typeStr.toUpperCase().replace("BUYITNOW", "BUY_IT_NOW"));
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            
            // Create the auction (using proper constructor parameters)
            Auction auction = auctionHouse.createAuction(player.getUUID(), itemName, itemName, 1, 
                startingPrice, duration * 1000L); // Convert seconds to milliseconds
            
            if (auction != null) {
                String auctionId = auction.getAuctionId().toString().substring(0, 8);
                MessageUtil.sendMessage(player, "§aSuccessfully created " + type.name().toLowerCase().replace("_", "-") + 
                    " auction for §e" + itemName);
                MessageUtil.sendMessage(player, "§7Auction ID: §e" + auctionId);
                MessageUtil.sendMessage(player, "§7Starting Price: §e" + defaultCurrency.format(startingPrice));
                MessageUtil.sendMessage(player, "§7Duration: §e" + formatDuration(duration));
                MessageUtil.sendMessage(player, "§7Players can now bid using: §e/auction bid " + auctionId + " <amount>");
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to create auction. Please try again.");
                return 0;
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendErrorMessage(player, "Invalid auction type. Valid types: standard, buyitnow, reserve, dutch");
            return 0;
        }
    }
    
    private int listAuctions(ServerPlayer player, String typeFilter) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            List<Auction> auctions = auctionHouse.getActiveAuctions();
            
            if (typeFilter != null && !typeFilter.equalsIgnoreCase("all")) {
                // Filter by type
                String filterUpper = typeFilter.toUpperCase().replace("BUYITNOW", "BUY_IT_NOW");
                if (filterUpper.equals("ENDING")) {
                    // Show auctions ending soon (within 1 hour)
                    long oneHour = 3600;
                    auctions = auctions.stream()
                        .filter(auction -> auction.getTimeRemaining() <= oneHour)
                        .toList();
                } else {
                    try {
                        Auction.AuctionType type = Auction.AuctionType.valueOf(filterUpper);
                        auctions = auctions.stream()
                            .filter(auction -> auction.getAuctionType() == type)
                            .toList();
                    } catch (IllegalArgumentException e) {
                        MessageUtil.sendErrorMessage(player, "Invalid auction type filter.");
                        return 0;
                    }
                }
            }
            
            if (auctions.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No active auctions found.");
                return 1;
            }
            
            String title = typeFilter != null ? "=== " + typeFilter.toUpperCase() + " Auctions ===" : "=== Active Auctions ===";
            MessageUtil.sendMessage(player, "§6" + title);
            
            for (Auction auction : auctions.stream().limit(10).toList()) {
                Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
                String timeLeft = formatDuration((int) auction.getTimeRemaining());
                String currentBid = auction.getCurrentBid() > 0 ? 
                    defaultCurrency.format(auction.getCurrentBid()) : 
                    defaultCurrency.format(auction.getStartingBid());
                
                String auctionId = auction.getAuctionId().toString().substring(0, 8);
                MessageUtil.sendMessage(player, "§e" + auctionId + " §7- §e" + auction.getItemName() + 
                    " §7- §a" + currentBid + " §7- §e" + timeLeft + " §8[" + auction.getAuctionType().name() + "]");
            }
            
            if (auctions.size() > 10) {
                MessageUtil.sendMessage(player, "§7... and " + (auctions.size() - 10) + " more auctions.");
            }
            
            MessageUtil.sendMessage(player, "§7Use §e/auction info <id>§7 for detailed information.");
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while listing auctions: " + e.getMessage());
            return 0;
        }
    }
    
    private int showAuctionInfo(ServerPlayer player, String auctionId) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            Auction auction = auctionHouse.getAuctionById(auctionUUID);
            if (auction == null) {
                MessageUtil.sendErrorMessage(player, "Auction not found.");
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§6=== Auction Info ===");
            MessageUtil.sendMessage(player, "§7ID: §e" + auction.getAuctionId());
            MessageUtil.sendMessage(player, "§7Item: §e" + auction.getItemName() + " §7x" + auction.getQuantity());
            MessageUtil.sendMessage(player, "§7Current Bid: §e" + auction.getCurrentBid());
            MessageUtil.sendMessage(player, "§7Status: §e" + auction.getStatus());
            MessageUtil.sendMessage(player, "§7Time Remaining: §e" + formatTimeRemaining(auction.getEndTime()));
            
            if (auction.getCurrentBidder() != null) {
                MessageUtil.sendMessage(player, "§7Current Bidder: §e" + auction.getCurrentBidder());
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while retrieving auction info: " + e.getMessage());
            return 0;
        }
    }
    
    private int placeBid(ServerPlayer player, String auctionId, double amount) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            Auction auction = auctionHouse.getAuctionById(auctionUUID);
            if (auction == null) {
                MessageUtil.sendErrorMessage(player, "Auction not found.");
                return 0;
            }
            
            if (!auction.isActive()) {
                MessageUtil.sendErrorMessage(player, "This auction has ended.");
                return 0;
            }
            
            if (auction.getSellerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "You cannot bid on your own auction.");
                return 0;
            }
            
            // Check if player has enough balance
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            if (economyManager.getBalance(player.getUUID(), defaultCurrency) < amount) {
                MessageUtil.sendErrorMessage(player, "You don't have enough money to place this bid.");
                return 0;
            }
            
            // Place the bid
            if (auction.placeBid(player.getUUID(), amount)) {
                MessageUtil.sendMessage(player, "§aBid placed successfully!");
                MessageUtil.sendMessage(player, "§7Your bid of §e" + amount + "§7 is now the highest bid.");
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to place bid. Your bid may be too low.");
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while placing bid: " + e.getMessage());
            return 0;
        }
    }
    
    private int buyoutAuction(ServerPlayer player, String auctionId) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            WalletManager walletManager = economyManager.getWalletManager();
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            Auction auction = auctionHouse.getAuctionById(auctionUUID);
            if (auction == null) {
                MessageUtil.sendErrorMessage(player, "Auction not found.");
                return 0;
            }
            
            if (!auction.isActive()) {
                MessageUtil.sendErrorMessage(player, "This auction has ended.");
                return 0;
            }
            
            if (auction.getSellerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "You cannot buy your own auction.");
                return 0;
            }
            
            // Check if auction supports buyout (buy-it-now type)
            if (auction.getAuctionType() != Auction.AuctionType.BUY_IT_NOW) {
                MessageUtil.sendErrorMessage(player, "This auction does not support immediate purchase. Use /auction bid instead.");
                return 0;
            }
            
            double buyoutPrice = auction.getBuyoutPrice();
            if (buyoutPrice <= 0) {
                MessageUtil.sendErrorMessage(player, "This auction does not have a buyout price set.");
                return 0;
            }
            
            // Check if player has enough money
            if (!walletManager.hasCash(player.getUUID(), defaultCurrency, buyoutPrice)) {
                MessageUtil.sendErrorMessage(player, "You don't have enough money. Required: " + 
                    defaultCurrency.format(buyoutPrice));
                return 0;
            }
            
            // Process the buyout
            if (walletManager.subtractCash(player.getUUID(), defaultCurrency, buyoutPrice)) {
                // Pay the seller
                walletManager.addCash(auction.getSellerId(), defaultCurrency, buyoutPrice);
                
                // Complete the auction
                auction.setWinnerId(player.getUUID());
                auction.setCurrentBid(buyoutPrice);
                auction.completeAuction();
                
                MessageUtil.sendMessage(player, "§aSuccessfully purchased §e" + auction.getItemName() + 
                    "§a for §e" + defaultCurrency.format(buyoutPrice));
                MessageUtil.sendMessage(player, "§7Auction ID: §e" + auctionId);
                MessageUtil.sendMessage(player, "§7The item has been delivered to your inventory.");
                
                // Notify seller if online
                if (player.getServer() != null) {
                    ServerPlayer seller = player.getServer().getPlayerList().getPlayer(auction.getSellerId());
                    if (seller != null) {
                        MessageUtil.sendMessage(seller, "§aYour auction for §e" + auction.getItemName() + 
                            "§a was purchased by §e" + player.getScoreboardName());
                        MessageUtil.sendMessage(seller, "§aYou received §e" + defaultCurrency.format(buyoutPrice));
                    }
                }
                
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Payment failed. Please try again.");
                return 0;
            }
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred during buyout: " + e.getMessage());
            return 0;
        }
    }
    
    private int cancelAuction(ServerPlayer player, String auctionId) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            Auction auction = auctionHouse.getAuctionById(auctionUUID);
            if (auction == null) {
                MessageUtil.sendErrorMessage(player, "Auction not found.");
                return 0;
            }
            
            if (!auction.getSellerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "You can only cancel your own auctions.");
                return 0;
            }
            
            if (!auction.isActive()) {
                MessageUtil.sendErrorMessage(player, "This auction has already ended.");
                return 0;
            }
            
            if (auction.cancelAuction()) {
                MessageUtil.sendMessage(player, "§aAuction cancelled successfully!");
                MessageUtil.sendMessage(player, "§7Your item has been returned and any bids have been refunded.");
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to cancel auction.");
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while cancelling auction: " + e.getMessage());
            return 0;
        }
    }
    
    private int showAuctionHistory(ServerPlayer player, ServerPlayer target) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            UUID targetId = target != null ? target.getUUID() : player.getUUID();
            String targetName = target != null ? target.getScoreboardName() : "Your";
            
            // Get all auctions by the target player (both active and completed)
            List<Auction> playerAuctions = auctionHouse.getAuctionsBySeller(targetId);
            
            MessageUtil.sendMessage(player, "§6=== " + targetName + " Auction History ===");
            
            if (playerAuctions.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No auction history found.");
                return 1;
            }
            
            // Sort by start time (newest first)
            playerAuctions.sort((a1, a2) -> Long.compare(a2.getStartTime(), a1.getStartTime()));
            
            // Show up to 10 most recent auctions
            int count = 0;
            for (Auction auction : playerAuctions) {
                if (count >= 10) break;
                
                String statusColor = switch (auction.getStatus()) {
                    case ACTIVE -> "§a";
                    case COMPLETED -> "§e";
                    case CANCELLED -> "§c";
                    case EXPIRED -> "§7";
                };
                
                String auctionId = auction.getAuctionId().toString().substring(0, 8);
                String price = auction.getStatus() == Auction.AuctionStatus.COMPLETED ? 
                    auction.getCurrency().format(auction.getCurrentBid()) : 
                    auction.getCurrency().format(auction.getStartingBid());
                
                MessageUtil.sendMessage(player, statusColor + auctionId + " §7- §e" + auction.getItemName() + 
                    " §7- " + statusColor + price + " §7- " + statusColor + auction.getStatus().name());
                
                count++;
            }
            
            if (playerAuctions.size() > 10) {
                MessageUtil.sendMessage(player, "§7Showing 10 of " + playerAuctions.size() + " auctions");
            }
            
            MessageUtil.sendMessage(player, "§7Use §e/auction info <id>§7 for detailed information");
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while retrieving auction history: " + e.getMessage());
            return 0;
        }
    }
    
    private int searchAuctions(ServerPlayer player, String itemQuery) {
        try {
            EconomyManager economyManager = com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            // Use the searchAuctions method from AuctionHouse
            List<Auction> matchingAuctions = auctionHouse.searchAuctions(itemQuery);
            
            if (matchingAuctions.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No auctions found for '" + itemQuery + "'");
                return 1;
            }
            
            MessageUtil.sendMessage(player, "§6=== Search Results for '" + itemQuery + "' ===");
            
            for (Auction auction : matchingAuctions.stream().limit(10).toList()) {
                Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
                String timeLeft = formatTimeRemaining(auction.getEndTime());
                String currentBid = auction.getCurrentBid() > 0 ? 
                    defaultCurrency.format(auction.getCurrentBid()) : 
                    defaultCurrency.format(auction.getStartingBid());
                
                String auctionId = auction.getAuctionId().toString().substring(0, 8);
                MessageUtil.sendMessage(player, "§e" + auctionId + " §7- §e" + auction.getItemName() + 
                    " §7- §a" + currentBid + " §7- §e" + timeLeft);
            }
            
            if (matchingAuctions.size() > 10) {
                MessageUtil.sendMessage(player, "§7... and " + (matchingAuctions.size() - 10) + " more matches.");
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while searching auctions: " + e.getMessage());
            return 0;
        }
    }
    
    private int watchAuction(ServerPlayer player, String auctionId) {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager.AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            AuctionNotificationManager notificationManager = AuctionNotificationManager.getInstance();
            
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            Auction auction = auctionHouse.getAuctionById(auctionUUID);
            if (auction == null) {
                MessageUtil.sendErrorMessage(player, "Auction not found.");
                return 0;
            }
            
            if (!auction.isActive()) {
                MessageUtil.sendErrorMessage(player, "This auction has already ended.");
                return 0;
            }
            
            UUID playerId = player.getUUID();
            
            // Check if already watching
            if (notificationManager.isWatching(playerId, auctionUUID)) {
                MessageUtil.sendErrorMessage(player, "You are already watching this auction.");
                return 0;
            }
            
            // Add to watch list using notification manager
            notificationManager.addWatcher(playerId, auctionUUID);
            
            MessageUtil.sendMessage(player, "§aYou are now watching auction §e" + auctionId);
            MessageUtil.sendMessage(player, "§7Item: §e" + auction.getItemName());
            MessageUtil.sendMessage(player, "§7Current bid: §e" + auction.getCurrency().format(auction.getCurrentBid()));
            MessageUtil.sendMessage(player, "§7Time remaining: §e" + auction.getFormattedTimeRemaining());
            MessageUtil.sendMessage(player, "§7You'll receive notifications when this auction ends or is outbid.");
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while adding auction to watch list: " + e.getMessage());
            return 0;
        }
    }
    
    private int unwatchAuction(ServerPlayer player, String auctionId) {
        try {
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            UUID playerId = player.getUUID();
            Set<UUID> playerWatchList = watchLists.get(playerId);
            
            if (playerWatchList == null || !playerWatchList.contains(auctionUUID)) {
                MessageUtil.sendErrorMessage(player, "You are not watching this auction.");
                return 0;
            }
            
            playerWatchList.remove(auctionUUID);
            if (playerWatchList.isEmpty()) {
                watchLists.remove(playerId);
            }
            
            MessageUtil.sendMessage(player, "§aYou are no longer watching auction §e" + auctionId);
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while removing auction from watch list: " + e.getMessage());
            return 0;
        }
    }
    
    // Helper methods
    
    private String formatTimeRemaining(long endTime) {
        long currentTime = System.currentTimeMillis();
        long timeRemaining = endTime - currentTime;
        
        if (timeRemaining <= 0) {
            return "Ended";
        }
        
        int seconds = (int) (timeRemaining / 1000);
        return formatDuration(seconds);
    }
    
    private String formatDuration(int seconds) {
        if (seconds <= 0) return "Ended";
        
        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m " + (seconds % 60) + "s";
        }
    }
}
