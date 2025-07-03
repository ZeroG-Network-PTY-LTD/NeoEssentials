package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced auction commands system for NeoEssentials.
 * Provides a comprehensive auction house interface with improved UX.
 */
public class AuctionCommandsNew {
    
    private final AuctionManagerNew auctionManager;
    
    public AuctionCommandsNew() {
        this.auctionManager = AuctionManagerNew.getInstance();
    }
    
    /**
     * Registers all auction commands with the command dispatcher.
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Main /auction command with comprehensive subcommands
        dispatcher.register(
            Commands.literal("auction")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction"))
                .executes(this::showMainMenu)
                
                // /auction create <type> <starting-price> [duration] [buyout-price]
                .then(Commands.literal("create")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.create"))
                    .then(Commands.argument("type", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_TYPE_SUGGESTIONS)
                        .then(Commands.argument("starting_price", DoubleArgumentType.doubleArg(0.01))
                            .executes(this::createAuctionBasic)
                            .then(Commands.argument("duration_minutes", IntegerArgumentType.integer(5, 10080))
                                .executes(this::createAuctionWithDuration)
                                .then(Commands.argument("buyout_price", DoubleArgumentType.doubleArg(0.01))
                                    .executes(this::createAuctionWithBuyout)
                                )
                            )
                        )
                    )
                )
                
                // /auction list [category|ending|featured|mine]
                .then(Commands.literal("list")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.list"))
                    .executes(this::listActiveAuctions)
                    .then(Commands.argument("filter", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_LIST_FILTER_SUGGESTIONS)
                        .executes(this::listFilteredAuctions)
                    )
                )
                
                // /auction info <auction-id>
                .then(Commands.literal("info")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.info"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(this::showAuctionInfo)
                    )
                )
                
                // /auction bid <auction-id> <amount>
                .then(Commands.literal("bid")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.bid"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(this::placeBid)
                        )
                    )
                )
                
                // /auction buyout <auction-id>
                .then(Commands.literal("buyout")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.buyout"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(this::buyoutAuction)
                    )
                )
                
                // /auction cancel <auction-id>
                .then(Commands.literal("cancel")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.cancel"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(this::cancelAuction)
                    )
                )
                
                // /auction search <item-name>
                .then(Commands.literal("search")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.search"))
                    .then(Commands.argument("search_term", StringArgumentType.greedyString())
                        .executes(this::searchAuctions)
                    )
                )
                
                // /auction watch <auction-id>
                .then(Commands.literal("watch")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.watch"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(this::watchAuction)
                    )
                )
                
                // /auction unwatch <auction-id>
                .then(Commands.literal("unwatch")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.watch"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(this::unwatchAuction)
                    )
                )
                
                // /auction autobid <auction-id> <max-amount> [increment]
                .then(Commands.literal("autobid")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.autobid"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .then(Commands.argument("max_amount", DoubleArgumentType.doubleArg(0.01))
                            .executes(this::setAutoBidBasic)
                            .then(Commands.argument("increment", DoubleArgumentType.doubleArg(0.01))
                                .executes(this::setAutoBidWithIncrement)
                            )
                        )
                    )
                )
                
                // /auction autocancel <auction-id>
                .then(Commands.literal("autocancel")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.autobid"))
                    .then(Commands.argument("auction_id", StringArgumentType.string())
                        .suggests(TabCompletionUtil.AUCTION_ID_SUGGESTIONS)
                        .executes(this::cancelAutoBid)
                    )
                )
                
                // /auction autolist
                .then(Commands.literal("autolist")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.autobid"))
                    .executes(this::listAutoBids)
                )
                
                // /auction history [player]
                .then(Commands.literal("history")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.history"))
                    .executes(this::showMyHistory)
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.history.others"))
                        .executes(this::showPlayerHistory)
                    )
                )
                
                // /auction stats
                .then(Commands.literal("stats")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.stats"))
                    .executes(this::showStatistics)
                )
                
                // /auction categories
                .then(Commands.literal("categories")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.list"))
                    .executes(this::showCategories)
                )
                
                // Admin commands
                .then(Commands.literal("admin")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction.admin"))
                    .then(Commands.literal("reload")
                        .executes(this::reloadAuctionSystem)
                    )
                    .then(Commands.literal("purge")
                        .then(Commands.literal("expired")
                            .executes(this::purgeExpiredAuctions)
                        )
                    )
                    .then(Commands.literal("force-end")
                        .then(Commands.argument("auction_id", StringArgumentType.string())
                            .executes(this::forceEndAuction)
                        )
                    )
                )
        );
        
        // Add convenient aliases
        dispatcher.register(
            Commands.literal("auc")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction"))
                .redirect(dispatcher.getRoot().getChild("auction"))
        );
        
        dispatcher.register(
            Commands.literal("ah")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.auction"))
                .redirect(dispatcher.getRoot().getChild("auction"))
        );
    }
    
    // Command implementations
    
    private int showMainMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        MutableComponent message = Component.literal("")
            .append(Component.literal("§6§l═══════ AUCTION HOUSE ═══════§r\n"))
            .append(Component.literal("§7Welcome to the NeoEssentials Auction House!\n\n"))
            .append(createClickableCommand("§a▶ Create Auction", "/auction create standard", 
                "Start selling your items"))
            .append(Component.literal("\n"))
            .append(createClickableCommand("§b▶ Browse Auctions", "/auction list", 
                "View all active auctions"))
            .append(Component.literal("\n"))
            .append(createClickableCommand("§e▶ Search Items", "/auction search ", 
                "Search for specific items"))
            .append(Component.literal("\n"))
            .append(createClickableCommand("§d▶ My Auctions", "/auction list mine", 
                "View your active auctions"))
            .append(Component.literal("\n"))
            .append(createClickableCommand("§c▶ Ending Soon", "/auction list ending", 
                "View auctions ending soon"))
            .append(Component.literal("\n\n"))
            .append(Component.literal("§7Use §f/auction help§7 for detailed command information"));
        
        player.sendSystemMessage(message);
        return 1;
    }
    
    private int createAuctionBasic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String typeStr = StringArgumentType.getString(context, "type");
        double startingPrice = DoubleArgumentType.getDouble(context, "starting_price");
        
        return createAuctionInternal(player, typeStr, startingPrice, 1440, 0.0); // 24 hours default
    }
    
    private int createAuctionWithDuration(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String typeStr = StringArgumentType.getString(context, "type");
        double startingPrice = DoubleArgumentType.getDouble(context, "starting_price");
        int duration = IntegerArgumentType.getInteger(context, "duration_minutes");
        
        return createAuctionInternal(player, typeStr, startingPrice, duration, 0.0);
    }
    
    private int createAuctionWithBuyout(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String typeStr = StringArgumentType.getString(context, "type");
        double startingPrice = DoubleArgumentType.getDouble(context, "starting_price");
        int duration = IntegerArgumentType.getInteger(context, "duration_minutes");
        double buyoutPrice = DoubleArgumentType.getDouble(context, "buyout_price");
        
        return createAuctionInternal(player, typeStr, startingPrice, duration, buyoutPrice);
    }
    
    private int createAuctionInternal(ServerPlayer player, String typeStr, double startingPrice, 
                                    int duration, double buyoutPrice) {
        
        // Get held item
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cYou must hold an item to auction!"));
            return 0;
        }
        
        // Parse auction type
        AuctionNew.AuctionType auctionType;
        try {
            auctionType = parseAuctionType(typeStr);
        } catch (IllegalArgumentException e) {
            player.sendSystemMessage(Component.literal("§cInvalid auction type: " + typeStr));
            player.sendSystemMessage(Component.literal("§7Valid types: standard, buyitnow, reserve"));
            return 0;
        }
        
        // Determine category based on item
        String category = ItemHandler.getItemCategory(ItemHandler.getItemId(heldItem.getItem()));
        
        // Create the auction
        AuctionManagerNew.CreateAuctionResult result = auctionManager.createAuction(
            player, heldItem, startingPrice, duration, auctionType, category);
        
        if (result.isSuccess()) {
            AuctionNew auction = result.getAuction();
            
            // Set buyout price if specified
            if (buyoutPrice > 0) {
                auction.setBuyoutPrice(buyoutPrice);
            }
            
            MutableComponent successMessage = Component.literal("")
                .append(Component.literal("§a✓ Auction created successfully!\n"))
                .append(Component.literal("§7ID: §e" + auction.getAuctionId().toString().substring(0, 8) + "\n"))
                .append(Component.literal("§7Item: §f" + auction.getItemDisplayName() + "\n"))
                .append(Component.literal("§7Starting Bid: §e" + auction.getCurrency().format(startingPrice) + "\n"))
                .append(Component.literal("§7Duration: §e" + duration + " minutes\n"));
            
            if (buyoutPrice > 0) {
                successMessage.append(Component.literal("§7Buyout Price: §e" + 
                    auction.getCurrency().format(buyoutPrice) + "\n"));
            }
            
            successMessage.append(Component.literal("\n"))
                .append(createClickableCommand("§b▶ View Auction", 
                    "/auction info " + auction.getAuctionId().toString().substring(0, 8), 
                    "Click to view auction details"));
            
            player.sendSystemMessage(successMessage);
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to create auction: " + result.getMessage()));
            return 0;
        }
    }
    
    private int listActiveAuctions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return listAuctionsInternal(player, "all", 1);
    }
    
    private int listFilteredAuctions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String filter = StringArgumentType.getString(context, "filter");
        return listAuctionsInternal(player, filter, 1);
    }
    
    private int listAuctionsInternal(ServerPlayer player, String filter, int page) {
        List<AuctionNew> auctions;
        String title;
        
        switch (filter.toLowerCase()) {
            case "mine":
                auctions = auctionManager.getActiveAuctionsByPlayer(player.getUUID());
                title = "Your Active Auctions";
                break;
            case "ending":
                auctions = auctionManager.getAuctionsEndingSoon(60); // Within 1 hour
                title = "Auctions Ending Soon";
                break;
            case "featured":
                auctions = auctionManager.getActiveAuctions().stream()
                    .filter(AuctionNew::isFeatured)
                    .collect(Collectors.toList());
                title = "Featured Auctions";
                break;
            default:
                if (auctionManager.getConfig().allowedCategories.contains(filter.toLowerCase())) {
                    auctions = auctionManager.getActiveAuctionsByCategory(filter.toLowerCase());
                    title = "Auctions in " + filter.substring(0, 1).toUpperCase() + filter.substring(1);
                } else {
                    auctions = auctionManager.getActiveAuctions();
                    title = "All Active Auctions";
                }
                break;
        }
        
        if (auctions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No auctions found for filter: " + filter));
            return 1;
        }
        
        // Sort by ending time
        auctions.sort((a, b) -> Long.compare(a.getTimeRemaining(), b.getTimeRemaining()));
        
        // Pagination
        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double) auctions.size() / itemsPerPage);
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, auctions.size());
        
        MutableComponent message = Component.literal("")
            .append(Component.literal("§6§l═══ " + title.toUpperCase() + " ═══\n"))
            .append(Component.literal("§7Page " + page + " of " + totalPages + " (" + auctions.size() + " total)\n\n"));
        
        for (int i = startIndex; i < endIndex; i++) {
            AuctionNew auction = auctions.get(i);
            message.append(formatAuctionListEntry(auction, i + 1));
        }
        
        // Add pagination controls
        if (totalPages > 1) {
            message.append(Component.literal("\n§7Navigate: "));
            if (page > 1) {
                message.append(createClickableCommand("§a◀ Previous", 
                    "/auction list " + filter + " " + (page - 1), "Previous page"));
            }
            if (page < totalPages) {
                if (page > 1) message.append(Component.literal(" §7| "));
                message.append(createClickableCommand("§a▶ Next", 
                    "/auction list " + filter + " " + (page + 1), "Next page"));
            }
        }
        
        player.sendSystemMessage(message);
        return 1;
    }
    
    private int showAuctionInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        
        // Find auction by partial ID
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        AuctionNew auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        // Increment view count
        auction.incrementViewCount();
        
        MutableComponent message = Component.literal("")
            .append(Component.literal("§6§l═══════ AUCTION DETAILS ═══════\n"))
            .append(Component.literal("§7ID: §e" + auction.getAuctionId().toString().substring(0, 8) + "\n"))
            .append(Component.literal("§7Item: §f" + auction.getItemDisplayName() + " §7x" + auction.getQuantity() + "\n"))
            .append(Component.literal("§7Seller: §a" + auction.getSellerName() + "\n"))
            .append(Component.literal("§7Type: §b" + auction.getAuctionType().getDisplayName() + "\n"))
            .append(Component.literal("§7Category: §d" + auction.getCategory() + "\n"))
            .append(Component.literal("§7Status: " + auction.getStatus().getFormattedName() + "\n\n"))
            
            .append(Component.literal("§6Pricing Information:\n"))
            .append(Component.literal("§7Starting Bid: §e" + auction.getCurrency().format(auction.getStartingBid()) + "\n"))
            .append(Component.literal("§7Current Bid: §e" + auction.getCurrency().format(auction.getCurrentBid()) + "\n"));
        
        if (auction.getCurrentBidderName() != null) {
            message.append(Component.literal("§7Current Bidder: §a" + auction.getCurrentBidderName() + "\n"));
        }
        
        if (auction.getBuyoutPrice() > 0) {
            message.append(Component.literal("§7Buyout Price: §e" + auction.getCurrency().format(auction.getBuyoutPrice()) + "\n"));
        }
        
        if (auction.getReservePrice() > 0) {
            message.append(Component.literal("§7Reserve Price: §e" + auction.getCurrency().format(auction.getReservePrice()) + "\n"));
        }
        
        message.append(Component.literal("\n§6Time Information:\n"))
            .append(Component.literal("§7Time Remaining: §e" + auction.getFormattedTimeRemaining() + "\n"))
            .append(Component.literal("§7Views: §7" + auction.getViewCount() + "\n"));
        
        if (!auction.getDescription().isEmpty()) {
            message.append(Component.literal("\n§6Description:\n§7" + auction.getDescription() + "\n"));
        }
        
        // Add action buttons
        if (auction.isActive() && !auction.getSellerId().equals(player.getUUID())) {
            message.append(Component.literal("\n§6Actions:\n"));
            
            double nextBid = auction.getCurrentBid() + Math.max(1.0, auction.getCurrentBid() * 0.01);
            message.append(createClickableCommand("§a▶ Bid " + auction.getCurrency().format(nextBid), 
                "/auction bid " + auction.getAuctionId().toString().substring(0, 8) + " " + nextBid, 
                "Place a bid"));
            
            if (auction.getBuyoutPrice() > 0) {
                message.append(Component.literal(" "))
                    .append(createClickableCommand("§c▶ Buyout", 
                        "/auction buyout " + auction.getAuctionId().toString().substring(0, 8), 
                        "Buy immediately"));
            }
            
            message.append(Component.literal("\n"))
                .append(createClickableCommand("§e▶ Watch", 
                    "/auction watch " + auction.getAuctionId().toString().substring(0, 8), 
                    "Get notifications about this auction"));
        }
        
        player.sendSystemMessage(message);
        return 1;
    }
    
    private int placeBid(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        double bidAmount = DoubleArgumentType.getDouble(context, "amount");
        
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        AuctionNew.BidResult result = auctionManager.placeBid(
            player.getUUID(), player.getName().getString(), auctionId, bidAmount);
        
        if (result.isSuccess()) {
            player.sendSystemMessage(Component.literal("§a✓ Bid placed successfully!"));
            player.sendSystemMessage(Component.literal("§7Your bid: §e" + 
                CurrencyManager.getInstance().getDefaultCurrency().format(bidAmount)));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to place bid: " + result.getMessage()));
            return 0;
        }
    }
    
    private int buyoutAuction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        AuctionNew.BuyoutResult result = auctionManager.buyoutAuction(
            player.getUUID(), player.getName().getString(), auctionId);
        
        if (result.isSuccess()) {
            player.sendSystemMessage(Component.literal("§a✓ Auction bought out successfully!"));
            player.sendSystemMessage(Component.literal("§7The item will be delivered to your account."));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to buyout auction: " + result.getMessage()));
            return 0;
        }
    }
    
    private int cancelAuction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        if (auctionManager.cancelAuction(player.getUUID(), auctionId)) {
            player.sendSystemMessage(Component.literal("§a✓ Auction cancelled successfully!"));
            player.sendSystemMessage(Component.literal("§7Your item will be returned to your account."));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to cancel auction. You may not own this auction or it may have ended."));
            return 0;
        }
    }
    
    private int searchAuctions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String searchTerm = StringArgumentType.getString(context, "search_term");
        
        List<AuctionNew> results = auctionManager.searchAuctions(searchTerm);
        
        if (results.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7No auctions found matching: §e" + searchTerm));
            return 1;
        }
        
        MutableComponent message = Component.literal("")
            .append(Component.literal("§6§l═══ SEARCH RESULTS ═══\n"))
            .append(Component.literal("§7Found " + results.size() + " auctions matching: §e" + searchTerm + "\n\n"));
        
        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            AuctionNew auction = results.get(i);
            message.append(formatAuctionListEntry(auction, i + 1));
        }
        
        if (results.size() > 10) {
            message.append(Component.literal("\n§7... and " + (results.size() - 10) + " more results"));
        }
        
        player.sendSystemMessage(message);
        return 1;
    }
    
    private int watchAuction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        if (auctionManager.addWatcher(auctionId, player.getName().getString())) {
            player.sendSystemMessage(Component.literal("§a✓ Now watching auction " + auctionIdStr));
            player.sendSystemMessage(Component.literal("§7You'll receive notifications about bid updates."));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to watch auction"));
            return 0;
        }
    }
    
    private int unwatchAuction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        if (auctionManager.removeWatcher(auctionId, player.getName().getString())) {
            player.sendSystemMessage(Component.literal("§a✓ No longer watching auction " + auctionIdStr));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to unwatch auction"));
            return 0;
        }
    }
    
    private int setAutoBidBasic(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        double maxAmount = DoubleArgumentType.getDouble(context, "max_amount");
        
        return setAutoBidInternal(player, auctionIdStr, maxAmount, 1.0);
    }
    
    private int setAutoBidWithIncrement(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        double maxAmount = DoubleArgumentType.getDouble(context, "max_amount");
        double increment = DoubleArgumentType.getDouble(context, "increment");
        
        return setAutoBidInternal(player, auctionIdStr, maxAmount, increment);
    }
    
    private int setAutoBidInternal(ServerPlayer player, String auctionIdStr, double maxAmount, double increment) {
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        if (auctionManager.setAutoBid(player.getUUID(), auctionId, maxAmount, increment)) {
            player.sendSystemMessage(Component.literal("§a✓ Auto-bid configured successfully!"));
            player.sendSystemMessage(Component.literal("§7Max Amount: §e" + 
                CurrencyManager.getInstance().getDefaultCurrency().format(maxAmount)));
            player.sendSystemMessage(Component.literal("§7Increment: §e" + 
                CurrencyManager.getInstance().getDefaultCurrency().format(increment)));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ Failed to set auto-bid"));
            return 0;
        }
    }
    
    private int cancelAutoBid(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String auctionIdStr = StringArgumentType.getString(context, "auction_id");
        
        UUID auctionId = findAuctionByPartialId(auctionIdStr);
        if (auctionId == null) {
            player.sendSystemMessage(Component.literal("§cAuction not found: " + auctionIdStr));
            return 0;
        }
        
        if (auctionManager.removeAutoBid(player.getUUID(), auctionId)) {
            player.sendSystemMessage(Component.literal("§a✓ Auto-bid cancelled"));
            return 1;
        } else {
            player.sendSystemMessage(Component.literal("§c✗ No auto-bid found for this auction"));
            return 0;
        }
    }
    
    private int listAutoBids(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // This would need to be implemented in AuctionManagerNew
        player.sendSystemMessage(Component.literal("§eAuto-bid listing feature coming soon!"));
        return 1;
    }
    
    private int showMyHistory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        // Implementation would show player's auction history
        player.sendSystemMessage(Component.literal("§eAuction history feature coming soon!"));
        return 1;
    }
    
    private int showPlayerHistory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        // Implementation would show specified player's auction history
        player.sendSystemMessage(Component.literal("§ePlayer auction history feature coming soon!"));
        return 1;
    }
    
    private int showStatistics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        AuctionManagerNew.AuctionStatistics stats = auctionManager.getStatistics();
        
        MutableComponent message = Component.literal("")
            .append(Component.literal("§6§l═══ AUCTION HOUSE STATISTICS ═══\n"))
            .append(Component.literal("§7Total Auctions Created: §e" + stats.getTotalAuctionsCreated() + "\n"))
            .append(Component.literal("§7Total Auctions Completed: §e" + stats.getTotalAuctionsCompleted() + "\n"))
            .append(Component.literal("§7Total Auctions Cancelled: §e" + stats.getTotalAuctionsCancelled() + "\n"))
            .append(Component.literal("§7Total Volume: §e" + 
                CurrencyManager.getInstance().getDefaultCurrency().format(stats.getTotalVolume()) + "\n"))
            .append(Component.literal("§7Total Fees Collected: §e" + 
                CurrencyManager.getInstance().getDefaultCurrency().format(stats.getTotalFees()) + "\n\n"))
            .append(Component.literal("§7Active Auctions: §e" + auctionManager.getActiveAuctions().size()));
        
        player.sendSystemMessage(message);
        return 1;
    }
    
    private int showCategories(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        MutableComponent message = Component.literal("")
            .append(Component.literal("§6§l═══ AUCTION CATEGORIES ═══\n"));
        
        for (String category : auctionManager.getConfig().allowedCategories) {
            int count = auctionManager.getActiveAuctionsByCategory(category).size();
            message.append(createClickableCommand("§b▶ " + category.substring(0, 1).toUpperCase() + 
                category.substring(1) + " §7(" + count + ")", 
                "/auction list " + category, 
                "View auctions in " + category))
                .append(Component.literal("\n"));
        }
        
        player.sendSystemMessage(message);
        return 1;
    }
    
    private int reloadAuctionSystem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Implementation would reload auction configuration
        context.getSource().sendSystemMessage(Component.literal("§aAuction system reload feature coming soon!"));
        return 1;
    }
    
    private int purgeExpiredAuctions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Implementation would clean up old auction data
        context.getSource().sendSystemMessage(Component.literal("§aExpired auction purge feature coming soon!"));
        return 1;
    }
    
    private int forceEndAuction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Implementation would force end an auction (admin only)
        context.getSource().sendSystemMessage(Component.literal("§aForce end auction feature coming soon!"));
        return 1;
    }
    
    // Helper methods
    
    private AuctionNew.AuctionType parseAuctionType(String typeStr) {
        switch (typeStr.toLowerCase()) {
            case "standard":
            case "std":
                return AuctionNew.AuctionType.STANDARD;
            case "buyitnow":
            case "bin":
            case "buynow":
                return AuctionNew.AuctionType.BUY_IT_NOW;
            case "reserve":
            case "res":
                return AuctionNew.AuctionType.RESERVE;
            case "dutch":
                return AuctionNew.AuctionType.DUTCH;
            case "silent":
                return AuctionNew.AuctionType.SILENT;
            default:
                throw new IllegalArgumentException("Unknown auction type: " + typeStr);
        }
    }
    
    private UUID findAuctionByPartialId(String partialId) {
        if (partialId.length() >= 36) {
            try {
                return UUID.fromString(partialId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        // Search by partial ID
        return auctionManager.getActiveAuctions().stream()
            .filter(auction -> auction.getAuctionId().toString().startsWith(partialId))
            .map(AuctionNew::getAuctionId)
            .findFirst()
            .orElse(null);
    }
    
    private MutableComponent formatAuctionListEntry(AuctionNew auction, int index) {
        String shortId = auction.getAuctionId().toString().substring(0, 8);
        String timeRemaining = auction.getFormattedTimeRemaining();
        String currentBid = auction.getCurrency().format(auction.getCurrentBid());
        
        MutableComponent entry = Component.literal("§7" + index + ". ")
            .append(createClickableCommand("§e" + shortId, 
                "/auction info " + shortId, 
                "Click for details"))
            .append(Component.literal(" §f" + auction.getItemDisplayName()))
            .append(Component.literal(" §7- §e" + currentBid))
            .append(Component.literal(" §7(" + timeRemaining + ")"));
        
        if (auction.getBuyoutPrice() > 0) {
            entry.append(Component.literal(" §c[BUYOUT]"));
        }
        
        entry.append(Component.literal("\n"));
        return entry;
    }
    
    private MutableComponent createClickableCommand(String text, String command, String hoverText) {
        return Component.literal(text)
            .withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))
            );
    }
}
