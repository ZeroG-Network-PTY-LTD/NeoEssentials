package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Main NeoEssentials command with subcommands for help, version, and reload.
 * Provides core functionality and information about the mod.
 */
public class NeoEssentialsCommands {
    
    /**
     * Registers the main /neoessentials command with subcommands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        dispatcher.register(
            Commands.literal("neoessentials")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.main"))
                .executes(context -> {
                    // Show main help when no subcommand is provided
                    return showMainHelp(context.getSource());
                })
                
                // /neoessentials help
                .then(Commands.literal("help")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.help"))
                    .executes(context -> {
                        return showDetailedHelp(context.getSource());
                    })
                )
                
                // /neoessentials version
                .then(Commands.literal("version")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.version"))
                    .executes(context -> {
                        return showVersion(context.getSource());
                    })
                )
                
                // /neoessentials reload
                .then(Commands.literal("reload")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.reload"))
                    .executes(context -> {
                        return reloadConfiguration(context.getSource());
                    })
                )
                
                // /neoessentials info
                .then(Commands.literal("info")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.info"))
                    .executes(context -> {
                        return showSystemInfo(context.getSource());
                    })
                )
        );
    }
    
    /**
     * Shows the main help message with basic information
     */
    private int showMainHelp(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7v1.0.2.75");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§7Welcome to NeoEssentials! Here are the main command categories:");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§a§l💰 Economy Commands:");
                MessageUtil.sendMessage(player, "§7  • §e/balance §7- Check your balance");
                MessageUtil.sendMessage(player, "§7  • §e/pay <player> <amount> §7- Send money");
                MessageUtil.sendMessage(player, "§7  • §e/bank §7- Banking operations");
                MessageUtil.sendMessage(player, "§7  • §e/loan §7- Loan management");
                MessageUtil.sendMessage(player, "§7  • §e/shop §7- Shop system");
                MessageUtil.sendMessage(player, "§7  • §e/auction §7- Auction house");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§b§l🏠 Utility Commands:");
                MessageUtil.sendMessage(player, "§7  • §e/home §7- Home management");
                MessageUtil.sendMessage(player, "§7  • §e/warp §7- Server warps");
                MessageUtil.sendMessage(player, "§7  • §e/tpa §7- Teleport requests");
                MessageUtil.sendMessage(player, "§7  • §e/kit §7- Available kits");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§c§l📋 Main Commands:");
                MessageUtil.sendMessage(player, "§7  • §e/neoessentials help §7- Detailed help");
                MessageUtil.sendMessage(player, "§7  • §e/neoessentials version §7- Mod version");
                MessageUtil.sendMessage(player, "§7  • §e/neoessentials info §7- System information");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials v1.0.2.75 - Use /neoessentials help for detailed information"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Shows detailed help information
     */
    private int showDetailedHelp(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7Detailed Help");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§a§lEconomy System:");
                MessageUtil.sendMessage(player, "§7  The economy system includes wallet, banking, loans, shops, and auctions.");
                MessageUtil.sendMessage(player, "§7  All transactions use real-world time for daily/monthly calculations.");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§b§lBanking Features:");
                MessageUtil.sendMessage(player, "§7  • Multiple account types (Checking, Savings, Business, Investment)");
                MessageUtil.sendMessage(player, "§7  • Monthly withdrawal limits enforced in real-time");
                MessageUtil.sendMessage(player, "§7  • Interest calculations and automatic processing");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§c§lLoan System:");
                MessageUtil.sendMessage(player, "§7  • Personal, Business, and Mortgage loans available");
                MessageUtil.sendMessage(player, "§7  • Credit scoring system with automatic approval");
                MessageUtil.sendMessage(player, "§7  • Payment scheduling and late fee management");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§e§lShop & Auction:");
                MessageUtil.sendMessage(player, "§7  • Create player-owned shops with GUI management");
                MessageUtil.sendMessage(player, "§7  • Auction house with bidding and buyout options");
                MessageUtil.sendMessage(player, "§7  • Dynamic pricing and market analytics");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§7For specific command help, type the command followed by §ehelp§7.");
                MessageUtil.sendMessage(player, "§7Example: §e/bank help §7or §e/loan help");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials Detailed Help - Check console or use in-game for formatted output"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Shows version information
     */
    private int showVersion(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            // Get version info from the mod
            String version = "1.0.2.75";
            String mcVersion = "1.21.1";
            String neoVersion = "21.1.169+";
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7Version Information");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§a§lMod Information:");
                MessageUtil.sendMessage(player, "§7  • §eNeoEssentials Version: §a" + version);
                MessageUtil.sendMessage(player, "§7  • §eMinecraft Version: §a" + mcVersion);
                MessageUtil.sendMessage(player, "§7  • §eNeoForge Version: §a" + neoVersion);
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§b§lFeature Status:");
                MessageUtil.sendMessage(player, "§7  • §aEconomy System: §2✓ Complete");
                MessageUtil.sendMessage(player, "§7  • §aBanking System: §2✓ Complete");
                MessageUtil.sendMessage(player, "§7  • §aLoan System: §2✓ Complete");
                MessageUtil.sendMessage(player, "§7  • §aShop System: §2✓ Complete");
                MessageUtil.sendMessage(player, "§7  • §aAuction House: §2✓ Complete");
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§c§lBuild Information:");
                MessageUtil.sendMessage(player, "§7  • §eBuild Date: §aJuly 1, 2025");
                MessageUtil.sendMessage(player, "§7  • §eBuild Status: §aProduction Ready");
                MessageUtil.sendMessage(player, "§7  • §eDatabase: §aMySQL/PostgreSQL Support");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials v" + version + " for Minecraft " + mcVersion + " (NeoForge " + neoVersion + ")"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reloads the configuration
     */
    private int reloadConfiguration(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            // Perform the reload
            boolean success = true;
            StringBuilder reloadResults = new StringBuilder();
            
            try {
                // Reload economy configuration
                com.zerog.neoessentials.economy.EconomyManager economyManager = 
                    com.zerog.neoessentials.economy.EconomyManager.getInstance();
                economyManager.reloadConfiguration();
                reloadResults.append("§a✓ Economy configuration reloaded\n");
            } catch (Exception e) {
                reloadResults.append("§c✗ Economy configuration failed: ").append(e.getMessage()).append("\n");
                success = false;
            }
            
            try {
                // Reload bank configuration
                com.zerog.neoessentials.economy.BankManager bankManager = 
                    com.zerog.neoessentials.economy.EconomyManager.getInstance().getBankManager();
                bankManager.reloadConfiguration();
                reloadResults.append("§a✓ Banking configuration reloaded\n");
            } catch (Exception e) {
                reloadResults.append("§c✗ Banking configuration failed: ").append(e.getMessage()).append("\n");
                success = false;
            }
            
            try {
                // Reload shop configuration
                com.zerog.neoessentials.economy.ShopManager shopManager = 
                    com.zerog.neoessentials.economy.EconomyManager.getInstance().getShopManager();
                shopManager.reloadConfiguration();
                reloadResults.append("§a✓ Shop configuration reloaded\n");
            } catch (Exception e) {
                reloadResults.append("§c✗ Shop configuration failed: ").append(e.getMessage()).append("\n");
                success = false;
            }
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7Configuration Reload");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                String[] lines = reloadResults.toString().split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        MessageUtil.sendMessage(player, "§7" + line);
                    }
                }
                
                MessageUtil.sendMessage(player, "");
                if (success) {
                    MessageUtil.sendMessage(player, "§a§l✓ Configuration reload completed successfully!");
                } else {
                    MessageUtil.sendMessage(player, "§c§l⚠ Configuration reload completed with errors!");
                    MessageUtil.sendMessage(player, "§7Check the console for detailed error information.");
                }
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                if (success) {
                    source.sendSuccess(() -> Component.literal("NeoEssentials configuration reloaded successfully"), true);
                } else {
                    source.sendFailure(Component.literal("NeoEssentials configuration reload completed with errors - check console"));
                }
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Configuration reload failed: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Shows system information
     */
    private int showSystemInfo(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7System Information");
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                try {
                    // Get economy system status
                    com.zerog.neoessentials.economy.EconomyManager economyManager = 
                        com.zerog.neoessentials.economy.EconomyManager.getInstance();
                    
                    MessageUtil.sendMessage(player, "§a§lEconomy System Status:");
                    MessageUtil.sendMessage(player, "§7  • §eEconomy Manager: §a" + (economyManager != null ? "Active" : "Inactive"));
                    MessageUtil.sendMessage(player, "§7  • §eDatabase: §a" + (economyManager.getPersistenceManager() != null ? "Connected" : "Disconnected"));
                    MessageUtil.sendMessage(player, "§7  • §eCurrency System: §aOperational");
                    MessageUtil.sendMessage(player, "");
                    
                    // Get banking system status
                    com.zerog.neoessentials.economy.BankManager bankManager = economyManager.getBankManager();
                    MessageUtil.sendMessage(player, "§b§lBanking System Status:");
                    MessageUtil.sendMessage(player, "§7  • §eBank Manager: §a" + (bankManager != null ? "Active" : "Inactive"));
                    MessageUtil.sendMessage(player, "§7  • §eTotal Accounts: §a" + bankManager.getAllAccounts().size());
                    MessageUtil.sendMessage(player, "§7  • §eActive Loans: §a" + bankManager.getAllActiveLoans().size());
                    MessageUtil.sendMessage(player, "");
                    
                    // Get shop system status
                    com.zerog.neoessentials.economy.ShopManager shopManager = economyManager.getShopManager();
                    MessageUtil.sendMessage(player, "§c§lShop System Status:");
                    MessageUtil.sendMessage(player, "§7  • §eShop Manager: §a" + (shopManager != null ? "Active" : "Inactive"));
                    MessageUtil.sendMessage(player, "§7  • §eTotal Shops: §a" + shopManager.getAllShops().size());
                    MessageUtil.sendMessage(player, "§7  • §eAuction House: §a" + (shopManager.getAuctionHouse() != null ? "Active" : "Inactive"));
                    
                } catch (Exception e) {
                    MessageUtil.sendMessage(player, "§c§lError retrieving system status:");
                    MessageUtil.sendMessage(player, "§7  • §cSome systems may not be initialized yet");
                    MessageUtil.sendMessage(player, "§7  • §cError: " + e.getMessage());
                }
                
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§e§lServer Information:");
                MessageUtil.sendMessage(player, "§7  • §eUptime: §a" + getServerUptimeString());
                MessageUtil.sendMessage(player, "§7  • §eJava Version: §a" + System.getProperty("java.version"));
                MessageUtil.sendMessage(player, "§7  • §eMemory Usage: §a" + getMemoryUsageString());
                MessageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials System Information - Use in-game for detailed output"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Gets formatted server uptime string
     */
    private String getServerUptimeString() {
        try {
            long uptimeMs = System.currentTimeMillis() - 
                java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
            long hours = uptimeMs / (1000 * 60 * 60);
            long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
            return hours + "h " + minutes + "m";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Gets formatted memory usage string
     */
    private String getMemoryUsageString() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
            long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
            long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
            long usedMemory = totalMemory - freeMemory;
            
            return usedMemory + "/" + maxMemory + " MB (" + 
                   (usedMemory * 100 / maxMemory) + "%)";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
