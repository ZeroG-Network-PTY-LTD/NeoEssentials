package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.EconomyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Transaction history command for viewing economy transaction logs
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TransactionHistoryCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("transactions")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.PLAYER_DEFAULT)) // All players can view their own
            .executes(context -> showTransactionHistory(context, null, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(context -> showTransactionHistory(context, null, IntegerArgumentType.getInteger(context, "page")))
            )
            .then(Commands.literal("player")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // Admins can view others
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> showTransactionHistory(context, EntityArgument.getPlayer(context, "target"), 1))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> showTransactionHistory(context, EntityArgument.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "page")))
                    )
                )
            )
        );
        
        // Alias commands
        dispatcher.register(Commands.literal("txhistory")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.PLAYER_DEFAULT))
            .executes(context -> showTransactionHistory(context, null, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(context -> showTransactionHistory(context, null, IntegerArgumentType.getInteger(context, "page")))
            )
        );
    }
    
    /**
     * Show transaction history for a player
     */
    private static int showTransactionHistory(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, int page) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Determine whose transactions to show
        UUID playerId;
        String playerName;
        
        if (targetPlayer != null) {
            // Admin viewing another player's transactions
            playerId = targetPlayer.getUUID();
            playerName = targetPlayer.getName().getString();
        } else {
            // Player viewing their own transactions
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("§cThis command can only be used by players!"));
                return 0;
            }
            playerId = player.getUUID();
            playerName = player.getName().getString();
        }
        
        // Get transaction manager
        EconomyManager economyManager = EconomyManager.getInstance();
        
        // Get transaction history (limit to 50 for performance)
        List<EconomyManager.Transaction> transactions = economyManager.getTransactionHistory(playerId, 50);
        
        if (transactions.isEmpty()) {
            source.sendFailure(Component.literal("§7No transaction history found for " + playerName));
            return 1;
        }
        
        // Pagination
        int transactionsPerPage = 10;
        int totalPages = (transactions.size() + transactionsPerPage - 1) / transactionsPerPage;
        page = Math.max(1, Math.min(page, totalPages));
        
        int startIndex = (page - 1) * transactionsPerPage;
        int endIndex = Math.min(startIndex + transactionsPerPage, transactions.size());
        
        // Display header
        source.sendFailure(Component.literal("§6=== Transaction History: " + playerName + " ==="));
        source.sendFailure(Component.literal("§7Page " + page + " of " + totalPages + " (" + transactions.size() + " transactions)"));
        source.sendFailure(Component.literal(""));
        
        // Display transactions
        for (int i = startIndex; i < endIndex; i++) {
            EconomyManager.Transaction transaction = transactions.get(i);
            String amount = economyManager.formatCurrency(transaction.amount.abs());
            String type = transaction.type.toString().replace("_", " ");
            String reason = transaction.reason != null ? transaction.reason : "No reason";
            
            // Color code based on transaction type
            String color = transaction.amount.compareTo(java.math.BigDecimal.ZERO) >= 0 ? "§a+" : "§c-";
            
            source.sendFailure(Component.literal(String.format("%s%s §7%s - %s", 
                color, amount, type, reason)));
        }
        
        // Navigation footer
        source.sendFailure(Component.literal(""));
        if (page < totalPages) {
            source.sendFailure(Component.literal("§7Use §a/transactions " + (page + 1) + " §7for next page"));
        }
        if (page > 1) {
            source.sendFailure(Component.literal("§7Use §a/transactions " + (page - 1) + " §7for previous page"));
        }
        
        return 1;
    }
}
