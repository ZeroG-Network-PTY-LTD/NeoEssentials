package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.EconomyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Economy analytics command for viewing economy statistics and trends
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyAnalyticsCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ecoanalytics")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // Admin only
            .executes(context -> showEconomyOverview(context))
            .then(Commands.literal("overview")
                .executes(context -> showEconomyOverview(context)))
            .then(Commands.literal("balances")
                .executes(context -> showBalanceDistribution(context)))
            .then(Commands.literal("transactions")
                .executes(context -> showTransactionStatistics(context)))
            .then(Commands.literal("top")
                .executes(context -> showTopBalances(context)))
        );
        
        // Alias command
        dispatcher.register(Commands.literal("economyanalytics")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .executes(context -> showEconomyOverview(context))
            .then(Commands.literal("overview")
                .executes(context -> showEconomyOverview(context)))
            .then(Commands.literal("balances")
                .executes(context -> showBalanceDistribution(context)))
            .then(Commands.literal("transactions")
                .executes(context -> showTransactionStatistics(context)))
            .then(Commands.literal("top")
                .executes(context -> showTopBalances(context)))
        );
    }
    
    /**
     * Show economy overview with key metrics
     */
    private static int showEconomyOverview(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (!economyManager.isEnabled()) {
            source.sendFailure(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        // Calculate overview statistics
        List<Map.Entry<UUID, BigDecimal>> allBalances = economyManager.getTopBalances(Integer.MAX_VALUE);
        
        BigDecimal totalMoney = allBalances.stream()
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageBalance = allBalances.isEmpty() ? BigDecimal.ZERO : 
            totalMoney.divide(BigDecimal.valueOf(allBalances.size()), 2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal highestBalance = allBalances.isEmpty() ? BigDecimal.ZERO : allBalances.get(0).getValue();
        
        int activeAccounts = (int) allBalances.stream()
            .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
            .count();
        
        // Send overview
        source.sendSuccess(() -> Component.literal("§6§l=== Economy Analytics Overview ==="), false);
        source.sendSuccess(() -> Component.literal("§7Total Money in Circulation: §e" + economyManager.formatCurrency(totalMoney)), false);
        source.sendSuccess(() -> Component.literal("§7Total Accounts: §a" + allBalances.size()), false);
        source.sendSuccess(() -> Component.literal("§7Active Accounts (>0): §a" + activeAccounts), false);
        source.sendSuccess(() -> Component.literal("§7Average Balance: §e" + economyManager.formatCurrency(averageBalance)), false);
        source.sendSuccess(() -> Component.literal("§7Highest Balance: §e" + economyManager.formatCurrency(highestBalance)), false);
        source.sendSuccess(() -> Component.literal("§7Use §f/ecoanalytics balances §7for distribution details"), false);
        
        return 1;
    }
    
    /**
     * Show balance distribution statistics
     */
    private static int showBalanceDistribution(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (!economyManager.isEnabled()) {
            source.sendFailure(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        List<Map.Entry<UUID, BigDecimal>> allBalances = economyManager.getTopBalances(Integer.MAX_VALUE);
        
        if (allBalances.isEmpty()) {
            source.sendFailure(Component.literal("§7No economy data available."));
            return 1;
        }
        
        // Distribution analysis
        AtomicInteger poorPlayers = new AtomicInteger(0);  // < 100
        AtomicInteger middleClass = new AtomicInteger(0);  // 100-1000
        AtomicInteger wealthy = new AtomicInteger(0);      // 1000-10000
        AtomicInteger richPlayers = new AtomicInteger(0);  // > 10000
        
        allBalances.forEach(entry -> {
            BigDecimal balance = entry.getValue();
            if (balance.compareTo(BigDecimal.valueOf(100)) < 0) {
                poorPlayers.incrementAndGet();
            } else if (balance.compareTo(BigDecimal.valueOf(1000)) < 0) {
                middleClass.incrementAndGet();
            } else if (balance.compareTo(BigDecimal.valueOf(10000)) < 0) {
                wealthy.incrementAndGet();
            } else {
                richPlayers.incrementAndGet();
            }
        });
        
        source.sendSuccess(() -> Component.literal("§6§l=== Balance Distribution Analysis ==="), false);
        source.sendSuccess(() -> Component.literal("§c§lPoor (< 100): §f" + poorPlayers.get() + " players"), false);
        source.sendSuccess(() -> Component.literal("§e§lMiddle Class (100-1000): §f" + middleClass.get() + " players"), false);
        source.sendSuccess(() -> Component.literal("§a§lWealthy (1000-10000): §f" + wealthy.get() + " players"), false);
        source.sendSuccess(() -> Component.literal("§2§lRich (> 10000): §f" + richPlayers.get() + " players"), false);
        source.sendSuccess(() -> Component.literal("§7Use §f/ecoanalytics top §7to see top balances"), false);
        
        return 1;
    }
    
    /**
     * Show transaction statistics
     */
    private static int showTransactionStatistics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (!economyManager.isEnabled()) {
            source.sendFailure(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        // Get all players' transaction data
        List<Map.Entry<UUID, BigDecimal>> allBalances = economyManager.getTopBalances(Integer.MAX_VALUE);
        
        AtomicReference<BigDecimal> totalWithdrawals = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> totalDeposits = new AtomicReference<>(BigDecimal.ZERO);
        AtomicInteger totalTransactions = new AtomicInteger(0);
        AtomicInteger shopPurchases = new AtomicInteger(0);
        
        // Analyze transaction data for all players (simplified version)
        allBalances.forEach(entry -> {
            UUID playerId = entry.getKey();
            List<EconomyManager.Transaction> transactions = economyManager.getTransactionHistory(playerId, 100);
            
            transactions.forEach(transaction -> {
                totalTransactions.incrementAndGet();
                
                switch (transaction.type) {
                    case DEPOSIT:
                        totalDeposits.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case WITHDRAWAL:
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case PURCHASE:
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case SALE:
                        totalDeposits.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case SHOP_PURCHASE:
                        shopPurchases.incrementAndGet();
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case SHOP_SALE:
                        totalDeposits.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case TRANSFER_IN:
                        totalDeposits.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case TRANSFER_OUT:
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case EconomyManager.TransactionType.TRANSFER_SENT:
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case EconomyManager.TransactionType.TRANSFER_RECEIVED:
                        totalDeposits.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case ADMIN_GIVE:
                        totalDeposits.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case ADMIN_TAKE:
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                    case FEE:
                    case EconomyManager.TransactionType.COMMAND_COST:
                        totalWithdrawals.updateAndGet(current -> current.add(transaction.amount));
                        break;
                }
            });
        });
        
        source.sendSuccess(() -> Component.literal("§6§l=== Transaction Statistics ==="), false);
        source.sendSuccess(() -> Component.literal("§7Total Transactions: §a" + totalTransactions.get()), false);
        source.sendSuccess(() -> Component.literal("§7Total Deposits: §a" + economyManager.formatCurrency(totalDeposits.get())), false);
        source.sendSuccess(() -> Component.literal("§7Total Withdrawals: §c" + economyManager.formatCurrency(totalWithdrawals.get())), false);
        source.sendSuccess(() -> Component.literal("§7Shop Purchases: §e" + shopPurchases.get()), false);
        
        BigDecimal netFlow = totalDeposits.get().subtract(totalWithdrawals.get());
        String flowColor = netFlow.compareTo(BigDecimal.ZERO) >= 0 ? "§a" : "§c";
        source.sendSuccess(() -> Component.literal("§7Net Money Flow: " + flowColor + economyManager.formatCurrency(netFlow)), false);
        
        return 1;
    }
    
    /**
     * Show top balances
     */
    private static int showTopBalances(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EconomyManager economyManager = EconomyManager.getInstance();
        
        if (!economyManager.isEnabled()) {
            source.sendFailure(Component.literal("§cEconomy system is disabled."));
            return 0;
        }
        
        List<Map.Entry<UUID, BigDecimal>> topBalances = economyManager.getTopBalances(10);
        
        if (topBalances.isEmpty()) {
            source.sendFailure(Component.literal("§7No balance data available."));
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("§6§l=== Top 10 Balances ==="), false);
        
        for (int i = 0; i < topBalances.size(); i++) {
            Map.Entry<UUID, BigDecimal> entry = topBalances.get(i);
            UUID playerId = entry.getKey();
            BigDecimal balance = entry.getValue();
            
            String playerName = "Unknown Player";
            try {
                ServerPlayer player = source.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    playerName = player.getName().getString();
                }
            } catch (Exception e) {
                // Keep default name
            }
            
            String rankColor = switch (i) {
                case 0 -> "§6";  // Gold
                case 1 -> "§7";  // Silver  
                case 2 -> "§c";  // Bronze
                default -> "§f"; // White
            };
            
            final String displayName = playerName;
            final int rank = i + 1;
            source.sendSuccess(() -> Component.literal(String.format("%s#%d. %s: §e%s", 
                rankColor, rank, displayName, economyManager.formatCurrency(balance))), false);
        }
        
        return 1;
    }
}
