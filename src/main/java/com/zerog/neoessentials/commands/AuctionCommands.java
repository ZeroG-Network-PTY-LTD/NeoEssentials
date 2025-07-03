package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.economy.ShopManager.AuctionHouse;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS) // Add suggestion provider here
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
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS) // Add suggestion provider here
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
                
                // /auction autobid <auction-id> <max-amount> - Set up automatic bidding
                .then(Commands.literal("autobid")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.autobid"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .then(Commands.argument("max_amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                String auctionId = StringArgumentType.getString(context, "auction_id");
                                double maxAmount = DoubleArgumentType.getDouble(context, "max_amount");
                                return setAutoBid(player, auctionId, maxAmount);
                            })
                            .then(Commands.argument("increment", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String auctionId = StringArgumentType.getString(context, "auction_id");
                                    double maxAmount = DoubleArgumentType.getDouble(context, "max_amount");
                                    double increment = DoubleArgumentType.getDouble(context, "increment");
                                    return setAutoBidWithIncrement(player, auctionId, maxAmount, increment);
                                })
                            )
                        )
                    )
                )
                
                // /auction autocancel <auction-id> - Cancel automatic bidding
                .then(Commands.literal("autocancel")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.autobid"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String auctionId = StringArgumentType.getString(context, "auction_id");
                            return cancelAutoBid(player, auctionId);
                        })
                    )
                )
                
                // /auction autolist - List your active auto-bids
                .then(Commands.literal("autolist")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.auction.autobid"))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return listAutoBids(player);
                    })
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
                String auctionTypeDisplay = getAuctionTypeDisplayName(auction.getAuctionType());
                MessageUtil.sendMessage(player, "§e" + auctionId + " §7- §e" + auction.getItemName() + 
                    " §7- §a" + currentBid + " §7- §e" + timeLeft + " §8[" + auctionTypeDisplay + "]");
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
            
            // Check if player has enough total available funds
            Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
            double totalAvailable = economyManager.getTotalAvailableFunds(player.getUUID(), defaultCurrency);
            if (totalAvailable < amount) {
                MessageUtil.sendErrorMessage(player, "You don't have enough money to place this bid.");
                MessageUtil.sendMessage(player, "§7Required: §e" + defaultCurrency.format(amount));
                MessageUtil.sendMessage(player, "§7Available: §e" + defaultCurrency.format(totalAvailable) + " §7(wallet + bank)");
                return 0;
            }
            
            // Place the bid
            if (auction.placeBid(player.getUUID(), amount)) {
                MessageUtil.sendMessage(player, "§aBid placed successfully!");
                MessageUtil.sendMessage(player, "§7Your bid of §e" + amount + "§7 is now the highest bid.");
                
                // Send notifications to watchers
                if (player.getServer() != null) {
                    AuctionNotificationManager.getInstance().notifyNewBid(auction, player, amount, player.getServer());
                }
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to place bid. Your bid may be too low.");
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while placing bid: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Set up automatic bidding for an auction
     */
    private int setAutoBid(ServerPlayer player, String auctionId, double maxAmount) {
        return setAutoBidWithIncrement(player, auctionId, maxAmount, 1.0); // Default increment of $1
    }
    
    /**
     * Set up automatic bidding for an auction with custom increment
     */
    private int setAutoBidWithIncrement(ServerPlayer player, String auctionId, double maxAmount, double increment) {
        try {
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            // Parse auction ID
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            // Find the auction
            Auction auction = auctionHouse.getAuctionById(auctionUUID);
            if (auction == null) {
                MessageUtil.sendErrorMessage(player, "Auction not found.");
                return 0;
            }
            
            // Check if auction is still active
            if (!auction.isActive()) {
                MessageUtil.sendErrorMessage(player, "This auction has ended.");
                return 0;
            }
            
            // Check if player is the seller
            if (auction.getSellerId().equals(player.getUUID())) {
                MessageUtil.sendErrorMessage(player, "You cannot bid on your own auction.");
                return 0;
            }
            
            // Validate maximum amount
            if (maxAmount <= auction.getCurrentBid()) {
                MessageUtil.sendErrorMessage(player, "Maximum bid amount must be higher than current bid of " + 
                    economyManager.getCurrencyManager().getDefaultCurrency().format(auction.getCurrentBid()));
                return 0;
            }
            
            // Check if player has sufficient funds for maximum bid
            WalletManager walletManager = economyManager.getWalletManager();
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            double totalAvailable = walletManager.getCashBalance(player.getUUID(), defaultCurrency) + 
                                    economyManager.getBankManager().getTotalPlayerBalance(player.getUUID(), defaultCurrency);
            
            if (totalAvailable < maxAmount) {
                MessageUtil.sendErrorMessage(player, "Insufficient funds for maximum bid amount.");
                MessageUtil.sendMessage(player, "§7Available: §e" + defaultCurrency.format(totalAvailable));
                MessageUtil.sendMessage(player, "§7Required: §e" + defaultCurrency.format(maxAmount));
                return 0;
            }
            
            // Set up auto-bidding
            boolean success = auctionHouse.setAutoBid(auctionUUID, player.getUUID(), maxAmount, increment);
            
            if (success) {
                MessageUtil.sendSuccessMessage(player, "Auto-bidding set up successfully!");
                MessageUtil.sendMessage(player, "§7Auction: §e" + auction.getItemName());
                MessageUtil.sendMessage(player, "§7Maximum bid: §e" + defaultCurrency.format(maxAmount));
                MessageUtil.sendMessage(player, "§7Increment: §e" + defaultCurrency.format(increment));
                MessageUtil.sendMessage(player, "§7The system will automatically bid for you when outbid, up to your maximum.");
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Auto-bidding feature is not yet implemented. Please check back in a future update.");
                return 0;
            }
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while setting up auto-bidding: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Cancel automatic bidding for an auction
     */
    private int cancelAutoBid(ServerPlayer player, String auctionId) {
        try {
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            // Parse auction ID
            UUID auctionUUID;
            try {
                auctionUUID = UUID.fromString(auctionId);
            } catch (IllegalArgumentException e) {
                MessageUtil.sendErrorMessage(player, "Invalid auction ID format.");
                return 0;
            }
            
            // Cancel auto-bidding
            boolean success = auctionHouse.cancelAutoBid(auctionUUID, player.getUUID());
            
            if (success) {
                MessageUtil.sendSuccessMessage(player, "Auto-bidding cancelled successfully!");
                MessageUtil.sendMessage(player, "§7You will no longer automatically bid on this auction.");
                return 1;
            } else {
                MessageUtil.sendErrorMessage(player, "Auto-bidding feature is not yet implemented. Please check back in a future update.");
                return 0;
            }
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while cancelling auto-bidding: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * List all active auto-bids for the player
     */
    private int listAutoBids(ServerPlayer player) {
        try {
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            AuctionHouse auctionHouse = economyManager.getShopManager().getAuctionHouse();
            
            // Get player's auto-bids
            List<AutoBid> autoBids = auctionHouse.getAutoBidsForPlayer(player.getUUID());
            
            if (autoBids.isEmpty()) {
                MessageUtil.sendMessage(player, "§6=== Your Auto-Bids ===");
                MessageUtil.sendMessage(player, "§7You have no active auto-bids.");
                return 1;
            }
            
            Currency defaultCurrency = economyManager.getCurrencyManager().getDefaultCurrency();
            MessageUtil.sendMessage(player, "§6=== Your Auto-Bids ===");
            
            for (AutoBid autoBid : autoBids) {
                Auction auction = auctionHouse.getAuctionById(autoBid.getAuctionId());
                if (auction != null && auction.isActive()) {
                    MessageUtil.sendMessage(player, "§e" + auction.getItemName() + " §7(ID: " + 
                        autoBid.getAuctionId().toString().substring(0, 8) + "...)");
                    MessageUtil.sendMessage(player, "  §7Max Bid: §e" + defaultCurrency.format(autoBid.getMaxAmount()) +
                        " §7| Increment: §e" + defaultCurrency.format(autoBid.getIncrement()));
                    MessageUtil.sendMessage(player, "  §7Current Bid: §e" + defaultCurrency.format(auction.getCurrentBid()) +
                        " §7| Time Left: §e" + getTimeRemaining(auction));
                    MessageUtil.sendMessage(player, "");
                }
            }
            
            MessageUtil.sendMessage(player, "§7Use §e/auction autocancel <id> §7to cancel auto-bidding.");
            return 1;
            
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(player, "An error occurred while listing auto-bids: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Inner class to represent an auto-bid
     * This would typically be defined in the AuctionHouse or as a separate class
     */
    private static class AutoBid {
        private final UUID auctionId;
        private final UUID playerId;
        private final double maxAmount;
        private final double increment;
        
        public AutoBid(UUID auctionId, UUID playerId, double maxAmount, double increment) {
            this.auctionId = auctionId;
            this.playerId = playerId;
            this.maxAmount = maxAmount;
            this.increment = increment;
        }
        
        public UUID getAuctionId() { return auctionId; }
        public UUID getPlayerId() { return playerId; }
        public double getMaxAmount() { return maxAmount; }
        public double getIncrement() { return increment; }
    }
    
    /**
     * Formats the time remaining for an auction
     */
    private String formatTimeRemaining(long endTime) {
        long timeRemaining = endTime - System.currentTimeMillis();
        
        if (timeRemaining <= 0) {
            return "Ended";
        }
        
        int seconds = (int) (timeRemaining / 1000);
        return formatDuration(seconds);
    }
    
    /**
     * Formats a duration in seconds to a human-readable string
     */
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
    
    /**
     * Get user-friendly display name for auction type
     */
    private String getAuctionTypeDisplayName(Auction.AuctionType type) {
        return switch (type) {
            case STANDARD -> "Standard";
            case BUY_IT_NOW -> "Buy It Now";
            case RESERVE -> "Reserve";
            case DUTCH -> "Dutch";
            default -> type.name(); // Fallback to enum name
        };
    }
}
